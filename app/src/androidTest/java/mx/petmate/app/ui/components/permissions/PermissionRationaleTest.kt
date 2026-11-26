package mx.petmate.app.ui.components.permissions

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.domain.permissions.PermissionType
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Explicacion previa, secuencia y rotacion (spec 003, T-003 y T-004, C-02, C-09). */
@RunWith(AndroidJUnit4::class)
class PermissionRationaleTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun la_explicacion_muestra_sus_textos_y_avisa_cada_boton() {
        var continued = 0
        var dismissed = 0
        compose.setContent {
            PetMateEdelTheme {
                PermissionRationale(PermissionType.Camera, onContinue = { continued++ }, onNotNow = { dismissed++ })
            }
        }

        compose.onNodeWithText(context.getString(R.string.permission_camera_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.permission_camera_body)).assertIsDisplayed()
        compose.onNodeWithTag(PermissionRationaleTags.CONFIRM).performClick()
        compose.onNodeWithTag(PermissionRationaleTags.DISMISS).performClick()

        assertEquals(1, continued)
        assertEquals(1, dismissed)
    }

    @Test
    fun pedir_muestra_primero_la_explicacion_y_sobrevive_a_la_rotacion() {
        assumeTrue(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            PetMateEdelTheme {
                val request = rememberPermissionRequest(PermissionType.Camera, onGranted = {})
                Button(onClick = request) { Text("pedir") }
            }
        }

        compose.onNodeWithText("pedir").performClick()
        compose.onNodeWithTag(PermissionRationaleTags.RATIONALE).assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag(PermissionRationaleTags.RATIONALE).assertIsDisplayed()
    }

    @Test
    fun ahora_no_cierra_la_explicacion_sin_lanzar_el_dialogo_del_sistema() {
        assumeTrue(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
        var denied = 0
        compose.setContent {
            PetMateEdelTheme {
                val request = rememberPermissionRequest(PermissionType.Camera, onGranted = {}, onDenied = { denied++ })
                Button(onClick = request) { Text("pedir") }
            }
        }

        compose.onNodeWithText("pedir").performClick()
        compose.onNodeWithTag(PermissionRationaleTags.DISMISS).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PermissionRationaleTags.RATIONALE).assertDoesNotExist()
        assertEquals(1, denied)
    }
}
