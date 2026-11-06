package mx.petmate.app.core.result

/**
 * Jerarquia cerrada de errores de la aplicacion.
 *
 * Las excepciones se atrapan en la capa de datos y se traducen aqui: nunca
 * llegan a la interfaz (constitution.md, AGENTS.md seccion 4).
 */
sealed interface AppError {
    /** No hay conexion o la peticion no llego al servidor. */
    data object Network : AppError

    /** El servidor tardo demasiado en responder. */
    data object Timeout : AppError

    /** El servidor respondio con un error propio. */
    data class Server(val code: Int) : AppError

    /** Credenciales invalidas o sesion expirada. */
    data object Unauthorized : AppError

    /** Un dato que envio el usuario no es valido. */
    data class Validation(val field: String, val reason: ValidationReason) : AppError

    /** Cualquier otra falla no prevista. */
    data class Unknown(val cause: Throwable? = null) : AppError
}

/** Motivos de validacion, para que la interfaz elija el mensaje. */
enum class ValidationReason {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    PASSWORDS_DO_NOT_MATCH,
}
