package mx.petmate.app.domain.health

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.health.usecase.EditHealthRecordUseCase
import mx.petmate.app.domain.health.usecase.GetCriticalAllergiesUseCase
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Alergias criticas y regla de origen (spec 008, T-003, C-06 y C-07). */
class HealthRulesTest {

    private val vet = RecordOrigin(byBusiness = true, businessName = "Veterinaria Centro")
    private val owner = RecordOrigin.Owner

    private val critical = GetCriticalAllergiesUseCase()
    private val repository: mx.petmate.app.domain.repository.PetRepository = mockk(relaxed = true)
    private val edit = EditHealthRecordUseCase(repository)

    private fun allergy(
        id: String,
        kind: AllergyKind = AllergyKind.Allergy,
        active: Boolean = true,
        origin: RecordOrigin = owner,
    ) = Allergy(id = id, name = "Polen $id", kind = kind, notes = null, active = active, origin = origin)

    // ---- C-06: alergias criticas ----

    @Test
    fun `devuelve todas las alergias activas sin importar el origen`() {
        val carnet = Carnet(
            allergies = listOf(
                allergy("1"),
                allergy("2", origin = vet),
                allergy("3", active = false),
            ),
        )

        assertEquals(listOf("1", "2"), critical(carnet).map(Allergy::id))
    }

    @Test
    fun `las condiciones no son alergias criticas`() {
        val carnet = Carnet(allergies = listOf(allergy("1", kind = AllergyKind.Condition), allergy("2")))

        assertEquals(listOf("2"), critical(carnet).map(Allergy::id))
    }

    @Test
    fun `sin alergias activas devuelve lista vacia y no nulo`() {
        assertTrue(critical(Carnet()).isEmpty())
        assertTrue(critical(listOf(allergy("1", active = false))).isEmpty())
    }

    // ---- C-07: origen veterinario no se edita ni se borra ----

    @Test
    fun `canModify rechaza el origen veterinario y acepta el del dueno`() {
        assertFalse(edit.canModify(vet))
        assertTrue(edit.canModify(owner))
    }

    @Test
    fun `borrar una vacuna del negocio no llama al repositorio`() = runTest {
        val vaccine = Vaccine("v1", "Rabia", "2026-01-10", null, null, null, vet)

        assertEquals(EditHealthRecordUseCase.Outcome.VeterinaryRecord, edit.deleteVaccine(vaccine))
        coVerify(exactly = 0) { repository.deleteVaccine(any()) }
    }

    @Test
    fun `borrar una desparasitacion del negocio no llama al repositorio`() = runTest {
        val deworming = Deworming("d1", "Pipeta", "2026-02-01", DewormingType.External, null, null, vet)

        assertEquals(EditHealthRecordUseCase.Outcome.VeterinaryRecord, edit.deleteDeworming(deworming))
        coVerify(exactly = 0) { repository.deleteDeworming(any()) }
    }

    @Test
    fun `editar una alergia del negocio no llama al repositorio`() = runTest {
        val draft = AllergyDraft("Polen", AllergyKind.Allergy, "", true)

        assertEquals(
            EditHealthRecordUseCase.Outcome.VeterinaryRecord,
            edit.updateAllergy(allergy("a1", origin = vet), draft),
        )
        coVerify(exactly = 0) { repository.updateAllergy(any(), any()) }
    }

    @Test
    fun `borrar una vacuna propia llega al repositorio`() = runTest {
        coEvery { repository.deleteVaccine("v2") } returns AppResult.Success(Unit)
        val vaccine = Vaccine("v2", "Rabia", "2026-01-10", null, null, null, owner)

        assertEquals(EditHealthRecordUseCase.Outcome.Done, edit.deleteVaccine(vaccine))
        coVerify(exactly = 1) { repository.deleteVaccine("v2") }
    }

    @Test
    fun `un error del repositorio se devuelve tal cual`() = runTest {
        coEvery { repository.deleteVaccine("v3") } returns AppResult.Failure(AppError.Network)
        val vaccine = Vaccine("v3", "Rabia", "2026-01-10", null, null, null, owner)

        assertEquals(
            EditHealthRecordUseCase.Outcome.Failed(AppError.Network),
            edit.deleteVaccine(vaccine),
        )
    }
}
