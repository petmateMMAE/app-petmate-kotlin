package mx.petmate.app.domain.model

/** Contenido de un mensaje del chat: texto o imagen, conjunto cerrado (spec 016, T-001). */
sealed interface MessageContent {
    data class Text(val text: String) : MessageContent

    /** [url] es la imagen ya subida y aprobada por moderacion (004); [caption] es opcional. */
    data class Image(val url: String, val caption: String? = null) : MessageContent
}
