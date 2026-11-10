package mx.petmate.app.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetQr
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.model.WeightRecord
import mx.petmate.app.domain.repository.PetRepository
import java.util.UUID

/**
 * Mascotas de ejemplo en memoria.
 *
 * Se usa en dos casos: el modo demo ("Probar sin cuenta", la maqueta muestra "Mascotas de ejemplo")
 * y el backend simulado (USE_FAKE_BACKEND=true). Nada se guarda en disco ni sale del telefono.
 */
class SamplePetRepository : PetRepository {

    private val mutex = Mutex()
    private val pets = linkedMapOf<String, Pet>()
    private val qrs = mutableMapOf<String, PetQr>()
    private val carnets = mutableMapOf<String, Carnet>()

    init {
        seed()
    }

    override suspend fun getPets(): AppResult<List<Pet>> = locked { AppResult.Success(pets.values.reversed()) }

    override suspend fun getPet(id: String): AppResult<Pet> = locked { found(pets[id]) }

    override suspend fun createPet(draft: PetDraft): AppResult<Pet> = locked {
        val pet = draft.toPet(UUID.randomUUID().toString())
        pets[pet.id] = pet
        qrs[pet.id] = newQr(pet.id)
        carnets[pet.id] = Carnet()
        AppResult.Success(pet)
    }

    override suspend fun updatePet(id: String, draft: PetDraft): AppResult<Pet> = locked {
        if (id !in pets) return@locked notFound()
        val pet = draft.toPet(id)
        pets[id] = pet
        AppResult.Success(pet)
    }

    override suspend fun deletePet(id: String): AppResult<Unit> = locked {
        if (pets.remove(id) == null) return@locked notFound()
        qrs.remove(id)
        carnets.remove(id)
        AppResult.Success(Unit)
    }

    override suspend fun getQr(petId: String): AppResult<PetQr> = locked { found(qrs[petId]) }

    override suspend fun regenerateQr(petId: String): AppResult<PetQr> = locked {
        if (petId !in pets) return@locked notFound()
        val qr = newQr(petId)
        qrs[petId] = qr
        AppResult.Success(qr)
    }

    override suspend fun getCarnet(petId: String): AppResult<Carnet> = locked { found(carnets[petId]) }

    override suspend fun addVaccine(petId: String, draft: VaccineDraft): AppResult<Vaccine> = locked {
        val carnet = carnets[petId] ?: return@locked notFound()
        val vaccine = Vaccine(newId(), draft.name, draft.appliedOn, draft.batch.ifBlank { null }, draft.appliedBy.ifBlank { null }, draft.nextDoseOn, RecordOrigin.Owner)
        carnets[petId] = carnet.copy(vaccines = (carnet.vaccines + vaccine).sortedByDescending { it.appliedOn })
        AppResult.Success(vaccine)
    }

    override suspend fun deleteVaccine(id: String): AppResult<Unit> = locked {
        val entry = carnets.entries.firstOrNull { (_, c) -> c.vaccines.any { it.id == id } } ?: return@locked notFound()
        val record = entry.value.vaccines.first { it.id == id }
        if (record.origin.byBusiness) return@locked AppResult.Failure(AppError.Server(403))
        carnets[entry.key] = entry.value.copy(vaccines = entry.value.vaccines.filterNot { it.id == id })
        AppResult.Success(Unit)
    }

    override suspend fun addDeworming(petId: String, draft: DewormingDraft): AppResult<Deworming> = locked {
        val carnet = carnets[petId] ?: return@locked notFound()
        val record = Deworming(newId(), draft.product, draft.appliedOn, draft.type, draft.appliedBy.ifBlank { null }, draft.nextOn, RecordOrigin.Owner)
        carnets[petId] = carnet.copy(dewormings = (carnet.dewormings + record).sortedByDescending { it.appliedOn })
        AppResult.Success(record)
    }

    override suspend fun deleteDeworming(id: String): AppResult<Unit> = locked {
        val entry = carnets.entries.firstOrNull { (_, c) -> c.dewormings.any { it.id == id } } ?: return@locked notFound()
        val record = entry.value.dewormings.first { it.id == id }
        if (record.origin.byBusiness) return@locked AppResult.Failure(AppError.Server(403))
        carnets[entry.key] = entry.value.copy(dewormings = entry.value.dewormings.filterNot { it.id == id })
        AppResult.Success(Unit)
    }

