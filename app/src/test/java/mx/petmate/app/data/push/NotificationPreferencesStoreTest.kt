package mx.petmate.app.data.push

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.domain.permissions.PermissionGateway
import mx.petmate.app.domain.permissions.PermissionStatus
import mx.petmate.app.domain.permissions.PermissionType
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Preferencias que sobreviven al cierre y permiso al encender (spec 006, T-007 y T-009, C-02, C-03, C-07). */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(scope: CoroutineScope, file: File) =
        NotificationPreferencesStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))

    private fun newFile() = folder.newFolder().resolve("prefs.preferences_pb")

    @Test
    fun `sin nada guardado se usan los valores por defecto`() = runTest(UnconfinedTestDispatcher()) {
        assertEquals(NotificationPreferences.DEFAULT, store(backgroundScope, newFile()).current())
    }

    @Test
    fun `un cambio se ve de inmediato`() = runTest(UnconfinedTestDispatcher()) {
        val store = store(backgroundScope, newFile())

        val saved = store.set(NotificationCategory.Chat, on = false)

        assertFalse(saved.isEnabled(NotificationCategory.Chat))
        assertFalse(store.current().isEnabled(NotificationCategory.Chat))
    }

    @Test
    fun `lo guardado sobrevive a cerrar y abrir la app`() = runTest(UnconfinedTestDispatcher()) {
        val file = newFile()
        // La app se cierra: su DataStore se cancela y suelta el archivo.
        val closedApp = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        store(closedApp, file).set(NotificationCategory.News, on = true)
        closedApp.cancel()

        assertTrue(store(backgroundScope, file).current().isEnabled(NotificationCategory.News))
    }

    @Test
    fun `seguridad no se apaga ni guardando`() = runTest(UnconfinedTestDispatcher()) {
        val store = store(backgroundScope, newFile())

        store.set(NotificationCategory.Security, on = false)

        assertTrue(store.current().isEnabled(NotificationCategory.Security))
    }

    @Test
    fun `encender sin permiso del sistema pide el permiso y deja la categoria encendida`() =
        runTest(UnconfinedTestDispatcher()) {
            val gateway = mockk<PermissionGateway> {
                every { status(PermissionType.Notifications) } returns PermissionStatus.NotRequested
            }
            val activation = NotificationActivation(store(backgroundScope, newFile()), gateway)

            val result = activation.set(NotificationCategory.News, on = true)

            assertTrue(result is ActivationResult.SavedNeedsPermission)
            assertTrue(result.preferences?.isEnabled(NotificationCategory.News) == true)
        }

    @Test
    fun `con permiso concedido o al apagar no se pide nada`() = runTest(UnconfinedTestDispatcher()) {
        val gateway = mockk<PermissionGateway> {
            every { status(PermissionType.Notifications) } returns PermissionStatus.Granted
        }
        val activation = NotificationActivation(store(backgroundScope, newFile()), gateway)

        assertTrue(activation.set(NotificationCategory.News, on = true) is ActivationResult.Saved)
        assertTrue(activation.set(NotificationCategory.Chat, on = false) is ActivationResult.Saved)
    }

    @Test
    fun `intentar apagar seguridad no cambia nada`() = runTest(UnconfinedTestDispatcher()) {
        val activation = NotificationActivation(store(backgroundScope, newFile()), mockk(relaxed = true))

        assertEquals(ActivationResult.Locked, activation.set(NotificationCategory.Security, on = false))
    }
}
