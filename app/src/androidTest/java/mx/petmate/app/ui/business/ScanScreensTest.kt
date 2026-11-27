package mx.petmate.app.ui.business

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.FichaConsultation
import mx.petmate.app.domain.model.Medication
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.ScanFicha
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.WeightRecord
import mx.petmate.app.ui.ficha.AllergyBannerTags
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Escaner y ficha de la mascota (spec 013, T-008 y T-011).
 *
 * Se dibujan los `Content`, que no tienen ViewModel ni camara ni permisos: por
 * eso se pueden probar. Quien pide el permiso es el `Screen`, y esa parte la
 * cubre la prueba de permisos de la spec 003.
 */
@RunWith(AndroidJUnit4::class)
class ScanScreensTest {

    @get:Rule
    val compose = createComposeRule()

    // ---- Escaner (T-008) ----

    private class ScanClicks {
        var camera = 0
        var resolve = 0
        val codes = mutableListOf<String>()
    }

    private fun showScanner(state: ScanUiState, cameraDenied: Boolean = false): ScanClicks {
        val clicks = ScanClicks()
        compose.setContent {
            PetMateEdelTheme {
                ScannerContent(
                    state = state,
                    cameraDenied = cameraDenied,
                    onOpenCamera = { clicks.camera += 1 },
                    onCode = { clicks.codes += it },
                    onResolveCode = { clicks.resolve += 1 },
                    bottomBar = { Text("barra") },
                )
            }
        }
        return clicks
    }

    @Test
    fun el_escaner_ofrece_la_camara_y_la_guia_visual() {
        showScanner(ScanUiState())

        compose.onNodeWithTag(ScanTags.SCANNER).assertIsDisplayed()
        compose.onNodeWithTag(ScanTags.FRAME).assertIsDisplayed()
        compose.onNodeWithTag(ScanTags.OPEN_CAMERA).assertIsDisplayed()
    }

    @Test
    fun tocar_abrir_camara_lo_avisa() {
        val clicks = showScanner(ScanUiState())

        compose.onNodeWithTag(ScanTags.OPEN_CAMERA).performClick()

        assertEquals(1, clicks.camera)
    }

    @Test
    fun el_cuadro_de_la_guia_tambien_abre_la_camara() {
        val clicks = showScanner(ScanUiState())

        compose.onNodeWithTag(ScanTags.FRAME).performClick()

        assertEquals(1, clicks.camera)
    }

    @Test
    fun el_codigo_manual_esta_siempre_no_solo_si_se_niega_la_camara() {
        // C-10: la entrada manual es una ruta propia, no un consuelo.
        showScanner(ScanUiState(), cameraDenied = false)

        compose.onNodeWithTag(ScanTags.MANUAL_CODE).assertIsDisplayed()
        compose.onNodeWithTag(ScanTags.MANUAL_SUBMIT).assertIsDisplayed()
    }

    @Test
    fun escribir_el_codigo_lo_avisa() {
        val clicks = showScanner(ScanUiState())

        // Se busca el campo por su etiqueta visible: `MANUAL_CODE` marca el
        // contenedor, y el texto no se escribe en un contenedor.
        // Se busca por la marca de agua, que vive dentro del campo: la
        // etiqueta es un `Text` aparte y en un `Text` no se escribe.
        compose.onNodeWithText("8 caracteres", substring = true)
            .performScrollTo()
            .performTextInput("K7")

        // `performTextInput` entrega el texto de una vez, no letra por letra.
        assertEquals(listOf("K7"), clicks.codes)
    }

    @Test
    fun pedir_la_ficha_con_el_codigo_escrito_lo_avisa() {
        val clicks = showScanner(ScanUiState(code = "K7Q2M9XA"))

        compose.onNodeWithTag(ScanTags.MANUAL_SUBMIT).performClick()

        assertEquals(1, clicks.resolve)
    }

    @Test
    fun sin_permiso_de_camara_queda_la_alternativa_manual_sin_bloquearse() {
        // C-02 y C-09: negar la camara no deja la pantalla inservible.
        showScanner(ScanUiState(), cameraDenied = true)

        compose.onNodeWithTag(ScanTags.CAMERA_DENIED).assertIsDisplayed()
        compose.onNodeWithTag(ScanTags.MANUAL_CODE).assertIsDisplayed()
        compose.onNodeWithTag(ScanTags.MANUAL_SUBMIT).assertIsDisplayed()
    }

