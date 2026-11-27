package mx.petmate.app.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AffiliationDraft
import mx.petmate.app.domain.model.AffiliationRules
import mx.petmate.app.domain.model.AffiliationStep
import mx.petmate.app.domain.model.BusinessDraft
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.BusinessService
import mx.petmate.app.domain.model.DraftedService
import mx.petmate.app.domain.model.OwnBusiness
import mx.petmate.app.domain.model.TimeRange
import mx.petmate.app.domain.repository.BusinessRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Borrador y envio de la afiliacion (spec 011, T-004). */
class AffiliationUseCasesTest {

    private val repository: BusinessRepository = mockk(relaxed = true)
    private val save = SaveAffiliationDraftUseCase(repository)
    private val submit = SubmitAffiliationUseCase(repository)

    private val schedule = mapOf("monday" to listOf(TimeRange("09:00", "18:00")))

    private val business = BusinessDraft(
        kind = BusinessKind.Veterinary,
        name = "Veterinaria Centro",
        phone = "5512345678",
        whatsApp = "",
        address = "Calle 1, Centro",
        description = "",
        licenseNumber = "CED-100",
        schedule = schedule,
    )

    private val draft = AffiliationDraft(
        business = business,
        services = listOf(DraftedService("Consulta general", 350.0)),
        latitude = 19.43,
        longitude = -99.13,
        step = AffiliationStep.Pin,
    )

    private val created = OwnBusiness(
        id = "b1",
        kind = BusinessKind.Veterinary,
        name = "Veterinaria Centro",
        phone = "5512345678",
        whatsApp = null,
        address = "Calle 1, Centro",
        description = null,
        latitude = 19.43,
        longitude = -99.13,
        licenseNumber = "CED-100",
        schedule = schedule,
    )

    // ---- Validacion de los tres pasos ----

    @Test
    fun `un borrador vacio reporta un problema por cada paso y no envia nada`() = runTest {
        val result = submit(AffiliationDraft.Empty)

        val submission = (result as AppResult.Success).data as SubmitAffiliationUseCase.Submission.Incomplete
        val fields = submission.issues.map(AffiliationRules.Issue::field)
        assertTrue(AffiliationRules.Field.Kind in fields)
        assertTrue(AffiliationRules.Field.Name in fields)
        assertTrue(AffiliationRules.Field.Phone in fields)
        assertTrue(AffiliationRules.Field.Address in fields)
        assertTrue(AffiliationRules.Field.Services in fields)
        assertTrue(AffiliationRules.Field.Pin in fields)
        coVerify(exactly = 0) { repository.createBusiness(any(), any(), any()) }
    }

    @Test
    fun `sin pin no se envia aunque los otros dos pasos esten completos`() = runTest {
        val result = submit(draft.copy(latitude = null, longitude = null))

        val submission = (result as AppResult.Success).data as SubmitAffiliationUseCase.Submission.Incomplete
        assertEquals(
            listOf(AffiliationRules.Issue(AffiliationRules.Field.Pin, AffiliationRules.Problem.Required)),
            submission.issues,
        )
        coVerify(exactly = 0) { repository.createBusiness(any(), any(), any()) }
    }

    @Test
    fun `sin servicios no se envia`() = runTest {
        val result = submit(draft.copy(services = emptyList()))

        val submission = (result as AppResult.Success).data as SubmitAffiliationUseCase.Submission.Incomplete
        assertEquals(
            listOf(AffiliationRules.Issue(AffiliationRules.Field.Services, AffiliationRules.Problem.NoneChosen)),
            submission.issues,
        )
    }

    @Test
    fun `una veterinaria sin cedula no se envia`() = runTest {
        val result = submit(draft.copy(business = business.copy(licenseNumber = "")))

        val submission = (result as AppResult.Success).data as SubmitAffiliationUseCase.Submission.Incomplete
        assertTrue(
            AffiliationRules.Issue(AffiliationRules.Field.License, AffiliationRules.Problem.Required) in submission.issues,
        )
    }

