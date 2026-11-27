package mx.petmate.app.data.sync

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Marca de que un elemento se borro en este dispositivo (spec 005, T-007; plan D-06).
 *
 * Vive en Room y no en memoria **a proposito**: la lapida tiene que sobrevivir a la muerte del
 * proceso. Si se perdiera, la siguiente lectura del servidor devolveria el elemento y "reapareceria",
 * que es justo lo que la tarea existe para evitar (R-09, C-08).
 *
 * La llave primaria es el par tipo + id, no un autogenerado: borrar dos veces el mismo elemento es
 * la misma lapida, y con `OnConflictStrategy.REPLACE` el segundo borrado solo refresca la marca de
 * tiempo en lugar de dejar dos filas que luego habria que deduplicar.
 */
@Entity(tableName = "sync_tombstones", primaryKeys = ["entityType", "entityId"])
data class TombstoneEntity(
    val entityType: String,
    val entityId: String,
    /** Cuando se borro en el dispositivo. Sirve para depurar y para la limpieza por antiguedad. */
    val deletedAtMillis: Long,
    /**
     * `true` cuando el servidor ya confirmo el borrado.
     *
     * La fila **no se borra** al confirmar: se marca. El motivo es que una lectura del servidor
     * puede venir en camino con el elemento todavia dentro, y si la lapida ya no existiera, ese
     * dato entraria de nuevo a la base local. La limpieza la hace [TombstoneStore.pruneConfirmed]
     * cuando ya paso tiempo suficiente.
     */
    val confirmed: Boolean = false,
)

/** Acceso a las lapidas (spec 005, T-007). */
@Dao
interface TombstoneDao {

    /** Deja la lapida; si ya existia, refresca su marca de tiempo. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tombstone: TombstoneEntity)

    @Query("SELECT * FROM sync_tombstones WHERE entityType = :type AND entityId = :id LIMIT 1")
    suspend fun find(type: String, id: String): TombstoneEntity?

    @Query("SELECT entityId FROM sync_tombstones WHERE entityType = :type")
    suspend fun idsOfType(type: String): List<String>

    @Query("UPDATE sync_tombstones SET confirmed = 1 WHERE entityType = :type AND entityId = :id")
    suspend fun markConfirmed(type: String, id: String)

    @Query("SELECT * FROM sync_tombstones WHERE confirmed = 0")
    suspend fun unconfirmed(): List<TombstoneEntity>

    /** Borra las lapidas ya confirmadas y mas viejas que [beforeMillis]. */
    @Query("DELETE FROM sync_tombstones WHERE confirmed = 1 AND deletedAtMillis < :beforeMillis")
    suspend fun pruneConfirmed(beforeMillis: Long): Int
}
