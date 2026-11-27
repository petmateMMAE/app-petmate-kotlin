package mx.petmate.app.ui.panel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.domain.model.FollowUpAlert
import mx.petmate.app.domain.usecase.ReviewDateClass
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lista de alertas: color por severidad, navegacion con el identificador
 * correcto y estado vacio (spec 012, T-011, C-07 y C-10).
 */
@RunWith(AndroidJUnit4::class)
class AlertListTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val overdue =
        FollowUpAlert("f1", "p1", "Nube", "2026-11-20", ReviewDateClass.Overdue, unreadMessages = 2)
    private val urgent = FollowUpAlert("f2", "p2", "Rocco", "2026-11-27", ReviewDateClass.Urgent)
    private val normal = FollowUpAlert("f3", "p3", "Luna", "2027-05-01", ReviewDateClass.Normal)
    private val noDate = FollowUpAlert("f4", "p4", "Milo", null, ReviewDateClass.None)

    private fun show(alerts: List<FollowUpAlert>, onClick: (String) -> Unit = {}) {
        compose.setContent { PetMateEdelTheme { AlertList(alerts = alerts, onAlertClick = onClick) } }
    }

    // ---- C-10: estado vacio explicativo ----

    @Test
    fun sin_alertas_muestra_el_estado_vacio_explicativo() {
        show(emptyList())

        compose.onNodeWithTag(AlertListTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dashboard_alerts_empty_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dashboard_alerts_empty_body)).assertIsDisplayed()
        compose.onNodeWithTag(AlertListTags.LIST).assertDoesNotExist()
    }

    @Test
    fun con_alertas_no_muestra_el_estado_vacio() {
        show(listOf(overdue))

        compose.onNodeWithTag(AlertListTags.LIST).assertIsDisplayed()
        compose.onNodeWithTag(AlertListTags.EMPTY).assertDoesNotExist()
    }

    // ---- C-07: navegacion con el identificador correcto ----

    @Test
    fun tocar_una_alerta_navega_con_su_identificador() {
        var clicked: String? = null
        show(listOf(overdue, urgent)) { clicked = it }

        compose.onNodeWithTag(AlertListTags.item("f2")).performClick()

        assertEquals("f2", clicked)
    }

    @Test
    fun cada_alerta_manda_su_propio_identificador() {
        val clicks = mutableListOf<String>()
        show(listOf(overdue, urgent, normal)) { clicks += it }

        compose.onNodeWithTag(AlertListTags.item("f3")).performClick()
        compose.onNodeWithTag(AlertListTags.item("f1")).performClick()

        assertEquals(listOf("f3", "f1"), clicks)
    }

    @Test
    fun sin_tocar_nada_no_navega() {
        var clicked: String? = null
        show(listOf(overdue)) { clicked = it }

        assertEquals(null, clicked)
    }

    // ---- Lo que se pinta ----

    @Test
    fun cada_alerta_pinta_el_nombre_de_su_mascota() {
        show(listOf(overdue, urgent, normal, noDate))

        listOf("Nube", "Rocco", "Luna", "Milo").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun una_revision_atrasada_se_dice_con_palabras_no_solo_con_color() {
        show(listOf(overdue))

        compose.onNodeWithText(context.getString(R.string.dashboard_alert_overdue)).assertIsDisplayed()
    }

    @Test
    fun una_alerta_sin_fecha_lo_dice() {
        show(listOf(noDate))

        compose.onNodeWithText(context.getString(R.string.dashboard_alert_no_review)).assertIsDisplayed()
    }

    @Test
    fun la_fecha_de_revision_se_muestra() {
        show(listOf(urgent))

        compose.onNodeWithText(context.getString(R.string.dashboard_alert_review, "2026-11-27"))
            .assertIsDisplayed()
    }

    @Test
    fun los_mensajes_sin_leer_se_cuentan() {
        show(listOf(overdue))

        val expected = context.resources.getQuantityString(R.plurals.dashboard_alert_unread, 2, 2)
        compose.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun sin_mensajes_sin_leer_no_se_muestra_el_conteo() {
        show(listOf(urgent))

        val zero = context.resources.getQuantityString(R.plurals.dashboard_alert_unread, 0, 0)
        compose.onNodeWithText(zero).assertDoesNotExist()
    }

    @Test
    fun una_alerta_normal_no_lleva_distintivo_de_urgencia() {
        show(listOf(normal))

        compose.onNodeWithText(context.getString(R.string.dashboard_alert_overdue)).assertDoesNotExist()
    }
}
