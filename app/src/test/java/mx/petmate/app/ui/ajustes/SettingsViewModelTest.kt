package mx.petmate.app.ui.ajustes

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mx.petmate.app.R
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences
import mx.petmate.app.domain.repository.SettingsRepository
import mx.petmate.app.domain.usecase.LogoutUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pantalla de ajustes: interruptores, regla de seguridad y cerrar sesion (spec 017, T-006, T-008). */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settings: SettingsRepository = mockk(relaxed = true)
    private val logout: LogoutUseCase = mockk(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()
    private val preferences = MutableStateFlow(NotificationPreferences.DEFAULT)
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settings.observeNotificationPreferences() } returns preferences
        viewModel = SettingsViewModel(settings, logout)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `lista todas las categorias al cargar`() {
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(NotificationCategory.entries.size, state.toggles.size)
        // El orden es el del enum: los interruptores no se mueven entre cargas.
        assertEquals(NotificationCategory.entries.toList(), state.toggles.map { it.category })
    }

    @Test
    fun `novedades viene apagada y salud encendida por defecto`() {
        val toggles = viewModel.state.value.toggles.associateBy { it.category }
        assertFalse(toggles.getValue(NotificationCategory.News).isOn)
        assertTrue(toggles.getValue(NotificationCategory.Health).isOn)
    }

    @Test
    fun `seguridad no se puede apagar`() {
        val security = viewModel.state.value.toggles.first { it.category == NotificationCategory.Security }
        assertTrue(security.isOn)
        assertFalse(security.canBeDisabled)
    }

    @Test
    fun `apagar seguridad no llega al repositorio`() {
        viewModel.toggle(NotificationCategory.Security, on = false)
        coVerify(exactly = 0) { settings.setNotification(NotificationCategory.Security, any()) }
    }

    @Test
    fun `encender una categoria la guarda`() {
        viewModel.toggle(NotificationCategory.News, on = true)
        coVerify(exactly = 1) { settings.setNotification(NotificationCategory.News, true) }
    }

    @Test
    fun `el estado sigue lo que emite el repositorio`() {
        preferences.value = NotificationPreferences.of(setOf(NotificationCategory.News))
        val toggles = viewModel.state.value.toggles.associateBy { it.category }
        assertTrue(toggles.getValue(NotificationCategory.News).isOn)
        assertFalse(toggles.getValue(NotificationCategory.Health).isOn)
    }

    @Test
    fun `cerrar sesion con exito marca la salida`() {
        coEvery { logout() } returns AppResult.Success(Unit)
        viewModel.logOut()
        val state = viewModel.state.value
        assertTrue(state.loggedOut)
        assertFalse(state.isLoggingOut)
        assertNull(state.error)
    }

    @Test
    fun `cerrar sesion con fallo muestra error y no sale`() {
        coEvery { logout() } returns AppResult.Failure(AppError.Network)
        viewModel.logOut()
        val state = viewModel.state.value
        assertFalse(state.loggedOut)
        assertEquals(R.string.settings_error_logout, state.error)
    }

    @Test
    fun `descartar el error lo limpia`() {
        coEvery { logout() } returns AppResult.Failure(AppError.Network)
        viewModel.logOut()
        viewModel.dismissError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `sin enlaces legales configurados no se ofrece ninguno`() {
        assertFalse(viewModel.state.value.legalLinks.isConfigured)
    }
}
