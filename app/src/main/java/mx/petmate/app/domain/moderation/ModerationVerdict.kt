package mx.petmate.app.domain.moderation

/**
 * Por que se rechazo algo: conjunto cerrado y generico (spec 004, T-001, R-07). No dice categorias internas,
 * puntajes ni el proveedor: el usuario solo necesita saber que corregir.
 */
enum class ModerationRejection {
    /** El contenido no esta permitido en PetMate. */
    ContentNotAllowed,

    /** El archivo pasa del tamano permitido. */
    FileTooLarge,

    /** El formato del archivo no se acepta. */
    UnsupportedFormat,
}

/**
 * Veredicto de moderacion (spec 004, T-001). Rechazado y no validado son estados distintos (plan RT-04), pero
 * **ninguno** deja publicar: si el servicio falla, tarda o no esta, la operacion se rechaza (R-08).
 */
sealed interface ModerationVerdict {
    /** Unico veredicto que deja guardar y enviar. */
    val isPublishable: Boolean

    data object Approved : ModerationVerdict {
        override val isPublishable = true
    }

    data class Rejected(val reason: ModerationRejection) : ModerationVerdict {
        override val isPublishable = false
    }

    /** No se pudo validar (sin red, timeout, servicio caido). Se reintenta despues; mientras, no se publica. */
    data object NotValidated : ModerationVerdict {
        override val isPublishable = false
    }
}
