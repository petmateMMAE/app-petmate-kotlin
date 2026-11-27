package mx.petmate.app.data.moderation

import java.io.File
import mx.petmate.app.domain.moderation.ModerationContentType
import mx.petmate.app.domain.moderation.ModerationGateway
import mx.petmate.app.domain.moderation.ModerationVerdict

/**
 * Modera una imagen ANTES de subirla (spec 004, T-006).
 *
 * Tres garantias, y cada una existe por un fallo concreto que se evita:
 *
 *  - **Se modera antes de subir.** Al reves, una imagen rechazada ya estaria en el servidor y habria que
 *    borrarla de ahi, con la ventana en la que alguien pudo verla.
 *  - **Lo aprobado se sube una sola vez.** El bloque de subida se invoca una vez, nunca dos.
 *  - **El temporal se borra pase lo que pase.** Va en un `finally`: si se rechaza, si falla la red o si algo
 *    lanza, el archivo no se queda en la cache del telefono. El usuario cree que no se guardo nada; tiene que
 *    ser cierto.
 *
 * Sin red no se sube ni se encola el archivo: el veredicto es NotValidated y quien llama guarda la entidad
 * sin imagen. Encolar un archivo sin moderar seria subirlo mas tarde sin revisar.
 */
class ImageModerator(
    private val gateway: ModerationGateway,
    private val store: TemporaryFileStore,
    private val audit: ModerationAuditLogger,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun moderateThenUpload(
        bytes: ByteArray,
        mimeType: String,
        onApproved: suspend (File) -> Unit,
    ): ModerationVerdict {
        val start = clock()
        val temporary = store.write(bytes, extensionFor(mimeType))

        return try {
            val verdict = runCatching { gateway.moderateImage(bytes, mimeType) }
                .getOrElse { ModerationPolicy.fromFailure() }

            audit.record(ModerationContentType.Image, verdict, clock() - start, temporary.name)

            if (verdict.isPublishable) {
                onApproved(temporary)
            }
            verdict
        } finally {
            // Siempre. Un rechazo que deja el archivo en la cache es una copia de lo que se acaba de prohibir.
            store.delete(temporary)
        }
    }

    private fun extensionFor(mimeType: String): String = when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}
