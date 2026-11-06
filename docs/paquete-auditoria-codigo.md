# Paquete de auditoría — código ya leído

**Generado:** 04/11/2026
**Para qué sirve:** los modelos gratuitos de OpenCode gastan su límite en
las *lecturas* de archivo, no en los mensajes. Pedirle que abra 57 archivos
lo mata antes de que opine. Este paquete trae el código ya pegado, así gasta
**una lectura en vez de 57**.

Contiene la capa que decide la arquitectura: `domain`, `data`, `di` y
`core` — 24 archivos, 867 líneas. Las otras 4,700 líneas son Compose
pintando pantallas y no sirven para auditar trazabilidad ni dirección de
dependencias.

---

## Índice de los 54 archivos de `app/src/main`

Los marcados con ✅ van completos más abajo.

```
✅ domain/model/Credentials.kt (16 líneas)
✅ domain/model/SessionState.kt (13 líneas)
✅ domain/model/User.kt (9 líneas)
✅ domain/repository/AuthRepository.kt (34 líneas)
✅ domain/usecase/LoginUseCase.kt (19 líneas)
✅ domain/usecase/LogoutUseCase.kt (12 líneas)
✅ domain/usecase/ObserveSessionUseCase.kt (13 líneas)
✅ domain/usecase/RegisterUseCase.kt (19 líneas)
✅ domain/usecase/ValidateCredentialsUseCase.kt (69 líneas)
✅ data/remote/AuthApiService.kt (26 líneas)
✅ data/remote/AuthMapper.kt (12 líneas)
✅ data/remote/ErrorMapper.kt (24 líneas)
✅ data/remote/dto/AuthDto.kt (40 líneas)
✅ data/repository/AuthRepositoryImpl.kt (107 líneas)
✅ data/repository/FakeAuthRepository.kt (105 líneas)
✅ data/session/SessionStore.kt (86 líneas)
✅ di/AuthModule.kt (40 líneas)
✅ di/CoreModule.kt (34 líneas)
✅ di/NetworkModule.kt (56 líneas)
✅ core/coroutines/DispatcherProvider.kt (22 líneas)
✅ core/log/Logger.kt (25 líneas)
✅ core/result/AppError.kt (35 líneas)
✅ core/result/AppResult.kt (29 líneas)
✅ core/ui/UiText.kt (22 líneas)
   ui/auth/AuthComponents.kt (122 líneas)
   ui/auth/AuthErrorMessages.kt (30 líneas)
   ui/auth/LoginScreen.kt (211 líneas)
   ui/auth/LoginViewModel.kt (77 líneas)
   ui/auth/RegisterScreen.kt (410 líneas)
   ui/auth/RegisterViewModel.kt (121 líneas)
   ui/auth/WelcomeScreen.kt (441 líneas)
   ui/components/DecorativeQr.kt (67 líneas)
   ui/components/PetMateButtons.kt (167 líneas)
   ui/components/PetMateChrome.kt (260 líneas)
   ui/components/PetMatePreviews.kt (24 líneas)
   ui/components/PetMateTextField.kt (237 líneas)
   ui/home/HomeScreen.kt (113 líneas)
   ui/home/HomeViewModel.kt (39 líneas)
   ui/map/IllustratedMap.kt (363 líneas)
   ui/map/MapSampleData.kt (105 líneas)
   ui/map/MapScreen.kt (745 líneas)
   ui/map/MapUiState.kt (79 líneas)
   ui/map/OsmMap.kt (216 líneas)
   ui/map/OsmMapConfig.kt (60 líneas)
   ui/navigation/PetMateNavHost.kt (117 líneas)
   ui/navigation/Route.kt (13 líneas)
   ui/navigation/SessionViewModel.kt (40 líneas)
   ui/splash/SplashScreen.kt (182 líneas)
   ui/theme/Color.kt (90 líneas)
   ui/theme/PetMateColors.kt (152 líneas)
   ui/theme/Theme.kt (108 líneas)
   ui/theme/Type.kt (125 líneas)
```

