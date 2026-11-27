package mx.petmate.app.ui.pets

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Carnet de salud (spec 008, T-007).
 *
 * Camino feliz, el error principal y que las seis secciones esten.
 */
@RunWith(AndroidJUnit4::class)
class CarnetScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val nube = Pet(
        id = "p1",
        name = "Nube",
        species = PetSpecies.Dog,
        breed = "Criollo",
        birthDate = "2023-01-15",
        sex = PetSex.Female,
    )

    private val carnet = Carnet(
        vaccines = listOf(
            Vaccine(
                id = "v1",
                name = "Rabia",
                appliedOn = "2026-03-10",
                batch = null,
                appliedBy = "Vet Sur",
                nextDoseOn = "2027-03-10",
                origin = RecordOrigin.Owner,
            ),
        ),
    )

    private fun show(state: CarnetUiState): MutableList<CarnetSection> {
        val opened = mutableListOf<CarnetSection>()
        compose.setContent {
            PetMateEdelTheme {
                CarnetContent(
                    state = state,
                    isGuest = false,
                    onBackClick = {},
                    onEditClick = {},
                    onOpen = { opened += it },
                    onNeedAccount = {},
                    onRetry = {},
                )
            }
        }
        return opened
    }

    @Test
    fun el_carnet_muestra_la_mascota_y_sus_seis_secciones() {
        show(CarnetUiState(isLoading = false, pet = nube, carnet = carnet))

        compose.onNodeWithText("Nube", substring = true).assertExists()
        compose.onNodeWithTag(CarnetTags.SUMMARY).assertExists()
        CarnetSection.entries.forEach {
            compose.onNodeWithTag(CarnetTags.section(it)).assertExists()
        }
    }

    @Test
    fun tocar_una_seccion_la_abre() {
        val opened = show(CarnetUiState(isLoading = false, pet = nube, carnet = carnet))

        compose.onNodeWithTag(CarnetTags.section(CarnetSection.Vaccines)).performClick()

        assertEquals(listOf(CarnetSection.Vaccines), opened)
    }

    @Test
    fun sin_mascota_cargada_se_ve_la_carga_y_no_el_carnet() {
        show(CarnetUiState(isLoading = true, pet = null))

        compose.onNodeWithTag(CarnetTags.LOADING).assertExists()
        compose.onNodeWithTag(CarnetTags.SECTIONS).assertDoesNotExist()
    }

    @Test
    fun si_la_mascota_no_se_pudo_cargar_se_explica_en_vez_de_quedar_en_blanco() {
        show(CarnetUiState(isLoading = false, pet = null, error = UiText.Resource(R.string.error_network)))

        compose.onNodeWithTag(CarnetTags.ERROR).assertExists()
        compose.onNodeWithText(context.getString(R.string.action_retry), substring = true).assertExists()
    }
}
