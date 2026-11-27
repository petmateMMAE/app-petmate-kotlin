package mx.petmate.app.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Si el telefono tiene red (spec 001, T-035).
 *
 * Es un `Flow<Boolean>`: emite el estado actual al suscribirse y luego cada
 * cambio. **No sondea**: se registra en `ConnectivityManager` y espera a que el
 * sistema avise (la aceptacion pide explicitamente que no haya sondeo agresivo).
 */
interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}

/**
 * Implementacion real, sobre `ConnectivityManager`.
 *
 * Necesita `ACCESS_NETWORK_STATE`, que ya esta en el manifiesto. "Con red"
 * significa validada por el sistema: una wifi de hotel que todavia no deja
 * salir no cuenta, porque la app la trataria como buena y las peticiones
 * fallarian.
 */
class AndroidConnectivityObserver(private val context: Context) : ConnectivityObserver {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        if (manager == null) {
            // Sin el servicio no se puede saber: se asume con red para no
            // bloquear la app, y las peticiones reportaran el error de red.
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            private val available = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                available += network
                trySend(true)
            }

            override fun onLost(network: Network) {
                available -= network
                trySend(available.isNotEmpty())
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        trySend(manager.hasValidatedNetwork())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        manager.registerNetworkCallback(request, callback)

        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()

    private fun ConnectivityManager.hasValidatedNetwork(): Boolean {
        val network = activeNetwork ?: return false
        val capabilities = getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/**
 * Observador de prueba: la aceptacion de T-035 pide una implementacion de
 * prueba junto a la real.
 *
 * Empieza con red y se cambia con [set]. Vive en `main` y no en `test` porque
 * tambien lo usan las pruebas instrumentadas y las previsualizaciones.
 */
class FakeConnectivityObserver(initiallyOnline: Boolean = true) : ConnectivityObserver {

    private val state = MutableStateFlow(initiallyOnline)

    override val isOnline: Flow<Boolean> = state.asStateFlow()

    /** Prende o apaga la red. */
    fun set(online: Boolean) {
        state.value = online
    }
}
