package mx.petmate.app.ui.components.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.ui.components.PetMateSmallButton
import mx.petmate.app.ui.theme.petMateColors

/** Etiqueta para las pruebas de interfaz. */
const val PHOTO_FALLBACK_INITIALS_TAG = "photo_fallback_initials"

/** Hasta dos iniciales de [name]: "Luna" -> "L", "Don Gato" -> "DG". */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }

/**
 * Sin foto (spec 003, T-007, R-07): la mascota se muestra con sus iniciales y se puede volver a elegir.
 * El selector de fotos del sistema no pide permiso, asi que esto cubre tambien cuando el usuario cancela.
 */
@Composable
fun PhotoFallback(name: String, onPickAgain: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.petMateColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .background(colors.brandSoft, CircleShape)
                .testTag(PHOTO_FALLBACK_INITIALS_TAG),
        ) {
            Text(initialsOf(name), style = MaterialTheme.typography.headlineMedium, color = colors.brandText)
        }
        Text(
            stringResource(R.string.permission_fallback_photo_body),
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink2,
        )
        PetMateSmallButton(
            text = stringResource(R.string.permission_fallback_photo_action),
            onClick = onPickAgain,
            modifier = Modifier.testTag(PermissionFallbackTags.ACTION),
        )
    }
}
