package mx.petmate.app.ui.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.push.DeviceRegistration
import mx.petmate.app.domain.usecase.GetProfileUseCase
import mx.petmate.app.domain.usecase.LogoutUseCase
import mx.petmate.app.util.AuthRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.perfilFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pantalla de perfil (pm-037): carga, error y cierre de sesion.
 *
 * Usa `StandardTestDispatcher` y no `Unconfined` a proposito: con el no confinado las corrutinas
 * terminan antes de que la prueba pueda mirar el estado intermedio, y entonces las guardas contra
 * peticiones duplicadas no se podrian comprobar. Aqui el reloj lo mueve la prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = AuthRepositoryFalso()

    private fun viewModel() = HomeViewModel(
        getProfile = GetProfileUseCase(repository),
        // 001/T-039: el logout da de baja el dispositivo para push. Aqui no se prueba eso, solo
        // se satisface la dependencia con una que no hace nada.
        logout = LogoutUseCase(repository, NoDeviceRegistration),
    )

    // ---- start ----

    @Test
    fun `en modo demo no se le pide el perfil al servidor`() = runTest(mainDispatcher.dispatcher) {
        // "Probar sin cuenta" no tiene cuenta que consultar: pedirla daria un 401 y un error en
        // pantalla sin que el usuario haya hecho nada mal.
        val vm = viewModel()

        vm.start(userName = "Invitada", isGuest = true)
        advanceUntilIdle()

        assertFalse(repository.seLlamo("loadProfile"))
        assertEquals("Invitada", vm.uiState.value.userName)
        assertNull(vm.uiState.value.profile)
    }

    @Test
    fun `con sesion iniciada se carga el perfil y se muestra el nombre del servidor`() =
        runTest(mainDispatcher.dispatcher) {
            repository.responderPerfilCon(AppResult.Success(perfilFalso(name = "Ana Lopez")))
            val vm = viewModel()

            vm.start(userName = "Ana", isGuest = false)
            advanceUntilIdle()

            assertTrue(repository.seLlamo("loadProfile"))
            assertEquals("Ana Lopez", vm.uiState.value.userName)
            assertEquals("Ana Lopez", vm.uiState.value.profile?.name)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `volver a la pantalla no vuelve a pedir el perfil`() = runTest(mainDispatcher.dispatcher) {
        // El perfil ya esta en memoria: pedirlo otra vez cada vez que se entra a la pestana
        // gastaria datos del usuario sin ensenarle nada nuevo.
        val vm = viewModel()
        vm.start(userName = "Ana", isGuest = false)
        advanceUntilIdle()

        vm.start(userName = "Ana", isGuest = false)
        advanceUntilIdle()

        assertEquals(1, repository.vecesQueSeLlamo("loadProfile"))
    }

    @Test
    fun `al volver se conserva el nombre del perfil, no el que venia por parametro`() =
        runTest(mainDispatcher.dispatcher) {
            repository.responderPerfilCon(AppResult.Success(perfilFalso(name = "Ana Lopez")))
            val vm = viewModel()
            vm.start(userName = "Ana", isGuest = false)
            advanceUntilIdle()

            vm.start(userName = "nombre viejo de la sesion", isGuest = false)

            assertEquals("Ana Lopez", vm.uiState.value.userName)
        }

    // ---- refresh ----

    @Test
    fun `un fallo del servidor deja un mensaje y apaga el cargando`() =
        runTest(mainDispatcher.dispatcher) {
            repository.responderPerfilCon(AppResult.Failure(AppError.Timeout))
            val vm = viewModel()

            vm.refresh()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertNotNull("La pantalla necesita algo que ensenar", vm.uiState.value.error)
            assertNull(vm.uiState.value.profile)
        }

    @Test
    fun `mientras carga no se encima otra peticion`() = runTest(mainDispatcher.dispatcher) {
        // Dos toques rapidos al boton de recargar no deben salir dos veces a la red.
        val vm = viewModel()

        vm.refresh()
        vm.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.vecesQueSeLlamo("loadProfile"))
    }

    @Test
    fun `recargar despues de un error limpia el error anterior`() = runTest(mainDispatcher.dispatcher) {
        repository.responderPerfilCon(AppResult.Failure(AppError.Network))
        val vm = viewModel()
        vm.refresh()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        repository.responderPerfilCon(AppResult.Success(perfilFalso()))
        vm.refresh()
        advanceUntilIdle()

        assertNull("Un error viejo no puede quedarse en pantalla tras un reintento bueno", vm.uiState.value.error)
        assertNotNull(vm.uiState.value.profile)
    }

    @Test
    fun `mientras carga se avisa en el estado`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.refresh()

        assertTrue("Sin esto la pantalla se queda muda mientras espera", vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    // ---- cerrar sesion ----

    @Test
    fun `cerrar sesion llama al repositorio y no navega por su cuenta`() =
        runTest(mainDispatcher.dispatcher) {
            // Quien navega es el grafo al observar el estado de sesion: si esta pantalla navegara
            // tambien, habria dos dueños de la misma decision.
            val vm = viewModel()

            vm.onLogoutClicked()
            advanceUntilIdle()

            assertTrue(repository.seLlamo("logout"))
            assertFalse(vm.uiState.value.isLoggingOut)
        }

    @Test
    fun `dos toques al boton de cerrar sesion cierran una sola vez`() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.onLogoutClicked()
            vm.onLogoutClicked()
            advanceUntilIdle()

            assertEquals(1, repository.vecesQueSeLlamo("logout"))
        }

    @Test
    fun `mientras cierra sesion se avisa en el estado`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.onLogoutClicked()

        assertTrue(vm.uiState.value.isLoggingOut)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoggingOut)
    }

    @Test
    fun `si el servidor falla al cerrar sesion la pantalla no se queda colgada`() =
        runTest(mainDispatcher.dispatcher) {
            // El repositorio borra lo local aunque el servidor falle (spec 002, C-11); lo que no
            // puede pasar es que el boton se quede girando para siempre.
            repository.responderCierreDeSesionCon(AppResult.Failure(AppError.Network))
            val vm = viewModel()

            vm.onLogoutClicked()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoggingOut)
        }
}

/** `DeviceRegistration` que no hace nada, para las pruebas que no van de push. */
private object NoDeviceRegistration : DeviceRegistration {
    override suspend fun register() = Unit
    override suspend fun unregister() = Unit
}
