package mx.petmate.app.ui.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.domain.model.LegalLinks
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.ui.components.PetMateBackBar

/**
 * Ajustes del usuario (spec 017, T-006 y T-008). Se abre desde el perfil.
 *
 * Lista las categorias de notificacion de 006, los enlaces legales y el acceso a cerrar sesion. No reimplementa
 * nada: los interruptores escriben por el repositorio y cerrar sesion usa el caso de uso de 002.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenLegal: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Cerrada la sesion, navegar es responsabilidad de quien llama: la pantalla solo avisa.
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Column(modifier = modifier.fillMaxSize()) {
        PetMateBackBar(onBackClick = onBack)

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SectionHeader(stringResource(R.string.settings_notifications_header))
            state.toggles.forEach { toggle ->
                CategoryRow(
                    label = stringResource(labelOf(toggle.category)),
                    // Seguridad explica por que no se puede apagar, en vez de dejar un interruptor muerto.
                    caption = if (toggle.canBeDisabled) {
                        null
                    } else {
                        stringResource(R.string.settings_category_security_locked)
                    },
                    isOn = toggle.isOn,
                    enabled = toggle.canBeDisabled,
                    onChange = { viewModel.toggle(toggle.category, it) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SectionHeader(stringResource(R.string.settings_legal_header))
            LegalSection(links = state.legalLinks, onOpenLegal = onOpenLegal)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SectionHeader(stringResource(R.string.settings_session_header))
            Text(
                text = stringResource(R.string.settings_log_out),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.isLoggingOut) { viewModel.logOut() }
                    .padding(vertical = 14.dp),
            )

            state.error?.let { error ->
                Text(
                    text = stringResource(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun CategoryRow(
    label: String,
    caption: String?,
    isOn: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            caption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = isOn,
            onCheckedChange = onChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun LegalSection(links: LegalLinks, onOpenLegal: (String) -> Unit) {
    // Sin URLs reales (Q-02) no se dibuja un enlace que llevaria a ninguna parte: se dice que faltan.
    if (!links.isConfigured) {
        Text(
            text = stringResource(R.string.settings_legal_pending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        return
    }

    LegalLink(stringResource(R.string.settings_legal_privacy)) { onOpenLegal(links.privacyUrl!!) }
    LegalLink(stringResource(R.string.settings_legal_terms)) { onOpenLegal(links.termsUrl!!) }
}

@Composable
private fun LegalLink(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

private fun labelOf(category: NotificationCategory): Int = when (category) {
    NotificationCategory.Health -> R.string.settings_category_health
    NotificationCategory.FollowUp -> R.string.settings_category_followup
    NotificationCategory.Chat -> R.string.settings_category_chat
    NotificationCategory.News -> R.string.settings_category_news
    NotificationCategory.Security -> R.string.settings_category_security
}
