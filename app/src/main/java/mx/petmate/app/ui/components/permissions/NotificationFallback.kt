package mx.petmate.app.ui.components.permissions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mx.petmate.app.R
import mx.petmate.app.ui.theme.petMateColors

/**
 * Sin notificaciones (spec 003, T-008, R-08): la app sigue usable y los avisos se ven dentro de la interfaz.
 * El permiso solo se pide si el usuario toca [onEnable] en la pantalla que activa notificaciones (006 o 017),
 * nunca al arrancar.
 */
@Composable
fun NotificationFallback(onEnable: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.petMateColors
    InAppNotice(
        title = stringResource(R.string.permission_fallback_notifications_title),
        body = stringResource(R.string.permission_fallback_notifications_body),
        icon = R.drawable.ic_pm_bell,
        container = colors.surface2,
        content = colors.ink2,
        actionText = stringResource(R.string.permission_fallback_notifications_action),
        onAction = onEnable,
        modifier = modifier,
    )
}
