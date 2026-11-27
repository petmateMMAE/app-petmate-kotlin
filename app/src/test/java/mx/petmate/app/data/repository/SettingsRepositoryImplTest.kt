package mx.petmate.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.data.push.NotificationPreferencesStore
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Ajustes legibles sin conexion (spec 017, T-005, C-11).
 *
 * Se usa un DataStore de verdad sobre un archivo temporal, no un doble: lo que se comprueba es justamente que
 * el valor queda en disco y se vuelve a leer, y un doble en memoria no demostraria eso.
 *
 * No hay red en ninguna prueba: si los ajustes necesitaran el servidor para leerse, todas fallarian.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun newFile() = folder.newFolder().resolve("prefs.preferences_pb")

    private fun repository(scope: CoroutineScope, file: File) = SettingsRepositoryImpl(
        NotificationPreferencesStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })),
    )

    @Test
    fun `sin nada guardado devuelve los valores por defecto`() = runTest(UnconfinedTestDispatcher()) {
        val preferences = repository(backgroundScope, newFile()).observeNotificationPreferences().first()
        assertEquals(NotificationPreferences.DEFAULT, preferences)
    }

    @Test
    fun `apagar una categoria se refleja al observar`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository(backgroundScope, newFile())
        repository.setNotification(NotificationCategory.Health, on = false)
        val preferences = repository.observeNotificationPreferences().first()
        assertFalse(preferences.isEnabled(NotificationCategory.Health))
    }

    @Test
    fun `lo guardado sobrevive a cerrar y abrir la app`() = runTest(UnconfinedTestDispatcher()) {
        val file = newFile()
        // DataStore no admite dos instancias sobre el mismo archivo a la vez, asi que la primera se cancela:
        // eso es justamente lo que pasa cuando la app se cierra y suelta el archivo.
        val closedApp = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        repository(closedApp, file).setNotification(NotificationCategory.News, on = true)
        closedApp.cancel()

        val reopened = repository(backgroundScope, file).observeNotificationPreferences().first()
        assertTrue(reopened.isEnabled(NotificationCategory.News))
    }

    @Test
    fun `seguridad sigue encendida aunque se pida apagarla`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository(backgroundScope, newFile())
        val saved = repository.setNotification(NotificationCategory.Security, on = false)
        assertTrue(saved.isEnabled(NotificationCategory.Security))
    }
}
