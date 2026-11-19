package mx.petmate.app.core.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.petmate.app.domain.permissions.PermissionCatalog
import mx.petmate.app.domain.permissions.PermissionGateway
import mx.petmate.app.domain.permissions.PermissionStatus
import mx.petmate.app.domain.permissions.PermissionType

/**
 * Gateway con las APIs del sistema (spec 003, T-002).
 *
 * `checkSelfPermission` lee una tabla en memoria del sistema, no disco: no bloquea el hilo principal (R-12).
 * Las negativas se recuerdan mientras vive el proceso; al volver, el sistema mismo dice si sigue negado.
 */
class AndroidPermissionGateway(private val context: Context) : PermissionGateway {

    private val denials = mutableMapOf<PermissionType, PermissionStatus>()
    private val states = PermissionType.entries.associateWith { MutableStateFlow(read(it)) }

    override fun status(type: PermissionType): PermissionStatus = read(type).also { states.getValue(type).value = it }

    override fun observe(type: PermissionType): Flow<PermissionStatus> = states.getValue(type).asStateFlow()

    @SuppressLint("InlinedApi") // POST_NOTIFICATIONS solo se usa en API 33+.
    override fun systemPermissions(type: PermissionType): List<String> {
        if (!PermissionCatalog.ruleFor(type).needsSystemDialog) return emptyList()
        return when (type) {
            PermissionType.Location -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            PermissionType.Camera -> listOf(Manifest.permission.CAMERA)
            PermissionType.Notifications ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList()
                }
            PermissionType.Photos -> emptyList()
        }
    }

    override fun onSystemResult(type: PermissionType, granted: Boolean, canAskAgain: Boolean) {
        if (granted) {
            denials.remove(type)
        } else {
            denials[type] = if (canAskAgain) PermissionStatus.Denied else PermissionStatus.PermanentlyDenied
        }
        states.getValue(type).value = read(type)
    }

    override fun refresh() {
        PermissionType.entries.forEach { states.getValue(it).value = read(it) }
    }

    private fun read(type: PermissionType): PermissionStatus {
        val granted = when (type) {
            // Antes de API 33 no hay dialogo, pero el usuario puede apagar las notificaciones en ajustes.
            PermissionType.Notifications -> NotificationManagerCompat.from(context).areNotificationsEnabled()
            // Con la aproximada basta para ordenar por cercania: cualquiera de las dos cuenta.
            else -> systemPermissions(type).let { perms -> perms.isEmpty() || perms.any(::isGranted) }
        }
        return if (granted) PermissionStatus.Granted else denials[type] ?: PermissionStatus.NotRequested
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