    override suspend fun addWeight(petId: String, measuredOn: String, kilograms: Double): AppResult<WeightRecord> = locked {
        val carnet = carnets[petId] ?: return@locked notFound()
        val record = WeightRecord(newId(), measuredOn, kilograms, RecordOrigin.Owner)
        carnets[petId] = carnet.copy(weights = (carnet.weights + record).sortedByDescending { it.measuredOn })
        AppResult.Success(record)
    }

    override suspend fun addAllergy(petId: String, draft: AllergyDraft): AppResult<Allergy> = locked {
        val carnet = carnets[petId] ?: return@locked notFound()
        val record = Allergy(newId(), draft.name, draft.kind, draft.notes.ifBlank { null }, draft.active, RecordOrigin.Owner)
        carnets[petId] = carnet.copy(allergies = sortAllergies(carnet.allergies + record))
        AppResult.Success(record)
    }

    override suspend fun updateAllergy(id: String, draft: AllergyDraft): AppResult<Allergy> = locked {
        val entry = carnets.entries.firstOrNull { (_, c) -> c.allergies.any { it.id == id } } ?: return@locked notFound()
        val old = entry.value.allergies.first { it.id == id }
        if (old.origin.byBusiness) return@locked AppResult.Failure(AppError.Server(403))
        val updated = old.copy(name = draft.name, kind = draft.kind, notes = draft.notes.ifBlank { null }, active = draft.active)
        carnets[entry.key] = entry.value.copy(allergies = sortAllergies(entry.value.allergies.map { if (it.id == id) updated else it }))
        AppResult.Success(updated)
    }

    private suspend fun <T> locked(block: () -> AppResult<T>): AppResult<T> = mutex.withLock { block() }

    private fun <T> found(value: T?): AppResult<T> = value?.let { AppResult.Success(it) } ?: notFound()

    private fun notFound(): AppResult.Failure = AppResult.Failure(AppError.Server(404))

    private fun newId() = UUID.randomUUID().toString()

    private fun newQr(petId: String) = PetQr(
        petId = petId,
        token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").take(11),
        shortCode = (1..8).map { CODE_ALPHABET.random() }.joinToString(""),
    )

    private fun sortAllergies(list: List<Allergy>) = list.sortedByDescending { it.active }

    private fun PetDraft.toPet(id: String) = Pet(
        id = id,
        name = name.trim(),
        species = species ?: PetSpecies.Dog,
        breed = breed.trim().ifEmpty { null },
        birthDate = birthDate,
        sex = sex,
    )

    private fun seed() {
        val michi = Pet("ejemplo-michi", "Michi", PetSpecies.Cat, "Siamés", "2022-03-14", PetSex.Female)
        val firulais = Pet("ejemplo-firulais", "Firulais", PetSpecies.Dog, "Beagle", "2020-05-01", PetSex.Male)
        pets[michi.id] = michi
        pets[firulais.id] = firulais
        qrs[firulais.id] = PetQr(firulais.id, "ejemplo-token-firulais-no-funciona-en-negocios", "PM7K3QXA")
        qrs[michi.id] = PetQr(michi.id, "ejemplo-token-michi-no-funciona-en-negocios", "PM4HD9TW")
        val vet = RecordOrigin(byBusiness = true, businessName = "Veterinaria San Marcos")
        carnets[firulais.id] = Carnet(
            vaccines = listOf(
                Vaccine("v2", "Múltiple", "2026-05-12", "MX-2214", "Veterinaria San Marcos", "2026-11-16", vet),
                Vaccine("v1", "Rabia", "2025-11-03", "RB-0931", "Yo", "2026-11-03", RecordOrigin.Owner),
            ),
            dewormings = listOf(
                Deworming("d1", "Drontal Plus", "2026-08-20", DewormingType.Internal, "Yo", "2026-11-20", RecordOrigin.Owner),
            ),
            weights = listOf(
                WeightRecord("w3", "2026-10-13", 12.4, RecordOrigin.Owner),
                WeightRecord("w2", "2026-07-10", 12.1, RecordOrigin.Owner),
                WeightRecord("w1", "2026-04-02", 11.6, vet),
            ),
            allergies = listOf(
                Allergy("a1", "Pollo", AllergyKind.Allergy, "Le da comezón y se rasca las orejas. Croquetas sin pollo.", true, RecordOrigin.Owner),
            ),
        )
        carnets[michi.id] = Carnet(
            vaccines = listOf(Vaccine("v3", "Triple felina", "2026-02-10", null, "Yo", "2027-02-10", RecordOrigin.Owner)),
            weights = listOf(WeightRecord("w4", "2026-09-30", 3.8, RecordOrigin.Owner)),
        )
    }

    private companion object {
        /** Sin simbolos ambiguos (0/O, 1/I), como el codigo corto del backend. */
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
