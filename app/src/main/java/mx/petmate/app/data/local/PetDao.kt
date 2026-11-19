package mx.petmate.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Mascotas en Room (spec 007, T-002). Todas las consultas filtran por dueno y, la lista, por activas.
 *
 * Es clase abstracta para que [replaceAll] corra en una sola transaccion: la lista nunca queda a medias.
 */
@Dao
abstract class PetDao {

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId AND active = 1 ORDER BY name COLLATE NOCASE")
    abstract fun observeActive(ownerId: String): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId AND active = 1 ORDER BY name COLLATE NOCASE")
    abstract suspend fun getActive(ownerId: String): List<PetEntity>

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId AND id = :id AND active = 1")
    abstract suspend fun get(ownerId: String, id: String): PetEntity?

    @Query("SELECT COUNT(*) FROM pets WHERE ownerId = :ownerId")
    abstract suspend fun count(ownerId: String): Int

    @Upsert
    abstract suspend fun upsert(pet: PetEntity)

    @Upsert
    abstract suspend fun upsertAll(pets: List<PetEntity>)

    @Query("UPDATE pets SET active = :active WHERE ownerId = :ownerId AND id = :id")
    abstract suspend fun setActive(ownerId: String, id: String, active: Boolean)

    @Query("DELETE FROM pets WHERE ownerId = :ownerId AND id = :id")
    abstract suspend fun delete(ownerId: String, id: String)

    @Query("DELETE FROM pets WHERE ownerId = :ownerId")
    abstract suspend fun deleteAll(ownerId: String)

    /** Deja en Room exactamente la lista que mando el servidor para [ownerId]. Lo de otros duenos no se toca. */
    @Transaction
    open suspend fun replaceAll(ownerId: String, pets: List<PetEntity>) {
        deleteAll(ownerId)
        upsertAll(pets)
    }
}
