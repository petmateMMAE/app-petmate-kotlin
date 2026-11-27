package mx.petmate.app.data.moderation

import mx.petmate.app.domain.moderation.ModerationRejection
import mx.petmate.app.domain.moderation.ModerationVerdict

/**
 * Traduce lo que responde el moderador a uno de los tres veredictos (spec 004, T-003).
 *
 * Son tres casos a proposito y solo uno deja publicar:
 *
 *  - **Approved**: el contenido paso.
 *  - **Rejected**: el moderador dijo que no. La razon que se guarda es generica; la categoria interna y el
 *    puntaje se descartan aqui mismo, en la frontera, para que no puedan filtrarse a la pantalla ni al log.
 *  - **NotValidated**: no se pudo decidir (sin red, tiempo agotado, servicio caido, respuesta rara).
 *
 * La regla que importa (R-08): ante la duda NO se publica. Un moderador que falla no es permiso para pasar;
 * si lo fuera, tirar el servicio seria la forma mas facil de saltarse la moderacion.
 */
object ModerationPolicy {

    /** Lo que el moderador considera suficiente para rechazar, de 0 a 1. */
    const val REJECTION_THRESHOLD = 0.5

    /**
     * Veredicto a partir de una respuesta del moderador.
     *
     * @param approved lo que dijo el servicio, o null si no hubo respuesta utilizable.
     * @param highestScore el puntaje mas alto entre todas las categorias, si vino.
     */
    fun fromResponse(approved: Boolean?, highestScore: Double? = null): ModerationVerdict = when {
        approved == null -> ModerationVerdict.NotValidated
        !approved -> ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed)
        // Un servicio puede decir "aprobado" y aun asi devolver un puntaje alto. Se hace caso al puntaje: el
        // umbral es nuestro, no suyo.
        highestScore != null && highestScore >= REJECTION_THRESHOLD ->
            ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed)
        else -> ModerationVerdict.Approved
    }

    /** Veredicto cuando la llamada fallo: siempre NotValidated, nunca aprobado. */
    fun fromFailure(): ModerationVerdict = ModerationVerdict.NotValidated

    /** Rechazo por el archivo en si, sin consultar al moderador. */
    fun forFile(
        sizeBytes: Long,
        mimeType: String,
        maxBytes: Long,
        allowedTypes: Set<String>,
    ): ModerationVerdict = when {
        mimeType.lowercase() !in allowedTypes -> ModerationVerdict.Rejected(ModerationRejection.UnsupportedFormat)
        sizeBytes > maxBytes -> ModerationVerdict.Rejected(ModerationRejection.FileTooLarge)
        else -> ModerationVerdict.Approved
    }
}
