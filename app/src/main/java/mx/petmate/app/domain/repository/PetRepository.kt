package mx.petmate.app.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.core.result.getOrNull
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
    /**
     * Lista de mascotas. En la implementacion offline-first sale de Room y no toca la red (spec 007, T-004);
     * para traer lo nuevo del servidor se llama [refreshPets].
     */
    suspend fun getPets(): AppResult<List<Pet>>
    suspend fun getPet(id: String): AppResult<Pet>
    suspend fun createPet(draft: PetDraft): AppResult<Pet>
    suspend fun updatePet(id: String, draft: PetDraft): AppResult<Pet>

    /** Borrado fisico: se borra tambien todo su carnet (cascada del backend, T122). */
    suspend fun deletePet(id: String): AppResult<Unit>

    suspend fun getQr(petId: String): AppResult<PetQr>

    /** Genera otro QR; el anterior deja de funcionar. */
    suspend fun regenerateQr(petId: String): AppResult<PetQr>

    /** Carnet completo. En la implementacion offline-first sale de Room y no toca la red (spec 008, C-02). */
    suspend fun getCarnet(petId: String): AppResult<Carnet>

    suspend fun addVaccine(petId: String, draft: VaccineDraft): AppResult<Vaccine>
    suspend fun deleteVaccine(id: String): AppResult<Unit>

    suspend fun addDeworming(petId: String, draft: DewormingDraft): AppResult<Deworming>
    suspend fun deleteDeworming(id: String): AppResult<Unit>

    suspend fun addWeight(petId: String, measuredOn: String, kilograms: Double): AppResult<WeightRecord>

    suspend fun addAllergy(petId: String, draft: AllergyDraft): AppResult<Allergy>
    suspend fun updateAllergy(id: String, draft: AllergyDraft): AppResult<Allergy>

    // ---- Offline-first (specs 007/T-004 y 008/T-005; constitucion P4) ----
    // Los valores por defecto sirven a las implementaciones sin base local (datos de ejemplo y remota directa):
    // leen como siempre y no tienen nada que refrescar.

    /** Lista que cambia sola cuando cambia la base local. */
    fun observePets(): Flow<List<Pet>> = flow { getPets().getOrNull()?.let { emit(it) } }

    /** Carnet que cambia solo cuando cambia la base local. */
    fun observeCarnet(petId: String): Flow<Carnet> = flow { getCarnet(petId).getOrNull()?.let { emit(it) } }

    /** Trae del servidor las mascotas y sus carnets y los guarda en el telefono. */
    suspend fun refreshPets(): AppResult<Unit> = AppResult.Success(Unit)

    /** Trae del servidor el carnet de [petId] y lo guarda en el telefono. */
    suspend fun refreshCarnet(petId: String): AppResult<Unit> = AppResult.Success(Unit)
}
