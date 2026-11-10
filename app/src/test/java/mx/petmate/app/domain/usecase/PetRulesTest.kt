package mx.petmate.app.domain.usecase

import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.model.VaccineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas del contrato del backend (pm-040, pm-097 a pm-102) con un dia fijo (spec 007, D-01). */
class PetRulesTest {

    private val today = "2026-11-10"
    private fun pet(name: String = "Firulais", species: PetSpecies? = PetSpecies.Dog, breed: String = "", birth: String? = null) =
        PetDraft(name, species, breed, birth, PetSex.Unknown)

    @Test
    fun `una mascota con nombre y especie es valida`() {
        assertTrue(PetRules.validatePet(pet(), today).isEmpty())
    }

    @Test
    fun `nombre y especie son obligatorios`() {
        val issues = PetRules.validatePet(pet(name = "  ", species = null), today)
        assertEquals(
            setOf(PetRules.Field.Name to PetRules.Problem.Required, PetRules.Field.Species to PetRules.Problem.Required),
            issues.map { it.field to it.problem }.toSet(),
        )
    }

    @Test
    fun `nombre de mas de 50 y raza de mas de 60 caracteres no pasan`() {
        val issues = PetRules.validatePet(pet(name = "a".repeat(51), breed = "b".repeat(61)), today)
        assertEquals(listOf(PetRules.Problem.TooLong, PetRules.Problem.TooLong), issues.map { it.problem })
    }

    @Test
    fun `la fecha de nacimiento no puede ser futura ni de hace mas de 50 anos`() {
        assertEquals(PetRules.Problem.Future, PetRules.validatePet(pet(birth = "2026-11-11"), today).single().problem)
        assertEquals(PetRules.Problem.TooOld, PetRules.validatePet(pet(birth = "1976-11-09"), today).single().problem)
        assertTrue(PetRules.validatePet(pet(birth = "2026-11-10"), today).isEmpty())
    }

    @Test
    fun `la proxima dosis debe ser posterior a la aplicacion`() {
        val same = VaccineDraft("Rabia", "2026-11-01", "", "", nextDoseOn = "2026-11-01")
        assertEquals(PetRules.Problem.NotAfterApplied, PetRules.validateVaccine(same, today).single().problem)
        val future = VaccineDraft("Rabia", "2026-11-12", "", "", null)
        assertEquals(PetRules.Problem.Future, PetRules.validateVaccine(future, today).single().problem)
    }

    @Test
    fun `la proxima desparasitacion debe ser futura`() {
        val draft = DewormingDraft("Drontal", "2026-11-01", DewormingType.Internal, "", nextOn = today)
        assertEquals(PetRules.Problem.NotFuture, PetRules.validateDeworming(draft, today).single().problem)
    }

    @Test
    fun `el peso va de mas de 0 a 200 kg con un decimal`() {
        assertTrue(PetRules.validateWeight(0.0).isNotEmpty())
        assertTrue(PetRules.validateWeight(200.1).isNotEmpty())
        assertTrue(PetRules.validateWeight(12.4).isEmpty())
        assertEquals(12.4, PetRules.roundKilograms(12.36), 0.0)
    }

    @Test
    fun `la alergia necesita nombre`() {
        assertTrue(PetRules.validateAllergy(AllergyDraft(" ", AllergyKind.Allergy, "", true)).isNotEmpty())
    }

    @Test
    fun `edad y dias se calculan sin java time`() {
        assertEquals(6 to 6, PetDates.age("2020-05-01", today))
        assertEquals(0 to 11, PetDates.age("2025-11-11", today))
        assertNull(PetDates.age("2027-01-01", today))
        assertEquals(6, PetDates.daysBetween(today, "2026-11-16"))
        assertEquals(-1, PetDates.daysBetween(today, "2026-11-09"))
        assertEquals("1976-11-10", PetDates.minusYears(today, 50))
    }
}
