package mx.petmate.app.data.push

import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.DeviceApiService
import mx.petmate.app.data.remote.DeviceTokenRequestDto
import mx.petmate.app.domain.push.DeviceRegistration
import mx.petmate.app.domain.push.PushTokenProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registra y da de baja el token de este dispositivo en el backend (spec 001, T-039; D-19).
 *
 * El ciclo es el que fija D-19: **tras el login** se pide el token y se manda a
 * `devices/register`; **en el logout** se da de baja en `devices/unregister` y se borra en el
 * proveedor. El token sin sesion no sirve para nada, y un token que sobrevive al logout hace que
 * Firebase siga mandando avisos de la cuenta anterior a un telefono donde ya entro otra persona
 * (P9).
 *
 * **Nada de esto puede impedir entrar ni salir de la app.** Ninguno de los dos metodos lanza ni
 * devuelve error: el push es un extra, y si el registro falla la persona sigue usando la app sin
 * notificaciones. Por eso el resultado solo se registra en el log. Es la misma regla que ya aplica
 * `logout()` en `AuthRepositoryImpl`, donde lo local se borra aunque el servidor falle (spec 002,
 * C-11).
 *
 * Firebase no se menciona aqui: vive detras de [PushTokenProvider]. Mientras Q-08 siga abierta
 * —que proyecto de Firebase se usa y quien administra `google-services.json`— el proveedor
 * inyectado es `NoPushTokenProvider`, que devuelve `null`, y entonces estos dos metodos no hacen
 * nada. Eso es exactamente D-19: la app arranca y funciona igual sin Firebase configurado.
 */
@Singleton
class PushRegistrar @Inject constructor(
    private val tokenProvider: PushTokenProvider,
    private val deviceApi: DeviceApiService,
    private val caller: AuthorizedCaller,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : DeviceRegistration {

    /**
     * Manda el token de este dispositivo al backend. Se llama **despues** de que la sesion quedo
     * guardada, porque [AuthorizedCaller] necesita el access token para la cabecera.
     *
     * Si no hay token (sin Firebase, o Firebase fallo) no se llama al backend: mandar un registro
     * vacio solo gastaria una peticion.
     */
    override suspend fun register() {
        val token = currentTokenOrNull() ?: return
        val result = caller.call { bearer -> deviceApi.register(bearer, DeviceTokenRequestDto(token)) }
        // No se propaga: el push es opcional y no puede romper el login.
        result.onFailureLog("No se pudo registrar el dispositivo para notificaciones")
    }

    /**
     * Da de baja el token y lo borra en el proveedor.
     *
     * **El orden importa.** Primero la baja en el backend y despues el borrado local, porque la
     * peticion necesita el token para decir *cual* de los telefonos de la persona se da de baja:
     * si se borrara antes, no habria nada que mandar y el backend seguiria teniendo el token
     * registrado.
     *
     * El borrado local corre **aunque la baja remota falle**. Sin internet, la baja no se logra,
     * pero el telefono si tiene que dejar de recibir: igual que el logout, lo local se limpia
     * siempre.
     */
    override suspend fun unregister() {
        val token = currentTokenOrNull()
        if (token != null) {
            val result = caller.call { bearer -> deviceApi.unregister(bearer, DeviceTokenRequestDto(token)) }
            result.onFailureLog("No se pudo dar de baja el dispositivo para notificaciones")
        }
        deleteTokenQuietly()
    }

    /**
     * El token, o `null`.
     *
     * El contrato de [PushTokenProvider] dice que no lanza, pero el `runCatching` se queda a
     * proposito: la implementacion real sera un envoltorio de `FirebaseMessaging`, y basta una
     * version del SDK que lance desde el hilo equivocado para tumbar el login. Una promesa en un
     * KDoc no es una garantia del compilador.
     */
    private suspend fun currentTokenOrNull(): String? = withContext(dispatchers.io) {
        runCatching { tokenProvider.currentToken() }
            .onFailure { logger.error(TAG, "El proveedor de token de push fallo", it) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private suspend fun deleteTokenQuietly() = withContext(dispatchers.io) {
        runCatching { tokenProvider.deleteToken() }
            .onFailure { logger.error(TAG, "No se pudo borrar el token de push", it) }
        Unit
    }

    private fun <T> AppResult<T>.onFailureLog(message: String) {
        if (this is AppResult.Failure) {
            // Solo el tipo de error, nunca el token: es un dato de la sesion (P6).
            logger.error(TAG, "$message: $error")
        }
    }

    private companion object {
        const val TAG = "PushRegistrar"
    }
}