    @Test
    fun con_permiso_no_se_muestra_la_alternativa() {
        showScanner(ScanUiState(), cameraDenied = false)

        compose.onNodeWithTag(ScanTags.CAMERA_DENIED).assertDoesNotExist()
    }

    @Test
    fun mientras_resuelve_no_se_puede_pedir_dos_veces() {
        showScanner(ScanUiState(code = "K7Q2M9XA", resolving = true))

        compose.onNodeWithTag(ScanTags.MANUAL_SUBMIT).assertIsNotEnabled()
    }

    @Test
    fun un_codigo_que_falla_muestra_el_error_sin_salir_de_la_pantalla() {
        showScanner(ScanUiState(code = "MALO", error = "Ese codigo no existe."))

        compose.onNodeWithTag(ScanTags.ERROR).assertIsDisplayed()
        // La pantalla sigue siendo la del escaner: no se fue a ningun lado.
        compose.onNodeWithTag(ScanTags.SCANNER).assertIsDisplayed()
        compose.onNodeWithTag(ScanTags.MANUAL_CODE).assertIsDisplayed()
    }

    @Test
    fun sin_error_no_hay_aviso() {
        showScanner(ScanUiState())

        compose.onNodeWithTag(ScanTags.ERROR).assertDoesNotExist()
    }

    // ---- Ficha (T-011) ----

    private fun allergy(name: String, notes: String? = null) = Allergy(
        id = name,
        name = name,
        kind = AllergyKind.Allergy,
        notes = notes,
        active = true,
        origin = RecordOrigin.Owner,
    )

    private val vetFicha = ScanFicha(
        petId = "p1",
        name = "Nube",
        species = "Perro",
        breed = "Criollo",
        ageYears = 3,
        ageMonths = 2,
        allergies = listOf(allergy("Pollo", "Le provoca comezon")),
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
        dewormings = listOf(
            Deworming(
                id = "d1",
                product = "Drontal",
                appliedOn = "2026-05-01",
                type = null,
                appliedBy = null,
                nextOn = null,
                origin = RecordOrigin.Owner,
            ),
        ),
        weights = listOf(WeightRecord(id = "w1", measuredOn = "2026-06-01", kilograms = 12.4, origin = RecordOrigin.Owner)),
        consultations = listOf(
            FichaConsultation(
                date = "2026-06-02",
                businessName = "Vet Sur",
                reason = "Tos seca",
                diagnosis = "Traqueobronquitis",
                medications = listOf(Medication("Doxiciclina", "100 mg", "cada 12 h", "7 dias", null)),
            ),
        ),
    )

    /** Lo que le llega a una tienda o estetica: las listas clinicas vienen en null. */
    private val shopFicha = vetFicha.copy(
        vaccines = null,
        dewormings = null,
        weights = null,
        consultations = null,
    )

    private class FichaClicks {
        var closed = 0
        var consult = 0
        var vaccine = 0
        var deworm = 0
    }

    private fun showFicha(ficha: ScanFicha, isVet: Boolean): FichaClicks {
        val clicks = FichaClicks()
        compose.setContent {
            PetMateEdelTheme {
                FichaContent(
                    ficha = ficha,
                    isVet = isVet,
                    onClose = { clicks.closed += 1 },
                    onConsultClick = { clicks.consult += 1 },
                    onVaccineClick = { clicks.vaccine += 1 },
                    onDewormClick = { clicks.deworm += 1 },
                )
            }
        }
        return clicks
    }

    @Test
    fun la_ficha_muestra_los_basicos_de_la_mascota() {
        showFicha(vetFicha, isVet = true)

        compose.onNodeWithTag(ScanTags.FICHA).assertIsDisplayed()
        // Texto exacto: con subcadena, «Nube» coincide tambien con el titulo
        // «Ficha de Nube».
        compose.onNodeWithText("Nube").assertExists()
        compose.onNodeWithText("Perro", substring = true).assertExists()
    }

