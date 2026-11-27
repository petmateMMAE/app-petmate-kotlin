package mx.petmate.app.data.push

import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.push.NotificationCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decide que hacer con un mensaje de push recibido (spec 001, T-039; spec 006, T-004 y T-005).
 *
 * Esta es **toda** la decision de un mensaje entrante. Vive aparte del
 * `FirebaseMessagingService` a proposito: un servicio de Android no se puede instanciar en una
 * prueba de JVM —lo construye el sistema y necesita un `Context` vivo—, asi que si la regla de
 * "no mostrar lo que el usuario apago" viviera dentro del servicio, no habria forma de probarla
 * sin un emulador. Aqui si, y el servicio queda como lo que pide D-19: una cascara que traduce y
 * delega.
 *
 * Lo que NO hace, tambien a proposito (es de spec 006, no de la base): no arma la notificacion de
 * Android, no elige canal ni sonido, y no navega. Devuelve una [PushRoute] y quien la pinta
 * decide.
 */
@Singleton
class PushMessageRouter @Inject constructor(
    private val preferences: NotificationPreferencesStore,
    private val logger: Logger,
) {

    /**
     * Que hacer con [payload].
     *
     * Se descarta en tres casos, en este orden:
     * 1. La categoria no se reconoce ([NotificationPresenter.present] devuelve null): puede venir
     *    de una version mas nueva de la app, y es mejor no mostrar nada que un aviso que no lleva
     *    a ningun lado.
     * 2. El usuario apago esa categoria. Se revisa **en el dispositivo** aunque el backend ya
     *    guarde las preferencias (`notification-settings/get` y `/update`): el servidor puede ir
     *    atrasado respecto de lo que la persona acaba de apagar, y entre los dos manda el telefono.
     *    [NotificationCategory.Security] nunca se puede apagar, asi que ese caso no existe (A-09).
     * 3. Ya se mostro ese mismo evento (`eventKey` repetido). FCM puede entregar el mismo mensaje
     *    mas de una vez: lo garantiza "al menos una vez", no "exactamente una".
     */
    suspend fun route(payload: Map<String, String>): PushRoute {
        val notification = NotificationPresenter.present(payload)
            ?: return PushRoute.Ignore(IgnoreReason.UnknownCategory)

        if (!isAllowed(notification.category)) {
            return PushRoute.Ignore(IgnoreReason.CategoryDisabled)
        }

        val eventKey = NotificationPresenter.eventKeyOf(payload)
        if (eventKey != null && !recordIfNew(eventKey)) {
            return PushRoute.Ignore(IgnoreReason.AlreadyShown)
        }

        return PushRoute.Show(notification, eventKey)
    }

    /**
     * Si no se pueden leer las preferencias, se **muestra**.
     *
     * Decidido asi y no al contrario: el unico aviso que no se puede apagar es el de seguridad
     * (inicio de sesion nuevo, cambio de contrasena), y callar ese por un fallo de DataStore es
     * peor que mostrar de mas uno que la persona habia apagado.
     */
    private suspend fun isAllowed(category: NotificationCategory): Boolean =
        runCatching { preferences.current().isEnabled(category) }
            .onFailure { logger.error(TAG, "No se pudieron leer las preferencias de notificacion", it) }
            .getOrDefault(true)

    /**
     * Claves ya mostradas, solo mientras el proceso viva.
     *
     * No se persiste a proposito: la entrega repetida de FCM ocurre en segundos, y guardar cada
     * clave en disco para siempre seria una tabla que solo crece. Si el proceso muere y Android
     * reentrega el mismo mensaje, se muestra dos veces; es un duplicado raro y visible, no un dato
     * perdido.
     */
    private val seen = LinkedHashSet<String>()

    /** `true` si [eventKey] es nueva; descarta la mas vieja al pasar de [MAX_RECORDADAS]. */
    private fun recordIfNew(eventKey: String): Boolean {
        if (!seen.add(eventKey)) return false
        if (seen.size > MAX_RECORDADAS) {
            val oldest = seen.iterator()
            oldest.next()
            oldest.remove()
        }
        return true
    }

    private companion object {
        const val TAG = "PushMessageRouter"

        /** Tope del historial de deduplicacion: suficiente para una rafaga, acotado en memoria. */
        const val MAX_RECORDADAS = 200
    }
}

/** Que hacer con un mensaje entrante. */
sealed interface PushRoute {

    /** Mostrarla. [eventKey] viaja para que quien la pinte pueda agrupar o reemplazar. */
    data class Show(val notification: PushNotification, val eventKey: String?) : PushRoute

    /** No mostrar nada. [reason] existe para el log y para las pruebas. */
    data class Ignore(val reason: IgnoreReason) : PushRoute
}

/** Por que se descarto un mensaje. */
enum class IgnoreReason {
    /** La categoria del payload no esta en [NotificationCategory]. */
    UnknownCategory,

    /** El usuario apago esa categoria. */
    CategoryDisabled,

    /** Ya se mostro un mensaje con ese `eventKey`. */
    AlreadyShown,
}
