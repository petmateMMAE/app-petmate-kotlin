package mx.petmate.app.ui.pets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.ui.components.OwnerTab
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lista de mis mascotas y sus estados (spec 007, T-007 y T-011).
 *
 * Se dibuja `PetsContent`, que recibe el estado ya hecho: sin Hilt, sin Room y
 * sin red.
 */
@RunWith(AndroidJUnit4::class)
class PetsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun pet(id: String, name: String, species: PetSpecies = PetSpecies.Dog, breed: String? = "Criollo") = Pet(
        id = id,
        name = name,
        species = species,
        breed = breed,
        birthDate = "2023-01-15",
        sex = PetSex.Female,
    )

    private class Clicks {
        val pets = mutableListOf<String>()
        var add = 0
        var needAccount = 0
        var retry = 0
        val tabs = mutableListOf<OwnerTab>()
    }

    private fun show(state: PetsUiState, isGuest: Boolean = false): Clicks {
        val clicks = Clicks()
        compose.setContent {
            PetMateEdelTheme {
                PetsContent(
                    state = state,
                    userName = "Eduardo Olvera",
                    isGuest = isGuest,
                    onTabSelected = { clicks.tabs += it },
                    onPetClick = { clicks.pets += it },
                    onAddPet = { clicks.add += 1 },
                    onNeedAccount = { clicks.needAccount += 1 },
                    onRetry = { clicks.retry += 1 },
                )
            }
        }
        return clicks
    }

    private fun loaded(vararg summaries: PetSummary) =
        PetsUiState(isLoading = false, pets = summaries.toList())

    // ---- T-007: la lista y la tarjeta ----

    @Test
    fun la_lista_pinta_una_tarjeta_por_mascota() {
        val state = loaded(
            PetSummary(pet("p1", "Nube"), next = null, activeAllergies = 0),
            PetSummary(pet("p2", "Simba", PetSpecies.Cat), next = null, activeAllergies = 0),
        )
        show(state)

        compose.onNodeWithTag(PetsTags.LIST).assertExists()
        compose.onNodeWithTag(PetsTags.card("p1")).assertIsDisplayed()
        compose.onNodeWithTag(PetsTags.card("p2")).assertIsDisplayed()
    }

    @Test
    fun la_tarjeta_muestra_nombre_especie_y_raza() {
        show(loaded(PetSummary(pet("p1", "Nube", PetSpecies.Dog, "Labrador"), null, 0)))

        compose.onNodeWithText("Nube", substring = true).assertExists()
        compose.onNodeWithText("Labrador", substring = true).assertExists()
        compose.onNodeWithText(context.getString(R.string.pets_species_dog), substring = true).assertExists()
    }

    @Test
    fun tocar_una_tarjeta_avisa_con_el_identificador_de_esa_mascota() {
        val clicks = show(
            loaded(
                PetSummary(pet("p1", "Nube"), null, 0),
                PetSummary(pet("p2", "Simba"), null, 0),
            ),
        )

        compose.onNodeWithTag(PetsTags.card("p2")).performClick()

        assertEquals(listOf("p2"), clicks.pets)
    }

    @Test
    fun con_mascotas_sigue_estando_la_tarjeta_de_agregar() {
        show(loaded(PetSummary(pet("p1", "Nube"), null, 0)))

        compose.onNodeWithTag(PetsTags.ADD_CARD).assertExists()
    }

    @Test
    fun agregar_desde_la_lista_lo_avisa() {
        val clicks = show(loaded(PetSummary(pet("p1", "Nube"), null, 0)))

        compose.onNodeWithTag(PetsTags.ADD_CARD).performClick()

        assertEquals(1, clicks.add)
        assertEquals(0, clicks.needAccount)
    }

    @Test
    fun la_proxima_vacuna_de_la_primera_mascota_tiene_su_espacio() {
        val next = NextEvent(isVaccine = true, name = "Rabia", date = "2026-12-01")
        show(loaded(PetSummary(pet("p1", "Nube"), next, 0)))

        compose.onNodeWithTag(PetsTags.NEXT_STRIP).assertExists()
    }

    @Test
    fun sin_proximo_evento_no_hay_tira_superior() {
        show(loaded(PetSummary(pet("p1", "Nube"), next = null, activeAllergies = 0)))

        compose.onNodeWithTag(PetsTags.NEXT_STRIP).assertDoesNotExist()
    }

    @Test
    fun de_invitado_agregar_pide_crear_cuenta_en_vez_de_abrir_el_alta() {
        val clicks = show(loaded(PetSummary(pet("p1", "Nube"), null, 0)), isGuest = true)

        compose.onNodeWithTag(PetsTags.ADD_CARD).performClick()

        assertEquals(1, clicks.needAccount)
        assertEquals(0, clicks.add)
    }

    // ---- T-011: carga, error y vacio ----

    @Test
    fun mientras_carga_no_se_pinta_la_lista() {
        show(PetsUiState(isLoading = true))

        compose.onNodeWithTag(PetsTags.LOADING).assertExists()
        compose.onNodeWithTag(PetsTags.LIST).assertDoesNotExist()
        compose.onNodeWithTag(PetsTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun sin_mascotas_hay_estado_vacio_con_su_texto_y_su_accion() {
        show(PetsUiState(isLoading = false, pets = emptyList()))

        compose.onNodeWithTag(PetsTags.EMPTY).assertExists()
        compose.onNodeWithText(context.getString(R.string.pets_empty_title), substring = true).assertExists()
        compose.onNodeWithText(context.getString(R.string.pets_add_first), substring = true).assertExists()
    }

    @Test
    fun el_estado_vacio_lleva_a_crear_la_primera_mascota() {
        val clicks = show(PetsUiState(isLoading = false, pets = emptyList()))

        compose.onNodeWithText(context.getString(R.string.pets_add_first), substring = true).performClick()

        assertEquals(1, clicks.add)
    }

    @Test
    fun un_error_de_red_muestra_el_aviso_en_vez_de_dejar_la_pantalla_en_blanco() {
        show(PetsUiState(isLoading = false, pets = emptyList(), error = UiText.Resource(R.string.error_network)))

        compose.onNodeWithTag(PetsTags.ERROR).assertExists()
        compose.onNodeWithTag(PetsTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun el_reintento_del_error_funciona() {
        val clicks = show(
            PetsUiState(isLoading = false, pets = emptyList(), error = UiText.Resource(R.string.error_network)),
        )

        compose.onNodeWithText(context.getString(R.string.action_retry), substring = true).performClick()

        assertEquals(1, clicks.retry)
    }

    @Test
    fun un_error_con_mascotas_guardadas_no_tapa_la_lista() {
        // Offline-first: si ya hay datos locales, el fallo de red no borra lo
        // que el usuario ya tenia delante.
        show(
            PetsUiState(
                isLoading = false,
                pets = listOf(PetSummary(pet("p1", "Nube"), null, 0)),
                error = UiText.Resource(R.string.error_network),
            ),
        )

        compose.onNodeWithTag(PetsTags.LIST).assertExists()
        compose.onNodeWithTag(PetsTags.ERROR).assertDoesNotExist()
    }
}