Más `MainActivity.kt` y `PetMateApplication.kt` en la raíz del paquete.

---

## Código completo

### `core/`

#### core/coroutines/DispatcherProvider.kt

```kotlin
package mx.petmate.app.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatchers inyectables.
 *
 * Escribir Dispatchers.IO dentro de una clase la vuelve no determinista en
 * pruebas; por eso se inyectan (AGENTS.md seccion 4).
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
```

#### core/log/Logger.kt

```kotlin
package mx.petmate.app.core.log

import android.util.Log
import mx.petmate.app.BuildConfig

/**
 * Registro de eventos y errores.
 *
 * Nunca debe recibir contrasenas, tokens ni datos personales
 * (constitution.md P6). En release queda en silencio.
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

class AndroidLogger : Logger {
    override fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
    }
}
```

#### core/result/AppError.kt

```kotlin
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
    PASSWORDS_DO_NOT_MATCH,
}
```

#### core/result/AppResult.kt

```kotlin
package mx.petmate.app.core.result

/**
 * Resultado de una operacion que puede fallar.
 *
 * Se usa en lugar de lanzar excepciones para que el compilador obligue a
 * manejar el error en cada punto donde se consume.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
```

#### core/ui/UiText.kt

```kotlin
package mx.petmate.app.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Texto destinado a la interfaz.
 *
 * Permite que un ViewModel produzca mensajes sin depender de Context, lo que
 * lo hace probable en la JVM (AGENTS.md seccion 4).
 */
sealed interface UiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Dynamic(val value: String) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Resource -> stringResource(id, *args.toTypedArray())
        is Dynamic -> value
    }
}
```

### `domain/`

#### domain/model/Credentials.kt

```kotlin
package mx.petmate.app.domain.model

/** Datos para iniciar sesion. */
data class LoginCredentials(
    val email: String,
    val password: String,
)

/** Datos para crear una cuenta. */
data class RegisterData(
    val name: String,
    val email: String,
    val password: String,
    val passwordConfirmation: String,
    val privacyAccepted: Boolean,
)
```

#### domain/model/SessionState.kt

```kotlin
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
    data class SignedIn(val user: User) : SessionState
}
```

#### domain/model/User.kt

```kotlin
package mx.petmate.app.domain.model

/** Usuario autenticado. */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val isAffiliate: Boolean = false,
)
```

#### domain/repository/AuthRepository.kt

```kotlin
package mx.petmate.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User

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
}
```

#### domain/usecase/LoginUseCase.kt

```kotlin
package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Inicia sesion despues de validar las credenciales. */
class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val validate: ValidateCredentialsUseCase,
) {
    suspend operator fun invoke(credentials: LoginCredentials): AppResult<User> {
        val errors = validate.validateLogin(credentials)
        if (errors.isNotEmpty()) return AppResult.Failure(errors.first())
        return repository.login(credentials.copy(email = credentials.email.trim()))
    }
}
```

#### domain/usecase/LogoutUseCase.kt

```kotlin
package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Cierra la sesion del usuario. */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.logout()
}
```

#### domain/usecase/ObserveSessionUseCase.kt

```kotlin
package mx.petmate.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Observa el estado de la sesion para decidir que grafo mostrar. */
class ObserveSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<SessionState> = repository.sessionState
}
```

#### domain/usecase/RegisterUseCase.kt

```kotlin
package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Crea una cuenta despues de validar los datos. */
class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val validate: ValidateCredentialsUseCase,
) {
    suspend operator fun invoke(data: RegisterData): AppResult<User> {
        val errors = validate.validateRegister(data)
        if (errors.isNotEmpty()) return AppResult.Failure(errors.first())
        return repository.register(data.copy(email = data.email.trim()))
    }
}
```

#### domain/usecase/ValidateCredentialsUseCase.kt

