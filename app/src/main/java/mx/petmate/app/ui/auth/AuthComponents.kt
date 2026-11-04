package mx.petmate.app.ui.auth

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.ui.theme.petMateColors

/** Titulo grande y texto de apoyo de login y registro (.big-title + .lead). */
@Composable
internal fun AuthHeader(title: String, lead: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
        )
        Text(
            text = lead,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.petMateColors.ink2,
            modifier = Modifier.padding(bottom = 18.dp),
        )
    }
}

/** Enlace en texto morado y negritas (.link de la maqueta). */
@Composable
internal fun AuthLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.petMateColors.brandText,
        )
    }
}

/** Renglon centrado "¿No tienes cuenta? Crear cuenta". */
@Composable
internal fun AuthSwitchRow(
    question: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.petMateColors.ink2,
        )
        AuthLink(text = action, onClick = onClick, enabled = enabled)
    }
}

/**
 * Caja gris con la cuenta de prueba (.test-acc de la maqueta). Solo se muestra
 * mientras BuildConfig.USE_FAKE_BACKEND sea true.
 */
@Composable
internal fun DemoAccountsNotice(modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(extra.surface2, MaterialTheme.shapes.medium)
            .padding(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pm_info),
            contentDescription = null,
            tint = extra.ink2,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp),
        )
        val title = stringResource(R.string.login_demo_accounts_title)
        val body = stringResource(R.string.demo_notice)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(title)
                }
                append(" ")
                append(body)
            },
            style = MaterialTheme.typography.bodySmall,
            color = extra.ink2,
        )
    }
}
