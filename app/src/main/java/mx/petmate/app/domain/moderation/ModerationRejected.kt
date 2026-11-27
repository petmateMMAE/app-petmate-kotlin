package mx.petmate.app.domain.moderation

/**
 * Un contenido no se pudo publicar porque no paso moderacion (spec 004; 001/T-038, R-07, R-08).
 *
 * Por que es una `Exception` y no un caso nuevo de `AppError`: `AuthErrorMessages.kt`,
 * `BusinessViewModels.kt` y `VeterinaryActionViewModel.kt` hacen `when` exhaustivo sobre
 * `AppError` **sin** `else`, asi que agregar un caso dejaria esas tres pantallas sin compilar.
 * Siendo `Exception` viaja dentro de `AppError.Unknown(cause)` sin tocar `ui/`, y la pantalla
 * ensena su mensaje generico mientras nadie le ponga uno propio.
 * Lo propio seria `AppError.Moderation(reason)`; queda anotado como deuda en memoria.md porque
 * exige editar esos tres archivos, que son territorio de Eduardo.
 *
 * No se lanza nunca: se construye y se devuelve dentro de `AppResult.Failure`.
 */
sealed class ModerationRejected(message: String) : Exception(message) {

    /** Se reviso el contenido y no esta permitido. El motivo es generico a proposito (R-07). */
    data class NotAllowed(val reason: ModerationRejection) :
        ModerationRejected("Moderacion rechazo el contenido: $reason")

    /**
     * No se pudo revisar: sin red, timeout o el servicio caido. Tampoco se publica (R-08): ante la
     * duda no se guarda, porque dejar pasar algo sin revisar es peor que pedirle al usuario que lo
     * intente de nuevo.
     */
    data object CouldNotValidate :
        ModerationRejected("Moderacion no pudo validar el contenido; no se publica")
}
