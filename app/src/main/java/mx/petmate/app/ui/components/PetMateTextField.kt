package mx.petmate.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.ui.theme.petMateColors

/**
 * Campo de texto de la maqueta: etiqueta arriba, icono a la izquierda,
 * borde de 2 dp que se pinta de morado al enfocar y de rojo con error.
 *
 * Con [isPassword] agrega el boton del ojo para mostrar u ocultar.
 */
@Composable
fun PetMateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    @DrawableRes leadingIcon: Int? = null,
    required: Boolean = false,
    hint: String? = null,
    error: UiText? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    bottomSpacing: Dp = 14.dp,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.petMateColors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val hasError = error != null

    val borderColor = when {
        hasError -> extra.alert
        focused -> colors.primary
        else -> extra.line
    }
    val fieldBackground = if (hasError) extra.alertSoft else colors.surface
    val shape = MaterialTheme.shapes.small

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomSpacing),
    ) {
        val requiredMark = stringResource(R.string.field_required_mark)
        Text(
            text = buildAnnotatedString {
                append(label)
                if (required) {
                    append(" ")
                    withStyle(SpanStyle(color = extra.alert)) { append(requiredMark) }
                }
            },
            style = MaterialTheme.typography.titleSmall,
            color = colors.onBackground,
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .background(fieldBackground, shape)
                        .border(2.dp, borderColor, shape)
                        .padding(start = 12.dp, end = if (isPassword) 2.dp else 12.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            painter = painterResource(leadingIcon),
                            contentDescription = null,
                            tint = extra.ink3,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = extra.ink3,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    if (isPassword) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(
                                    if (passwordVisible) R.drawable.ic_pm_eye_off else R.drawable.ic_pm_eye,
                                ),
                                contentDescription = stringResource(
                                    if (passwordVisible) R.string.field_hide_password else R.string.field_show_password,
                                ),
                                tint = extra.ink2,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
        )

        when {
            error != null -> FieldError(error.asString())
            hint != null -> Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = extra.ink2,
            )
        }
    }
}

/** Mensaje de error bajo un campo: icono de alerta y texto rojo. */
@Composable
fun FieldError(text: String, modifier: Modifier = Modifier) {
    val alert = MaterialTheme.petMateColors.alert
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pm_alert),
            contentDescription = null,
            tint = alert,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = alert,
        )
    }
}

/** Aviso de error general (caja roja suave con icono), como en el login de la maqueta. */
@Composable
fun ErrorNotice(text: String, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .background(extra.alertSoft, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pm_alert),
            contentDescription = null,
            tint = extra.alert,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
