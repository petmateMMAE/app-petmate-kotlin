package mx.petmate.app.ui.navigation

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.usecase.ObserveSessionUseCase
import mx.petmate.app.util.AuthRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.usuarioFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * El ViewModel que decide que grafo se muestra (spec 002, C-07).
 *
 * Lo que de verdad importa aqui es que arranque en `Checking` y restaure la sesion **antes** de
 * pintar: si arrancara en `SignedOut`, el usuario veria el login y luego un salto al contenido
 * cada vez que abre la app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = AuthRepositoryFalso()

    private fun viewModel() = SessionViewModel(
        observeSession = ObserveSessionUseCase(repository),
        repository = repository,
    )

    @Test
    fun `al crearse restaura la sesion guardada`() = runTest {
        viewModel()

        assertTrue(
            "Sin esto la app abriria en el login aunque haya sesion guardada",
            repository.seLlamo("restoreSession"),
        )
    }

    @Test
    fun `arranca en Checking para que la pantalla no parpadee`() = runTest {
        val vm = viewModel()

        vm.sessionState.test {
            assertEquals(SessionState.Checking, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando la sesion queda iniciada el estado lo refleja`() = runTest {
        val vm = viewModel()
        val usuaria = usuarioFalso(name = "Ana Lopez")

        vm.sessionState.test {
            assertEquals(SessionState.Checking, awaitItem())

            repository.emitir(SessionState.SignedIn(usuaria))

            assertEquals(SessionState.SignedIn(usuaria), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando no hay sesion el estado pasa a SignedOut`() = runTest {
        val vm = viewModel()

        vm.sessionState.test {
            assertEquals(SessionState.Checking, awaitItem())

            repository.emitir(SessionState.SignedOut)

            assertEquals(SessionState.SignedOut, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `entrar al modo demo lo decide el repositorio, no el ViewModel`() = runTest {
        val vm = viewModel()

        vm.enterGuestMode()

        assertTrue(repository.seLlamo("enterGuestMode"))
    }

    @Test
    fun `salir del modo demo tambien se delega`() = runTest {
        val vm = viewModel()

        vm.exitGuestMode()

        assertTrue(repository.seLlamo("exitGuestMode"))
    }

    @Test
    fun `el modo demo se refleja en el estado`() = runTest {
        val vm = viewModel()

        vm.sessionState.test {
            assertEquals(SessionState.Checking, awaitItem())

            vm.enterGuestMode()

            assertEquals(SessionState.Guest, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cambiar a modo negocio se delega y se refleja`() = runTest {
        val vm = viewModel()
        val duena = usuarioFalso(isAffiliate = true)

        vm.sessionState.test {
            assertEquals(SessionState.Checking, awaitItem())
            repository.emitir(SessionState.SignedIn(duena))
            assertEquals(SessionState.SignedIn(duena, businessMode = false), awaitItem())

            vm.setBusinessMode(true)

            assertEquals(SessionState.SignedIn(duena, businessMode = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repository.seLlamo("setBusinessMode"))
    }

    @Test
    fun `volver a mis mascotas apaga el modo negocio`() = runTest {
        val vm = viewModel()
        val duena = usuarioFalso(isAffiliate = true)
        repository.emitir(SessionState.SignedIn(duena, businessMode = true))

        vm.sessionState.test {
            assertEquals(SessionState.SignedIn(duena, businessMode = true), awaitItem())

            vm.setBusinessMode(false)

            assertEquals(SessionState.SignedIn(duena, businessMode = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
