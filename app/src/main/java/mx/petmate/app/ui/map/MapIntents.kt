package mx.petmate.app.ui.map

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.CancellationSignal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import mx.petmate.app.domain.permissions.PermissionStatus
import mx.petmate.app.domain.permissions.PermissionType
import mx.petmate.app.ui.components.permissions.permissionStatus
import mx.petmate.app.ui.components.permissions.rememberPermissionRequest

/* Acciones del mapa que abren otras apps o leen la ubicacion del telefono. */

/**
 * Devuelve una funcion que lee la ubicacion. Con `ask = true` pide el permiso si falta;
 * con `false` solo la lee si ya se concedio. Sin permiso o sin lectura, avisa con null.
 *
 * El pedido pasa por `rememberPermissionRequest` (spec 003): explicacion previa, a la segunda negativa los
 * ajustes de la app, y al volver de ajustes con permiso se lee la ubicacion sin reiniciar.
 */
@Composable
fun rememberLocationRequest(onResult: (Double?, Double?) -> Unit): (ask: Boolean) -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val request = rememberPermissionRequest(
        type = PermissionType.Location,
        onGranted = { readLocation(context) { callback(it?.latitude, it?.longitude) } },
        onDenied = { callback(null, null) },
    )
    return remember(context, request) {
        { ask ->
            when {
                context.hasLocationPermission() -> readLocation(context) { callback(it?.latitude, it?.longitude) }
                ask -> request()
                else -> callback(null, null)
            }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean = permissionStatus(PermissionType.Location) == PermissionStatus.Granted

@SuppressLint("MissingPermission") // Se llama solo con el permiso concedido.
private fun readLocation(context: Context, onLocation: (Location?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return onLocation(null)
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    val last = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
    if (last != null) return onLocation(last)
    val provider = providers.firstOrNull() ?: return onLocation(null)
    LocationManagerCompat.getCurrentLocation(manager, provider, CancellationSignal(), ContextCompat.getMainExecutor(context)) { onLocation(it) }
}

fun Context.dial(phone: String) {
    if (phone.isBlank()) return
    safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}")))
}

/** wa.me necesita el numero con lada de pais: los de 10 digitos son de Mexico (+52). */
fun Context.openWhatsApp(phone: String) {
    val digits = phone.filter { it.isDigit() }
    if (digits.isEmpty()) return
    val full = if (digits.length == 10) "52$digits" else digits
    safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$full")))
}

fun Context.openDirections(latitude: Double, longitude: Double, label: String) {
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})"))
    if (!safeStart(geo)) {
        safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=17/$latitude/$longitude")))
    }
}

private fun Context.safeStart(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}
