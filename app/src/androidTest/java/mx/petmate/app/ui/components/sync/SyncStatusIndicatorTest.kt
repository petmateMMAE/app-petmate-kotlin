package mx.petmate.app.ui.components.sync

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.petmate.app.R
import mx.petmate.app.domain.sync.SyncStatus
import mx.petmate.app.ui.theme.PetMateEdelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** El indicador pinta pendiente, sincronizando y error, y el error permite reintentar (spec 005, T-008, C-11). */
@RunWith(AndroidJUnit4::class)
class SyncStatusIndicatorTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(status: SyncStatus, onRetry: () -> Unit = {}) {
        compose.setContent { PetMateEdelTheme { SyncStatusIndicator(status = status, onRetry = onRetry) } }
    }

    @Test
    fun sincronizado_no_pinta_nada() {
        show(SyncStatus.Synced)

        compose.onNodeWithTag(SyncStatusIndicatorTags.BADGE).assertDoesNotExist()
        compose.onNodeWithTag(SyncStatusIndicatorTags.RETRY).assertDoesNotExist()
    }

    @Test
    fun pendiente_muestra_su_etiqueta_sin_reintentar() {
        show(SyncStatus.Pending)

        compose.onNodeWithText(context.getString(R.string.sync_status_pending)).assertIsDisplayed()
        compose.onNodeWithTag(SyncStatusIndicatorTags.RETRY).assertDoesNotExist()
    }

    @Test
    fun sincronizando_muestra_su_etiqueta_sin_reintentar() {
        show(SyncStatus.Syncing)

        compose.onNodeWithText(context.getString(R.string.sync_status_syncing)).assertIsDisplayed()
        compose.onNodeWithTag(SyncStatusIndicatorTags.RETRY).assertDoesNotExist()
    }

    @Test
    fun error_muestra_su_etiqueta_y_reintentar_avisa() {
        var retries = 0
        show(SyncStatus.Error) { retries++ }

        compose.onNodeWithText(context.getString(R.string.sync_status_error)).assertIsDisplayed()
        compose.onNodeWithTag(SyncStatusIndicatorTags.RETRY).performClick()

        assertEquals(1, retries)
    }
}
