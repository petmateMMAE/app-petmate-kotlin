package mx.petmate.app.data.repository

import mx.petmate.app.domain.repository.PetRepository

/**
 * Elige de donde salen las mascotas en cada llamada:
 * - modo demo ("Probar sin cuenta") -> mascotas de ejemplo, sin red;
 * - backend simulado (USE_FAKE_BACKEND) -> tambien en memoria;
 * - sesion real -> backend.
 *
 * Las pantallas no saben cual se usa: siempre piden un PetRepository (constitution.md P3).
 */
class PetRepositoryRouter(
    private val remote: PetRepository,
    private val demo: PetRepository,
    private val isGuest: () -> Boolean,
) : PetRepository by DelegatingPetRepository({ if (isGuest()) demo else remote })

/** Delegacion dinamica: cada metodo pregunta a [target] a cual implementacion llamar. */
private class DelegatingPetRepository(private val target: () -> PetRepository) : PetRepository {
    override suspend fun getPets() = target().getPets()
    override suspend fun getPet(id: String) = target().getPet(id)
    override suspend fun createPet(draft: mx.petmate.app.domain.model.PetDraft) = target().createPet(draft)
    override suspend fun updatePet(id: String, draft: mx.petmate.app.domain.model.PetDraft) = target().updatePet(id, draft)
    override suspend fun deletePet(id: String) = target().deletePet(id)
    override suspend fun getQr(petId: String) = target().getQr(petId)
    override suspend fun regenerateQr(petId: String) = target().regenerateQr(petId)
    override suspend fun getCarnet(petId: String) = target().getCarnet(petId)
    override suspend fun addVaccine(petId: String, draft: mx.petmate.app.domain.model.VaccineDraft) = target().addVaccine(petId, draft)
    override suspend fun deleteVaccine(id: String) = target().deleteVaccine(id)
    override suspend fun addDeworming(petId: String, draft: mx.petmate.app.domain.model.DewormingDraft) = target().addDeworming(petId, draft)
    override suspend fun deleteDeworming(id: String) = target().deleteDeworming(id)
    override suspend fun addWeight(petId: String, measuredOn: String, kilograms: Double) = target().addWeight(petId, measuredOn, kilograms)
    override suspend fun addAllergy(petId: String, draft: mx.petmate.app.domain.model.AllergyDraft) = target().addAllergy(petId, draft)
    override suspend fun updateAllergy(id: String, draft: mx.petmate.app.domain.model.AllergyDraft) = target().updateAllergy(id, draft)
}
