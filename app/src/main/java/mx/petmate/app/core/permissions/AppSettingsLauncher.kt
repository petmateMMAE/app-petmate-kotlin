package mx.petmate.app.core.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Abre la pantalla de ajustes de PetMate (spec 003, T-005). Al volver, `rememberPermissionRequest` vuelve a
 * consultar el permiso y activa la funcion sin reiniciar.
 *
 * Devuelve falso si el sistema no tiene esa pantalla; quien llama deja el permiso como negado.
 */
fun Context.openAppSettings(): Boolean = try {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    true
} catch (_: ActivityNotFoundException) {
    false
}
