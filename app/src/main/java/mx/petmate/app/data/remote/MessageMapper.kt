package mx.petmate.app.data.remote

import mx.petmate.app.data.remote.dto.ConversationSummaryDto
import mx.petmate.app.data.remote.dto.MessageDto
import mx.petmate.app.data.remote.dto.MessageListDto
import mx.petmate.app.domain.model.Conversation
import mx.petmate.app.domain.model.Message
import mx.petmate.app.domain.model.MessageContent
import mx.petmate.app.domain.model.MessageStatus

/**
 * Traduce el transporte del chat al modelo de dominio (spec 016, T-003).
 *
 * El orden lo fija el servidor con `seq`, no la fecha. `createdAt` llega en segundos y
 * dos mensajes del mismo segundo empatarian; `seq` es monotono por conversacion y no
 * depende del reloj de nadie. Por eso [toDomainList] ordena por [MessageDto.seq] y la
 * fecha queda solo para mostrarse.
 */

/** `seq` viaja como entero o como cadena (`int64` en el contrato). Ilegible → 0, que ordena al principio. */
private fun MessageDto.sequence(): Long = seq.trim().toLongOrNull() ?: 0L

/**
 * Un mensaje que ya volvio del servidor esta enviado. [MessageStatus.Pending] y
 * [MessageStatus.Failed] son estados **locales**: los pone la cola de envio, no el
 * transporte. El contrato no trae acuse de recibo del otro participante, asi que
 * [MessageStatus.Delivered] no se infiere aqui: inventarlo mostraria "entregado" sin
 * que nadie lo confirme.
 */
fun MessageDto.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    authorId = if (isMine) AUTHOR_ME else AUTHOR_OTHER,
    content = toContent(),
    sentAt = createdAt,
    status = MessageStatus.Sent,
)

/**
 * El backend distingue tres clases (`Text`, `System`, `Photo`) y el dominio solo modela
 * texto e imagen. Un mensaje de sistema es texto que nadie escribio: se mapea a
 * [MessageContent.Text] para no perderlo de la conversacion.
 *
 * **La URL de la foto no viene aqui.** Se pide aparte a `messages/get-photo-url` y
 * expira, asi que la imagen se mapea con la URL vacia y el texto como pie: quien la
 * muestre resuelve la URL en ese momento (D-05). Guardarla seria guardar una URL muerta.
 */
private fun MessageDto.toContent(): MessageContent = when {
    kind == MessageDto.KIND_PHOTO || hasPhoto ->
        MessageContent.Image(url = "", caption = text.takeIf { it.isNotBlank() })
    else -> MessageContent.Text(text)
}

/** Mensajes ordenados por secuencia del servidor, del mas viejo al mas nuevo. */
fun MessageListDto.toDomainList(): List<Message> =
    items.sortedBy { it.sequence() }.map { it.toDomain() }

/**
 * Id del ultimo mensaje de la pagina, que es el cursor para pedir la siguiente
 * (`after`). Es el **id**, no el `seq`: ver [ChatApiService.getMessages].
 * Pagina vacia → `null`, que significa "pide desde el principio".
 */
fun MessageListDto.nextCursor(): String? = items.maxByOrNull { it.sequence() }?.id

fun ConversationSummaryDto.toDomain(participantIds: Set<String> = emptySet()): Conversation = Conversation(
    id = id,
    followupId = followupId,
    participantIds = participantIds,
    unreadCount = unreadCount.trim().toLongOrNull()?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt() ?: 0,
)

/**
 * Autores posibles desde el transporte: el contrato solo dice `isMine`, nunca el id del
 * otro participante (pm-159 no expone identidades en el chat). Estas dos constantes son
 * lo unico que se puede afirmar; `Message.isFrom` se compara contra [AUTHOR_ME].
 */
const val AUTHOR_ME = "me"
const val AUTHOR_OTHER = "other"
