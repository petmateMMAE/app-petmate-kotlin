package mx.petmate.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.remote.dto.AuthDataDto
import mx.petmate.app.data.remote.dto.LoginRequestDto
import mx.petmate.app.data.remote.dto.LogoutRequestDto
import mx.petmate.app.data.remote.dto.ProfileDto
import mx.petmate.app.data.remote.dto.RefreshRequestDto
import mx.petmate.app.data.remote.dto.RegisterRequestDto
import mx.petmate.app.data.remote.dto.UpdateProfileRequestDto
import mx.petmate.app.data.remote.dto.UserDto
import mx.petmate.app.data.remote.toAppError
import mx.petmate.app.data.remote.toDomain
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.model.UserProfile
import mx.petmate.app.domain.repository.AuthRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion contra el backend real (PetMate API en Azure).
 *
 * Se activa con USE_FAKE_BACKEND=false (valor por defecto desde el 06/11/2026).
 * Contrato: docs/api/endpoints.md del repositorio backend (spec 002, Q-01).
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
            val email = data.email.trim()
            runCatching {
                // El alta solo acepta correo y contrasena; el nombre se guarda despues con users/me/update.
                requireData(api.register(RegisterRequestDto(email = email, password = data.password)).data)
            }.fold(
                onSuccess = { auth ->
                    sessionStore.rememberName(email, data.name)
                    // Mejor esfuerzo: si falla, el nombre queda en el dispositivo y la cuenta sigue creada.
                    val serverName = runCatching {
                        api.updateProfile(bearer(auth.accessToken), UpdateProfileRequestDto(data.name.trim())).data?.displayName
                    }.onFailure { logger.error(TAG, "No se pudo guardar el nombre en el servidor", it) }.getOrNull()
                    signIn(auth, localName = serverName ?: data.name)
                },
                onFailure = { AppResult.Failure(it.toAppError()) },
            )
        }

    override suspend fun login(credentials: LoginCredentials): AppResult<User> =
        withContext(dispatchers.io) {
            val email = credentials.email.trim()
            runCatching {
                requireData(api.login(LoginRequestDto(email = email, password = credentials.password)).data)
            }.fold(
                onSuccess = { auth ->
                    // El login no trae el nombre: se pide el perfil (mejor esfuerzo) para mostrarlo desde el inicio.
                    val serverName = runCatching { api.getProfile(bearer(auth.accessToken)).data?.displayName }
                        .getOrNull()
                    signIn(auth, localName = serverName ?: sessionStore.knownNameFor(email))
                },
                onFailure = { AppResult.Failure(it.toAppError()) },
            )
        }

    override suspend fun logout(): AppResult<Unit> = withContext(dispatchers.io) {
        // Lo local se borra SIEMPRE, aunque el servidor falle: el usuario pidio
        // salir y debe salir (spec 002, C-11).
        val remote = runCatching { revokeRemoteSession() }
        endSession()
        remote.exceptionOrNull()?.let {
            logger.error(TAG, "No se pudo invalidar la sesion en el servidor", it)
        }
        AppResult.Success(Unit)
    }

    override fun enterGuestMode() {
        if (state.value is SessionState.SignedIn) return
        state.value = SessionState.Guest
    }

    override fun exitGuestMode() {
        if (state.value == SessionState.Guest) state.value = SessionState.SignedOut
    }

    override suspend fun loadProfile(): AppResult<UserProfile> = withContext(dispatchers.io) {
        authorized { access -> api.getProfile(bearer(access)).data }
    }

    override suspend fun updateName(name: String): AppResult<UserProfile> = withContext(dispatchers.io) {
        authorized { access -> api.updateProfile(bearer(access), UpdateProfileRequestDto(name.trim())).data }
    }

    /**
     * Ejecuta una llamada autenticada. Si el access token (15 min) expiro, lo renueva una vez con el
     * refresh token y reintenta. Si la renovacion tambien es rechazada, la sesion ya no sirve: se cierra.
     */
    private suspend fun authorized(call: suspend (String) -> ProfileDto?): AppResult<UserProfile> {
        val access = sessionStore.readAccessToken()
        val refresh = sessionStore.readRefreshToken()
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
            endSession()
            return AppResult.Failure(AppError.Unauthorized)
        }
        return runCatching {
            try {
                requireProfile(call(access))
            } catch (e: HttpException) {
                if (e.code() != 401) throw e
                val renewed = requireData(api.refresh(RefreshRequestDto(refresh)).data)
                sessionStore.updateTokens(renewed.accessToken, renewed.refreshToken)
                requireProfile(call(renewed.accessToken))
            }
        }.fold(
            onSuccess = { dto -> AppResult.Success(rememberProfile(dto)) },
            onFailure = {
                val error = it.toAppError()
                if (error == AppError.Unauthorized) endSession()
                AppResult.Failure(error)
            },
        )
    }

    /** Guarda el nombre del servidor como el vigente y actualiza el usuario de la sesion. */
    private fun rememberProfile(dto: ProfileDto): UserProfile {
        val name = dto.displayName?.takeIf { it.isNotBlank() }
            ?: sessionStore.knownNameFor(dto.email)
            ?: dto.email.substringBefore('@')
        sessionStore.rememberName(dto.email, name)
        val current = sessionStore.readUser()
        if (current != null && current.name != name) {
            val updated = current.copy(name = name)
            sessionStore.saveUser(updated)
            state.value = SessionState.SignedIn(updated)
        }
        return UserProfile(
            id = dto.id,
            email = dto.email,
            name = name,
            isAffiliate = dto.role == UserDto.ROLE_BUSINESS,
            memberSince = dto.createdAt,
        )
    }

    /**
     * Revoca la sesion en el servidor. Si el access token ya expiro, lo renueva con el refresh token
     * y reintenta una sola vez, como indica el contrato de logout.
     */
    private suspend fun revokeRemoteSession() {
        val access = sessionStore.readAccessToken()
        val refresh = sessionStore.readRefreshToken()
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) return
        try {
            api.logout(bearer(access), LogoutRequestDto(refresh))
        } catch (e: HttpException) {
            if (e.code() != 401) throw e
            val renewed = requireData(api.refresh(RefreshRequestDto(refresh)).data)
            sessionStore.updateTokens(renewed.accessToken, renewed.refreshToken)
            api.logout(bearer(renewed.accessToken), LogoutRequestDto(renewed.refreshToken))
        }
    }

    private fun signIn(data: AuthDataDto, localName: String?): AppResult<User> {
        val user = data.user.toDomain(localName)
        sessionStore.save(user, data.accessToken, data.refreshToken)
        state.value = SessionState.SignedIn(user)
        return AppResult.Success(user)
    }

    private fun endSession() {
        sessionStore.clear()
        state.value = SessionState.SignedOut
    }

    private fun requireData(data: AuthDataDto?): AuthDataDto =
        data ?: throw IllegalStateException("Respuesta de autenticacion sin data")

    private fun requireProfile(data: ProfileDto?): ProfileDto =
        data ?: throw IllegalStateException("Respuesta de perfil sin data")

    private fun bearer(token: String) = "Bearer $token"

    private companion object {
        const val TAG = "AuthRepository"
    }
}
