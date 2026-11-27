package mx.petmate.app.ui.pets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.usecase.PetRules
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Formulario de alta y edicion, y borrado con confirmacion
 * (spec 007, T-008, T-009 y T-011).
 *
 * Un solo formulario sirve para las dos cosas: lo que cambia es `isEdit`.
 */
@RunWith(AndroidJUnit4::class)
class PetFormTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private class Events {
        val names = mutableListOf<String>()
        val breeds = mutableListOf<String>()
        val species = mutableListOf<PetSpecies>()
        val sexes = mutableListOf<PetSex>()
        var saved = 0
        var deleted = 0
        var back = 0
    }

    private fun show(state: PetFormUiState): Events {
        val events = Events()
        compose.setContent {
            PetMateEdelTheme {
                PetFormContent(
                    state = state,
                    onBackClick = { events.back += 1 },
                    callbacks = PetFormCallbacks(
                        onName = { events.names += it },
                        onBreed = { events.breeds += it },
                        onSpecies = { events.species += it },
                        onSex = { events.sexes += it },
                        onSave = { events.saved += 1 },
                        onDelete = { events.deleted += 1 },
                    ),
                )
            }
        }
        return events
    }

    // ---- T-008: un formulario para alta y edicion ----

    @Test
    fun el_alta_se_titula_como_mascota_nueva() {
        show(PetFormUiState(isEdit = false))

        compose.onNodeWithTag(PetsTags.FORM).assertExists()
        compose.onNodeWithText(context.getString(R.string.pet_form_new_title), substring = true).assertExists()
    }

    @Test
    fun la_edicion_se_titula_con_el_nombre_de_la_mascota() {
        show(PetFormUiState(isEdit = true, name = "Nube"))

        // Texto exacto: con subcadena, «Nube» tambien coincide con el campo del
        // nombre y con el boton de borrar.
        compose.onNodeWithText(context.getString(R.string.pet_form_edit_title, "Nube")).assertExists()
    }

    @Test
    fun el_alta_no_ofrece_borrar() {
        // T-009: no se puede borrar lo que todavia no existe.
        show(PetFormUiState(isEdit = false))

        compose.onNodeWithTag(PetsTags.FORM_DELETE).assertDoesNotExist()
    }

    @Test
    fun la_edicion_si_ofrece_borrar() {
        show(PetFormUiState(isEdit = true, name = "Nube"))

        compose.onNodeWithTag(PetsTags.FORM_DELETE).assertExists()
    }

    @Test
    fun escribir_el_nombre_lo_avisa() {
        val events = show(PetFormUiState())

        compose.onNodeWithText(context.getString(R.string.pet_form_name_placeholder)).performTextInput("Nu")

        // `performTextInput` entrega el texto de una vez, no letra por letra.
        assertEquals(listOf("Nu"), events.names)
    }

    @Test
    fun elegir_especie_lo_avisa() {
        val events = show(PetFormUiState())

        compose.onNodeWithTag(PetsTags.SPECIES_DOG).performClick()
        compose.onNodeWithTag(PetsTags.SPECIES_CAT).performClick()

        assertEquals(listOf(PetSpecies.Dog, PetSpecies.Cat), events.species)
    }

    @Test
    fun guardar_lo_avisa() {
        val events = show(PetFormUiState(name = "Nube", species = PetSpecies.Dog))

        compose.onNodeWithTag(PetsTags.FORM_SAVE).performClick()

        assertEquals(1, events.saved)
    }

    @Test
    fun los_errores_de_cada_campo_se_muestran_con_su_texto() {
        // C-02: no se guarda con datos invalidos, y se dice por que.
        show(
            PetFormUiState(
                errors = mapOf(
                    PetRules.Field.Name to UiText.Resource(R.string.error_required),
                ),
            ),
        )

        compose.onNodeWithText(context.getString(R.string.error_required), substring = true).assertExists()
    }

    @Test
    fun mientras_carga_la_mascota_no_se_pinta_el_formulario_ni_se_puede_guardar() {
        show(PetFormUiState(isEdit = true, isLoading = true))

        compose.onNodeWithTag(PetsTags.FORM_LOADING).assertExists()
        compose.onNodeWithTag(PetsTags.FORM_SAVE).assertIsNotEnabled()
    }

    // ---- T-009: borrado con confirmacion ----

    @Test
    fun borrar_pide_confirmacion_explicita_antes_de_nada() {
        val events = show(PetFormUiState(isEdit = true, name = "Nube"))

        // El formulario lleva scroll y el borrado esta al final: sin esto el
        // clic no llega al boton.
        compose.onNodeWithTag(PetsTags.FORM_DELETE).performScrollTo().performClick()

        // C-08: aparece el dialogo y todavia no se ha borrado nada.
        compose.onNodeWithText(context.getString(R.string.pet_delete_title, "Nube")).assertIsDisplayed()
        assertEquals(0, events.deleted)
    }

    @Test
    fun confirmar_el_borrado_lo_ejecuta() {
        val events = show(PetFormUiState(isEdit = true, name = "Nube"))

        compose.onNodeWithTag(PetsTags.FORM_DELETE).performScrollTo().performClick()
        // Texto exacto: «Eliminar» por subcadena tambien coincide con el boton
        // «Eliminar a Nube» del propio formulario.
        compose.onNodeWithText(context.getString(R.string.pet_delete_confirm)).performClick()

        assertEquals(1, events.deleted)
    }

    @Test
    fun cancelar_el_borrado_no_cambia_nada() {
        val events = show(PetFormUiState(isEdit = true, name = "Nube"))

        compose.onNodeWithTag(PetsTags.FORM_DELETE).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.profile_cancel)).performClick()

        assertEquals(0, events.deleted)
        // El dialogo se cerro y el formulario sigue ahi.
        compose.onNodeWithTag(PetsTags.FORM).assertExists()
    }

    @Test
    fun sin_tocar_borrar_no_hay_dialogo() {
        show(PetFormUiState(isEdit = true, name = "Nube"))

        // Se busca el titulo del dialogo, que es inequivoco: «Eliminar» a secas
        // coincidiria con el boton del formulario.
        compose.onNodeWithText(context.getString(R.string.pet_delete_title, "Nube")).assertDoesNotExist()
    }
}
