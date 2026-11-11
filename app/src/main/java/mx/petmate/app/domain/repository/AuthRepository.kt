package mx.petmate.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.model.UserProfile

/**
 * Contrato de identidad y sesion.
 *
 * Lo declara el dominio y lo implementa la capa de datos
 * (constitution.md P3).
 */
interface AuthRepository {
    /** Estado de la sesion, observable por la interfaz. */
    val sessionState: Flow<SessionState>

    /** Restaura la sesion guardada al arrancar la aplicacion. */
    suspend fun restoreSession()

    suspend fun register(data: RegisterData): AppResult<User>

    suspend fun login(credentials: LoginCredentials): AppResult<User>

    /**
     * Cierra la sesion: invalida en el servidor y borra lo local.
     *
     * Los datos locales se borran siempre, aunque el servidor falle
     * (spec 002, C-11).
     */
    suspend fun logout(): AppResult<Unit>

    /** Entra al modo demo ("Probar sin cuenta"). Solo valido sin sesion iniciada. */
    fun enterGuestMode()

    /** Sale del modo demo para crear cuenta o iniciar sesion. */
    fun exitGuestMode()

    /** Perfil propio desde el servidor (pm-037). Si la sesion ya no es valida, se cierra. */
    suspend fun loadProfile(): AppResult<UserProfile>

    /** Cambia el nombre visible en el servidor (pm-038). */
    suspend fun updateName(name: String): AppResult<UserProfile>

    /** La sesion ya no es valida (ni el refresh token sirvio): se borra lo local y se vuelve al login. */
    fun expireSession()

    /**
     * Renueva la sesion para leer de nuevo los datos que calcula el servidor, como el tipo de negocio
     * (pm-087: no cambia en un access token ya emitido; se lee al renovar).
     */
    suspend fun refreshSession(): AppResult<User>

    /** Para una cuenta con negocio: "Ir a mi negocio" (true) o "Ir a mis mascotas" (false). Se recuerda. */
    fun setBusinessMode(enabled: Boolean)

    /** true si la cuenta tiene negocio y el usuario eligio verlo. */
    fun isBusinessMode(): Boolean
}
