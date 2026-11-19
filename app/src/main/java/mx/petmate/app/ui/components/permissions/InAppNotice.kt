package mx.petmate.app.ui.components.permissions

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mx.petmate.app.ui.components.PetMateSmallButton

/** Etiquetas para las pruebas de interfaz de las alternativas. */
object PermissionFallbackTags {
    const val NOTICE = "permission_notice"
    const val ACTION = "permission_notice_action"
}

/**
 * Aviso dentro de la interfaz (spec 003, T-008): explica que se pierde y como recuperarlo, sin bloquear la
 * pantalla. Las alternativas de cada permiso lo reutilizan.
 */
@Composable
fun InAppNotice(
    title: String,
    body: String,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(container, MaterialTheme.shapes.medium)
            .padding(14.dp)
            .testTag(PermissionFallbackTags.NOTICE),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = content)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (actionText != null) {
                PetMateSmallButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.padding(top = 4.dp).testTag(PermissionFallbackTags.ACTION),
                )
            }
        }
    }
}
