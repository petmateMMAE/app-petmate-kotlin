package mx.petmate.app.ui.components.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.domain.sync.SyncStatus
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.components.PetMateSmallButton
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/** Etiquetas para las pruebas de interfaz. */
object SyncStatusIndicatorTags {
    const val BADGE = "sync_status_badge"
    const val RETRY = "sync_status_retry"
}

/**
 * Indicador de sincronizacion de un elemento (spec 005, T-008, C-11).
 *
 * Sin estado propio: recibe el [status] y avisa con [onRetry]. Si el elemento ya esta sincronizado no pinta
 * nada, para no llenar las tarjetas de etiquetas. Solo el error ofrece "Reintentar".
 */
@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.petMateColors
    when (status) {
        SyncStatus.Synced -> Unit
        SyncStatus.Pending -> PetMateBadge(
            text = stringResource(R.string.sync_status_pending),
            container = colors.sunSoft,
            content = colors.sunInk,
            icon = R.drawable.ic_pm_calendar,
            modifier = modifier.testTag(SyncStatusIndicatorTags.BADGE),
        )
        SyncStatus.Syncing -> PetMateBadge(
            text = stringResource(R.string.sync_status_syncing),
            container = colors.brandSoft,
            content = colors.brandText,
            icon = R.drawable.ic_pm_refresh,
            modifier = modifier.testTag(SyncStatusIndicatorTags.BADGE),
        )
        SyncStatus.Error -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier,
        ) {
            PetMateBadge(
                text = stringResource(R.string.sync_status_error),
                container = colors.alertSoft,
                content = colors.alert,
                icon = R.drawable.ic_pm_alert,
                modifier = Modifier.testTag(SyncStatusIndicatorTags.BADGE),
            )
            PetMateSmallButton(
                text = stringResource(R.string.sync_status_retry),
                onClick = onRetry,
                modifier = Modifier.testTag(SyncStatusIndicatorTags.RETRY),
            )
        }
    }
}

@PetMatePreviews
@Composable
private fun SyncStatusIndicatorPreview() {
    PetMateEdelTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                SyncStatus.entries.forEach { SyncStatusIndicator(status = it, onRetry = {}) }
            }
        }
    }
}
