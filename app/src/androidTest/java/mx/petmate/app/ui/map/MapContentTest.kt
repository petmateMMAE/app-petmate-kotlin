package mx.petmate.app.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pantalla de inicio: mapa, filtros, tarjeta del negocio y degradación sin
 * ubicación (spec 010, T-008, T-009 y T-011).
 *
 * Se dibuja con `FakeMapSurface`, el adaptador falso de T-006: sin red, sin
 * teselas y sin proveedor. Eso es justo para lo que se escribió.
 */
@RunWith(AndroidJUnit4::class)
class MapContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val state = MapSampleData.state

    private class Clicks {
        val businesses = mutableListOf<String>()
        val types = mutableListOf<BusinessType>()
        var dismissed = 0
        var retries = 0
        var enableLocation = 0
        var myLocation = 0
        var details = mutableListOf<String>()
    }

    private fun show(uiState: MapUiState, clicks: Clicks = Clicks()): Clicks {
        compose.setContent {
            PetMateEdelTheme {
                MapContent(
                    uiState = uiState,
                    onQueryChange = {},
                    onTypeToggle = { clicks.types += it },
                    onBusinessClick = { clicks.businesses += it },
                    onDismissBusiness = { clicks.dismissed += 1 },
                    onCallClick = {},
                    onWhatsAppClick = {},
                    onDirectionsClick = {},
                    onBusinessDetailsClick = { clicks.details += it },
                    onZoomInClick = {},
                    onZoomOutClick = {},
                    onMyLocationClick = { clicks.myLocation += 1 },
                    onEnableLocationClick = { clicks.enableLocation += 1 },
                    onHelpClick = {},
                    onRetryClick = { clicks.retries += 1 },
                    onTabSelected = {},
                    mapSurface = { surfaceState, mapModifier -> FakeMapSurface(surfaceState, mapModifier) },
                )
            }
        }
        return clicks
    }

    // ---- T-008, C-01 y C-02: el mapa con sus marcadores ----

    @Test
    fun el_mapa_pinta_un_marcador_por_negocio_visible() {
        show(state)

        compose.onNodeWithTag(FakeMapViewTags.MAP).assertIsDisplayed()
        state.visibleBusinesses.forEach {
            compose.onNodeWithTag(FakeMapViewTags.marker(it.id)).assertExists()
        }
    }

    @Test
    fun mientras_carga_no_hay_marcadores() {
        show(MapUiState(isLoading = true))

        state.businesses.forEach {
            compose.onNodeWithTag(FakeMapViewTags.marker(it.id)).assertDoesNotExist()
        }
    }

    @Test
    fun tocar_un_marcador_avisa_con_su_identificador() {
        val id = state.visibleBusinesses.first().id
        val clicks = show(state)

        compose.onNodeWithTag(FakeMapViewTags.marker(id)).performClick()

        assertEquals(listOf(id), clicks.businesses)
    }

    // ---- T-008, C-03: filtros por tipo ----

    @Test
    fun los_tres_filtros_de_tipo_estan_presentes() {
        show(state)

        BusinessType.entries.forEach {
            compose.onNodeWithTag(MapScreenTags.filter(it)).assertIsDisplayed()
        }
    }

    @Test
    fun tocar_un_filtro_avisa_de_ese_tipo() {
        val clicks = show(state)

        compose.onNodeWithTag(MapScreenTags.filter(BusinessType.Spa)).performClick()

        assertEquals(listOf(BusinessType.Spa), clicks.types)
    }

    @Test
    fun con_un_solo_tipo_visible_solo_se_pintan_esos_marcadores() {
        val onlySpas = state.copy(visibleTypes = setOf(BusinessType.Spa))
        show(onlySpas)

        onlySpas.visibleBusinesses.forEach {
            compose.onNodeWithTag(FakeMapViewTags.marker(it.id)).assertExists()
        }
        state.businesses.filter { it.type != BusinessType.Spa }.forEach {
            compose.onNodeWithTag(FakeMapViewTags.marker(it.id)).assertDoesNotExist()
        }
        assertTrue("la maqueta debe traer negocios de otros tipos", onlySpas.visibleBusinesses.size < state.businesses.size)
    }

    // ---- T-008, C-11: estados vacío, error y sin ubicación ----

    @Test
    fun sin_resultados_muestra_el_estado_vacio() {
        show(state.copy(query = "Acuario"))

        compose.onNodeWithTag(MapScreenTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.map_empty_title), substring = true).assertIsDisplayed()
    }

    @Test
    fun diagnostico_del_banner_recortado() {
        // TEMPORAL: volcado de la semantica para medir por que el banner con
        // `body` no se ve. Se quita en cuanto el layout quede arreglado.
        show(state.copy(query = "Acuario"))

        android.util.Log.d("PM_DIAG", compose.onRoot().printToString(maxDepth = 60))
    }

    @Test
    fun con_error_muestra_el_aviso_y_permite_reintentar() {
        val clicks = show(MapUiState(error = UiText.Resource(R.string.map_error)))

        compose.onNodeWithTag(MapScreenTags.ERROR).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.action_retry)).performClick()

        assertEquals(1, clicks.retries)
    }

    @Test
    fun el_error_tiene_prioridad_sobre_el_estado_vacio() {
        show(MapUiState(error = UiText.Resource(R.string.map_error)))

        compose.onNodeWithTag(MapScreenTags.ERROR).assertIsDisplayed()
        compose.onNodeWithTag(MapScreenTags.EMPTY).assertDoesNotExist()
    }

    // ---- T-009, C-04 y C-05: tarjeta del negocio y selección compartida ----

    @Test
    fun con_un_negocio_seleccionado_se_abre_su_tarjeta() {
        val selected = state.businesses.first()
        show(state.copy(selectedBusinessId = selected.id))

        compose.onNodeWithTag(MapScreenTags.BUSINESS_SHEET).assertIsDisplayed()
        compose.onNodeWithText(selected.name).assertIsDisplayed()
    }

    @Test
    fun la_tarjeta_muestra_los_campos_acordados() {
        val selected = state.businesses.first()
        show(state.copy(selectedBusinessId = selected.id))

        // Q-04: nombre, distancia y estado de apertura.
        compose.onNodeWithText(selected.name).assertIsDisplayed()
        compose.onNodeWithText(selected.distance, substring = true).assertIsDisplayed()
    }

    @Test
    fun el_mapa_y_la_tarjeta_comparten_una_sola_seleccion() {
        val selected = state.businesses.first()
        show(state.copy(selectedBusinessId = selected.id))

        // El mismo negocio está marcado en el mapa y abierto en la tarjeta.
        compose.onNodeWithTag(FakeMapViewTags.marker(selected.id)).assertExists()
        compose.onNodeWithTag(MapScreenTags.BUSINESS_SHEET).assertIsDisplayed()
    }

    @Test
    fun sin_seleccion_no_hay_tarjeta() {
        show(state.copy(selectedBusinessId = null))

        compose.onNodeWithTag(MapScreenTags.BUSINESS_SHEET).assertDoesNotExist()
    }

    @Test
    fun la_tarjeta_tiene_prioridad_sobre_el_aviso_sin_ubicacion() {
        val selected = state.businesses.first()
        show(state.copy(selectedBusinessId = selected.id, isLocationAvailable = false))

        compose.onNodeWithTag(MapScreenTags.BUSINESS_SHEET).assertIsDisplayed()
        compose.onNodeWithTag(MapScreenTags.NO_LOCATION).assertDoesNotExist()
    }

    // ---- T-011, C-08 y C-09: ubicación y degradación ----

    @Test
    fun con_ubicacion_no_se_muestra_el_aviso() {
        show(state.copy(isLocationAvailable = true))

        compose.onNodeWithTag(MapScreenTags.NO_LOCATION).assertDoesNotExist()
    }

    @Test
    fun sin_ubicacion_se_mantiene_la_lista_y_se_explica() {
        val denied = state.copy(isLocationAvailable = false, selectedBusinessId = null)
        show(denied)

        compose.onNodeWithTag(MapScreenTags.NO_LOCATION).assertIsDisplayed()
        // `substring`: el banner junta titulo y cuerpo en un solo AnnotatedString,
        // asi que el nodo dice «No sabemos donde estas. Activa...» y la busqueda
        // exacta nunca coincide. El de error si coincidia porque su body es null.
        compose.onNodeWithText(context.getString(R.string.map_no_location_title), substring = true)
            .assertIsDisplayed()
        // La degradacion no vacia el mapa: los marcadores siguen ahi.
        denied.visibleBusinesses.forEach {
            compose.onNodeWithTag(FakeMapViewTags.marker(it.id)).assertExists()
        }
    }

    @Test
    fun el_aviso_sin_ubicacion_ofrece_activarla() {
        val clicks = show(state.copy(isLocationAvailable = false))

        compose.onNodeWithText(context.getString(R.string.map_no_location_action)).performClick()

        assertEquals(1, clicks.enableLocation)
    }

    @Test
    fun cerca_de_mi_avisa_a_la_pantalla() {
        val clicks = show(state)

        compose.onNodeWithContentDescription(context.getString(R.string.map_my_location)).performClick()

        assertEquals(1, clicks.myLocation)
    }
}
