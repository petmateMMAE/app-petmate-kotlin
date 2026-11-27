package mx.petmate.app.ui.pets

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.usecase.PetRules
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.PetRepositoryFalso
import mx.petmate.app.util.alergiaFalsa
import mx.petmate.app.util.carnetFalso
import mx.petmate.app.util.desparasitacionFalsa
import mx.petmate.app.util.mascotaFalsa
import mx.petmate.app.util.pesoFalso
import mx.petmate.app.util.qrFalso
import mx.petmate.app.util.vacunaFalsa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Carnet de salud de una mascota (spec 008).
 *
 * Es la pantalla con mas escrituras de la app, y la regla que las gobierna a todas es la misma: lo
 * que el dominio rechaza **no sale del telefono**, y lo que se guarda bien recarga el carnet, para
 * que el dueno vea el dato nuevo sin volver a entrar.
 *
 * Lo otro que se cuida aqui es el offline-first (C-01): con el carnet ya guardado y sin red, la
 * pantalla se queda con lo guardado y **no** pinta error. Un error ahi haria que el dueno creyera
 * que perdio el carnet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetCarnetViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = PetRepositoryFalso()

    private fun viewModel() = PetCarnetViewModel(
        repository,
        SavedStateHandle(mapOf(PetFormViewModel.ARG_PET_ID to PET_ID)),
    )

    /** Deja el repositorio con la mascota en el telefono, que es el caso normal. */
    private fun conMascota() {
        repository.mascotas = AppResult.Success(listOf(mascotaFalsa(id = PET_ID)))
    }

    private val vacunaBuena = VaccineDraft(
        name = "Rabia",
        appliedOn = "2026-01-10",
        batch = "L-1",
        appliedBy = "MVZ Ruiz",
        nextDoseOn = "2027-01-10",
    )

    private val desparasitacionBuena = DewormingDraft(
        product = "Drontal",
        appliedOn = "2026-01-10",
        type = DewormingType.Internal,
        appliedBy = "MVZ Ruiz",
        nextOn = null,
    )

    @Test
    fun `el id de la mascota sale de la navegacion`() {
        conMascota()

        assertEquals(PET_ID, viewModel().petId)
    }

    @Test
    fun `al refrescar trae la mascota y su carnet`() = runTest(mainDispatcher.dispatcher) {
        conMascota()
        repository.carnetPorMascota[PET_ID] = AppResult.Success(
            carnetFalso(
                vaccines = listOf(vacunaFalsa(name = "Rabia")),
                dewormings = listOf(desparasitacionFalsa()),
                weights = listOf(pesoFalso()),
                allergies = listOf(alergiaFalsa(name = "Pollo")),
            ),
        )
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertEquals("Firulais", vm.uiState.value.pet?.name)
        assertEquals(listOf("Rabia"), vm.uiState.value.carnet.vaccines.map { it.name })
        assertEquals(listOf("Pollo"), vm.uiState.value.carnet.allergies.map { it.name })
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `primero muestra lo guardado y luego lo del servidor`() = runTest(mainDispatcher.dispatcher) {
        // Offline-first (P4): el carnet guardado se pinta sin esperar la red.
        conMascota()
        repository.carnetPorMascota[PET_ID] = AppResult.Success(carnetFalso(vaccines = listOf(vacunaFalsa())))
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertTrue("Lee local, refresca, y vuelve a leer local", repository.vecesQueSeLlamo("getCarnet") >= 2)
        assertTrue(repository.seLlamo("refreshCarnet"))
    }

    @Test
    fun `sin red pero con carnet guardado no muestra error`() = runTest(mainDispatcher.dispatcher) {
        // C-01: el dueno no puede pensar que perdio el carnet por quedarse sin señal.
        conMascota()
        repository.carnetPorMascota[PET_ID] = AppResult.Success(carnetFalso(vaccines = listOf(vacunaFalsa())))
        repository.recarga = AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.carnet.vaccines.size)
        assertNotNull(vm.uiState.value.pet)
    }

    @Test
    fun `sin mascota guardada si muestra error`() = runTest(mainDispatcher.dispatcher) {
        // Aqui no hay nada que mostrar, asi que callar el error dejaria la pantalla vacia sin
        // explicacion.
        repository.recarga = AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `si falla el carnet pero no la mascota, se ve la mascota y el error`() =
        runTest(mainDispatcher.dispatcher) {
            conMascota()
            repository.carnetPorMascota[PET_ID] = AppResult.Failure(AppError.Server(500))
            val vm = viewModel()

            vm.refresh()
            advanceUntilIdle()

            assertEquals("Firulais", vm.uiState.value.pet?.name)
            assertNotNull(vm.uiState.value.error)
        }

    @Test
    fun `el QR se pide aparte y llega al estado`() = runTest(mainDispatcher.dispatcher) {
        repository.qr = AppResult.Success(qrFalso(shortCode = "ABCD1234"))
        val vm = viewModel()

        vm.loadQr()
        advanceUntilIdle()

        assertEquals("ABCD1234", vm.uiState.value.qr?.shortCode)
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `si el QR falla avisa con un mensaje y no con la pantalla de error`() =
        runTest(mainDispatcher.dispatcher) {
            // El QR es un extra de la pantalla: perderlo no vale tapar el carnet con un error.
            repository.qr = AppResult.Failure(AppError.Network)
            val vm = viewModel()

            vm.loadQr()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.message)
            assertNull(vm.uiState.value.error)
            assertNull(vm.uiState.value.qr)
        }

    @Test
    fun `regenerar el QR deja el nuevo y avisa`() = runTest(mainDispatcher.dispatcher) {
        repository.qr = AppResult.Success(qrFalso(shortCode = "VIEJO123"))
        repository.qrRegenerado = AppResult.Success(qrFalso(shortCode = "NUEVO123"))
        val vm = viewModel()
        vm.loadQr()
        advanceUntilIdle()

        vm.regenerateQr()
        advanceUntilIdle()

        assertEquals("NUEVO123", vm.uiState.value.qr?.shortCode)
        assertNotNull(vm.uiState.value.message)
        assertFalse(vm.uiState.value.isBusy)
    }

    @Test
    fun `si regenerar el QR falla se queda el de antes`() = runTest(mainDispatcher.dispatcher) {
        repository.qr = AppResult.Success(qrFalso(shortCode = "VIEJO123"))
        repository.qrRegenerado = AppResult.Failure(AppError.Server(429))
        val vm = viewModel()
        vm.loadQr()
        advanceUntilIdle()

        vm.regenerateQr()
        advanceUntilIdle()

        assertEquals("Un QR que no se cambio sigue sirviendo", "VIEJO123", vm.uiState.value.qr?.shortCode)
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `una vacuna sin nombre no sale del telefono`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.saveVaccine(vacunaBuena.copy(name = "  "))
        advanceUntilIdle()

        assertFalse(repository.seLlamo("addVaccine"))
        assertTrue(PetRules.Field.Name in vm.uiState.value.formErrors)
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun `una vacuna aplicada en el futuro no sale del telefono`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.saveVaccine(vacunaBuena.copy(appliedOn = "2099-01-01"))
        advanceUntilIdle()

        assertFalse(repository.seLlamo("addVaccine"))
        assertTrue(PetRules.Field.AppliedOn in vm.uiState.value.formErrors)
    }

    @Test
    fun `una vacuna buena se manda completa y la pantalla regresa`() = runTest(mainDispatcher.dispatcher) {
        repository.altaDeVacuna = AppResult.Success(vacunaFalsa())
        val vm = viewModel()

        vm.saveVaccine(vacunaBuena)
        advanceUntilIdle()

        assertEquals(vacunaBuena, repository.ultimaVacuna)
        assertTrue(vm.uiState.value.saved)
        assertTrue(vm.uiState.value.formErrors.isEmpty())
    }

    @Test
    fun `si guardar la vacuna falla no dice que se guardo`() = runTest(mainDispatcher.dispatcher) {
        repository.altaDeVacuna = AppResult.Failure(AppError.Timeout)
        val vm = viewModel()

        vm.saveVaccine(vacunaBuena)
        advanceUntilIdle()

        assertFalse("Con saved en cierto la pantalla se cierra y el dueno cree que quedo", vm.uiState.value.saved)
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `una desparasitacion con la proxima fecha ya pasada no sale`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.saveDeworming(desparasitacionBuena.copy(nextOn = "2020-01-01"))
        advanceUntilIdle()

        assertFalse(repository.seLlamo("addDeworming"))
        assertTrue(PetRules.Field.NextDate in vm.uiState.value.formErrors)
    }

    @Test
    fun `una desparasitacion buena se manda completa`() = runTest(mainDispatcher.dispatcher) {
        repository.altaDeDesparasitacion = AppResult.Success(desparasitacionFalsa())
        val vm = viewModel()

        vm.saveDeworming(desparasitacionBuena)
        advanceUntilIdle()

        assertEquals(desparasitacionBuena, repository.ultimaDesparasitacion)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun `el peso se redondea a un decimal antes de mandarlo`() = runTest(mainDispatcher.dispatcher) {
        // El backend acepta un decimal; mandar 12.449 provocaria un 400 por el formato.
        conMascota()
        repository.altaDePeso = AppResult.Success(pesoFalso())
        val vm = viewModel()

        vm.saveWeight(12.449)
        advanceUntilIdle()

        assertEquals(12.4, repository.ultimoPeso?.second!!, 0.0)
    }

    @Test
    fun `un peso fuera de rango no sale del telefono`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.saveWeight(0.0)
        advanceUntilIdle()
        assertFalse(repository.seLlamo("addWeight"))
        assertNotNull(vm.uiState.value.message)

        vm.messageShown()
        vm.saveWeight(PetRules.MAX_KILOGRAMS + 1)
        advanceUntilIdle()
        assertFalse(repository.seLlamo("addWeight"))
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `al guardar el peso recarga el carnet`() = runTest(mainDispatcher.dispatcher) {
        // El peso nuevo tiene que aparecer en la lista sin que el dueno vuelva a entrar.
        conMascota()
        repository.altaDePeso = AppResult.Success(pesoFalso())
        val vm = viewModel()
        val antes = repository.vecesQueSeLlamo("getCarnet")

        vm.saveWeight(12.4)
        advanceUntilIdle()

        assertTrue(repository.vecesQueSeLlamo("getCarnet") > antes)
        assertNotNull(vm.uiState.value.message)
    }

    @Test
    fun `una alergia sin nombre no sale del telefono`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.saveAllergy(null, AllergyDraft("  ", AllergyKind.Allergy, "", active = true))
        advanceUntilIdle()

        assertFalse(repository.seLlamo("addAllergy"))
        assertTrue(PetRules.Field.Name in vm.uiState.value.formErrors)
    }

    @Test
    fun `sin id la alergia se da de alta y con id se edita`() = runTest(mainDispatcher.dispatcher) {
        repository.altaDeAlergia = AppResult.Success(alergiaFalsa())
        repository.edicionDeAlergia = AppResult.Success(alergiaFalsa())
        val draft = AllergyDraft("Pollo", AllergyKind.Allergy, "ronchas", active = true)

        val alta = viewModel()
        alta.saveAllergy(null, draft)
        advanceUntilIdle()
        assertTrue(repository.seLlamo("addAllergy"))
        assertNull(repository.ultimoIdDeAlergia)

        val edicion = viewModel()
        edicion.saveAllergy("a-1", draft)
        advanceUntilIdle()
        assertEquals("a-1", repository.ultimoIdDeAlergia)
    }

    @Test
    fun `marcar una alergia como superada conserva el resto del dato`() = runTest(mainDispatcher.dispatcher) {
        // Solo cambia `active`: si se perdieran las notas, el veterinario perderia contexto.
        conMascota()
        repository.edicionDeAlergia = AppResult.Success(alergiaFalsa(active = false))
        val vm = viewModel()
        val alergia = alergiaFalsa(name = "Pollo", active = true).copy(notes = "ronchas en el lomo")

        vm.toggleAllergy(alergia)
        advanceUntilIdle()

        assertEquals("a-1", repository.ultimoIdDeAlergia)
        assertEquals("Pollo", repository.ultimaAlergia?.name)
        assertEquals("ronchas en el lomo", repository.ultimaAlergia?.notes)
        assertEquals(false, repository.ultimaAlergia?.active)
    }

    @Test
    fun `borrar una vacuna recarga el carnet`() = runTest(mainDispatcher.dispatcher) {
        conMascota()
        repository.bajaDeVacuna = AppResult.Success(Unit)
        val vm = viewModel()
        val antes = repository.vecesQueSeLlamo("getCarnet")

        vm.deleteVaccine("v-1")
        advanceUntilIdle()

        assertEquals(listOf("v-1"), repository.vacunasBorradas)
        assertTrue(repository.vecesQueSeLlamo("getCarnet") > antes)
    }

    @Test
    fun `si borrar falla avisa y no recarga`() = runTest(mainDispatcher.dispatcher) {
        conMascota()
        repository.bajaDeDesparasitacion = AppResult.Failure(AppError.Server(403))
        val vm = viewModel()
        val antes = repository.vecesQueSeLlamo("getCarnet")

        vm.deleteDeworming("d-1")
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.message)
        assertEquals(antes, repository.vecesQueSeLlamo("getCarnet"))
    }

    @Test
    fun `dos toques seguidos guardan una sola vez`() = runTest(mainDispatcher.dispatcher) {
        // El candado es `isBusy`: sin el, un doble toque crea la vacuna dos veces.
        repository.altaDeVacuna = AppResult.Success(vacunaFalsa())
        val vm = viewModel()

        vm.saveVaccine(vacunaBuena)
        vm.saveVaccine(vacunaBuena)
        advanceUntilIdle()

        assertEquals(1, repository.vecesQueSeLlamo("addVaccine"))
    }

    @Test
    fun `corregir un campo quita solo su error`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.saveVaccine(vacunaBuena.copy(name = "", appliedOn = "2099-01-01", nextDoseOn = null))
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.formErrors.size)

        vm.clearFieldError(PetRules.Field.Name)

        assertFalse(PetRules.Field.Name in vm.uiState.value.formErrors)
        assertTrue("El otro error sigue siendo cierto", PetRules.Field.AppliedOn in vm.uiState.value.formErrors)
    }

    @Test
    fun `un aviso se muestra una sola vez`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.saveWeight(0.0)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.message)

        vm.messageShown()

        assertNull(vm.uiState.value.message)
    }

    private companion object {
        const val PET_ID = "p-1"
    }
}
