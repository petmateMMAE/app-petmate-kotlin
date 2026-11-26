package mx.petmate.app.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Acceso a la cola de sincronizacion (spec 005, T-002). Orden FIFO: marca de tiempo y luego id local. */
@Dao
interface SyncOperationDao {

    @Insert
    suspend fun insert(operation: SyncOperationEntity): Long

    @Query("SELECT * FROM sync_operations ORDER BY createdAtMillis, localId")
    fun observeAll(): Flow<List<SyncOperationEntity>>

    @Query("SELECT * FROM sync_operations WHERE state = :state ORDER BY createdAtMillis, localId")
    suspend fun getByState(state: String): List<SyncOperationEntity>

    @Query(
        "UPDATE sync_operations SET state = :state, attempts = :attempts, lastError = :lastError " +
            "WHERE localId = :localId",
    )
    suspend fun updateState(localId: Long, state: String, attempts: Int, lastError: String?)

    @Query("UPDATE sync_operations SET state = :to WHERE state = :from")
    suspend fun moveAll(from: String, to: String): Int

    @Query("DELETE FROM sync_operations WHERE localId = :localId")
    suspend fun delete(localId: Long)
}
