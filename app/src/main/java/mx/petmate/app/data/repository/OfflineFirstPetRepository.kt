package mx.petmate.app.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.core.result.onSuccess
import mx.petmate.app.data.local.CarnetRows
import mx.petmate.app.data.local.HealthDao
import mx.petmate.app.data.local.PetDao
import mx.petmate.app.data.local.toDomain
import mx.petmate.app.data.local.toEntity
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.repository.PetRepository

/**
 * Mascotas y carnet offline-first (spec 007, T-004; spec 008, T-005; constitucion P4).
 *
 * - **Lecturas:** salen de Room y no tocan la red (008, C-02). Lo nuevo del servidor entra con [refreshPets] y
 *   [refreshCarnet], que la pantalla llama al aparecer.
 * - **Aislamiento:** todo se guarda y se lee con el usuario de la sesion ([currentUserId]); dos cuentas en el
 *   mismo telefono no se mezclan (007, C-09). Sin sesion no se lee ni se guarda nada local.
 * - **Escrituras:** van al servidor con [remote] (el `PetRepositoryImpl` de siempre) y, si las confirma, se
 *   guardan en Room. Escribir sin conexion y encolar (005, T-004 a T-007) queda pendiente de las respuestas
 *   del backend; mientras, sin red la escritura devuelve el error de red como antes.
 */
