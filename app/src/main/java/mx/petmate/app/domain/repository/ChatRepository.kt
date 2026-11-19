package mx.petmate.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Message
import mx.petmate.app.domain.model.MessageContent

/**
 * Chat de seguimiento (spec 016, T-002). Lo implementa la capa de datos cuando Miguel defina las rutas y la
 * paginacion (Q-01, Q-02).
 */
interface ChatRepository {
    /** Mensajes de [conversationId], del mas nuevo al mas viejo, hasta [limit] (paginado). */
    fun observeMessages(conversationId: String, limit: Int = PAGE_SIZE): Flow<List<Message>>

    /** Envia [content]. Pasa por moderacion (004) antes de salir; sin red queda como pendiente. */
    suspend fun send(conversationId: String, content: MessageContent): AppResult<Message>

    suspend fun markAsRead(conversationId: String): AppResult<Unit>

    /** Total de mensajes sin leer en todas las conversaciones del usuario. */
    fun observeUnreadCount(): Flow<Int>

    companion object {
        const val PAGE_SIZE = 30
    }
}
