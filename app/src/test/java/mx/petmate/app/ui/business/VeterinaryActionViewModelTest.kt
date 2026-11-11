package mx.petmate.app.ui.business

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Consultation
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.repository.VeterinaryRepository
import mx.petmate.app.domain.usecase.PetDates
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Validaciones de los formularios que llena la veterinaria sobre una mascota ajena (pm-131, pm-135, pm-136). */
@OptIn(ExperimentalCoroutinesApi::class)
class VeterinaryActionViewModelTest {

    private val repository: VeterinaryRepository = mockk(relaxed = true)
    private lateinit var viewModel: VeterinaryActionViewModel
    private val dispatcher = UnconfinedTestDispatcher()

    private val consultation = Consultation(
        id = "c-1",
        petId = "p-1",
        consultedOn = "2026-11-11",
        origin = RecordOrigin(byBusiness = true, businessName = "Vet Tula"),
        reason = "Vomito",
        diagnosis = "Gastritis",
        weightKg = null,
        medications = emptyList(),
        createdAt = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = VeterinaryActionViewModel(repository)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun yesterday() = PetDates.today().let { today ->
        // Resta un dia en texto yyyy-MM-dd sin java.time (minSdk 24 sin desugaring).
        val day = today.substring(8).toInt()
        if (day > 1) today.substring(0, 8) + (day - 1).toString().padStart(2, '0') else today
    }

    private fun tomorrow() = PetDates.today().let { today ->
        val day = today.substring(8).toInt()
        if (day < 28) today.substring(0, 8) + (day + 1).toString().padStart(2, '0') else today.substring(0, 8) + "28"
    }

    @Test
    fun `motivo y diagnostico son obligatorios`() = runTest(dispatcher) {
        var saved = false

        viewModel.createConsultation("p-1", "   ", "", "", emptyList()) { saved = true }

        val errors = viewModel.state.value.fieldErrors
        assertTrue("reason" in errors)
        assertTrue("diagnosis" in errors)
        assertFalse(saved)
        coVerify(exactly = 0) { repository.createConsultation(any()) }
    }

    @Test
    fun `cada medicamento necesita nombre, dosis, frecuencia y duracion`() = runTest(dispatcher) {
        viewModel.createConsultation("p-1", "Vomito", "Gastritis", "", listOf(MedicationDraft(name = "Amoxicilina"))) {}

        val errors = viewModel.state.value.fieldErrors
        assertFalse("med_0_name" in errors)
        assertTrue("med_0_dose" in errors)
        assertTrue("med_0_frequency" in errors)
        assertTrue("med_0_duration" in errors)
    }

    @Test
    fun `una consulta valida se envia al backend`() = runTest(dispatcher) {
        coEvery { repository.createConsultation(any()) } returns AppResult.Success(consultation)
        var saved = false

        viewModel.createConsultation("p-1", "Vomito", "Gastritis", "12.4", emptyList()) { saved = true }

        assertTrue(saved)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSubmitting)
        coVerify { repository.createConsultation(ConsultationDraft("p-1", "Vomito", "Gastritis", 12.4, emptyList())) }
    }

    @Test
    fun `un peso fuera de rango no pasa`() = runTest(dispatcher) {
        viewModel.createConsultation("p-1", "Vomito", "Gastritis", "500", emptyList()) {}

        assertTrue("weightKg" in viewModel.state.value.fieldErrors)
    }

    @Test
    fun `la vacuna no puede aplicarse en el futuro`() = runTest(dispatcher) {
        viewModel.createVaccine("p-1", "Rabia", tomorrow(), "", null) {}

        assertTrue("appliedOn" in viewModel.state.value.fieldErrors)
    }

    @Test
    fun `la proxima dosis debe ser posterior a la aplicacion`() = runTest(dispatcher) {
        val applied = yesterday()

        viewModel.createVaccine("p-1", "Rabia", applied, "", applied) {}

        assertTrue("nextDoseOn" in viewModel.state.value.fieldErrors)
    }

    @Test
    fun `la proxima desparasitacion debe ser futura`() = runTest(dispatcher) {
        viewModel.createDeworming("p-1", "Drontal", yesterday(), DewormingType.Internal, yesterday()) {}

        assertTrue("nextOn" in viewModel.state.value.fieldErrors)
    }

    @Test
    fun `un 403 avisa que la autorizacion vencio`() = runTest(dispatcher) {
        coEvery { repository.createConsultation(any()) } returns AppResult.Failure(AppError.Server(403))
        var saved = false

        viewModel.createConsultation("p-1", "Vomito", "Gastritis", "", emptyList()) { saved = true }

        assertFalse(saved)
        assertNotNull(viewModel.state.value.error)
        assertEquals("La autorización venció o no incluye a tu negocio.", viewModel.state.value.error)
    }
}
