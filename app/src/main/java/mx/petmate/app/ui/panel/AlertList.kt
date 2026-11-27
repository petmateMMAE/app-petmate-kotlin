package mx.petmate.app.ui.panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.domain.model.FollowUpAlert
import mx.petmate.app.domain.usecase.ReviewDateClass
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.components.dueColors
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/** Etiquetas para las pruebas de interfaz. */
object AlertListTags {
    const val LIST = "panel_alert_list"
    const val EMPTY = "panel_alert_empty"

    /** Etiqueta de una alerta: su identificador de seguimiento va al final. */
    fun item(followupId: String) = "panel_alert_$followupId"
}

/**
 * Lista de alertas de seguimiento del panel (spec 012, T-011, R-05 y R-07).
 *
 * Sin estado propio: recibe las alertas ya clasificadas y avisa con
 * [onAlertClick], que recibe el `followupId`. **Quien navega es la pantalla**,
 * no este componente: asi la lista no depende del grafo de navegacion y, si el
 * destino de 015 todavia no esta puesto, basta con que la pantalla mande el
 * toque a un destino provisional y aqui no se rompe nada.
 *
 * El color sale de la severidad que ya trae la alerta, con los mismos umbrales
 * compartidos del resto de la app (`DueThresholds` via `dueColors`): esta
 * lista no vuelve a clasificar fechas.
 */
@Composable
fun AlertList(
    alerts: List<FollowUpAlert>,
    onAlertClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (alerts.isEmpty()) {
        AlertsEmpty(modifier)
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag(AlertListTags.LIST),
    ) {
        alerts.forEach { alert ->
            AlertRow(alert = alert, onClick = { onAlertClick(alert.followupId) })
        }
    }
}

/** Estado vacio explicativo: dice por que esta vacio y que hacer (T-011, C-10). */
@Composable
private fun AlertsEmpty(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.petMateColors
    Surface(
        color = colors.surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(AlertListTags.EMPTY),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_alerts_empty_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink2,
            )
            Text(
                text = stringResource(R.string.dashboard_alerts_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.ink3,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AlertRow(alert: FollowUpAlert, onClick: () -> Unit) {
    val colors = MaterialTheme.petMateColors
    val due = dueColors(alert.severity)

    Surface(
        color = colors.surface2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlertListTags.item(alert.followupId))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = alert.petName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = reviewLabel(alert),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink2,
                    maxLines = 1,
                )
                if (alert.unreadMessages > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.dashboard_alert_unread,
                            alert.unreadMessages,
                            alert.unreadMessages,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.brandText,
                        maxLines = 1,
                    )
                }
            }

            // El distintivo solo aparece cuando hay algo que destacar: una lista
            // donde todo esta marcado no destaca nada.
            if (alert.severity != ReviewDateClass.Normal && alert.severity != ReviewDateClass.None) {
                PetMateBadge(
                    text = badgeLabel(alert),
                    container = due.container,
                    content = due.content,
                )
            }
        }
    }
}

/** Linea de la fecha de revision. */
@Composable
private fun reviewLabel(alert: FollowUpAlert): String {
    val date = alert.nextReviewOn
    return if (date.isNullOrBlank()) {
        stringResource(R.string.dashboard_alert_no_review)
    } else {
        stringResource(R.string.dashboard_alert_review, date)
    }
}

/** Texto del distintivo: lo atrasado se dice con palabras, no solo con color. */
@Composable
private fun badgeLabel(alert: FollowUpAlert): String =
    if (alert.isOverdue) {
        stringResource(R.string.dashboard_alert_overdue)
    } else {
        alert.nextReviewOn.orEmpty()
    }

@PetMatePreviews
@Preview
@Composable
private fun AlertListPreview() {
    PetMateEdelTheme {
        AlertList(
            alerts = listOf(
                FollowUpAlert("f1", "p1", "Nube", "2026-11-20", ReviewDateClass.Overdue, unreadMessages = 2),
                FollowUpAlert("f2", "p2", "Rocco", "2026-11-27", ReviewDateClass.Urgent),
                FollowUpAlert("f3", "p3", "Luna", "2026-12-05", ReviewDateClass.Soon),
                FollowUpAlert("f4", "p4", "Milo", null, ReviewDateClass.None),
            ),
            onAlertClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PetMatePreviews
@Preview
@Composable
private fun AlertListEmptyPreview() {
    PetMateEdelTheme {
        AlertList(alerts = emptyList(), onAlertClick = {}, modifier = Modifier.padding(16.dp))
    }
}
