package mx.petmate.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.model.UserProfile
import mx.petmate.app.domain.push.DeviceRegistration
import mx.petmate.app.domain.repository.AuthRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Que el alta y la baja del dispositivo se disparen donde deben (spec 001, T-039; D-19).
 *
 * Esto es lo que faltaba para que T-039 sirviera de algo: `PushTokenProvider` y
 * `DeviceApiService` ya existian, pero **nadie los llamaba**. Lo que se prueba aqui es el orden,
 * que es donde esta el error facil de cometer:
 *
 * - en el login, el alta va **despues** de guardar la sesion (si no, no hay access token);
 * - en el logout, la baja va **antes** de cerrarla (si no, ya no hay access token);
 * - un login fallido no registra nada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceRegistrationWiringTest {

    /** Anota el orden real de las llamadas, que es justamente lo que se quiere comprobar. */
    private val pasos = mutableListOf<String>()

    private inner class SpyDevices : DeviceRegistration {
        override suspend fun register() {
            pasos += "device.register"
        }

        override suspend fun unregister() {
            pasos += "device.unregister"
        }
    }

    private inner class SpyAuth(private val loginOk: Boolean = true) : AuthRepository {
        override val sessionState: Flow<SessionState> = MutableStateFlow(SessionState.SignedOut)

        override suspend fun login(credentials: LoginCredentials): AppResult<User> {
            pasos += "auth.login"
            return if (loginOk) AppResult.Success(USUARIO) else AppResult.Failure(AppError.Unauthorized)
        }

        override suspend fun logout(): AppResult<Unit> {
            pasos += "auth.logout"
            return AppResult.Success(Unit)
        }

        override suspend fun register(data: RegisterData): AppResult<User> = AppResult.Success(USUARIO)
        override suspend fun restoreSession() = Unit
        override suspend fun refreshSession(): AppResult<User> = AppResult.Success(USUARIO)
        override fun expireSession() = Unit
        override fun enterGuestMode() = Unit
        override fun exitGuestMode() = Unit
        override fun setBusinessMode(enabled: Boolean) = Unit
        override fun isBusinessMode(): Boolean = false
        override suspend fun loadProfile(): AppResult<UserProfile> = FALLO
        override suspend fun updateName(name: String): AppResult<UserProfile> = FALLO
    }

    @Test
    fun `el login registra el dispositivo despues de abrir la sesion`() = runTest {
        val useCase = LoginUseCase(SpyAuth(), ValidateCredentialsUseCase(), SpyDevices())

        useCase(LoginCredentials(email = "ana@petmate.mx", password = "petmate123"))

        // El orden importa: devices/register necesita el access token que login acaba de guardar.
        assertEquals(listOf("auth.login", "device.register"), pasos)
    }

    @Test
    fun `un login fallido no registra el dispositivo`() = runTest {
        val useCase = LoginUseCase(SpyAuth(loginOk = false), ValidateCredentialsUseCase(), SpyDevices())

        useCase(LoginCredentials(email = "ana@petmate.mx", password = "petmate123"))

        assertEquals(listOf("auth.login"), pasos)
    }

    @Test
    fun `unas credenciales invalidas no tocan ni la red ni el push`() = runTest {
        val useCase = LoginUseCase(SpyAuth(), ValidateCredentialsUseCase(), SpyDevices())

        useCase(LoginCredentials(email = "no-es-correo", password = ""))

        assertEquals(emptyList<String>(), pasos)
    }

    @Test
    fun `el logout da de baja el dispositivo antes de cerrar la sesion`() = runTest {
        val useCase = LogoutUseCase(SpyAuth(), SpyDevices())

        useCase()

        // Al reves, devices/unregister saldria sin credencial: el logout ya borro los tokens.
        assertEquals(listOf("device.unregister", "auth.logout"), pasos)
    }

    private companion object {
        val USUARIO = User(id = "u-1", name = "Ana", email = "ana@petmate.mx")
        val FALLO: AppResult<UserProfile> = AppResult.Failure(AppError.Unauthorized)
    }
}
