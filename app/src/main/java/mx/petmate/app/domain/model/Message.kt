package mx.petmate.app.domain.model

/** Estado de un mensaje desde que se escribe hasta que llega (spec 016, T-001). */
enum class MessageStatus {
    /** Guardado en el telefono, esperando red o moderacion. */
    Pending,
    Sent,
    Delivered,

    /** El servidor lo rechazo o la moderacion no lo aprobo; se puede reintentar o descartar. */
    Failed,
}

/** Un mensaje del chat de seguimiento (spec 016, T-001). Fechas en ISO-8601 UTC, como el resto de la app. */
data class Message(
    val id: String,
    val conversationId: String,
    val authorId: String,
    val content: MessageContent,
    val sentAt: String,
    val status: MessageStatus,
) {
    fun isFrom(userId: String): Boolean = authorId == userId
}
