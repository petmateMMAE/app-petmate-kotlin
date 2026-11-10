package mx.petmate.app.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.dto.RefreshRequestDto
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.repository.AuthRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ejecuta llamadas autenticadas a la API.
 *
 * - Agrega `Authorization: Bearer <access token>`.
 * - Si el access token (15 min) expiro (401), lo renueva UNA vez con el refresh token y reintenta.
 *   La renovacion va en un Mutex para que dos pantallas no roten el refresh token al mismo tiempo.
 * - Si la renovacion tambien es rechazada, la sesion ya no sirve: se cierra y la app vuelve al login.
 * - 403 NO cierra la sesion: en el carnet significa "lo registro el negocio, no se puede modificar".
 * - Ninguna excepcion sale de aqui: todo se traduce a AppError (constitution.md).
 */
@Singleton
class AuthorizedCaller @Inject constructor(
    private val authApi: AuthApiService,
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) {
    private val refreshMutex = Mutex()

    suspend fun <T> call(block: suspend (bearer: String) -> T): AppResult<T> {
        val access = sessionStore.readAccessToken()
        if (access.isNullOrBlank() || sessionStore.readRefreshToken().isNullOrBlank()) {
            authRepository.expireSession()
            return AppResult.Failure(AppError.Unauthorized)
        }
        return runCatching {
            try {
                block(bearer(access))
            } catch (e: HttpException) {
                if (e.code() != 401) throw e
                block(bearer(renew(usedAccess = access)))
            }
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { error ->
                val mapped = error.toPetsError()
                if (mapped == AppError.Unauthorized) authRepository.expireSession()
                AppResult.Failure(mapped)
            },
        )
    }

    /** Devuelve un access token vigente; si otra llamada ya lo renovo, usa ese. */
    private suspend fun renew(usedAccess: String): String = refreshMutex.withLock {
        val current = sessionStore.readAccessToken()
        if (!current.isNullOrBlank() && current != usedAccess) return@withLock current
        val refresh = sessionStore.readRefreshToken() ?: throw HttpException401()
        val renewed = authApi.refresh(RefreshRequestDto(refresh)).data ?: throw HttpException401()
        sessionStore.updateTokens(renewed.accessToken, renewed.refreshToken)
        renewed.accessToken
    }

    private fun bearer(token: String) = "Bearer $token"

    /** Marca interna para "no hay sesion valida". */
    private class HttpException401 : IllegalStateException("Sesion no valida")

    private fun Throwable.toPetsError(): AppError = when {
        this is HttpException401 -> AppError.Unauthorized
        this is HttpException && code() == 401 -> AppError.Unauthorized
        this is HttpException && code() == 403 -> AppError.Server(403)
        else -> toAppError()
    }
}
