package mx.petmate.app.data.sync

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Estado de una operacion en la cola (spec 005, plan D-02). */
enum class SyncOperationState {
    /** Espera su turno para enviarse. */
    Pending,

    /** Se esta enviando. Si el proceso muere aqui, al arrancar vuelve a [Pending]. */
    Sending,

    /** El servidor la rechazo con un 4xx: no se reintenta y queda visible (R-06). */
    PermanentError,
}

/**
 * Fila de la cola de escrituras pendientes (spec 005, T-002; plan SY-03).
 *
 * Tipos, acciones y estado se guardan por nombre para no depender del orden de los enums.
 * [localId] fija el FIFO cuando dos operaciones tienen la misma marca de tiempo.
 */
@Entity(
    tableName = "sync_operations",
    indices = [Index(value = ["targetType", "targetId"]), Index(value = ["idempotencyKey"], unique = true)],
)
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val targetType: String,
    val targetId: String,
    val action: String,
    val payload: String,
    val parentType: String?,
    val parentId: String?,
    val idempotencyKey: String,
    val state: String,
    val attempts: Int,
    val lastError: String?,
    val createdAtMillis: Long,
)
