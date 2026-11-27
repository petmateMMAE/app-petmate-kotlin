package mx.petmate.app.data.moderation

import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.moderation.ModerationGateway
import mx.petmate.app.domain.moderation.ModerationRequest
import mx.petmate.app.domain.moderation.ModerationVerdict
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moderacion que todavia no modera (spec 001, T-037, D-18).
 *
 * Es el relleno hasta que llegue la implementacion real contra el backend
 * (spec 004, T-002, que espera la ruta de Miguel en Q-01).
 *
 * **Devuelve [ModerationVerdict.NotValidated], no "aprobado".** La constitucion
 * P13 dice que nada se almacena sin moderar y 004/R-08 dice que solo "aprobado"
 * deja publicar. Un no-op que aprobara seria peor que no tener moderacion:
 * dejaria pasar todo mientras el codigo parece que valida. Asi, cuando alguien
 * enchufe este punto a una pantalla, lo notara de inmediato en lugar de
 * publicar contenido sin revisar.
 *
 * Cada llamada queda registrada, porque la tarea pide que el no-op sea
 * explicito y registrado.
 */
@Singleton
class NoOpModerationGateway @Inject constructor(private val logger: Logger) : ModerationGateway {

    override suspend fun moderateText(text: String): ModerationVerdict = notValidated("texto")

    override suspend fun moderateImage(bytes: ByteArray, mimeType: String): ModerationVerdict =
        notValidated("imagen $mimeType")

    override suspend fun moderateVideo(bytes: ByteArray, mimeType: String): ModerationVerdict =
        notValidated("video $mimeType")

    override suspend fun moderate(request: ModerationRequest): ModerationVerdict = when (request) {
        is ModerationRequest.Text -> moderateText(request.text)
        is ModerationRequest.Image -> moderateImage(request.bytes, request.mimeType)
        is ModerationRequest.Video -> moderateVideo(request.bytes, request.mimeType)
    }

    /** No se registra el contenido: podria traer datos personales (constitucion P6). */
    private fun notValidated(kind: String): ModerationVerdict {
        logger.debug(TAG, "Moderacion no implementada todavia ($kind): no se publica. Spec 004, T-002.")
        return ModerationVerdict.NotValidated
    }

    private companion object {
        const val TAG = "NoOpModeration"
    }
}
