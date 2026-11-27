package mx.petmate.app.ui.ficha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Banner de alergias (spec 013, T-010, criterio C-05).
 *
 * El banner no depende del tipo de negocio ni de las secciones autorizadas, asi
 * que se prueba solo, con y sin alergias.
 */
@RunWith(AndroidJUnit4::class)
class AllergyBannerTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(allergies: List<AllergyLine>) {
        compose.setContent { PetMateEdelTheme { AllergyBanner(allergies) } }
    }

    @Test
    fun con_alergias_se_pinta_el_aviso_rojo() {
        show(listOf(AllergyLine("Pollo", null)))

        compose.onNodeWithTag(AllergyBannerTags.ALERT).assertIsDisplayed()
        compose.onNodeWithTag(AllergyBannerTags.NONE).assertDoesNotExist()
    }

    @Test
    fun cada_alergia_aparece_con_su_nombre() {
        show(listOf(AllergyLine("Pollo", null), AllergyLine("Penicilina", null)))

        compose.onNodeWithText("Pollo", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Penicilina", substring = true).assertIsDisplayed()
    }

    @Test
    fun la_nota_de_la_alergia_se_muestra_junto_a_su_nombre() {
        show(listOf(AllergyLine("Pollo", "Le provoca comezón")))

        compose.onNodeWithText("Le provoca comezón", substring = true).assertIsDisplayed()
    }

    @Test
    fun sin_nota_no_queda_un_guion_suelto() {
        show(listOf(AllergyLine("Pollo", null)))

        compose.onNodeWithText("Pollo —", substring = true).assertDoesNotExist()
    }

    @Test
    fun sin_alergias_lo_dice_en_vez_de_desaparecer() {
        // C-05: un hueco en blanco se lee como «no es alergico», y eso no es lo
        // mismo que «no hay nada registrado».
        show(emptyList())

        compose.onNodeWithTag(AllergyBannerTags.NONE).assertIsDisplayed()
        compose.onNodeWithText("Sin alergias registradas.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(AllergyBannerTags.ALERT).assertDoesNotExist()
    }
}
