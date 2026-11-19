package mx.petmate.app.domain.moderation

/**
 * Lo que se manda a moderar (spec 004, T-001). Los archivos van como bytes con su tipo MIME, no como objetos de
 * Android, para que `domain` siga en Kotlin puro. El contenido nunca se escribe en registros (MD-08).
 */
sealed interface ModerationRequest {
    val type: ModerationContentType

    data class Text(val text: String) : ModerationRequest {
        override val type = ModerationContentType.Text
    }

    class Image(val bytes: ByteArray, val mimeType: String) : ModerationRequest {
        override val type = ModerationContentType.Image
    }

    class Video(val bytes: ByteArray, val mimeType: String) : ModerationRequest {
        override val type = ModerationContentType.Video
    }
}
