package mx.petmate.app.ui.permissionsdebug

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.domain.permissions.PermissionType
import mx.petmate.app.ui.components.permissions.PHOTO_FALLBACK_INITIALS_TAG
import mx.petmate.app.ui.components.permissions.PermissionFallbackTags
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cada negativa deja la app usable y explicada (spec 003, T-009, C-03 a C-06). */
@RunWith(AndroidJUnit4::class)
class PermissionMatrixTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        compose.setContent { PetMateEdelTheme { PermissionHarnessScreen() } }
    }

    private fun deny(type: PermissionType) {
        compose.onNodeWithTag(permissionSwitchTag(type)).performScrollTo().performClick()
    }

    @Test
    fun con_todo_concedido_no_hay_alternativas() {
        compose.onAllNodesWithTag(PermissionFallbackTags.NOTICE).assertCountEquals(0)
    }

    @Test
    fun sin_ubicacion_explica_el_centro_por_defecto() {
        deny(PermissionType.Location)
        compose.onNodeWithText(context.getString(R.string.permission_fallback_location_body)).assertIsDisplayed()
    }

    @Test
    fun sin_camara_lleva_al_codigo_manual() {
        deny(PermissionType.Camera)
        compose.onNodeWithText(context.getString(R.string.permission_fallback_camera_body)).assertIsDisplayed()
    }

    @Test
    fun sin_foto_muestra_iniciales() {
        deny(PermissionType.Photos)
        compose.onNodeWithTag(PHOTO_FALLBACK_INITIALS_TAG).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("L").assertIsDisplayed()
    }

    @Test
    fun sin_notificaciones_avisa_dentro_de_la_app() {
        deny(PermissionType.Notifications)
        compose.onNodeWithText(context.getString(R.string.permission_fallback_notifications_body))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun las_cuatro_negativas_a_la_vez_siguen_siendo_usables() {
        PermissionType.entries.forEach(::deny)
        compose.onAllNodesWithTag(PermissionFallbackTags.NOTICE).assertCountEquals(3)
        compose.onAllNodesWithTag(PHOTO_FALLBACK_INITIALS_TAG).assertCountEquals(1)
    }
}
