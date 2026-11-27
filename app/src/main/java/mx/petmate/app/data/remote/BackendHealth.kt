package mx.petmate.app.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger

/** En qué estado contestó el backend. */
enum class BackendState {
    /** Contestó que está listo para atender. */
    Ready,

    /** Está vivo pero todavía no listo: arrancando, o con una dependencia caída. */
    Starting,

    /** No contestó, o contestó un error. Puede ser el servidor o puede ser la red del teléfono. */
    Unreachable,
}

/**
 * Comprobación de salud del backend al arrancar (spec 001, T-033; R-25 y G-09).
 *
 * **Decisión del 27/11/2026, y es lo importante de este archivo: esta comprobación NO bloquea el
 * arranque.** La tarea dejaba la pregunta abierta ("¿no arrancar? ¿avisar y seguir?") y la respuesta
 * sale del resto del proyecto: la app es offline-first (spec 005), guarda mascotas y carnet en Room
 * y tiene modo invitado con datos de ejemplo. Una app así que se niegue a abrir porque un endpoint
 * de salud no contestó es **peor** que una que abre y muestra lo que tiene guardado. Y el caso
 * común no es que Azure esté caído: es que el dueño está en el veterinario sin señal.
 *
 * Así que esto solo **informa**: devuelve el estado y lo deja en la bitácora. Quien decida mostrar
 * un aviso es la interfaz, y puede no mostrar nada.
 *
 * Para qué sirve entonces: el balanceador de Azure Container Apps apaga los contenedores sin uso y
 * el primero que entra se come el arranque en frío (G-09). Pegarle a `health/ready` al abrir la app
 * levanta el contenedor mientras el usuario mira la primera pantalla, así que su primera consulta
 * de verdad ya encuentra el servidor despierto.
 *
 * Por eso nunca lanza: una comprobación que tira la app al arrancar es el peor fallo posible, y el
 * punto de todo esto es justo lo contrario.
 */
class BackendHealth(
    private val api: HealthApiService,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) {

    /**
     * Pregunta por la salud del backend. Nunca lanza y nunca bloquea nada.
     *
     * Se pregunta por `ready` y no por `live` porque `live` puede estar en 200 mientras el servidor
     * todavía no atiende consultas, y eso no le sirve de nada a la app.
     */
    suspend fun check(): BackendState = withContext(dispatchers.io) {
        try {
            val respuesta = api.ready()
            when {
                respuesta.isSuccessful -> BackendState.Ready
                // 503 es lo que contesta un contenedor que está arrancando. No es un error nuestro.
                respuesta.code() == CODIGO_NO_DISPONIBLE -> BackendState.Starting
                else -> BackendState.Unreachable
            }.also { estado -> logger.debug(ETIQUETA, "Backend: $estado (HTTP ${respuesta.code()})") }
        } catch (cancelacion: CancellationException) {
            // Se rethrow a propósito: cancelar no es un fallo del backend, y atraparlo rompería la
            // corrutina que lo canceló.
            throw cancelacion
        } catch (fallo: Throwable) {
            logger.debug(ETIQUETA, "Backend inalcanzable: ${fallo.javaClass.simpleName}")
            BackendState.Unreachable
        }
    }

    private companion object {
        const val ETIQUETA = "BackendHealth"
        const val CODIGO_NO_DISPONIBLE = 503
    }
}
