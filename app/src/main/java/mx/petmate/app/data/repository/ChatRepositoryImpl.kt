package mx.petmate.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.local.dao.MessageDao
import mx.petmate.app.data.local.entity.MessageEntity
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.ChatApiService
import mx.petmate.app.data.remote.dto.MarkReadRequestDto
import mx.petmate.app.data.remote.dto.MessageDto
import mx.petmate.app.data.remote.dto.SendMessageRequestDto
import mx.petmate.app.data.remote.toDomain
import mx.petmate.app.domain.model.Message
import mx.petmate.app.domain.model.MessageContent
import mx.petmate.app.domain.model.MessageStatus
import mx.petmate.app.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Chat de seguimiento offline-first (spec 016, T-005; constitucion P4).
 *
 * - **Lecturas:** salen de Room y no tocan la red. Lo nuevo entra con [refresh], que la pantalla
 *   llama al abrir la conversacion.
 * - **Un error de red no borra lo local** (C-10): [refresh] falla y deja el historial intacto. Lo
 *   que llego antes se sigue leyendo sin conexion.
 * - **Aislamiento por cuenta:** todo se lee y se guarda con el usuario de la sesion
 *   ([currentUserId]). Dos cuentas en el mismo telefono no se ven los mensajes (C-14). Sin sesion
 *   no se lee ni se guarda nada.
 * - **Paginacion hacia adelante por cursor:** se le pide al servidor lo que sigue al ultimo
 *   mensaje **confirmado** que hay en Room, mandando su `id` (no su `seq`). Ver [ChatRepository],
 *   donde esta escrito por que el contrato decia lo contrario.
 */
class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApiService,
    private val caller: AuthorizedCaller,
    private val messageDao: MessageDao,
    private val currentUserId: () -> String?,
    private val dispatchers: DispatcherProvider,
) : ChatRepository {

    // ---------------- Lecturas: solo Room ----------------

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        val owner = currentUserId() ?: return emptyFlow()
        return messageDao.observeConversation(owner, conversationId)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeUnreadCount(): Flow<Int> = emptyFlow()

    // ---------------- Traer del servidor ----------------

    /**
     * Pide lo que falta y lo guarda. Devuelve cuantos mensajes nuevos entraron.
     *
     * El cursor es el `id` del ultimo mensaje **confirmado**: un pendiente tiene id local y el
     * servidor responderia 404. Si no hay ninguno, se pide desde el principio.
     *
     * Se trae en ciclo mientras el servidor diga `hasMore`, porque una conversacion que estuvo
     * dias sin abrirse puede tener mas de una pagina. El tope de [MAX_PAGES] evita quedarse
     * girando si el servidor devolviera `hasMore` siempre.
     */
    override suspend fun refresh(conversationId: String): AppResult<Int> = io { owner ->
        var cursor = messageDao.lastConfirmed(owner, conversationId)?.id
        var guardados = 0
        var paginas = 0

        while (paginas < MAX_PAGES) {
            val pagina = caller.call { bearer ->
                api.getMessages(bearer, conversationId, after = cursor).data
            }
            val datos = when (pagina) {
                is AppResult.Failure -> return@io AppResult.Failure(pagina.error)
                is AppResult.Success -> pagina.data ?: break
            }
            if (datos.items.isEmpty()) break

            val filas = datos.items.map { it.toEntity(owner) }
            messageDao.upsert(filas)
            guardados += filas.size
            cursor = filas.maxByOrNull { it.seq }?.id
            paginas++
            if (!datos.hasMore) break
        }
        AppResult.Success(guardados)
    }

    // ---------------- Escrituras ----------------

    /**
     * Envia el mensaje y guarda el que confirma el servidor.
     *
     * La moderacion y la cola para enviar sin conexion son de T-006, que depende de Q-04: aqui un
     * fallo de red devuelve el error y **no** se guarda nada como pendiente, para no dejar en la
     * conversacion un mensaje que nadie va a reintentar. Cuando T-006 exista, este es el punto
     * donde entra la cola.
     *
     * Solo texto: la foto va por `messages/send-photo` y es T-011.
     */
    override suspend fun send(conversationId: String, content: MessageContent): AppResult<Message> = io { owner ->
        val texto = when (content) {
            is MessageContent.Text -> content.text
            is MessageContent.Image -> return@io AppResult.Failure(AppError.Unknown)
        }
        val enviado = caller.call { bearer ->
            api.sendMessage(bearer, SendMessageRequestDto(conversationId, texto)).data
        }
        when (enviado) {
            is AppResult.Failure -> AppResult.Failure(enviado.error)
            is AppResult.Success -> {
                val dto = enviado.data ?: return@io AppResult.Failure(AppError.Unknown)
                messageDao.upsert(dto.toEntity(owner))
                AppResult.Success(dto.toDomain())
            }
        }
    }

    /**
     * Marca como leido hasta [upToMessageId] y devuelve el contador que quedo.
     *
     * El backend exige ese id: no se marca "toda la conversacion", asi un mensaje que llego
     * mientras el usuario leia no se da por leido sin verse.
     */
    override suspend fun markAsRead(conversationId: String, upToMessageId: String): AppResult<Int> = io {
        val resumen = caller.call { bearer ->
            api.markRead(bearer, MarkReadRequestDto(conversationId, upToMessageId)).data
        }
        when (resumen) {
            is AppResult.Failure -> AppResult.Failure(resumen.error)
            is AppResult.Success ->
                AppResult.Success(resumen.data?.unreadCount?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0)
        }
    }

    // ---------------- Plomeria ----------------

    /** Corre en IO y exige sesion: sin ella no se toca la base ni la red. */
    private suspend fun <T> io(block: suspend (owner: String) -> AppResult<T>): AppResult<T> =
        withContext(dispatchers.io) {
            val owner = currentUserId() ?: return@withContext AppResult.Failure(AppError.Unauthorized)
            block(owner)
        }

    private companion object {
        /** Tope de paginas por refresco: con 30 por pagina son 300 mensajes de una vez. */
        const val MAX_PAGES = 10
    }
}

