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
 *
 * Codigos del backend v0.1.0: 400 validacion, 401 credenciales o sesion
 * invalidas, 403 sin permiso, 409 correo ya registrado, 429 demasiados
 * intentos, 503 dependencia del servidor no disponible.
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
