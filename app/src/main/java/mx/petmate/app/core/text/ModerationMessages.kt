package mx.petmate.app.core.text

import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.moderation.ModerationRejection
import mx.petmate.app.domain.moderation.ModerationVerdict

/**
 * El texto que ve la persona cuando la moderacion rechaza algo (spec 004, T-003, R-07).
 *
 * La regla de la spec es que el mensaje sea UNICO para todas las categorias de contenido no permitido. No es
 * pereza de redaccion: distinguir "esto es violento" de "esto es sexual" le dice al que prueba el sistema por
 * donde pasar, y de paso acusa a alguien que quiza solo escribio mal. Tampoco se muestra el puntaje ni el
 * nombre del proveedor; eso es detalle interno y no ayuda a corregir nada.
 *
 * Las tres razones de tamano y formato SI tienen mensaje propio, porque ahi la persona puede actuar: elegir
 * otro archivo o comprimirlo.
 */
object ModerationMessages {

    /** Mensaje visible para un veredicto. Devuelve null cuando no hay nada que decir (aprobado). */
    fun forVerdict(verdict: ModerationVerdict): UiText? = when (verdict) {
        is ModerationVerdict.Approved -> null
        is ModerationVerdict.NotValidated -> UiText.Resource(R.string.moderation_not_validated)
        is ModerationVerdict.Rejected -> forRejection(verdict.reason)
    }

    /** Mensaje visible para una razon de rechazo. */
    fun forRejection(reason: ModerationRejection): UiText = when (reason) {
        // Un solo mensaje para todo lo que el moderador considero no permitido, sea cual sea la categoria.
        ModerationRejection.ContentNotAllowed -> UiText.Resource(R.string.moderation_content_not_allowed)
        ModerationRejection.FileTooLarge -> UiText.Resource(R.string.moderation_file_too_large)
        ModerationRejection.UnsupportedFormat -> UiText.Resource(R.string.moderation_unsupported_format)
    }
}
