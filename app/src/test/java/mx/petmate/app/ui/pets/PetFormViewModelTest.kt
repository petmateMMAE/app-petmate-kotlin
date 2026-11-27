package mx.petmate.app.ui.pets

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.PetRepositoryFalso
import mx.petmate.app.util.mascotaFalsa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Alta y edicion de una mascota (spec 007, A-01, A-02, A-07).
 *
 * La misma pantalla sirve para crear y para editar, y lo distingue por si venia un id en la
 * navegacion. Varias de estas pruebas existen justo para que esa bifurcacion no se confunda: crear
 * cuando deberia editar significa duplicar la mascota del usuario.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetFormViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = PetRepositoryFalso()

    private fun nueva() = PetFormViewModel(repository, SavedStateHandle(emptyMap()))

    private fun editando(id: String = "p-1") =
        PetFormViewModel(repository, SavedStateHandle(mapOf(PetFormViewModel.ARG_PET_ID to id)))

    private fun PetFormViewModel.llenarBien() {
        onName("Firulais")
        onSpecies(PetSpecies.Dog)
        onBreed("Criollo")
    }

    // ---- Alta contra edicion ----

    @Test
    fun `sin id en la navegacion es alta y no pide nada al servidor`() =
        runTest(mainDispatcher.dispatcher) {
            val vm = nueva()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isEdit)
            assertFalse(vm.uiState.value.isLoading)
            assertFalse(repository.seLlamo("getPet"))
        }

    @Test
    fun `con id es edicion y carga los datos de esa mascota`() = runTest(mainDispatcher.dispatcher) {
        repository.mascotas = AppResult.Success(
            listOf(mascotaFalsa(id = "p-1", name = "Firulais", breed = "Criollo", sex = PetSex.Male)),
        )
        val vm = editando("p-1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEdit)
        assertEquals("Firulais", vm.uiState.value.name)
        assertEquals("Criollo", vm.uiState.value.breed)
        assertEquals(PetSpecies.Dog, vm.uiState.value.species)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `al editar arranca cargando para no ensenar un formulario vacio`() =
        runTest(mainDispatcher.dispatcher) {
            repository.mascotas = AppResult.Success(listOf(mascotaFalsa(id = "p-1")))
            val vm = editando("p-1")

            assertTrue(vm.uiState.value.isLoading)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `si no se puede cargar la mascota se avisa`() = runTest(mainDispatcher.dispatcher) {
        repository.mascotas = AppResult.Success(emptyList()) // getPet no la encuentra
        val vm = editando("p-9")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.message)
    }

    // ---- Escribir en el formulario ----

    @Test
    fun `tocar dos veces el mismo sexo lo deja sin dato`() = runTest(mainDispatcher.dispatcher) {
        // Es opcional: el usuario tiene que poder deshacer su eleccion sin reiniciar la pantalla.
        val vm = nueva()

        vm.onSex(PetSex.Female)
        assertEquals(PetSex.Female, vm.uiState.value.sex)

        vm.onSex(PetSex.Female)
        assertEquals(PetSex.Unknown, vm.uiState.value.sex)
    }

    @Test
    fun `la fecha de nacimiento se puede poner y se puede quitar`() = runTest(mainDispatcher.dispatcher) {
        // Tambien es opcional: hay mascotas adoptadas sin fecha conocida, y poner una inventada
        // ensuciaria la edad que calcula el carnet.
        val vm = nueva()

        vm.onBirthDate("2022-05-10")
        assertEquals("2022-05-10", vm.uiState.value.birthDate)

        vm.onBirthDate(null)
        assertNull(vm.uiState.value.birthDate)
    }

    @Test
    fun `elegir una fecha borra el error de la fecha`() = runTest(mainDispatcher.dispatcher) {
        val vm = nueva()
        vm.onBirthDate("2099-01-01")
        vm.save()
        assertTrue(
            vm.uiState.value.errors.containsKey(mx.petmate.app.domain.usecase.PetRules.Field.BirthDate),
        )

        vm.onBirthDate("2022-05-10")

        assertFalse(
            "El error de la fecha debe irse al corregirla",
            vm.uiState.value.errors.containsKey(mx.petmate.app.domain.usecase.PetRules.Field.BirthDate),
        )
    }

    @Test
    fun `corregir un campo borra su error`() = runTest(mainDispatcher.dispatcher) {
        val vm = nueva()
        vm.save() // sin nombre ni especie: deja errores
        assertTrue(vm.uiState.value.errors.isNotEmpty())

        vm.onName("Firulais")

        assertFalse(
            "El error del nombre debe irse al escribirlo",
            vm.uiState.value.errors.containsKey(mx.petmate.app.domain.usecase.PetRules.Field.Name),
        )
    }

    @Test
    fun `el aviso se puede cerrar`() = runTest(mainDispatcher.dispatcher) {
        repository.alta = AppResult.Failure(AppError.Network)
        val vm = nueva()
        vm.llenarBien()
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.message)

        vm.messageShown()

        assertNull(vm.uiState.value.message)
    }

    // ---- Guardar ----

    @Test
    fun `un formulario incompleto no sale a la red`() = runTest(mainDispatcher.dispatcher) {
        // Las reglas son del dominio (PetRules): aqui lo que se comprueba es que se respeten
        // antes de gastar una peticion.
        val vm = nueva()

        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errors.isNotEmpty())
        assertFalse(repository.seLlamo("createPet"))
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `al dar de alta se manda el borrador completo`() = runTest(mainDispatcher.dispatcher) {
        repository.alta = AppResult.Success(mascotaFalsa(id = "p-nueva"))
        val vm = nueva()
        vm.llenarBien()
        vm.onSex(PetSex.Male)

        vm.save()
        advanceUntilIdle()

        val enviado = repository.ultimoBorrador
        assertEquals("Firulais", enviado?.name)
        assertEquals(PetSpecies.Dog, enviado?.species)
        assertEquals("Criollo", enviado?.breed)
        assertEquals(PetSex.Male, enviado?.sex)
    }

    @Test
    fun `al dar de alta la pantalla recibe el id para ir al carnet`() =
        runTest(mainDispatcher.dispatcher) {
            repository.alta = AppResult.Success(mascotaFalsa(id = "p-nueva"))
            val vm = nueva()
            vm.llenarBien()

            vm.save()
            advanceUntilIdle()

            assertEquals("p-nueva", vm.uiState.value.savedPetId)
            assertFalse(vm.uiState.value.isSaving)
        }

    @Test
    fun `al editar se actualiza, no se crea otra mascota`() = runTest(mainDispatcher.dispatcher) {
        // Si llamara a createPet, el usuario acabaria con la mascota duplicada.
        repository.mascotas = AppResult.Success(listOf(mascotaFalsa(id = "p-1")))
        repository.edicion = AppResult.Success(mascotaFalsa(id = "p-1"))
        val vm = editando("p-1")
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertTrue(repository.seLlamo("updatePet"))
        assertFalse(repository.seLlamo("createPet"))
        assertEquals("p-1", repository.ultimoIdEditado)
    }

    @Test
    fun `dos toques al boton guardan una sola vez`() = runTest(mainDispatcher.dispatcher) {
        repository.alta = AppResult.Success(mascotaFalsa())
        val vm = nueva()
        vm.llenarBien()

        vm.save()
        vm.save()
        advanceUntilIdle()

        assertEquals(1, repository.vecesQueSeLlamo("createPet"))
    }

    @Test
    fun `si el servidor falla al guardar se avisa y no se navega`() =
        runTest(mainDispatcher.dispatcher) {
            repository.alta = AppResult.Failure(AppError.Server(500))
            val vm = nueva()
            vm.llenarBien()

            vm.save()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.message)
            assertNull("No se puede navegar al carnet de algo que no se guardo", vm.uiState.value.savedPetId)
            assertFalse(vm.uiState.value.isSaving)
        }

    // ---- Borrar ----

    @Test
    fun `al borrar se avisa a la pantalla`() = runTest(mainDispatcher.dispatcher) {
        repository.mascotas = AppResult.Success(listOf(mascotaFalsa(id = "p-1")))
        repository.borrado = AppResult.Success(Unit)
        val vm = editando("p-1")
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.deleted)
        assertEquals("p-1", repository.ultimoIdBorrado)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `en un alta no hay nada que borrar`() = runTest(mainDispatcher.dispatcher) {
        // Sin id no hay mascota en el servidor: llamar a deletePet seria borrar quien sabe que.
        val vm = nueva()

        vm.delete()
        advanceUntilIdle()

        assertFalse(repository.seLlamo("deletePet"))
        assertFalse(vm.uiState.value.deleted)
    }

    @Test
    fun `si falla el borrado se avisa y la mascota sigue ahi`() = runTest(mainDispatcher.dispatcher) {
        repository.mascotas = AppResult.Success(listOf(mascotaFalsa(id = "p-1")))
        repository.borrado = AppResult.Failure(AppError.Network)
        val vm = editando("p-1")
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertFalse("No se puede decir que se borro si fallo", vm.uiState.value.deleted)
        assertNotNull(vm.uiState.value.message)
    }
}