```kotlin
package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.ValidationReason
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import javax.inject.Inject

/**
 * Reglas de validacion de los formularios de identidad.
 *
 * Viven en el dominio y no en la pantalla: las mismas reglas se aplican al
 * escribir y al enviar, y duplicarlas garantiza que se desincronicen
 * (plan 002, D-05 de la unidad 011 aplica el mismo criterio).
 *
 * La politica definitiva de contrasena la define el backend (spec 002, Q-02).
 * Mientras tanto se exige un minimo conservador que no contradice ninguna
 * politica razonable.
 */
class ValidateCredentialsUseCase @Inject constructor() {

    fun validateLogin(credentials: LoginCredentials): List<AppError.Validation> = buildList {
        addAll(emailErrors(credentials.email))
        if (credentials.password.isBlank()) {
            add(AppError.Validation(FIELD_PASSWORD, ValidationReason.REQUIRED))
        }
    }

    fun validateRegister(data: RegisterData): List<AppError.Validation> = buildList {
        if (data.name.isBlank()) {
            add(AppError.Validation(FIELD_NAME, ValidationReason.REQUIRED))
        }
        addAll(emailErrors(data.email))
        when {
            data.password.isBlank() ->
                add(AppError.Validation(FIELD_PASSWORD, ValidationReason.REQUIRED))

            data.password.length < MIN_PASSWORD_LENGTH ->
                add(AppError.Validation(FIELD_PASSWORD, ValidationReason.PASSWORD_TOO_SHORT))
        }
        if (data.password.isNotBlank() && data.password != data.passwordConfirmation) {
            add(
                AppError.Validation(
                    FIELD_PASSWORD_CONFIRMATION,
                    ValidationReason.PASSWORDS_DO_NOT_MATCH,
                ),
            )
        }
    }

    private fun emailErrors(email: String): List<AppError.Validation> = when {
        email.isBlank() -> listOf(AppError.Validation(FIELD_EMAIL, ValidationReason.REQUIRED))
        !EMAIL_PATTERN.matches(email.trim()) ->
            listOf(AppError.Validation(FIELD_EMAIL, ValidationReason.INVALID_EMAIL))

        else -> emptyList()
    }

    companion object {
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_PASSWORD = "password"
        const val FIELD_PASSWORD_CONFIRMATION = "passwordConfirmation"

        const val MIN_PASSWORD_LENGTH = 8

        private val EMAIL_PATTERN = Regex("""^[^@\s]+@[^@\s.]+\.[^@\s]+$""")
    }
}
```

### `data/`

#### data/remote/AuthApiService.kt

```kotlin
package mx.petmate.app.data.remote

import mx.petmate.app.data.remote.dto.AuthResponseDto
import mx.petmate.app.data.remote.dto.LoginRequestDto
import mx.petmate.app.data.remote.dto.RegisterRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Endpoints de identidad.
 *
 * PROVISIONAL: las rutas son una propuesta; el contrato real esta pendiente
 * (spec 002, Q-01). No se consume hasta que el backend este desplegado y se
 * desactive USE_FAKE_BACKEND.
 */
interface AuthApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @POST("auth/logout")
    suspend fun logout()
}
```

#### data/remote/AuthMapper.kt

```kotlin
package mx.petmate.app.data.remote

import mx.petmate.app.data.remote.dto.UserDto
import mx.petmate.app.domain.model.User

/** Traduce el transporte al modelo de dominio. */
fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    isAffiliate = isAffiliate,
)
```

#### data/remote/ErrorMapper.kt

```kotlin
package mx.petmate.app.data.remote

import kotlinx.serialization.SerializationException
import mx.petmate.app.core.result.AppError
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Traduce cualquier falla de red a un AppError.
 *
 * Ninguna excepcion debe salir de la capa de datos
 * (constitution.md, AGENTS.md seccion 4).
 */
fun Throwable.toAppError(): AppError = when (this) {
    is SocketTimeoutException -> AppError.Timeout
    is IOException -> AppError.Network
    is SerializationException -> AppError.Unknown(this)
    is HttpException -> when (code()) {
        401, 403 -> AppError.Unauthorized
        else -> AppError.Server(code())
    }
    else -> AppError.Unknown(this)
}
```

#### data/remote/dto/AuthDto.kt

