package mx.petmate.app.data.sync

import mx.petmate.app.core.result.AppError

/**
 * Qué hacer con una operación de la cola que falló (spec 005, T-005, R-05 y R-06).
 *
 * El criterio es uno y vale para todo: **si el servidor dijo que el dato está mal, reintentar no lo
 * va a arreglar.** Un 400 reenviado mil veces sigue siendo un 400, y mientras tanto tapa la cola y
 * gasta batería. Lo que sí vale la pena reintentar es lo que falló por el camino y no por el
 * contenido: el servidor caído, el tiempo agotado, la red cortada.
 *
 * Respuesta de Adrián del 27/11/2026 a Q-08: **4xx son permanentes; 5xx y red son transitorios.**
 */
sealed interface SyncRetryDecision {

    /** Volver a intentar después de [delayMillis]. */
    data class Retry(val delayMillis: Long) : SyncRetryDecision

    /**
     * No reintentar. Queda visible para que el usuario corrija o descarte (R-06).
     *
     * [reason] es para la bitácora y para la pantalla de pendientes, no para el usuario final: el
     * texto que lee el usuario lo elige la interfaz desde `strings.xml`.
     */
    data class Permanent(val reason: String) : SyncRetryDecision

    /**
     * Se agotaron los intentos de un error transitorio.
     *
     * Se separa de [Permanent] a propósito: aquí el dato probablemente está bien y lo que falló es
     * la red, así que el reintento manual tiene sentido. En [Permanent] no lo tiene hasta que
     * alguien corrija el dato.
     */
    data class GaveUp(val reason: String) : SyncRetryDecision
}

/**
 * Decide reintento o error permanente, y con cuánta espera.
 *
 * La espera crece al doble en cada intento desde [baseDelayMillis] y se topa en [maxDelayMillis],
 * para no castigar al servidor que ya está en problemas ni dejar al usuario esperando media hora.
 * Es la misma forma de esperar que `RetryInterceptor` (001/T-033), pero vive aparte: ahí se decide
 * sobre **una petición** y aquí sobre **una operación de la cola**, que puede sobrevivir al cierre
 * de la app y llevar la cuenta de sus intentos en Room.
 */
class SyncRetryPolicy(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelayMillis: Long = DEFAULT_BASE_DELAY_MILLIS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS,
) {

    /**
     * Qué hacer con [error] en el intento número [attempts] (1 es el primero que ya falló).
     *
     * `AppError.Validation` también es permanente: el dato no pasa la validación del servidor, y
     * reenviarlo tal cual da el mismo resultado.
     */
    fun decide(error: AppError, attempts: Int): SyncRetryDecision = when {
        esPermanente(error) -> SyncRetryDecision.Permanent(describe(error))
        attempts >= maxAttempts -> SyncRetryDecision.GaveUp(describe(error))
        else -> SyncRetryDecision.Retry(esperaPara(attempts))
    }

    /** Si [error] no se arregla reintentando. */
    fun esPermanente(error: AppError): Boolean = when (error) {
        is AppError.Validation -> true
        // Unauthorized es aparte: no es el dato el que está mal, es la sesión. Quien lo atiende es
        // el renovador de sesión, no la cola, así que aquí se trata como permanente para que la
        // operación no se reenvíe en bucle con un token vencido.
        AppError.Unauthorized -> true
        is AppError.Server -> error.code in RANGO_4XX
        AppError.Network, AppError.Timeout -> false
        is AppError.Unknown -> false
    }

    /** Espera antes del intento siguiente, al doble cada vez y con tope. */
    fun esperaPara(attempts: Int): Long {
        val intento = attempts.coerceAtLeast(1)
        // Se topa el exponente antes de desplazar: con attempts grande, `shl` desborda y da negativo.
        val exponente = (intento - 1).coerceAtMost(MAX_EXPONENTE)
        return (baseDelayMillis shl exponente).coerceAtMost(maxDelayMillis)
    }

    private fun describe(error: AppError): String = when (error) {
        is AppError.Server -> "El servidor respondio ${error.code}"
        is AppError.Validation -> "El campo ${error.field} no es valido (${error.reason})"
        AppError.Unauthorized -> "La sesion ya no es valida"
        AppError.Network -> "Sin conexion"
        AppError.Timeout -> "El servidor tardo en responder"
        is AppError.Unknown -> "Fallo no previsto"
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 5
        const val DEFAULT_BASE_DELAY_MILLIS = 1_000L
        const val DEFAULT_MAX_DELAY_MILLIS = 60_000L

        private const val MAX_EXPONENTE = 16
        private val RANGO_4XX = 400..499
    }
}
