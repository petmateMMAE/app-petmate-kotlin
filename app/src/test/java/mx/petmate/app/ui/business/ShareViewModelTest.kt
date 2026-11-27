package mx.petmate.app.ui.business

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AccessGrant
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.ShareDuration
import mx.petmate.app.domain.model.SharedData
import mx.petmate.app.ui.map.MapViewModel
import mx.petmate.app.ui.navigation.Route
import mx.petmate.app.util.BusinessRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.negocioCercanoFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compartir el historial de una mascota con un negocio (spec 009).
 *
 * Lo delicado aqui es **que se manda**: la seleccion de secciones y el negocio destino viajan al
 * backend, y equivocarse significa compartir mas de lo que el dueno eligio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = BusinessRepositoryFalso()

    private fun viewModel(petId: String = "p-1") = ShareViewModel(
        savedState = SavedStateHandle(mapOf(Route.ARG_PET_ID to petId)),
        repository = repository,
    )

    private fun concedido() = AppResult.Success(
        AccessGrant(id = "g-1", businessId = null, data = emptyList(), expiresAt = "2026-11-27T12:00:00Z"),
    )

    // ---- Veterinarias cercanas ----

    @Test
    fun `solo ofrece veterinarias, no tiendas ni esteticas`() = runTest(mainDispatcher.dispatcher) {
        // Compartir un historial clinico con una tienda de croquetas no tiene sentido.
        repository.cercanos = AppResult.Success(
            listOf(
                negocioCercanoFalso(id = "v-1", name = "Vet San Jorge", kind = BusinessKind.Veterinary),
                negocioCercanoFalso(id = "t-1", name = "Croquetas El Perro", kind = BusinessKind.Store),
                negocioCercanoFalso(id = "e-1", name = "Estetica Pelusa", kind = BusinessKind.Grooming),
            ),
        )
        val vm = viewModel()

        vm.loadVets(latitude = 20.0, longitude = -99.0)
        advanceUntilIdle()

        assertEquals(listOf("Vet San Jorge"), vm.state.value.vets.map { it.name })
        assertFalse(vm.state.value.loadingVets)
    }

    @Test
    fun `sin ubicacion usa el centro por omision en vez de no buscar nada`() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.loadVets(latitude = null, longitude = null)
            advanceUntilIdle()

            assertEquals(MapViewModel.DEFAULT_CENTER, repository.ultimasCoordenadas)
        }

    @Test
    fun `si falla la busqueda la lista queda vacia y deja de cargar`() =
        runTest(mainDispatcher.dispatcher) {
            // No es un error que merezca pantalla: el dueno todavia puede compartir con
            // "cualquier negocio que escanee".
            repository.cercanos = AppResult.Failure(AppError.Network)
            val vm = viewModel()

            vm.loadVets(20.0, -99.0)
            advanceUntilIdle()

            assertTrue(vm.state.value.vets.isEmpty())
            assertFalse(vm.state.value.loadingVets)
            assertNull(vm.state.value.error)
        }

    // ---- Lo que el dueno elige ----

    @Test
    fun `por omision se comparten vacunas, desparasitaciones y pesos`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        assertEquals(
            setOf(SharedData.Vaccines, SharedData.Dewormings, SharedData.Weights),
            vm.state.value.data,
        )
    }

    @Test
    fun `las consultas no se comparten sin que el dueno lo pida`() = runTest(mainDispatcher.dispatcher) {
        // Son lo mas sensible del historial: tienen diagnostico.
        val vm = viewModel()

        assertFalse(SharedData.Consultations in vm.state.value.data)
    }

    @Test
    fun `marcar y desmarcar una seccion`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.toggle(SharedData.Consultations)
        assertTrue(SharedData.Consultations in vm.state.value.data)

        vm.toggle(SharedData.Consultations)
        assertFalse(SharedData.Consultations in vm.state.value.data)
    }

    @Test
    fun `elegir negocio y duracion queda en el estado`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.selectBusiness("v-1")
        vm.duration(ShareDuration.ThirtyMinutes)

        assertEquals("v-1", vm.state.value.businessId)
        assertEquals(ShareDuration.ThirtyMinutes, vm.state.value.duration)
    }

    // ---- Compartir ----

    @Test
    fun `se manda exactamente lo que el dueno eligio`() = runTest(mainDispatcher.dispatcher) {
        // Esta es la prueba que importa: si se mandara otra cosa, se compartiria mas de lo pedido.
        repository.comparticion = concedido()
        val vm = viewModel(petId = "p-9")
        vm.selectBusiness("v-1")
        vm.duration(ShareDuration.SevenDays)
        vm.toggle(SharedData.Weights) // lo quita

        vm.share()
        advanceUntilIdle()

        val enviado = repository.ultimaComparticion
        assertNotNull(enviado)
        assertEquals("p-9", enviado?.petId)
        assertEquals("v-1", enviado?.businessId)
        assertEquals(ShareDuration.SevenDays, enviado?.duration)
        assertEquals(setOf(SharedData.Vaccines, SharedData.Dewormings), enviado?.data)
    }

    @Test
    fun `al compartir con un negocio concreto se avisa con su nombre`() = runTest(mainDispatcher.dispatcher) {
        repository.cercanos = AppResult.Success(listOf(negocioCercanoFalso(id = "v-1", name = "Vet San Jorge")))
        repository.comparticion = concedido()
        val vm = viewModel()
        vm.loadVets(20.0, -99.0)
        advanceUntilIdle()
        vm.selectBusiness("v-1")

        vm.share()
        advanceUntilIdle()

        assertEquals("Vet San Jorge", vm.state.value.sharedWith)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `sin negocio elegido se avisa que vale para cualquiera que escanee`() =
        runTest(mainDispatcher.dispatcher) {
            repository.comparticion = concedido()
            val vm = viewModel()

            vm.share()
            advanceUntilIdle()

            assertNull(vm.state.value.businessId)
            assertEquals("Cualquier negocio que escanee", vm.state.value.sharedWith)
        }

    @Test
    fun `dos toques al boton comparten una sola vez`() = runTest(mainDispatcher.dispatcher) {
        // Compartir dos veces crearia dos autorizaciones y dos QR distintos.
        repository.comparticion = concedido()
        val vm = viewModel()

        vm.share()
        vm.share()
        advanceUntilIdle()

        assertEquals(1, repository.vecesQueSeLlamo("shareAccess"))
    }

    @Test
    fun `un fallo deja mensaje y no dice que se compartio`() = runTest(mainDispatcher.dispatcher) {
        repository.comparticion = AppResult.Failure(AppError.Server(409))
        val vm = viewModel()

        vm.share()
        advanceUntilIdle()

        assertFalse(vm.state.value.saving)
        assertNotNull(vm.state.value.error)
        assertNull("No se puede decir que se compartio si fallo", vm.state.value.sharedWith)
    }

    @Test
    fun `cerrar el aviso lo quita del estado`() = runTest(mainDispatcher.dispatcher) {
        repository.comparticion = concedido()
        val vm = viewModel()
        vm.share()
        advanceUntilIdle()
        assertNotNull(vm.state.value.sharedWith)

        vm.dismissShared()

        assertNull(vm.state.value.sharedWith)
    }

    @Test
    fun `mientras comparte lo dice, para que no se toque dos veces`() = runTest(mainDispatcher.dispatcher) {
        repository.comparticion = concedido()
        val vm = viewModel()

        vm.share()

        assertTrue(vm.state.value.saving)
        advanceUntilIdle()
        assertFalse(vm.state.value.saving)
    }
}
