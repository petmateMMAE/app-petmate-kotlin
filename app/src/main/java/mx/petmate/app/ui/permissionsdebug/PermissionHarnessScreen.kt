package mx.petmate.app.ui.permissionsdebug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.domain.permissions.PermissionType
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.components.permissions.CameraFallback
import mx.petmate.app.ui.components.permissions.LocationFallback
import mx.petmate.app.ui.components.permissions.NotificationFallback
import mx.petmate.app.ui.components.permissions.PhotoFallback
import mx.petmate.app.ui.components.permissions.rationaleTitle
import mx.petmate.app.ui.theme.PetMateEdelTheme

/** Etiqueta del interruptor de cada permiso. */
fun permissionSwitchTag(type: PermissionType) = "permission_switch_${type.name}"

/**
 * Pantalla de prueba de la matriz de permisos (spec 003, T-009). Alterna cada permiso entre concedido y negado
 * y muestra su alternativa. **No** esta en el grafo de navegacion de producto: solo la usan las pruebas y las
 * vistas previas.
 */
@Composable
fun PermissionHarnessScreen(modifier: Modifier = Modifier) {
    val granted = remember {
        mutableStateMapOf<PermissionType, Boolean>().apply { PermissionType.entries.forEach { put(it, true) } }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(stringResource(R.string.permission_harness_title), style = MaterialTheme.typography.titleLarge)
        PermissionType.entries.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(type.rationaleTitle()), modifier = Modifier.weight(1f))
                Switch(
                    checked = granted.getValue(type),
                    onCheckedChange = { granted[type] = it },
                    modifier = Modifier.testTag(permissionSwitchTag(type)),
                )
            }
            if (!granted.getValue(type)) {
                when (type) {
                    PermissionType.Location -> LocationFallback(onUseLocation = {})
                    PermissionType.Camera -> CameraFallback(onRetry = {})
                    PermissionType.Photos -> PhotoFallback(name = "Luna", onPickAgain = {})
                    PermissionType.Notifications -> NotificationFallback(onEnable = {})
                }
            }
        }
    }
}

@PetMatePreviews
@Composable
private fun PermissionHarnessPreview() {
    PetMateEdelTheme { Surface { PermissionHarnessScreen() } }
}
