package mx.petmate.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Las cuatro tablas del carnet de una mascota, ya leidas. */
data class CarnetRows(
    val vaccines: List<VaccineEntity>,
    val dewormings: List<DewormingEntity>,
    val weights: List<WeightEntity>,
    val allergies: List<AllergyEntity>,
)

/**
 * Carnet en Room (spec 008, T-002). Todo filtra por dueno y por mascota; [replaceCarnet] cambia el carnet
 * completo en una sola transaccion.
 */
@Dao
abstract class HealthDao {

    @Query("SELECT * FROM vaccines WHERE ownerId = :ownerId AND petId = :petId ORDER BY appliedOn DESC")
    abstract suspend fun vaccines(ownerId: String, petId: String): List<VaccineEntity>

    @Query("SELECT * FROM dewormings WHERE ownerId = :ownerId AND petId = :petId ORDER BY appliedOn DESC")
    abstract suspend fun dewormings(ownerId: String, petId: String): List<DewormingEntity>

    @Query("SELECT * FROM weights WHERE ownerId = :ownerId AND petId = :petId ORDER BY measuredOn DESC")
    abstract suspend fun weights(ownerId: String, petId: String): List<WeightEntity>

    @Query("SELECT * FROM allergies WHERE ownerId = :ownerId AND petId = :petId ORDER BY active DESC, name")
    abstract suspend fun allergies(ownerId: String, petId: String): List<AllergyEntity>

    /** Emite cada vez que cambia cualquier tabla del carnet; quien observa vuelve a leer con [carnet]. */
    @Query("SELECT syncedAtMillis FROM carnet_sync WHERE ownerId = :ownerId AND petId = :petId")
    abstract fun observeSync(ownerId: String, petId: String): Flow<Long?>

    @Query("SELECT syncedAtMillis FROM carnet_sync WHERE ownerId = :ownerId AND petId = :petId")
    abstract suspend fun syncedAt(ownerId: String, petId: String): Long?

    @Upsert abstract suspend fun upsertVaccines(rows: List<VaccineEntity>)

    @Upsert abstract suspend fun upsertDewormings(rows: List<DewormingEntity>)

    @Upsert abstract suspend fun upsertWeights(rows: List<WeightEntity>)

    @Upsert abstract suspend fun upsertAllergies(rows: List<AllergyEntity>)

    @Upsert abstract suspend fun markSynced(sync: CarnetSyncEntity)

    @Query("DELETE FROM vaccines WHERE ownerId = :ownerId AND id = :id")
    abstract suspend fun deleteVaccine(ownerId: String, id: String)

    @Query("DELETE FROM dewormings WHERE ownerId = :ownerId AND id = :id")
    abstract suspend fun deleteDeworming(ownerId: String, id: String)

    @Query("SELECT petId FROM allergies WHERE ownerId = :ownerId AND id = :id")
    abstract suspend fun petOfAllergy(ownerId: String, id: String): String?

    @Query("DELETE FROM vaccines WHERE ownerId = :ownerId AND petId = :petId")
    abstract suspend fun clearVaccines(ownerId: String, petId: String)

    @Query("DELETE FROM dewormings WHERE ownerId = :ownerId AND petId = :petId")
    abstract suspend fun clearDewormings(ownerId: String, petId: String)

    @Query("DELETE FROM weights WHERE ownerId = :ownerId AND petId = :petId")
    abstract suspend fun clearWeights(ownerId: String, petId: String)

    @Query("DELETE FROM allergies WHERE ownerId = :ownerId AND petId = :petId")
    abstract suspend fun clearAllergies(ownerId: String, petId: String)

    @Query("DELETE FROM carnet_sync WHERE ownerId = :ownerId AND petId = :petId")
    abstract suspend fun clearSync(ownerId: String, petId: String)

    @Transaction
    open suspend fun carnet(ownerId: String, petId: String) = CarnetRows(
        vaccines(ownerId, petId),
        dewormings(ownerId, petId),
        weights(ownerId, petId),
        allergies(ownerId, petId),
    )

    /** Deja en Room el carnet que mando el servidor y lo marca como descargado. */
    @Transaction
    open suspend fun replaceCarnet(ownerId: String, petId: String, rows: CarnetRows, nowMillis: Long) {
        clearPet(ownerId, petId)
        upsertVaccines(rows.vaccines)
        upsertDewormings(rows.dewormings)
        upsertWeights(rows.weights)
        upsertAllergies(rows.allergies)
        markSynced(CarnetSyncEntity(ownerId, petId, nowMillis))
    }

    /** Borra todo el carnet de una mascota, por ejemplo al borrar la mascota (cascada del backend). */
    @Transaction
    open suspend fun clearPet(ownerId: String, petId: String) {
        clearVaccines(ownerId, petId)
        clearDewormings(ownerId, petId)
        clearWeights(ownerId, petId)
        clearAllergies(ownerId, petId)
        clearSync(ownerId, petId)
    }

    /** Cambia la marca de sincronizacion para que [observeSync] avise a quien observa el carnet. */
    suspend fun touch(ownerId: String, petId: String, nowMillis: Long) =
        markSynced(CarnetSyncEntity(ownerId, petId, nowMillis))
}