    @Test
    fun `una tienda sin cedula si se envia`() = runTest {
        coEvery { repository.createBusiness(any(), any(), any()) } returns AppResult.Success(created)
        coEvery { repository.createService(any(), any(), any()) } returns
            AppResult.Success(BusinessService("s1", "Alimento", null, 100.0))

        val store = draft.copy(business = business.copy(kind = BusinessKind.Store, licenseNumber = ""))
        val result = submit(store)

        assertTrue((result as AppResult.Success).data is SubmitAffiliationUseCase.Submission.Affiliated)
    }

    @Test
    fun `el borrador completo se da por valido`() {
        assertTrue(draft.isComplete)
        assertTrue(AffiliationRules.allIssues(draft).isEmpty())
    }

    // ---- Envio ----

    @Test
    fun `envia el negocio y luego sus servicios`() = runTest {
        coEvery { repository.createBusiness(business, 19.43, -99.13) } returns AppResult.Success(created)
        coEvery { repository.createService(any(), any(), any()) } returns
            AppResult.Success(BusinessService("s1", "Consulta general", null, 350.0))

        val result = submit(draft)

        val affiliated = (result as AppResult.Success).data as SubmitAffiliationUseCase.Submission.Affiliated
        assertEquals(created, affiliated.business)
        assertTrue(affiliated.servicesFailed.isEmpty())
        coVerify(exactly = 1) { repository.createBusiness(business, 19.43, -99.13) }
        coVerify(exactly = 1) { repository.createService("Consulta general", null, 350.0) }
    }

    @Test
    fun `un servicio rechazado se reporta y el negocio sigue afiliado`() = runTest {
        coEvery { repository.createBusiness(any(), any(), any()) } returns AppResult.Success(created)
        coEvery { repository.createService("Consulta general", any(), any()) } returns
            AppResult.Success(BusinessService("s1", "Consulta general", null, 350.0))
        coEvery { repository.createService("Rayos X", any(), any()) } returns
            AppResult.Failure(AppError.Server(400))

        val result = submit(draft.copy(services = draft.services + DraftedService("Rayos X", 900.0)))

        val affiliated = (result as AppResult.Success).data as SubmitAffiliationUseCase.Submission.Affiliated
        assertEquals(listOf("Rayos X"), affiliated.servicesFailed)
    }

    @Test
    fun `si el negocio no se crea no se intenta ningun servicio`() = runTest {
        coEvery { repository.createBusiness(any(), any(), any()) } returns AppResult.Failure(AppError.Network)

        val result = submit(draft)

        assertEquals(AppError.Network, (result as AppResult.Failure).error)
        coVerify(exactly = 0) { repository.createService(any(), any(), any()) }
    }

    @Test
    fun `al terminar borra el borrador`() = runTest {
        coEvery { repository.createBusiness(any(), any(), any()) } returns AppResult.Success(created)
        coEvery { repository.createService(any(), any(), any()) } returns
            AppResult.Success(BusinessService("s1", "Consulta general", null, 350.0))

        submit(draft)

        coVerify(exactly = 1) { repository.clearAffiliationDraft() }
    }

    // ---- Guardado del avance ----

    @Test
    fun `guardar manda el borrador al repositorio`() = runTest {
        coEvery { repository.saveAffiliationDraft(any()) } returns AppResult.Success(Unit)

        save(draft)

        coVerify(exactly = 1) { repository.saveAffiliationDraft(draft) }
    }

    @Test
    fun `guardar con paso anota el paso en el borrador`() = runTest {
        coEvery { repository.saveAffiliationDraft(any()) } returns AppResult.Success(Unit)

        save(draft, AffiliationStep.Services)

        coVerify(exactly = 1) { repository.saveAffiliationDraft(draft.copy(step = AffiliationStep.Services)) }
    }

    @Test
    fun `leer devuelve lo que guardo el repositorio`() = runTest {
        coEvery { repository.readAffiliationDraft() } returns AppResult.Success(draft)

        assertEquals(draft, (save.read() as AppResult.Success).data)
    }
}