```kotlin
package mx.petmate.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Objetos de transporte de identidad.
 *
 * PROVISIONALES: el contrato real del backend todavia no esta definido
 * (spec 002, Q-01). Los nombres de campo son una propuesta y deben ajustarse
 * cuando Miguel publique el contrato. Mientras tanto la app usa
 * FakeAuthRepository y estos tipos no se ejercitan.
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponseDto(
    val user: UserDto,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("is_affiliate") val isAffiliate: Boolean = false,
)
```

#### data/repository/AuthRepositoryImpl.kt

```kotlin
package mx.petmate.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.remote.dto.LoginRequestDto
import mx.petmate.app.data.remote.dto.RegisterRequestDto
import mx.petmate.app.data.remote.toAppError
import mx.petmate.app.data.remote.toDomain
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion contra el backend real.
 *
 * TODAVIA NO SE USA: el backend no esta desplegado y su contrato esta
 * pendiente (spec 002, Q-01). Queda escrita para que activarla sea cambiar
 * USE_FAKE_BACKEND en app/build.gradle.kts.
 *
 * Al conectarla hay que revisar que las rutas y los nombres de campo de
 * AuthApiService y de los DTO coincidan con el contrato real.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : AuthRepository {

    private val state = MutableStateFlow<SessionState>(SessionState.Checking)

    override val sessionState: Flow<SessionState> = state.asStateFlow()

    override suspend fun restoreSession() = withContext(dispatchers.io) {
        val saved = sessionStore.readUser()
        state.value = if (saved != null) SessionState.SignedIn(saved) else SessionState.SignedOut
    }

    override suspend fun register(data: RegisterData): AppResult<User> =
        withContext(dispatchers.io) {
            runCatching {
                api.register(
                    RegisterRequestDto(
                        name = data.name,
                        email = data.email,
                        password = data.password,
                    ),
                )
            }.fold(
                onSuccess = { response ->
                    val user = response.user.toDomain()
                    sessionStore.save(user, response.accessToken, response.refreshToken)
                    state.value = SessionState.SignedIn(user)
                    AppResult.Success(user)
                },
                onFailure = { AppResult.Failure(it.toAppError()) },
            )
        }

    override suspend fun login(credentials: LoginCredentials): AppResult<User> =
        withContext(dispatchers.io) {
            runCatching {
                api.login(
                    LoginRequestDto(
                        email = credentials.email,
                        password = credentials.password,
                    ),
                )
            }.fold(
                onSuccess = { response ->
                    val user = response.user.toDomain()
                    sessionStore.save(user, response.accessToken, response.refreshToken)
                    state.value = SessionState.SignedIn(user)
                    AppResult.Success(user)
                },
                onFailure = { AppResult.Failure(it.toAppError()) },
            )
        }

    override suspend fun logout(): AppResult<Unit> = withContext(dispatchers.io) {
        // Lo local se borra SIEMPRE, aunque el servidor falle: el usuario pidio
        // salir y debe salir (spec 002, C-11).
        val remote = runCatching { api.logout() }
        sessionStore.clear()
        state.value = SessionState.SignedOut
        remote.exceptionOrNull()?.let {
            logger.error(TAG, "No se pudo invalidar la sesion en el servidor", it)
        }
        AppResult.Success(Unit)
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
```

#### data/repository/FakeAuthRepository.kt

```kotlin
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
import mx.petmate.app.domain.model.User
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
            user = User(id = "demo-1", name = "Adrian", email = DEMO_EMAIL),
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
        const val DEMO_EMAIL = "demo@petmate.mx"
        const val DEMO_PASSWORD = "petmate123"
    }
}
```

#### data/session/SessionStore.kt

