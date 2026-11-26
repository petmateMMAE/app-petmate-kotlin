package mx.petmate.app.ui.components.permissions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mx.petmate.app.R
import mx.petmate.app.ui.theme.petMateColors

/**
 * Sin ubicacion (spec 003, T-006, R-05): el mapa sigue en su centro por defecto con lista y busqueda. El aviso
 * no bloquea y solo vuelve a pedir el permiso si el usuario toca [onUseLocation].
 */
@Composable
fun LocationFallback(onUseLocation: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.petMateColors
    InAppNotice(
        title = stringResource(R.string.permission_fallback_location_title),
        body = stringResource(R.string.permission_fallback_location_body),
        icon = R.drawable.ic_pm_locate,
        container = colors.brandSoft,
        content = colors.brandText,
        actionText = stringResource(R.string.permission_fallback_location_action),
        onAction = onUseLocation,
        modifier = modifier,
    )
}
