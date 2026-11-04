package mx.petmate.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.petmate.app.ui.theme.petMateColors

/** Variantes de boton de la maqueta (.btn.primary, .outline, .ghost, .soft). */
enum class PetMateButtonStyle { Primary, Outline, Ghost, Soft }

/**
 * Boton de ancho completo de PetMate: 54 dp de alto y esquinas de 16 dp.
 *
 * Con [loading] muestra un indicador en lugar del texto y no responde.
 */
@Composable
fun PetMateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PetMateButtonStyle = PetMateButtonStyle.Primary,
    @DrawableRes icon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.petMateColors
    val shape = MaterialTheme.shapes.medium

    val container: Color
    val content: Color
    var border: BorderStroke? = null
    var minHeight: Dp = 54.dp
    when (style) {
        PetMateButtonStyle.Primary -> {
            container = colors.primary
            content = colors.onPrimary
        }
        PetMateButtonStyle.Outline -> {
            container = colors.surface
            content = colors.primary
            border = BorderStroke(2.dp, colors.primary)
        }
        PetMateButtonStyle.Ghost -> {
            container = Color.Transparent
            content = colors.onBackground
            minHeight = 48.dp
        }
        PetMateButtonStyle.Soft -> {
            container = extra.brandSoft
            content = extra.brandText
        }
    }

    val shadowModifier = if (style == PetMateButtonStyle.Primary && enabled) {
        Modifier.shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = colors.primary,
            spotColor = colors.primary,
        )
    } else {
        Modifier
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = if (loading) 1f else 0.5f),
            disabledContentColor = content.copy(alpha = if (loading) 1f else 0.6f),
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 18.dp),
        modifier = modifier
            .then(shadowModifier)
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = content,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(text = text, style = MaterialTheme.typography.titleMedium)
                if (trailingIcon != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(trailingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Boton chico de fondo suave (.btn.small.soft), por ejemplo "Activar". */
@Composable
fun PetMateSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.petMateColors.brandSoft,
        contentColor = MaterialTheme.petMateColors.brandText,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
