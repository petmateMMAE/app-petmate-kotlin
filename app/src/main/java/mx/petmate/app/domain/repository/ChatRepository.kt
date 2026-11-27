package mx.petmate.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Message
import mx.petmate.app.domain.model.MessageContent

/**
 * Chat de seguimiento (spec 016, T-002).
 *
 * ## Correccion del 27/11/2026: el orden iba al reves
 *
 * Este contrato se escribio antes de que el backend estuviera desplegado y documentaba los
 * mensajes **"del mas nuevo al mas viejo"**, con un `limit` como unica forma de paginar. El
 * backend hace lo contrario, y es el que manda:
 *
 * - `messages/get-all` devuelve los mensajes con secuencia **posterior** a un ancla, en orden
 *   **ascendente** (`ListMessagesHandler` del backend). Se pagina **hacia adelante**.
 * - El cursor es el **id** del ultimo mensaje que ya se tiene, no su `seq`: el backend recibe
 *   `after` como uuid, busca ese mensaje y usa *su* secuencia como corte. Mandar el `seq`
 *   revienta el parseo del Guid.
 * - El orden lo fija `seq` y no la fecha. `createdAt` llega en segundos y dos mensajes del mismo
 *   segundo empatarian; `seq` es monotono por conversacion y no depende del reloj de nadie.
 *
 * Dejarlo como estaba habria hecho que [ChatRepository] pidiera las paginas al reves: la
 * implementacion de T-005 habria mandado el `seq` donde va un id, y la conversacion se habria
 * mostrado invertida. Autorizado por Adrian el 27/11/2026.
 *
 * Un chat se lee de abajo hacia arriba, asi que la **pantalla** muestra el mas reciente abajo
 * (T-008); eso es presentacion. El dominio los entrega en el orden del servidor, ascendente.
 */
interface ChatRepository {

    /**
     * Historial completo de [conversationId] que hay guardado, en orden **ascendente** (del mas
     * viejo al mas nuevo), observado de forma continua.
     *
     * No lleva `limit`: lo que se pagina es lo que se **trae del servidor** ([refresh]), no lo
     * que se observa. Un `limit` aqui cortaria el historial ya descargado y la pantalla no
     * podria desplazarse hacia arriba.
     */
    fun observeMessages(conversationId: String): Flow<List<Message>>

    /**
     * Trae del servidor lo que falte de [conversationId] y lo guarda.
     *
     * Pagina hacia adelante desde el ultimo mensaje conocido. Devuelve cuantos mensajes nuevos
     * entraron; sin conexion falla sin borrar lo local (C-10).
     */
    suspend fun refresh(conversationId: String): AppResult<Int>

    /** Envia [content]. Pasa por moderacion (004) antes de salir; sin red queda como pendiente. */
    suspend fun send(conversationId: String, content: MessageContent): AppResult<Message>

    /**
     * Marca como leido **hasta [upToMessageId]** y devuelve el contador que quedo.
     *
     * El backend exige ese id (`MarkReadRequest`): no se marca "toda la conversacion". Asi un
     * mensaje que llego mientras el usuario leia no se da por leido sin haberse visto.
     */
    suspend fun markAsRead(conversationId: String, upToMessageId: String): AppResult<Int>

    /** Total de mensajes sin leer en todas las conversaciones del usuario. */
    fun observeUnreadCount(): Flow<Int>

    companion object {
        /** Cuantos mensajes se piden al servidor por pagina. */
        const val PAGE_SIZE = 30
    }
}
