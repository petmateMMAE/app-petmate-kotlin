package mx.petmate.app.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.model.UserProfile
import mx.petmate.app.domain.repository.AuthRepository

/**
 * Doble reutilizable de [AuthRepository] (001/T-023).
 *
 * Es el "doble de repositorio" que pedia la tarea y que faltaba: antes cada prueba armaba el suyo
 * con `mockk`, lo que funciona pero obliga a programar el comportamiento llamada por llamada y
 * vuelve la prueba ilegible en cuanto hay dos o tres pasos.
 *
 * Este guarda estado de verdad: la sesion se emite por un flujo que la prueba mueve con [emitir],
 * y cada llamada queda apuntada en [llamadas] para poder exigir *que se llamo* y no solo *que
 * devolvio*. Las respuestas se cambian con los `responder...` antes de ejercitar.
 *
 * No se llama `FakeAuthRepository` a proposito: ya existe una clase con ese nombre en
 * `data/repository`, que es el backend simulado de la app en modo demo. Son cosas distintas y
 * confundirlas al leer una prueba cuesta tiempo.
 *
 * Nota: `AuthRepository` tiene 13 miembros. Si alguien le agrega uno, esta clase deja de compilar
 * y hay que decidir a proposito que hace el doble, que es justo lo que se quiere.
 */
class AuthRepositoryFalso(
    private var perfil: AppResult<UserProfile> = AppResult.Success(perfilFalso()),
    private var registro: AppResult<User> = AppResult.Success(usuarioFalso()),
    private var inicioDeSesion: AppResult<User> = AppResult.Success(usuarioFalso()),
    private var cierreDeSesion: AppResult<Unit> = AppResult.Success(Unit),
) : AuthRepository {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)

    override val sessionState: Flow<SessionState> = _sessionState.asStateFlow()

    /** Cada metodo que se llamo, en orden. Para exigir que algo ocurrio, no solo que devolvio. */
    val llamadas = mutableListOf<String>()

    /** Las credenciales y los datos con que se llamo, por si la prueba los quiere revisar. */
    var ultimoRegistro: RegisterData? = null
        private set
    var ultimasCredenciales: LoginCredentials? = null
        private set
    var ultimoNombre: String? = null
        private set

    private var modoNegocio = false

    // ---- Lo que la prueba controla ----

    /** Mueve el estado de la sesion, como lo haria el repositorio real. */
    fun emitir(estado: SessionState) {
        _sessionState.value = estado
    }

    fun responderPerfilCon(resultado: AppResult<UserProfile>) {
        perfil = resultado
    }

    fun responderRegistroCon(resultado: AppResult<User>) {
        registro = resultado
    }

    fun responderInicioDeSesionCon(resultado: AppResult<User>) {
        inicioDeSesion = resultado
    }

    fun responderCierreDeSesionCon(resultado: AppResult<Unit>) {
        cierreDeSesion = resultado
    }

    fun seLlamo(metodo: String): Boolean = metodo in llamadas

    fun vecesQueSeLlamo(metodo: String): Int = llamadas.count { it == metodo }

    // ---- AuthRepository ----

    override suspend fun restoreSession() {
        llamadas += "restoreSession"
    }

    override suspend fun register(data: RegisterData): AppResult<User> {
        llamadas += "register"
        ultimoRegistro = data
        return registro
    }

    override suspend fun login(credentials: LoginCredentials): AppResult<User> {
        llamadas += "login"
        ultimasCredenciales = credentials
        return inicioDeSesion
    }

    override suspend fun logout(): AppResult<Unit> {
        llamadas += "logout"
        return cierreDeSesion
    }

    override fun enterGuestMode() {
        llamadas += "enterGuestMode"
        _sessionState.value = SessionState.Guest
    }

    override fun exitGuestMode() {
        llamadas += "exitGuestMode"
        _sessionState.value = SessionState.SignedOut
    }

    override suspend fun loadProfile(): AppResult<UserProfile> {
        llamadas += "loadProfile"
        return perfil
    }

    override suspend fun updateName(name: String): AppResult<UserProfile> {
        llamadas += "updateName"
        ultimoNombre = name
        return perfil
    }

    override fun expireSession() {
        llamadas += "expireSession"
        _sessionState.value = SessionState.SignedOut
    }

    override suspend fun refreshSession(): AppResult<User> {
        llamadas += "refreshSession"
        return inicioDeSesion
    }

    override fun setBusinessMode(enabled: Boolean) {
        llamadas += "setBusinessMode"
        modoNegocio = enabled
        val actual = _sessionState.value
        if (actual is SessionState.SignedIn) {
            _sessionState.value = actual.copy(businessMode = enabled)
        }
    }

    override fun isBusinessMode(): Boolean {
        llamadas += "isBusinessMode"
        return modoNegocio
    }
}
