package mx.petmate.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.remote.dto.AuthDataDto
import mx.petmate.app.data.remote.dto.LoginRequestDto
import mx.petmate.app.data.remote.dto.LogoutRequestDto
import mx.petmate.app.data.remote.dto.RefreshRequestDto
import mx.petmate.app.data.remote.dto.RegisterRequestDto
import mx.petmate.app.data.remote.toAppError
import mx.petmate.app.data.remote.toDomain
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.repository.AuthRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion contra el backend real (PetMate API v0.1.0 en Azure).
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
                // El backend solo acepta correo y contrasena; el nombre se queda en el dispositivo.
                requireData(api.register(RegisterRequestDto(email = email, password = data.password)).data)
            }.fold(
                onSuccess = { auth ->
                    sessionStore.rememberName(email, data.name)
                    signIn(auth, localName = data.name)
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
                onSuccess = { auth -> signIn(auth, localName = sessionStore.knownNameFor(email)) },
                onFailure = { AppResult.Failure(it.toAppError()) },
            )
        }

    override suspend fun logout(): AppResult<Unit> = withContext(dispatchers.io) {
        // Lo local se borra SIEMPRE, aunque el servidor falle: el usuario pidio
        // salir y debe salir (spec 002, C-11).
        val remote = runCatching { revokeRemoteSession() }
        sessionStore.clear()
        state.value = SessionState.SignedOut
        remote.exceptionOrNull()?.let {
            logger.error(TAG, "No se pudo invalidar la sesion en el servidor", it)
        }
        AppResult.Success(Unit)
    }

    /**
     * Revoca la sesion en el servidor. Si el access token (15 min) ya expiro,
     * lo renueva con el refresh token y reintenta una sola vez, como indica el
     * contrato de logout.
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

    private fun requireData(data: AuthDataDto?): AuthDataDto =
        data ?: throw IllegalStateException("Respuesta de autenticacion sin data")

    private fun bearer(token: String) = "Bearer $token"

    private companion object {
        const val TAG = "AuthRepository"
    }
}
