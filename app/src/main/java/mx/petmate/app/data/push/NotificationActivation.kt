package mx.petmate.app.data.push

import mx.petmate.app.domain.permissions.PermissionGateway
import mx.petmate.app.domain.permissions.PermissionStatus
import mx.petmate.app.domain.permissions.PermissionType
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences

/** Que paso al mover un interruptor de notificaciones. */
sealed interface ActivationResult {
    val preferences: NotificationPreferences?

    /** Guardado; no hace falta nada mas. */
    data class Saved(override val preferences: NotificationPreferences) : ActivationResult

    /**
     * Guardado, pero el sistema no deja mostrar notificaciones: la pantalla pide el permiso con el flujo de 003
     * (`rememberPermissionRequest(PermissionType.Notifications)`). Si se niega, la categoria se queda encendida y
     * la pantalla muestra `NotificationFallback` (R-07, C-07, C-08).
     */
    data class SavedNeedsPermission(override val preferences: NotificationPreferences) : ActivationResult

    /** Se intento apagar seguridad: no cambia nada (A-09). */
    data object Locked : ActivationResult {
        override val preferences: NotificationPreferences? = null
    }
}

/**
 * Encender o apagar una categoria (spec 006, T-009). El permiso se pide aqui, cuando el usuario enciende una
 * categoria, y nunca al arrancar (R-07; 003 R-01).
 */
class NotificationActivation(
    private val store: NotificationPreferencesStore,
    private val permissions: PermissionGateway,
) {

    suspend fun set(category: NotificationCategory, on: Boolean): ActivationResult {
        if (!on && !NotificationPreferences.canBeDisabled(category)) return ActivationResult.Locked
        val saved = store.set(category, on)
        val allowed = permissions.status(PermissionType.Notifications) == PermissionStatus.Granted
        return if (on && !allowed) ActivationResult.SavedNeedsPermission(saved) else ActivationResult.Saved(saved)
    }
}
