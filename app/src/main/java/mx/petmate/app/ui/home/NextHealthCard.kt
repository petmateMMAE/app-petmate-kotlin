package mx.petmate.app.ui.home

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.usecase.PetDates
import mx.petmate.app.domain.usecase.ReviewDateClass
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.components.dueColors
import mx.petmate.app.ui.pets.NextEvent
import mx.petmate.app.ui.pets.PetSummary
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/** Etiquetas para las pruebas de interfaz. */
object NextHealthCardTags {
    const val CARD = "home_next_health_card"
    const val EMPTY = "home_next_health_empty"
}

/**
 * El evento de salud más próximo, de entre todas las mascotas (spec 010, T-010, R-07).
 *
 * [daysAway] es negativo si la fecha ya pasó.
 */
data class NextHealthEvent(
    val pet: Pet,
    val event: NextEvent,
    val daysAway: Int,
) {
    val level: ReviewDateClass get() = mx.petmate.app.domain.usecase.DueThresholds.level(daysAway)
}

/**
 * Elige el evento más próximo entre las mascotas (spec 010, T-010, C-06).
 *
 * «Lo más próximo» es el próximo **evento de salud** —vacuna o
 * desparasitación—, no el negocio más cercano: es la decisión que `memoria.md`
 * registra del 29/10 y la razón por la que 010 va después de 008 en el mapa.
 *
 * Es lógica pura con «hoy» inyectado, así que se prueba en la JVM. Reutiliza
 * `NextEvent`, que ya calcula el próximo evento de cada carnet (007): aquí solo
 * se compara entre mascotas.
 */
fun nearestHealthEvent(pets: List<PetSummary>, today: String = PetDates.today()): NextHealthEvent? =
    pets.mapNotNull { summary ->
        val event = summary.next ?: return@mapNotNull null
        val days = PetDates.daysBetween(today, event.date) ?: return@mapNotNull null
        NextHealthEvent(summary.pet, event, days)
    }
        // A igual fecha, por nombre de la mascota: el orden no cambia entre dos
        // lecturas de la misma lista.
        .minWithOrNull(compareBy({ it.event.date }, { it.pet.name }))

/**
 * Tarjeta «lo más próximo» de la pantalla de inicio (spec 010, T-010, R-07).
 *
 * Sin estado propio. Tocarla abre el carnet de esa mascota mediante
 * [onOpenCarnet]; quien navega es la pantalla, no la tarjeta. Sin mascotas con
 * eventos, invita a registrar una.
 *
 * El color sale de `dueColors`, con los umbrales únicos de `DueThresholds`
 * (rojo ≤ 3, amarillo ≤ 14): esta tarjeta no define los suyos.
 */
@Composable
fun NextHealthCard(
    pets: List<PetSummary>,
    onOpenCarnet: (String) -> Unit,
    onRegisterPet: () -> Unit,
    modifier: Modifier = Modifier,
    today: String = PetDates.today(),
) {
    val nearest = nearestHealthEvent(pets, today)
    if (nearest == null) {
        NextHealthEmpty(onRegisterPet, modifier)
        return
    }

    val colors = MaterialTheme.petMateColors
    val due = dueColors(nearest.level)

    Surface(
        color = colors.surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(NextHealthCardTags.CARD)
            .clickable { onOpenCarnet(nearest.pet.id) },
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
                    text = stringResource(R.string.home_next_health_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink3,
                )
                Text(
                    text = eventLabel(nearest),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = nearest.event.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink2,
                    maxLines = 1,
                )
            }

            PetMateBadge(
                text = whenLabel(nearest),
                container = due.container,
                content = due.content,
            )
        }
    }
}

/** «Vacuna de Nube» o «Desparasitación de Nube». */
@Composable
private fun eventLabel(nearest: NextHealthEvent): String = stringResource(
    if (nearest.event.isVaccine) {
        R.string.home_next_health_vaccine
    } else {
        R.string.home_next_health_deworming
    },
    nearest.pet.name,
)

/** Cuánto falta, en palabras. Lo atrasado se dice, no solo se pinta de rojo. */
@Composable
private fun whenLabel(nearest: NextHealthEvent): String = when {
    nearest.daysAway < 0 -> stringResource(R.string.home_next_health_overdue, nearest.event.date)
    nearest.daysAway == 0 -> stringResource(R.string.home_next_health_today)
    nearest.daysAway == 1 -> stringResource(R.string.home_next_health_tomorrow)
    else -> stringResource(R.string.home_next_health_in_days, nearest.daysAway)
}

/** Sin eventos próximos: invita a registrar una mascota (T-010). */
@Composable
private fun NextHealthEmpty(onRegisterPet: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.petMateColors
    Surface(
        color = colors.surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(NextHealthCardTags.EMPTY)
            .clickable(onClick = onRegisterPet),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.home_next_health_empty_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink2,
            )
            Text(
                text = stringResource(R.string.home_next_health_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.ink3,
            )
            Text(
                text = stringResource(R.string.home_next_health_empty_action),
                style = MaterialTheme.typography.labelLarge,
                color = colors.brandText,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@PetMatePreviews
@Preview
@Composable
private fun NextHealthCardPreview() {
    val pet = Pet("p1", "Nube", PetSpecies.Dog, "Criollo", "2022-03-01", PetSex.Female)
    PetMateEdelTheme {
        NextHealthCard(
            pets = listOf(PetSummary(pet, NextEvent(true, "Rabia", "2026-11-29"), activeAllergies = 0)),
            onOpenCarnet = {},
            onRegisterPet = {},
            today = "2026-11-27",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PetMatePreviews
@Preview
@Composable
private fun NextHealthCardEmptyPreview() {
    PetMateEdelTheme {
        NextHealthCard(
            pets = emptyList(),
            onOpenCarnet = {},
            onRegisterPet = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
