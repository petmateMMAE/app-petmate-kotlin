package mx.petmate.app.domain.repository

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetQr
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.model.WeightRecord

/**
 * Mascotas, QR y carnet (specs 007, 008 y 009). Lo declara el dominio y lo implementa la capa de datos.
 * El backend solo devuelve lo del dueno de la sesion: aqui nunca se pasa un id de usuario.
 */
interface PetRepository {
    suspend fun getPets(): AppResult<List<Pet>>
    suspend fun getPet(id: String): AppResult<Pet>
    suspend fun createPet(draft: PetDraft): AppResult<Pet>
    suspend fun updatePet(id: String, draft: PetDraft): AppResult<Pet>

    /** Borrado fisico: se borra tambien todo su carnet (cascada del backend, T122). */
    suspend fun deletePet(id: String): AppResult<Unit>

    suspend fun getQr(petId: String): AppResult<PetQr>

    /** Genera otro QR; el anterior deja de funcionar. */
    suspend fun regenerateQr(petId: String): AppResult<PetQr>

    suspend fun getCarnet(petId: String): AppResult<Carnet>

    suspend fun addVaccine(petId: String, draft: VaccineDraft): AppResult<Vaccine>
    suspend fun deleteVaccine(id: String): AppResult<Unit>

    suspend fun addDeworming(petId: String, draft: DewormingDraft): AppResult<Deworming>
    suspend fun deleteDeworming(id: String): AppResult<Unit>

    suspend fun addWeight(petId: String, measuredOn: String, kilograms: Double): AppResult<WeightRecord>

    suspend fun addAllergy(petId: String, draft: AllergyDraft): AppResult<Allergy>
    suspend fun updateAllergy(id: String, draft: AllergyDraft): AppResult<Allergy>
}