// ---------------- Transporte <-> Room ----------------

/**
 * La foto **no** se guarda: solo [MessageEntity.hasPhoto]. La URL viene firmada y expira, asi que
 * guardarla seria guardar una URL muerta (D-05).
 *
 * `createdAt` se guarda como milisegundos para poder ordenar e indexar; si no se puede leer, queda
 * en 0 y el orden lo sostiene `seq`, que es quien manda.
 */
internal fun MessageDto.toEntity(ownerId: String): MessageEntity = MessageEntity(
    ownerId = ownerId,
    id = id,
    conversationId = conversationId,
    seq = seq.trim().toLongOrNull() ?: MessageEntity.PENDING_SEQ,
    kind = kind,
    isMine = isMine,
    text = text,
    createdAtMillis = createdAt.toEpochMillisOrZero(),
    hasPhoto = hasPhoto || kind == MessageDto.KIND_PHOTO,
    status = MessageStatus.Sent.name,
    localId = null,
)

internal fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    authorId = if (isMine) mx.petmate.app.data.remote.AUTHOR_ME else mx.petmate.app.data.remote.AUTHOR_OTHER,
    content = if (hasPhoto) {
        MessageContent.Image(url = "", caption = text.takeIf { it.isNotBlank() })
    } else {
        MessageContent.Text(text)
    },
    sentAt = createdAtMillis.toIsoUtc(),
    status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.Sent),
)

private fun String.toEpochMillisOrZero(): Long =
    runCatching { java.time.Instant.parse(this).toEpochMilli() }
        .recoverCatching { java.time.OffsetDateTime.parse(this).toInstant().toEpochMilli() }
        .getOrDefault(0L)

private fun Long.toIsoUtc(): String = java.time.Instant.ofEpochMilli(this).toString()
