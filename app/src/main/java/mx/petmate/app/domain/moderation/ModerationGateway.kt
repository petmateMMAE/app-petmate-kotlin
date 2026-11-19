package mx.petmate.app.domain.moderation

/**
 * Punto unico de moderacion (spec 004, T-001; constitucion P13). Lo implementa la capa de datos contra el
 * backend (T-002, espera la ruta de Miguel, Q-01). Nunca lanza excepciones: un error se devuelve como
 * [ModerationVerdict.NotValidated].
 */
interface ModerationGateway {
    suspend fun moderateText(text: String): ModerationVerdict

    suspend fun moderateImage(bytes: ByteArray, mimeType: String): ModerationVerdict

    suspend fun moderateVideo(bytes: ByteArray, mimeType: String): ModerationVerdict

    /** Despacha [request] al metodo de su tipo. */
    suspend fun moderate(request: ModerationRequest): ModerationVerdict = when (request) {
        is ModerationRequest.Text -> moderateText(request.text)
        is ModerationRequest.Image -> moderateImage(request.bytes, request.mimeType)
        is ModerationRequest.Video -> moderateVideo(request.bytes, request.mimeType)
    }
}