class OfflineFirstPetRepository(
    private val remote: PetRepository,
    private val petDao: PetDao,
    private val healthDao: HealthDao,
    private val currentUserId: () -> String?,
    private val dispatchers: DispatcherProvider,
    private val now: () -> Long = System::currentTimeMillis,
) : PetRepository {

    // ---------------- Lecturas: solo Room ----------------

    override suspend fun getPets(): AppResult<List<Pet>> = local { owner ->
        AppResult.Success(petDao.getActive(owner).map { it.toDomain() })
    }

    /** Desde Room; si esa mascota aun no se ha bajado (por ejemplo, un enlace directo), se pide una vez y se guarda. */
    override suspend fun getPet(id: String): AppResult<Pet> {
        val cached: AppResult<Pet> = local { owner ->
            petDao.get(owner, id)?.let { AppResult.Success(it.toDomain()) } ?: notDownloaded()
        }
        return if (cached is AppResult.Success) cached else remote.getPet(id).onSuccess { savePet(it) }
    }

    override suspend fun getCarnet(petId: String): AppResult<Carnet> = local { owner ->
        if (healthDao.syncedAt(owner, petId) == null) notDownloaded() else AppResult.Success(readCarnet(owner, petId))
    }

    override fun observePets(): Flow<List<Pet>> {
        val owner = currentUserId() ?: return emptyFlow()
        return petDao.observeActive(owner).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeCarnet(petId: String): Flow<Carnet> {
        val owner = currentUserId() ?: return emptyFlow()
        return healthDao.observeSync(owner, petId).filterNotNull().map { readCarnet(owner, petId) }
    }

    // ---------------- Refresco: red -> Room ----------------

    override suspend fun refreshPets(): AppResult<Unit> {
        val owner = currentUserId() ?: return AppResult.Failure(AppError.Unauthorized)
        val pets = when (val result = remote.getPets()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        withContext(dispatchers.io) { petDao.replaceAll(owner, pets.map { it.toEntity(owner, now()) }) }
        // Se bajan tambien los carnets para que la lista y cada carnet abran sin red. Si alguno falla, la lista
        // ya quedo guardada y ese carnet se intenta otra vez al abrirlo.
        coroutineScope { pets.map { pet -> async { refreshCarnet(pet.id) } }.awaitAll() }
        return AppResult.Success(Unit)
    }

    override suspend fun refreshCarnet(petId: String): AppResult<Unit> {
        val owner = currentUserId() ?: return AppResult.Failure(AppError.Unauthorized)
        val carnet = when (val result = remote.getCarnet(petId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val rows = CarnetRows(
            vaccines = carnet.vaccines.map { it.toEntity(owner, petId) },
            dewormings = carnet.dewormings.map { it.toEntity(owner, petId) },
            weights = carnet.weights.map { it.toEntity(owner, petId) },
            allergies = carnet.allergies.map { it.toEntity(owner, petId) },
        )
        withContext(dispatchers.io) { healthDao.replaceCarnet(owner, petId, rows, now()) }
        return AppResult.Success(Unit)
    }

    // ---------------- Escrituras: servidor y, si confirma, Room ----------------

    override suspend fun createPet(draft: PetDraft) = remote.createPet(draft).onSuccess { savePet(it) }

    override suspend fun updatePet(id: String, draft: PetDraft) = remote.updatePet(id, draft).onSuccess { savePet(it) }

    override suspend fun deletePet(id: String): AppResult<Unit> {
        val owner = currentUserId()
        // Deja de verse de inmediato; si el servidor falla, vuelve a aparecer.
        if (owner != null) withContext(dispatchers.io) { petDao.setActive(owner, id, false) }
        val result = remote.deletePet(id)
        if (owner != null) {
            withContext(dispatchers.io) {
                if (result is AppResult.Success) {
                    petDao.delete(owner, id)
                    healthDao.clearPet(owner, id)
                } else {
                    petDao.setActive(owner, id, true)
                }
            }
        }
        return result
    }

    override suspend fun getQr(petId: String) = remote.getQr(petId)

    override suspend fun regenerateQr(petId: String) = remote.regenerateQr(petId)

    override suspend fun addVaccine(petId: String, draft: VaccineDraft) =
        remote.addVaccine(petId, draft).onSuccess { saved ->
            carnetWrite(petId) { healthDao.upsertVaccines(listOf(saved.toEntity(it, petId))) }
        }

    override suspend fun deleteVaccine(id: String) = remote.deleteVaccine(id).onSuccess {
        ownerWrite { owner -> healthDao.deleteVaccine(owner, id) }
    }

    override suspend fun addDeworming(petId: String, draft: DewormingDraft) =
        remote.addDeworming(petId, draft).onSuccess { saved ->
            carnetWrite(petId) { healthDao.upsertDewormings(listOf(saved.toEntity(it, petId))) }
        }

    override suspend fun deleteDeworming(id: String) = remote.deleteDeworming(id).onSuccess {
        ownerWrite { owner -> healthDao.deleteDeworming(owner, id) }
    }

    override suspend fun addWeight(petId: String, measuredOn: String, kilograms: Double) =
        remote.addWeight(petId, measuredOn, kilograms).onSuccess { saved ->
            carnetWrite(petId) { healthDao.upsertWeights(listOf(saved.toEntity(it, petId))) }
        }

    override suspend fun addAllergy(petId: String, draft: AllergyDraft) =
        remote.addAllergy(petId, draft).onSuccess { saved -> saveAllergy(petId, saved) }

    override suspend fun updateAllergy(id: String, draft: AllergyDraft): AppResult<Allergy> {
        val result = remote.updateAllergy(id, draft)
        if (result is AppResult.Success) {
            val owner = currentUserId() ?: return result
            val petId = withContext(dispatchers.io) { healthDao.petOfAllergy(owner, id) } ?: return result
            saveAllergy(petId, result.data)
        }
        return result
    }

    // ---------------- Auxiliares ----------------

    private suspend fun <T> local(read: suspend (owner: String) -> AppResult<T>): AppResult<T> {
        val owner = currentUserId() ?: return AppResult.Failure(AppError.Unauthorized)
        return withContext(dispatchers.io) { read(owner) }
    }

    /** Nunca se descargo: sin conexion es el mismo error que antes, para que la pantalla muestre su aviso. */
    private fun <T> notDownloaded(): AppResult<T> = AppResult.Failure(AppError.Network)

    private suspend fun readCarnet(owner: String, petId: String): Carnet {
        val rows = withContext(dispatchers.io) { healthDao.carnet(owner, petId) }
        return Carnet(
            vaccines = rows.vaccines.map { it.toDomain() },
            dewormings = rows.dewormings.map { it.toDomain() },
            weights = rows.weights.map { it.toDomain() },
            allergies = rows.allergies.map { it.toDomain() },
        )
    }

    private suspend fun savePet(pet: Pet) = ownerWrite { owner -> petDao.upsert(pet.toEntity(owner, now())) }

    private suspend fun saveAllergy(petId: String, allergy: Allergy) =
        carnetWrite(petId) { healthDao.upsertAllergies(listOf(allergy.toEntity(it, petId))) }

    private suspend fun ownerWrite(write: suspend (owner: String) -> Unit) {
        val owner = currentUserId() ?: return
        withContext(dispatchers.io) { write(owner) }
    }

    /** Guarda un registro del carnet y avisa a quien observa ese carnet. */
    private suspend fun carnetWrite(petId: String, write: suspend (owner: String) -> Unit) = ownerWrite { owner ->
        write(owner)
        if (healthDao.syncedAt(owner, petId) != null) healthDao.touch(owner, petId, now())
    }
}
