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
import mx.petmate.app.R
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.Consultation
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.repository.VeterinaryRepository
import mx.petmate.app.domain.usecase.PetDates
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validación y errores de la consulta veterinaria (spec 014).
 *
 * Lo que fija esta prueba: **ningún mensaje es texto incrustado**. Todos son
 * `UiText.Resource`, así que salen de `strings.xml` y el ViewModel se puede
 * probar en la JVM sin `Context`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VeterinaryActionViewModelTest {

    private val repository: VeterinaryRepository = mockk(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: VeterinaryActionViewModel

    private val today = PetDates.today()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // Por omision el repositorio responde bien. Sin esto, un `mockk` relajado
        // devuelve un AppResult que no es Success ni Failure, el `when` del
        // ViewModel truena dentro de viewModelScope y la excepcion se filtra a
        // la prueba siguiente.
        coEvery { repository.createConsultation(any()) } returns AppResult.Success(consultation())
        coEvery { repository.createVaccineByBusiness(any(), any(), any(), any(), any()) } returns
            AppResult.Success(vaccine())
        coEvery { repository.createDewormingByBusiness(any(), any(), any(), any(), any()) } returns
            AppResult.Success(deworming())
        viewModel = VeterinaryActionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun consultation() = Consultation(
        id = "c-1",
        petId = "p-1",
        consultedOn = today,
        origin = RecordOrigin(byBusiness = true, businessName = "Veterinaria Centro"),
        reason = "Tos",
        diagnosis = "Traqueobronquitis",
        weightKg = 12.5,
        medications = emptyList(),
        createdAt = null,
    )

    private fun vaccine() = mx.petmate.app.domain.model.Vaccine(
        id = "v-1",
        name = "Rabia",
        appliedOn = today,
        batch = null,
        appliedBy = "Veterinaria Centro",
        nextDoseOn = null,
        origin = RecordOrigin(byBusiness = true, businessName = "Veterinaria Centro"),
    )

    private fun deworming() = mx.petmate.app.domain.model.Deworming(
        id = "d-1",
        product = "Pipeta",
        appliedOn = today,
        type = DewormingType.External,
        appliedBy = "Veterinaria Centro",
        nextOn = null,
        origin = RecordOrigin(byBusiness = true, businessName = "Veterinaria Centro"),
    )

    private fun fieldRes(key: String): Int? =
        (viewModel.state.value.fieldErrors[key] as? UiText.Resource)?.id

    private fun generalRes(): Int? = (viewModel.state.value.error as? UiText.Resource)?.id

    /** Llena una consulta válida salvo lo que se cambie. */
    private fun submitConsultation(
        reason: String = "Tos nocturna",
        diagnosis: String = "Traqueobronquitis",
        weight: String = "12.5",
        medications: List<MedicationDraft> = emptyList(),
        onSuccess: () -> Unit = {},
    ) = viewModel.createConsultation("p-1", reason, diagnosis, weight, medications, onSuccess)

    // ---- Ningún mensaje es texto incrustado ----

    @Test
    fun `todo error de campo es un recurso, no texto`() {
        submitConsultation(reason = "", diagnosis = "")

        val errors = viewModel.state.value.fieldErrors
        assertTrue("debe haber errores", errors.isNotEmpty())
        errors.forEach { (key, value) ->
            assertTrue("$key es texto incrustado: $value", value is UiText.Resource)
        }
    }

    @Test
    fun `el aviso general es un recurso, no texto`() {
        submitConsultation(reason = "")

        assertTrue(viewModel.state.value.error is UiText.Resource)
        assertEquals(R.string.veterinary_error_check_fields, generalRes())
    }

    @Test
    fun `todo error del servidor es un recurso, no texto`() = runTest {
        val errores = listOf(
            AppError.Server(403) to R.string.veterinary_error_grant_expired,
            AppError.Server(400) to R.string.veterinary_error_bad_request,
            AppError.Server(404) to R.string.veterinary_error_pet_not_found,
            AppError.Server(409) to R.string.veterinary_error_conflict,
            AppError.Server(500) to R.string.veterinary_error_server,
            AppError.Network to R.string.veterinary_error_network,
            AppError.Timeout to R.string.veterinary_error_timeout,
            AppError.Unauthorized to R.string.veterinary_error_session,
            AppError.Unknown() to R.string.veterinary_error_unknown,
        )

        errores.forEach { (error, expected) ->
            coEvery { repository.createConsultation(any()) } returns AppResult.Failure(error)
            submitConsultation()

            assertEquals("$error", expected, generalRes())
            assertTrue("$error debe ser recurso", viewModel.state.value.error is UiText.Resource)
        }
    }

    @Test
    fun `el error del servidor lleva el codigo como argumento`() = runTest {
        coEvery { repository.createConsultation(any()) } returns AppResult.Failure(AppError.Server(503))

        submitConsultation()

        val error = viewModel.state.value.error as UiText.Resource
        assertEquals(R.string.veterinary_error_server, error.id)
        assertEquals(listOf(503), error.args)
    }

    @Test
    fun `el error de validacion lleva el campo como argumento`() = runTest {
        coEvery { repository.createConsultation(any()) } returns
            AppResult.Failure(AppError.Validation("diagnosis", mx.petmate.app.core.result.ValidationReason.REQUIRED))

        submitConsultation()

        val error = viewModel.state.value.error as UiText.Resource
        assertEquals(R.string.veterinary_error_field, error.id)
        assertEquals(listOf("diagnosis"), error.args)
    }

    // ---- Reglas de la consulta ----

    @Test
    fun `el motivo y el diagnostico son obligatorios`() {
        submitConsultation(reason = "   ", diagnosis = "  ")

        assertEquals(R.string.veterinary_error_reason_required, fieldRes("reason"))
        assertEquals(R.string.veterinary_error_diagnosis_required, fieldRes("diagnosis"))
    }

    @Test
    fun `el motivo y el diagnostico tienen tope de largo`() {
        val long = "a".repeat(501)
        submitConsultation(reason = long, diagnosis = long)

        assertEquals(R.string.veterinary_error_too_long, fieldRes("reason"))
        assertEquals(R.string.veterinary_error_too_long, fieldRes("diagnosis"))
        val error = viewModel.state.value.fieldErrors.getValue("reason") as UiText.Resource
        assertEquals("el tope viaja como argumento", listOf(500), error.args)
    }

    @Test
    fun `el peso fuera de rango se rechaza y dentro de rango pasa`() {
        submitConsultation(weight = "0.05")
        assertEquals(R.string.veterinary_error_weight_range, fieldRes("weightKg"))

        submitConsultation(weight = "201")
        assertEquals(R.string.veterinary_error_weight_range, fieldRes("weightKg"))

        submitConsultation(weight = "12.5")
        assertNull(fieldRes("weightKg"))
    }

    @Test
    fun `el peso vacio se acepta porque es opcional`() {
        submitConsultation(weight = "")

        assertNull(fieldRes("weightKg"))
    }

    @Test
    fun `un peso con coma se lee igual que con punto`() {
        submitConsultation(weight = "12,5")

        assertNull(fieldRes("weightKg"))
    }

    @Test
    fun `mas de diez medicamentos se rechaza`() {
        val med = MedicationDraft("Amoxicilina", "250 mg", "cada 12 h", "7 dias")
        submitConsultation(medications = List(11) { med })

        assertEquals(R.string.veterinary_error_medications_max, fieldRes("medications"))
    }

    @Test
    fun `cada campo del medicamento se valida por separado`() {
        submitConsultation(medications = listOf(MedicationDraft("", "", "", "")))

        assertEquals(R.string.veterinary_error_med_name, fieldRes("med_0_name"))
        assertEquals(R.string.veterinary_error_med_dose, fieldRes("med_0_dose"))
        assertEquals(R.string.veterinary_error_med_frequency, fieldRes("med_0_frequency"))
        assertEquals(R.string.veterinary_error_med_duration, fieldRes("med_0_duration"))
    }

    @Test
    fun `una consulta valida llega al repositorio y guarda su id`() = runTest {
        coEvery { repository.createConsultation(any()) } returns AppResult.Success(consultation())
        var done = false

        submitConsultation(onSuccess = { done = true })

        assertTrue(done)
        assertEquals("c-1", viewModel.state.value.lastConsultationId)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `con errores no se llama al repositorio`() = runTest {
        submitConsultation(reason = "")

        io.mockk.coVerify(exactly = 0) { repository.createConsultation(any()) }
    }

    // ---- Vacuna ----

    @Test
    fun `la vacuna pide nombre y fecha`() {
        viewModel.createVaccine("p-1", "  ", "", "", null) {}

        assertEquals(R.string.veterinary_error_vaccine_name, fieldRes("name"))
        assertEquals(R.string.veterinary_error_applied_required, fieldRes("appliedOn"))
    }

    @Test
    fun `la fecha de aplicacion no puede ser futura`() {
        viewModel.createVaccine("p-1", "Rabia", "2099-01-01", "", null) {}

        assertEquals(R.string.veterinary_error_applied_future, fieldRes("appliedOn"))
    }

    @Test
    fun `la proxima dosis debe ser posterior a la aplicacion`() {
        viewModel.createVaccine("p-1", "Rabia", today, "", today) {}

        assertEquals(R.string.veterinary_error_next_dose_after, fieldRes("nextDoseOn"))
    }

    // ---- Desparasitacion ----

    @Test
    fun `la desparasitacion pide producto y fecha`() {
        viewModel.createDeworming("p-1", " ", "", DewormingType.Internal, null) {}

        assertEquals(R.string.veterinary_error_product_required, fieldRes("product"))
        assertEquals(R.string.veterinary_error_applied_required, fieldRes("appliedOn"))
    }

    @Test
    fun `la proxima desparasitacion debe ser futura`() {
        viewModel.createDeworming("p-1", "Pipeta", today, DewormingType.External, today) {}

        assertEquals(R.string.veterinary_error_next_deworming_future, fieldRes("nextOn"))
    }

    // ---- Lo que pasa cuando si se guarda ----

    @Test
    fun `una vacuna valida llega al repositorio sin lote vacio y avisa a la pantalla`() = runTest {
        // El lote es opcional: mandar "" en lugar de null guardaria un lote en blanco en el carnet.
        var aviso = 0

        viewModel.createVaccine("p-1", "  Rabia  ", today, "   ", null) { aviso++ }

        coVerify { repository.createVaccineByBusiness("p-1", "Rabia", today, null, null) }
        assertEquals(1, aviso)
        assertFalse(viewModel.state.value.isSubmitting)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `una vacuna que falla no avisa que se guardo`() = runTest {
        coEvery { repository.createVaccineByBusiness(any(), any(), any(), any(), any()) } returns
            AppResult.Failure(AppError.Server(403))
        var aviso = false

        viewModel.createVaccine("p-1", "Rabia", today, "", null) { aviso = true }

        assertFalse("Con el permiso vencido no hay nada guardado que anunciar", aviso)
        assertEquals(R.string.veterinary_error_grant_expired, generalRes())
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `una desparasitacion valida llega al repositorio y avisa a la pantalla`() = runTest {
        var aviso = 0

        viewModel.createDeworming("p-1", "  Pipeta  ", today, DewormingType.External, "  ") { aviso++ }

        coVerify { repository.createDewormingByBusiness("p-1", "Pipeta", today, DewormingType.External, null) }
        assertEquals(1, aviso)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `una desparasitacion que falla no avisa que se guardo`() = runTest {
        coEvery { repository.createDewormingByBusiness(any(), any(), any(), any(), any()) } returns
            AppResult.Failure(AppError.Network)
        var aviso = false

        viewModel.createDeworming("p-1", "Pipeta", today, DewormingType.Internal, null) { aviso = true }

        assertFalse(aviso)
        assertEquals(R.string.veterinary_error_network, generalRes())
    }

    // ---- Limpieza ----

    @Test
    fun `clearError deja el estado sin mensajes`() {
        submitConsultation(reason = "")
        assertTrue(viewModel.state.value.fieldErrors.isNotEmpty())

        viewModel.clearError()

        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
    }
}
