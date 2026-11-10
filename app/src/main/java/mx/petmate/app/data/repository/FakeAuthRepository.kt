package mx.petmate.app.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.DemoAccount
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.model.UserProfile
import mx.petmate.app.domain.repository.AuthRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion en memoria mientras el backend no esta desplegado.
 *
 * Permite construir y probar todo el flujo de identidad sin API. Las cuentas
 * registradas viven solo en memoria y se pierden al cerrar la aplicacion; la
 * SESION si se guarda cifrada, para cumplir "la sesion siempre viva"
 * (spec 002, R-03).
 *
 * Se sustituye por AuthRepositoryImpl cambiando USE_FAKE_BACKEND en
 * app/build.gradle.kts, sin tocar la interfaz ni el dominio.
 */
@Singleton
class FakeAuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
) : AuthRepository {

    private val accounts = mutableMapOf<String, Account>()
    private val state = MutableStateFlow<SessionState>(SessionState.Checking)

    override val sessionState: Flow<SessionState> = state.asStateFlow()

    init {
        // Cuenta de ejemplo para poder entrar sin registrarse primero.
        accounts[DEMO_EMAIL] = Account(
            user = User(id = "demo-1", name = DemoAccount.NAME, email = DEMO_EMAIL),
            password = DEMO_PASSWORD,
        )
    }

    override suspend fun restoreSession() = withContext(dispatchers.io) {
        val saved = sessionStore.readUser()
        state.value = if (saved != null) SessionState.SignedIn(saved) else SessionState.SignedOut
    }

    override suspend fun register(data: RegisterData): AppResult<User> =
        withContext(dispatchers.io) {
            delay(SIMULATED_LATENCY_MS)
            val email = data.email.lowercase()
            if (accounts.containsKey(email)) {
                // Mismo tratamiento que un correo ya registrado en el backend.
                return@withContext AppResult.Failure(AppError.Server(code = 409))
            }
            val user = User(id = UUID.randomUUID().toString(), name = data.name, email = email)
            accounts[email] = Account(user = user, password = data.password)
            persist(user)
            AppResult.Success(user)
        }

    override suspend fun login(credentials: LoginCredentials): AppResult<User> =
        withContext(dispatchers.io) {
            delay(SIMULATED_LATENCY_MS)
            val account = accounts[credentials.email.lowercase()]
            // Mismo error para credenciales invalidas y correo inexistente:
            // no se revela que cuentas existen (spec 002, C-06).
            if (account == null || account.password != credentials.password) {
                return@withContext AppResult.Failure(AppError.Unauthorized)
            }
            persist(account.user)
            AppResult.Success(account.user)
        }

    override suspend fun logout(): AppResult<Unit> = withContext(dispatchers.io) {
        sessionStore.clear()
        state.value = SessionState.SignedOut
        AppResult.Success(Unit)
    }

    override fun enterGuestMode() {
        if (state.value is SessionState.SignedIn) return
        state.value = SessionState.Guest
    }

    override fun exitGuestMode() {
        if (state.value == SessionState.Guest) state.value = SessionState.SignedOut
    }

    override fun expireSession() {
        sessionStore.clear()
        state.value = SessionState.SignedOut
    }

    override suspend fun loadProfile(): AppResult<UserProfile> = withContext(dispatchers.io) {
        val user = sessionStore.readUser() ?: return@withContext AppResult.Failure(AppError.Unauthorized)
        AppResult.Success(user.toProfile())
    }

    override suspend fun updateName(name: String): AppResult<UserProfile> = withContext(dispatchers.io) {
        val user = sessionStore.readUser() ?: return@withContext AppResult.Failure(AppError.Unauthorized)
        val updated = user.copy(name = name.trim())
        accounts[updated.email]?.let { accounts[updated.email] = it.copy(user = updated) }
        sessionStore.saveUser(updated)
        state.value = SessionState.SignedIn(updated)
        AppResult.Success(updated.toProfile())
    }

    private fun User.toProfile() = UserProfile(
        id = id,
        email = email,
        name = name,
        isAffiliate = isAffiliate,
        memberSince = null,
    )

    private fun persist(user: User) {
        sessionStore.save(
            user = user,
            accessToken = "fake-access-${user.id}",
            refreshToken = "fake-refresh-${user.id}",
        )
        state.value = SessionState.SignedIn(user)
    }

    private data class Account(val user: User, val password: String)

    private companion object {
        const val SIMULATED_LATENCY_MS = 600L
        const val DEMO_EMAIL = DemoAccount.EMAIL
        const val DEMO_PASSWORD = DemoAccount.PASSWORD
    }
}