    @Test
    fun la_ficha_dice_que_es_de_solo_lectura() {
        showFicha(vetFicha, isVet = true)

        compose.onNodeWithText("Solo lectura", substring = true).assertExists()
    }

    @Test
    fun una_veterinaria_ve_la_entrada_a_registrar_consulta() {
        // C-14.
        showFicha(vetFicha, isVet = true)

        compose.onNodeWithTag(ScanTags.CLINICAL_ACTIONS).assertIsDisplayed()
        compose.onNodeWithText("Registrar consulta y receta", substring = true).assertIsDisplayed()
    }

    @Test
    fun una_tienda_o_estetica_no_ve_las_acciones_clinicas() {
        // C-15: lo que no es veterinaria no registra nada.
        showFicha(shopFicha, isVet = false)

        compose.onNodeWithTag(ScanTags.CLINICAL_ACTIONS).assertDoesNotExist()
        compose.onNodeWithText("Registrar consulta y receta", substring = true).assertDoesNotExist()
    }

    @Test
    fun a_la_tienda_se_le_explica_por_que_ve_menos() {
        showFicha(shopFicha, isVet = false)

        compose.onNodeWithTag(ScanTags.NOT_VET_NOTICE).assertIsDisplayed()
    }

    @Test
    fun la_tienda_no_ve_ninguna_seccion_clinica() {
        // C-03 y C-04 en pantalla. La proteccion real es que el servidor no lo
        // manda (T-006); esto solo confirma que la pantalla no lo pinta.
        showFicha(shopFicha, isVet = false)

        // Se comprueba la ausencia de los DATOS, no de las palabras: «Vacunas»
        // aparece dentro del propio aviso de «solo ves datos basicos y
        // alergias», asi que buscar la palabra daria un falso positivo.
        compose.onNodeWithText("Rabia", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Drontal", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Traqueobronquitis", substring = true).assertDoesNotExist()
        compose.onNodeWithText("12.4", substring = true).assertDoesNotExist()
    }

    @Test
    fun la_veterinaria_si_ve_las_secciones_que_llegaron() {
        showFicha(vetFicha, isVet = true)

        compose.onNodeWithText("Rabia", substring = true).assertExists()
        compose.onNodeWithText("Drontal", substring = true).assertExists()
    }

    @Test
    fun las_alergias_se_ven_para_los_dos_tipos_de_negocio() {
        // T-010: el banner es independiente de las secciones autorizadas.
        showFicha(shopFicha, isVet = false)

        compose.onNodeWithTag(AllergyBannerTags.ALERT).assertIsDisplayed()
    }

    @Test
    fun la_veterinaria_tambien_ve_las_alergias() {
        showFicha(vetFicha, isVet = true)

        compose.onNodeWithTag(AllergyBannerTags.ALERT).assertIsDisplayed()
    }

    @Test
    fun sin_alergias_la_ficha_lo_dice() {
        showFicha(vetFicha.copy(allergies = emptyList()), isVet = true)

        compose.onNodeWithTag(AllergyBannerTags.NONE).assertIsDisplayed()
    }

    @Test
    fun las_acciones_clinicas_avisan_cada_una_por_su_cuenta() {
        val clicks = showFicha(vetFicha, isVet = true)

        compose.onNodeWithText("Vacunar", substring = true).performClick()
        compose.onNodeWithText("Desparasitar", substring = true).performClick()
        compose.onNodeWithText("Registrar consulta y receta", substring = true).performClick()

        assertEquals(1, clicks.vaccine)
        assertEquals(1, clicks.deworm)
        assertEquals(1, clicks.consult)
    }

    @Test
    fun sin_identificador_de_mascota_no_se_ofrecen_acciones_clinicas() {
        // Sin petId no se puede registrar nada, asi que no se promete poder.
        showFicha(vetFicha.copy(petId = null), isVet = true)

        compose.onNodeWithTag(ScanTags.CLINICAL_ACTIONS).assertDoesNotExist()
    }

    @Test
    fun terminar_y_salir_cierra_la_ficha() {
        val clicks = showFicha(vetFicha, isVet = true)

        compose.onNodeWithText("Terminar y salir", substring = true).performClick()

        assertEquals(1, clicks.closed)
    }
}
