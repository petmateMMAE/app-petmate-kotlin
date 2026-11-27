package mx.petmate.app.ui.business

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.util.BusinessRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.perfilPublicoFalso
import mx.petmate.app.util.servicioFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Perfil publico de un negocio, el que ve cualquiera desde el mapa (spec 010). */
@OptIn(ExperimentalCoroutinesApi::class)
class BizProfileViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = BusinessRepositoryFalso()

    private fun viewModel(id: String = "b-7") = BizProfileViewModel(
        savedState = SavedStateHandle(mapOf(BizProfileViewModel.ARG_BUSINESS_ID to id)),
        repository = repository,
    )

    @Test
    fun `pide el negocio que venia en la navegacion, no otro`() = runTest(mainDispatcher.dispatcher) {
        repository.perfilPublico = AppResult.Success(perfilPublicoFalso(id = "b-7"))

        viewModel(id = "b-7")
        advanceUntilIdle()

        assertEquals("b-7", repository.ultimoIdDePerfil)
    }

    @Test
    fun `el perfil llega a la pantalla con sus servicios`() = runTest(mainDispatcher.dispatcher) {
        repository.perfilPublico = AppResult.Success(
            perfilPublicoFalso(
                name = "Veterinaria San Jorge",
                services = listOf(servicioFalso(name = "Consulta general", price = 350.0)),
            ),
        )

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("Veterinaria San Jorge", vm.state.value.business?.name)
        assertEquals(listOf("Consulta general"), vm.state.value.business?.services?.map { it.name })
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `arranca cargando para no ensenar una pantalla vacia`() = runTest(mainDispatcher.dispatcher) {
        repository.perfilPublico = AppResult.Success(perfilPublicoFalso())

        val vm = viewModel()

        assertTrue(vm.state.value.loading)
        assertNull(vm.state.value.business)
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `un negocio que ya no existe deja mensaje, no pantalla en blanco`() =
        runTest(mainDispatcher.dispatcher) {
            repository.perfilPublico = AppResult.Failure(AppError.Server(404))

            val vm = viewModel()
            advanceUntilIdle()

            assertFalse(vm.state.value.loading)
            assertNotNull(vm.state.value.error)
            assertNull(vm.state.value.business)
        }

    @Test
    fun `sin red tambien deja mensaje`() = runTest(mainDispatcher.dispatcher) {
        repository.perfilPublico = AppResult.Failure(AppError.Network)

        val vm = viewModel()
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `reintentar vuelve a pedirlo y limpia el error`() = runTest(mainDispatcher.dispatcher) {
        repository.perfilPublico = AppResult.Failure(AppError.Network)
        val vm = viewModel()
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        repository.perfilPublico = AppResult.Success(perfilPublicoFalso())
        vm.load()
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertNotNull(vm.state.value.business)
        assertEquals(2, repository.vecesQueSeLlamo("getPublicBusiness"))
    }
}
