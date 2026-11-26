package mx.petmate.app.ui.components.permissions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mx.petmate.app.R
import mx.petmate.app.ui.theme.petMateColors

/**
 * Sin camara (spec 003, T-007, R-06): se escribe a mano el codigo de 8 caracteres del QR (Q-02, el formato que
 * ya valida `ScanViewModel`). [onRetry] vuelve a pedir la camara.
 */
@Composable
fun CameraFallback(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.petMateColors
    InAppNotice(
        title = stringResource(R.string.permission_fallback_camera_title),
        body = stringResource(R.string.permission_fallback_camera_body),
        icon = R.drawable.ic_pm_qr,
        container = colors.sunSoft,
        content = colors.sunInk,
        actionText = stringResource(R.string.permission_fallback_camera_action),
        onAction = onRetry,
        modifier = modifier,
    )
}
