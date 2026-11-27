package mx.petmate.app.data.remote

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Reintenta una peticion solo cuando la culpa no es del cliente (001/T-033, C-24, C-30).
 *
 * Que se reintenta y que no:
 *  - corte de red y timeout ([IOException]) y respuestas 5xx: se reintentan con espera
 *    exponencial.
 *  - 4xx: **nunca** se reintentan. El servidor ya dijo que la peticion esta mal; repetirla da
 *    el mismo error, gasta bateria del usuario y puede duplicar algo.
 *
 * Por que hace falta con el backend en Azure Container Apps: el balanceador corta conexiones y
 * devuelve 502 o 503 mientras mueve una replica. Eso no es una falla de la app y no tiene por que
 * llegarle al usuario como un error.
 *
 * No hay certificate pinning a proposito (lo pide T-033): el certificado del balanceador de Azure
 * rota sin avisarnos, y una app con el pin viejo deja de funcionar sin que podamos arreglarlo
 * desde el servidor.
 *
 * [sleeper] se inyecta para que las pruebas no tengan que esperar de verdad.
 */
class RetryInterceptor(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelayMillis: Long = DEFAULT_BASE_DELAY_MILLIS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS,
    private val sleeper: (Long) -> Unit = { millis -> Thread.sleep(millis) },
) : Interceptor {

    init {
        require(maxAttempts >= 1) { "maxAttempts debe ser al menos 1, no $maxAttempts" }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var ultimaFalla: IOException? = null

        for (intento in 0 until maxAttempts) {
            if (intento > 0) sleeper(esperaPara(intento))

            val respuesta = try {
                chain.proceed(request)
            } catch (e: IOException) {
                ultimaFalla = e
                null
            }

            if (respuesta != null) {
                val esElUltimoIntento = intento == maxAttempts - 1
                if (!seReintenta(respuesta.code) || esElUltimoIntento) return respuesta
                // Hay que cerrar el cuerpo antes de reintentar: si no, la conexion se queda
                // tomada y OkHttp acaba agotando el pool.
                respuesta.close()
            }
        }

        throw ultimaFalla
            ?: IOException("No se pudo completar la peticion despues de $maxAttempts intentos")
    }

    /** Espera antes del intento numero [intento] (1 = primer reintento): base, base*2, base*4... */
    internal fun esperaPara(intento: Int): Long {
        val factor = 1L shl (intento - 1)
        return (baseDelayMillis * factor).coerceAtMost(maxDelayMillis)
    }

    private fun seReintenta(codigo: Int): Boolean = codigo in 500..599

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        const val DEFAULT_BASE_DELAY_MILLIS = 500L
        const val DEFAULT_MAX_DELAY_MILLIS = 4_000L
    }
}
