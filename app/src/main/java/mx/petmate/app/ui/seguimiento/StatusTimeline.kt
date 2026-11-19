package mx.petmate.app.ui.seguimiento

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.domain.model.FollowUpStatus
import mx.petmate.app.domain.model.FollowupNote
import mx.petmate.app.domain.usecase.ReviewDateClass
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.theme.petMateColors

/** Etiquetas para pruebas de interfaz. */
object FollowUpTags {
    const val REVIEW_BADGE = "followup_review_badge"
    const val TIMELINE_ITEM = "followup_timeline_item"
}

@StringRes
fun FollowUpStatus?.label(): Int = when (this) {
    FollowUpStatus.Treating -> R.string.followup_status_treating
    FollowUpStatus.Improving -> R.string.followup_status_improving
    FollowUpStatus.Discharged -> R.string.followup_status_discharged
    null -> R.string.followup_status_unknown
}

/**
 * Distintivo de la proxima revision (spec 015, T-010): rojo si esta atrasada o faltan 3 dias o menos, amarillo
 * si faltan 14 o menos, verde si falta mas. Sin fecha no pinta nada.
 */
@Composable
fun ReviewDateBadge(reviewClass: ReviewDateClass, date: String?, modifier: Modifier = Modifier) {
    if (date == null || reviewClass == ReviewDateClass.None) return
    val colors = MaterialTheme.petMateColors
    val (container, content) = when (reviewClass) {
        ReviewDateClass.Overdue, ReviewDateClass.Urgent -> colors.alertSoft to colors.alert
        ReviewDateClass.Soon -> colors.sunSoft to colors.sunInk
        else -> colors.okSoft to colors.ok
    }
    val text = if (reviewClass == ReviewDateClass.Overdue) {
        stringResource(R.string.followup_review_overdue, date)
    } else {
        stringResource(R.string.followup_review_on, date)
    }
    PetMateBadge(
        text = text,
        container = container,
        content = content,
        icon = R.drawable.ic_pm_calendar,
        modifier = modifier.testTag(FollowUpTags.REVIEW_BADGE),
    )
}

/**
 * Bitacora del seguimiento, la nota mas reciente primero (spec 015, T-010, R-03).
 *
 * El backend guarda notas con fecha y texto; todavia no guarda el autor ni el estado de cada cambio, asi que
 * eso no se muestra (pregunta para Miguel en `memoria.md`).
 */
@Composable
fun StatusTimeline(notes: List<FollowupNote>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.petMateColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        Text(stringResource(R.string.followup_timeline), style = MaterialTheme.typography.titleSmall)
        if (notes.isEmpty()) {
            Text(stringResource(R.string.followup_timeline_empty), color = colors.ink3)
        }
        notes.sortedByDescending { it.at }.forEach { note ->
            Column(modifier = Modifier.testTag(FollowUpTags.TIMELINE_ITEM)) {
                Text(note.at.take(DATE_LENGTH), style = MaterialTheme.typography.labelMedium, color = colors.ink3)
                Text(note.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** `yyyy-MM-dd` del principio de la marca ISO que manda el backend. */
private const val DATE_LENGTH = 10
