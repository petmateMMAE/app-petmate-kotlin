package mx.petmate.app.domain.model

/**
 * Conversacion entre el dueno y la veterinaria de un seguimiento (spec 016, T-001). Se liga al seguimiento de
 * 015 (`Followup.conversationId`).
 */
data class Conversation(
    val id: String,
    val followupId: String,
    val participantIds: Set<String>,
    val unreadCount: Int,
) {
    init {
        require(unreadCount >= 0) { "No hay no leidos negativos" }
    }

    fun includes(userId: String): Boolean = userId in participantIds
}
