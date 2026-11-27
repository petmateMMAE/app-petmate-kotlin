package mx.petmate.app.ui.ficha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.ui.business.TipBox
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/** Etiquetas de prueba del banner de alergias (spec 013, T-010). */
object AllergyBannerTags {
    const val ALERT = "allergy_banner_alert"
    const val NONE = "allergy_banner_none"
}

/** Una alergia tal como la ve la ficha: nombre y, si la hay, una nota. */
data class AllergyLine(val name: String, val notes: String?)

/**
 * Alergias de la mascota (spec 013, T-010).
 *
 * Se pinta **siempre**, para veterinaria igual que para tienda o estetica, y es
 * independiente de las secciones que el dueno autorizo: una alergia es
 * informacion de seguridad, no un dato clinico mas. Si no hay ninguna
 * registrada lo dice explicitamente, porque un hueco en blanco se lee como
 * «no es alergico» y eso no es lo mismo que «no lo sabemos».
 */
@Composable
fun AllergyBanner(allergies: List<AllergyLine>, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    if (allergies.isEmpty()) {
        TipBox(
            "Sin alergias registradas.",
            icon = R.drawable.ic_pm_shield,
            soft = extra.okSoft,
            ink = extra.ok,
            modifier = modifier.testTag(AllergyBannerTags.NONE),
        )
        return
    }
    Column(
        modifier
            .fillMaxWidth()
            .background(extra.alertSoft, RoundedCornerShape(20.dp))
            .padding(14.dp)
            .testTag(AllergyBannerTags.ALERT),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_pm_alert),
                contentDescription = null,
                tint = extra.alert,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("¡Cuidado! Es alérgico a:", color = extra.alert, fontWeight = FontWeight.ExtraBold)
        }
        allergies.forEach { allergy ->
            Text(
                "• ${allergy.name}" + (allergy.notes?.let { " — $it" } ?: ""),
                color = extra.alert,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview
@Composable
private fun AllergyBannerPreview() {
    PetMateEdelTheme {
        AllergyBanner(
            listOf(
                AllergyLine("Pollo", "Le provoca comezón"),
                AllergyLine("Penicilina", null),
            ),
        )
    }
}

@Preview
@Composable
private fun AllergyBannerEmptyPreview() {
    PetMateEdelTheme { AllergyBanner(emptyList()) }
}
