package mx.petmate.app.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.ui.pets.NextEvent
import mx.petmate.app.ui.pets.PetSummary
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tarjeta «lo más próximo»: lo que se ve y a dónde navega
 * (spec 010, T-010, C-06, C-07 y C-13).
 */
@RunWith(AndroidJUnit4::class)
class NextHealthCardUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = "2026-11-27"

    private fun pet(id: String, name: String) =
        Pet(id, name, PetSpecies.Dog, "Criollo", "2022-03-01", PetSex.Female)

    private fun summary(id: String, name: String, date: String?, isVaccine: Boolean = true) =
        PetSummary(
            pet = pet(id, name),
            next = date?.let { NextEvent(isVaccine, if (isVaccine) "Rabia" else "Pipeta", it) },
            activeAllergies = 0,
        )

    private fun show(
        pets: List<PetSummary>,
        onOpenCarnet: (String) -> Unit = {},
        onRegisterPet: () -> Unit = {},
    ) {
        compose.setContent {
            PetMateEdelTheme {
                NextHealthCard(
                    pets = pets,
                    onOpenCarnet = onOpenCarnet,
                    onRegisterPet = onRegisterPet,
                    today = today,
                )
            }
        }
    }

    // ---- C-06: la tarjeta muestra el evento más próximo ----

    @Test
    fun muestra_la_vacuna_de_la_mascota_mas_proxima() {
        show(listOf(summary("p1", "Nube", "2026-12-20"), summary("p2", "Rocco", "2026-11-29")))

        compose.onNodeWithTag(NextHealthCardTags.CARD).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_next_health_vaccine, "Rocco"))
            .assertIsDisplayed()
    }

    @Test
    fun distingue_la_desparasitacion_de_la_vacuna() {
        show(listOf(summary("p1", "Nube", "2026-11-29", isVaccine = false)))

        compose.onNodeWithText(context.getString(R.string.home_next_health_deworming, "Nube"))
            .assertIsDisplayed()
    }

    @Test
    fun lleva_el_titulo_de_la_tarjeta() {
        show(listOf(summary("p1", "Nube", "2026-11-29")))

        compose.onNodeWithText(context.getString(R.string.home_next_health_title)).assertIsDisplayed()
    }

    @Test
    fun dice_cuanto_falta_en_palabras() {
        show(listOf(summary("p1", "Nube", "2026-11-28")))

        compose.onNodeWithText(context.getString(R.string.home_next_health_tomorrow)).assertIsDisplayed()
    }

    @Test
    fun hoy_se_dice_hoy() {
        show(listOf(summary("p1", "Nube", today)))

        compose.onNodeWithText(context.getString(R.string.home_next_health_today)).assertIsDisplayed()
    }

    @Test
    fun lo_atrasado_se_dice_con_palabras_no_solo_con_color() {
        show(listOf(summary("p1", "Nube", "2026-11-20")))

        compose.onNodeWithText(context.getString(R.string.home_next_health_overdue, "2026-11-20"))
            .assertIsDisplayed()
    }

    @Test
    fun a_mas_de_un_dia_dice_cuantos() {
        show(listOf(summary("p1", "Nube", "2026-12-11")))

        compose.onNodeWithText(context.getString(R.string.home_next_health_in_days, 14))
            .assertIsDisplayed()
    }

    // ---- C-07: tocar la tarjeta abre el carnet de esa mascota ----

    @Test
    fun tocar_la_tarjeta_abre_el_carnet_de_esa_mascota() {
        var opened: String? = null
        show(
            pets = listOf(summary("p1", "Nube", "2026-12-20"), summary("p2", "Rocco", "2026-11-29")),
            onOpenCarnet = { opened = it },
        )

        compose.onNodeWithTag(NextHealthCardTags.CARD).performClick()

        assertEquals("debe abrir el carnet de la mascota del evento, no de la primera", "p2", opened)
    }

    @Test
    fun sin_tocar_nada_no_navega() {
        var opened: String? = null
        show(pets = listOf(summary("p1", "Nube", "2026-11-29")), onOpenCarnet = { opened = it })

        assertEquals(null, opened)
    }

    // ---- C-13: sin mascotas invita a registrar una ----

    @Test
    fun sin_mascotas_invita_a_registrar_una() {
        show(emptyList())

        compose.onNodeWithTag(NextHealthCardTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_next_health_empty_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_next_health_empty_action)).assertIsDisplayed()
        compose.onNodeWithTag(NextHealthCardTags.CARD).assertDoesNotExist()
    }

    @Test
    fun con_mascotas_pero_sin_eventos_tambien_invita() {
        show(listOf(summary("p1", "Nube", null)))

        compose.onNodeWithTag(NextHealthCardTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun tocar_el_estado_vacio_lleva_a_registrar() {
        var registered = false
        show(emptyList(), onRegisterPet = { registered = true })

        compose.onNodeWithTag(NextHealthCardTags.EMPTY).performClick()

        assertTrue(registered)
    }

    @Test
    fun el_estado_vacio_no_navega_al_carnet() {
        var opened: String? = null
        show(emptyList(), onOpenCarnet = { opened = it })

        compose.onNodeWithTag(NextHealthCardTags.EMPTY).performClick()

        assertEquals(null, opened)
    }
}
