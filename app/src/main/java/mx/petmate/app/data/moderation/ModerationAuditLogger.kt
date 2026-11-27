package mx.petmate.app.data.moderation

import java.security.MessageDigest
import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.moderation.ModerationContentType
import mx.petmate.app.domain.moderation.ModerationVerdict

/**
 * Deja rastro de cada moderacion SIN guardar lo moderado (spec 004, T-009, R-11).
 *
 * El registro sirve para dos cosas: saber si el servicio esta fallando y poder demostrar que la moderacion
 * corre. Para eso basta el tipo, el resultado y cuanto tardo. NO hace falta el contenido, y guardarlo seria
 * peor que no tener registro: un log con el texto rechazado es una copia del contenido que se acaba de
 * prohibir, en un sitio que suele estar menos protegido que la base de datos y que a veces sale del telefono
 * en un reporte de errores.
 *
 * Tampoco se registra el puntaje ni la categoria interna: con eso alguien podria reconstruir por que fallo y
 * calibrar un texto hasta pasar el filtro.
 *
 * Para poder seguir un caso concreto sin guardar nada se usa un identificador opaco: los primeros 12
 * caracteres del SHA-256 del contenido. Sirve para correlacionar dos intentos del mismo texto, pero no se
 * puede revertir.
 */
class ModerationAuditLogger(private val logger: Logger) {

    fun record(
        type: ModerationContentType,
        verdict: ModerationVerdict,
        durationMillis: Long,
        content: String,
    ) {
        logger.debug(TAG, line(type, verdict, durationMillis, opaqueId(content)))
    }

    /** La linea tal como se escribe. Expuesta para que la prueba revise exactamente lo que sale. */
    internal fun line(
        type: ModerationContentType,
        verdict: ModerationVerdict,
        durationMillis: Long,
        id: String,
    ): String = "type=${type.name} result=${result(verdict)} ms=$durationMillis id=$id"

    /** Solo el nombre del caso, nunca la razon: "rechazado" basta, "rechazado por X" ya es una pista. */
    private fun result(verdict: ModerationVerdict): String = when (verdict) {
        is ModerationVerdict.Approved -> "approved"
        is ModerationVerdict.Rejected -> "rejected"
        is ModerationVerdict.NotValidated -> "not_validated"
    }

    companion object {
        const val TAG = "Moderation"

        /** Longitud del identificador: suficiente para correlacionar, inutil para reconstruir. */
        const val ID_LENGTH = 12

        fun opaqueId(content: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(content.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(ID_LENGTH)
    }
}
