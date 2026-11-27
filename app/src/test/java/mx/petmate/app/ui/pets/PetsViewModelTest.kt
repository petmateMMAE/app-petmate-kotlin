package mx.petmate.app.ui.pets

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.PetRepositoryFalso
import mx.petmate.app.util.alergiaFalsa
import mx.petmate.app.util.carnetFalso
import mx.petmate.app.util.mascotaFalsa
import mx.petmate.app.util.vacunaFalsa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Lista de "Mis mascotas" (spec 007, T-004; constitucion P4, offline-first).
 *
 * La prueba que de verdad importa es la de sin red con datos guardados: **no debe salir error**.
 * Esa es la promesa de offline-first, y es la que se rompe primero cuando alguien reordena este
 * metodo sin pensarlo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = PetRepositoryFalso()

    private fun viewModel() = PetsViewModel(repository)

    /** Una fecha que siempre esta en el futuro, para que el proximo evento no caduque con el tiempo. */
    private val enElFuturo = "2099-03-01"

    @Test
    fun `las mascotas guardadas llegan a la pantalla`() = runTest(mainDispatcher.dispatcher) {
        repository.mascotas = AppResult.Success(
            listOf(mascotaFalsa(id = "p-1", name = "Firulais"), mascotaFalsa(id = "p-2", name = "Pelusa")),
        )
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf("Firulais", "Pelusa"), vm.uiState.value.pets.map { it.pet.name })
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `primero lo guardado y luego se pide al servidor`() = runTest(mainDispatcher.dispatcher) {
        // Offline-first: la pantalla abre al instante con lo del telefono y despues se actualiza.
        repository.mascotas = AppResult.Success(listOf(mascotaFalsa()))
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertTrue(repository.seLlamo("refreshPets"))
        assertTrue(
            "Se lee la base antes y despues de la red",
            repository.vecesQueSeLlamo("getPets") >= 2,
        )
    }

    @Test
    fun `sin red pero con datos guardados NO se muestra error`() = runTest(mainDispatcher.dispatcher) {
        // Esta es la promesa de offline-first. Si saliera error, el usuario creeria que perdio sus
        // mascotas cuando en realidad las tiene enfrente.
        repository.mascotas = AppResult.Success(listOf(mascotaFalsa(name = "Firulais")))
        repository.recarga = AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertNull("Con datos en pantalla, un fallo de red no es un error que mostrar", vm.uiState.value.error)
        assertEquals(listOf("Firulais"), vm.uiState.value.pets.map { it.pet.name })
    }

    @Test
    fun `sin red y sin nada guardado si se avisa`() = runTest(mainDispatcher.dispatcher) {
        // Aqui no hay nada que ensenar, asi que callarse dejaria una pantalla vacia sin explicacion.
        repository.mascotas = AppResult.Success(emptyList())
        repository.recarga = AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.pets.isEmpty())
    }

    @Test
    fun `arranca cargando solo cuando no hay nada que ensenar`() = runTest(mainDispatcher.dispatcher) {
        repository.mascotas = AppResult.Success(listOf(mascotaFalsa()))
        val vm = viewModel()

        vm.refresh()
        assertTrue("Sin datos todavia, se avisa que carga", vm.uiState.value.isLoading)
        advanceUntilIdle()

        // Segunda vuelta: ya hay mascotas en pantalla, no debe parpadear a "cargando".
        vm.refresh()
        assertFalse("Con datos en pantalla, recargar no debe borrarlos visualmente", vm.uiState.value.isLoading)
    }

    @Test
    fun `la etiqueta del proximo evento sale del carnet de esa mascota`() =
        runTest(mainDispatcher.dispatcher) {
            repository.mascotas = AppResult.Success(
                listOf(mascotaFalsa(id = "p-1", name = "Firulais"), mascotaFalsa(id = "p-2", name = "Pelusa")),
            )
            repository.carnetPorMascota["p-1"] = AppResult.Success(
                carnetFalso(vaccines = listOf(vacunaFalsa(name = "Rabia", nextDoseOn = enElFuturo))),
            )
            repository.carnetPorMascota["p-2"] = AppResult.Success(carnetFalso())
            val vm = viewModel()

            vm.refresh()
            advanceUntilIdle()

            val resumenes = vm.uiState.value.pets.associateBy { it.pet.name }
            assertEquals("Rabia", resumenes.getValue("Firulais").next?.name)
            assertTrue(resumenes.getValue("Firulais").next?.isVaccine == true)
            assertNull("Pelusa no tiene nada pendiente", resumenes.getValue("Pelusa").next)
        }

    @Test
    fun `solo se cuentan las alergias activas`() = runTest(mainDispatcher.dispatcher) {
        // Una alergia dada de baja no debe seguir apareciendo en la etiqueta.
        repository.mascotas = AppResult.Success(listOf(mascotaFalsa(id = "p-1")))
        repository.carnetPorMascota["p-1"] = AppResult.Success(
            carnetFalso(
                allergies = listOf(
                    alergiaFalsa(id = "a-1", active = true),
                    alergiaFalsa(id = "a-2", active = true),
                    alergiaFalsa(id = "a-3", active = false),
                ),
            ),
        )
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.pets.single().activeAllergies)
    }

    @Test
    fun `si el carnet de una mascota falla, la lista sigue apareciendo`() =
        runTest(mainDispatcher.dispatcher) {
            // Perder una etiqueta es molesto; perder la lista de mascotas es inaceptable.
            repository.mascotas = AppResult.Success(listOf(mascotaFalsa(id = "p-1", name = "Firulais")))
            repository.carnetPorMascota["p-1"] = AppResult.Failure(AppError.Timeout)
            val vm = viewModel()

            vm.refresh()
            advanceUntilIdle()

            assertEquals(listOf("Firulais"), vm.uiState.value.pets.map { it.pet.name })
            assertNull(vm.uiState.value.pets.single().next)
            assertEquals(0, vm.uiState.value.pets.single().activeAllergies)
        }

    @Test
    fun `si la base local falla y la red no arregla nada, se avisa`() =
        runTest(mainDispatcher.dispatcher) {
            repository.mascotas = AppResult.Failure(AppError.Unknown())
            repository.recarga = AppResult.Success(Unit)
            val vm = viewModel()

            vm.refresh()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
        }
}
