package mx.petmate.app.domain.model

/**
 * Estado de la sesion.
 *
 * Checking existe para que la pantalla inicial no parpadee entre login y
 * contenido mientras se restaura la sesion guardada (spec 002, C-07).
 */
sealed interface SessionState {
    data object Checking : SessionState
    data object SignedOut : SessionState

    /**
     * "Probar sin cuenta" (maqueta, bienvenida): se entra a la app con datos de ejemplo,
     * sin registro ni tokens. No se guarda: al reabrir la app vuelve a la bienvenida.
     */
    data object Guest : SessionState
    /** [businessMode]: la cuenta tiene negocio y el usuario eligio "Ir a mi negocio". */
    data class SignedIn(val user: User, val businessMode: Boolean = false) : SessionState
}
