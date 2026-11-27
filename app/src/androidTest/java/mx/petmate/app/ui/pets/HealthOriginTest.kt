package mx.petmate.app.ui.pets

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Inmutabilidad y etiqueta de origen de un registro del carnet
 * (spec 008, T-011).
 *
 * Lo que registro un negocio no lo puede borrar el dueno: es el registro
 * clinico de un tercero. Lo que registro el dueno si.
 */
@RunWith(AndroidJUnit4::class)
class HealthOriginTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(origin: RecordOrigin, onDelete: (() -> Unit)?) {
        compose.setContent {
            PetMateEdelTheme {
                RecordCard(
                    title = "Rabia",
                    subtitle = "10 de marzo de 2026",
                    meta = emptyList(),
                    next = null,
                    origin = origin,
                    onDelete = onDelete,
                )
            }
        }
    }

    @Test
    fun un_registro_del_negocio_lleva_su_nombre_y_no_se_puede_borrar() {
        var deleted = 0
        show(RecordOrigin(byBusiness = true, businessName = "Vet Sur"), onDelete = { deleted += 1 })

        compose.onNodeWithText("Vet Sur", substring = true).assertExists()
        compose.onNodeWithText(context.getString(R.string.record_locked), substring = true).assertExists()
        // Aunque se le pase la accion de borrar, no se ofrece.
        compose.onNodeWithText(context.getString(R.string.record_delete), substring = true).assertDoesNotExist()
        assertEquals(0, deleted)
    }

    @Test
    fun un_registro_del_negocio_sin_nombre_dice_al_menos_que_es_de_un_negocio() {
        show(RecordOrigin(byBusiness = true, businessName = null), onDelete = null)

        compose.onNodeWithText(context.getString(R.string.record_by_business), substring = true).assertExists()
        compose.onNodeWithText(context.getString(R.string.record_delete), substring = true).assertDoesNotExist()
    }

    @Test
    fun un_registro_del_dueno_dice_que_lo_registro_el_y_si_se_puede_borrar() {
        var deleted = 0
        show(RecordOrigin.Owner, onDelete = { deleted += 1 })

        compose.onNodeWithText(context.getString(R.string.record_by_you), substring = true).assertExists()
        compose.onNodeWithText(context.getString(R.string.record_delete), substring = true).performClick()

        assertEquals(1, deleted)
    }

    @Test
    fun sin_accion_de_borrar_el_registro_del_dueno_tampoco_la_ofrece() {
        show(RecordOrigin.Owner, onDelete = null)

        compose.onNodeWithText(context.getString(R.string.record_by_you), substring = true).assertExists()
        compose.onNodeWithText(context.getString(R.string.record_delete), substring = true).assertDoesNotExist()
    }
}
