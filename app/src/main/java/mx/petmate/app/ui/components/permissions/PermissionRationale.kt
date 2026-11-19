package mx.petmate.app.ui.components.permissions

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mx.petmate.app.R
import mx.petmate.app.domain.permissions.PermissionType

/** Etiquetas para las pruebas de interfaz. */
object PermissionRationaleTags {
    const val RATIONALE = "permission_rationale"
    const val SETTINGS = "permission_settings"
    const val CONFIRM = "permission_confirm"
    const val DISMISS = "permission_dismiss"
}

@StringRes
internal fun PermissionType.rationaleTitle(): Int = when (this) {
    PermissionType.Location -> R.string.permission_location_title
    PermissionType.Camera -> R.string.permission_camera_title
    PermissionType.Photos -> R.string.permission_photos_title
    PermissionType.Notifications -> R.string.permission_notifications_title
}

@StringRes
internal fun PermissionType.rationaleBody(): Int = when (this) {
    PermissionType.Location -> R.string.permission_location_body
    PermissionType.Camera -> R.string.permission_camera_body
    PermissionType.Photos -> R.string.permission_photos_body
    PermissionType.Notifications -> R.string.permission_notifications_body
}

/**
 * Explicacion propia antes del dialogo del sistema (spec 003, T-003, R-02). Quien la muestra decide cuando: si
 * el permiso ya esta concedido, `rememberPermissionRequest` no la pinta.
 */
@Composable
fun PermissionRationale(
    type: PermissionType,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PermissionDialog(
        title = stringResource(type.rationaleTitle()),
        body = stringResource(type.rationaleBody()),
        confirm = stringResource(R.string.permission_continue),
        onConfirm = onContinue,
        onDismiss = onNotNow,
        modifier = modifier.testTag(PermissionRationaleTags.RATIONALE),
    )
}

/** Tras dos negativas o una permanente: se ofrecen los ajustes de la app en vez de otro dialogo (R-04). */
@Composable
fun PermissionSettingsPrompt(
    onOpenSettings: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PermissionDialog(
        title = stringResource(R.string.permission_settings_title),
        body = stringResource(R.string.permission_settings_body),
        confirm = stringResource(R.string.permission_open_settings),
        onConfirm = onOpenSettings,
        onDismiss = onNotNow,
        modifier = modifier.testTag(PermissionRationaleTags.SETTINGS),
    )
}

@Composable
private fun PermissionDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(PermissionRationaleTags.CONFIRM)) {
                Text(confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(PermissionRationaleTags.DISMISS)) {
                Text(stringResource(R.string.permission_not_now))
            }
        },
        modifier = modifier,
    )
}
