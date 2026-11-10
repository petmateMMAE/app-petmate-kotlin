package mx.petmate.app.data.repository

import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.ui.pets.nextEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mascotas de ejemplo del modo demo y del backend simulado. */
class SamplePetRepositoryTest {

    private val repo = SamplePetRepository()

    @Test
    fun `el modo demo trae mascotas de ejemplo con carnet`() = runTest {
        val pets = (repo.getPets() as AppResult.Success).data
        assertEquals(listOf("Firulais", "Michi"), pets.map { it.name })
        val carnet = (repo.getCarnet(pets.first().id) as AppResult.Success).data
        assertTrue(carnet.vaccines.isNotEmpty())
    }

    @Test
    fun `lo que registro un negocio no se puede borrar`() = runTest {
        val carnet = (repo.getCarnet("ejemplo-firulais") as AppResult.Success).data
        val fromVet = carnet.vaccines.first { it.origin.byBusiness }
        assertEquals(AppError.Server(403), (repo.deleteVaccine(fromVet.id) as AppResult.Failure).error)
    }

    @Test
    fun `crear, regenerar el QR y borrar una mascota en cascada`() = runTest {
        val pet = (repo.createPet(PetDraft("Rocky", PetSpecies.Dog, "", null, PetSex.Unknown)) as AppResult.Success).data
        repo.addVaccine(pet.id, VaccineDraft("Rabia", "2026-11-01", "", "", null))
        val qr = (repo.getQr(pet.id) as AppResult.Success).data
        val renewed = (repo.regenerateQr(pet.id) as AppResult.Success).data
        assertNotEquals(qr.token, renewed.token)
        assertEquals(8, renewed.shortCode.length)

        assertTrue(repo.deletePet(pet.id) is AppResult.Success)
        assertEquals(AppError.Server(404), (repo.getCarnet(pet.id) as AppResult.Failure).error)
    }

    @Test
    fun `lo proximo es la vacuna o desparasitacion mas cercana desde hoy`() = runTest {
        val carnet = (repo.getCarnet("ejemplo-firulais") as AppResult.Success).data
        val next = carnet.nextEvent(today = "2026-11-10")
        assertEquals("2026-11-16", next?.date)
        assertTrue(next!!.isVaccine)
    }
}