```kotlin
package mx.petmate.app.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda las credenciales de sesion cifradas en el dispositivo.
 *
 * Usa EncryptedSharedPreferences, respaldada por el Keystore del sistema
 * (constitution.md P6, spec 002 C-12).
 *
 * Si el cifrado falla al inicializarse, se degrada de forma controlada: se
 * registra y la app pide iniciar sesion de nuevo, en lugar de caer o de
 * guardar en texto claro (spec 002, G-05).
 */
@Singleton
class SessionStore @Inject constructor(
    private val context: Context,
    private val logger: Logger,
) {

    private val prefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ) as SharedPreferences
        }.onFailure {
            logger.error(TAG, "No se pudo abrir el almacenamiento cifrado", it)
        }.getOrNull()
    }

    fun save(user: User, accessToken: String, refreshToken: String) {
        prefs?.edit()
            ?.putString(KEY_USER_ID, user.id)
            ?.putString(KEY_NAME, user.name)
            ?.putString(KEY_EMAIL, user.email)
            ?.putBoolean(KEY_AFFILIATE, user.isAffiliate)
            ?.putString(KEY_ACCESS_TOKEN, accessToken)
            ?.putString(KEY_REFRESH_TOKEN, refreshToken)
            ?.apply()
    }

    fun readUser(): User? {
        val store = prefs ?: return null
        val id = store.getString(KEY_USER_ID, null) ?: return null
        val token = store.getString(KEY_ACCESS_TOKEN, null) ?: return null
        if (token.isBlank()) return null
        return User(
            id = id,
            name = store.getString(KEY_NAME, "").orEmpty(),
            email = store.getString(KEY_EMAIL, "").orEmpty(),
            isAffiliate = store.getBoolean(KEY_AFFILIATE, false),
        )
    }

    fun readAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)

    /** Borra todo rastro de la sesion. Se llama siempre al cerrar sesion. */
    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }

    private companion object {
        const val TAG = "SessionStore"
        const val FILE_NAME = "petmate_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_AFFILIATE = "is_affiliate"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
```

### `di/`

#### di/AuthModule.kt

```kotlin
package mx.petmate.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.BuildConfig
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.repository.AuthRepositoryImpl
import mx.petmate.app.data.repository.FakeAuthRepository
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Singleton

/**
 * Decide que implementacion de AuthRepository usa la aplicacion.
 *
 * PARA CONECTAR EL BACKEND REAL: pon USE_FAKE_BACKEND en false dentro de
 * app/build.gradle.kts. No hay que tocar nada mas: ni el dominio, ni los
 * ViewModels, ni las pantallas.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApiService,
        sessionStore: SessionStore,
        dispatchers: DispatcherProvider,
        logger: Logger,
    ): AuthRepository = if (BuildConfig.USE_FAKE_BACKEND) {
        FakeAuthRepository(sessionStore, dispatchers)
    } else {
        AuthRepositoryImpl(api, sessionStore, dispatchers, logger)
    }
}
```

#### di/CoreModule.kt

```kotlin
package mx.petmate.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.core.coroutines.DefaultDispatcherProvider
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.AndroidLogger
import mx.petmate.app.core.log.Logger
import mx.petmate.app.data.session.SessionStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideLogger(): Logger = AndroidLogger()

    @Provides
    @Singleton
    fun provideDispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideSessionStore(
        @ApplicationContext context: Context,
        logger: Logger,
    ): SessionStore = SessionStore(context, logger)
}
```

#### di/NetworkModule.kt

```kotlin
package mx.petmate.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import mx.petmate.app.BuildConfig
import mx.petmate.app.data.remote.AuthApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Cliente HTTP.
 *
 * Queda configurado aunque todavia no se consuma: mientras
 * BuildConfig.USE_FAKE_BACKEND sea true, el repositorio activo es el falso.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    private const val TIMEOUT_SECONDS = 30L
}
```

### Raíz del paquete

#### MainActivity.kt

```kotlin
package mx.petmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import mx.petmate.app.ui.navigation.PetMateNavHost
import mx.petmate.app.ui.theme.PetMateEdelTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetMateEdelTheme {
                // Sin padding global: cada pantalla maneja sus barras del
                // sistema, asi la splash y el mapa llegan hasta arriba.
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PetMateNavHost()
                }
            }
        }
    }
}
```

#### PetMateApplication.kt

```kotlin
package mx.petmate.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PetMateApplication : Application()
```

