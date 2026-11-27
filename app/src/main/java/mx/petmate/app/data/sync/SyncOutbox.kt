package mx.petmate.app.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.domain.sync.SyncAction
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncEntityType
import mx.petmate.app.domain.sync.SyncOperation

/** Operacion de la cola con su estado. */
data class QueuedOperation(
    val localId: Long,
    val operation: SyncOperation,
    val idempotencyKey: String,
    val state: SyncOperationState,
    val attempts: Int,
    val lastError: String?,
)

/**
 * La unica cola de escrituras pendientes de la app (spec 005, T-002; plan D-02). Vive en Room, asi que
 * sobrevive al cierre de la app y a la muerte del proceso (R-02).
 *
 * La clave de idempotencia se genera una vez al encolar y se guarda: cada reintento manda la misma (R-04).
 * La politica de que se puede encolar la decide `SyncPolicy` en el repositorio, no aqui (R-13).
 */
class SyncOutbox(
    private val dao: SyncOperationDao,
    private val dispatchers: DispatcherProvider,
    /**
     * De dónde sale la clave de idempotencia. Por omisión se **deriva** de la operación
     * ([SyncIdempotencyKey]) y no se sortea: si la fila se pierde y la app vuelve a encolar la
     * misma escritura, la clave vuelve a ser la misma y el servidor la reconoce como repetida en
     * vez de aplicarla dos veces (spec 005, T-004, R-04). Se deja inyectable para las pruebas.
     */
    private val newKey: (SyncOperation) -> String = SyncIdempotencyKey::of,
) {

    /** Toda la cola en orden FIFO, para pintar el estado de cada elemento. */
    fun observe(): Flow<List<QueuedOperation>> = dao.observeAll().map { rows -> rows.map { it.toQueued() } }

    /** Guarda [operation] como pendiente y devuelve su id local. */
    suspend fun enqueue(operation: SyncOperation): Long = withContext(dispatchers.io) {
        dao.insert(operation.toEntity(newKey(operation)))
    }

    /** Operaciones listas para enviarse, en orden FIFO. */
    suspend fun pending(): List<QueuedOperation> = withContext(dispatchers.io) {
        dao.getByState(SyncOperationState.Pending.name).map { it.toQueued() }
    }

    /** Marca el inicio del envio y suma un intento. */
    suspend fun markSending(item: QueuedOperation) = withContext(dispatchers.io) {
        dao.updateState(item.localId, SyncOperationState.Sending.name, item.attempts + 1, null)
    }

    /** Devuelve a la espera una operacion que fallo por un error transitorio. */
    suspend fun markRetry(item: QueuedOperation, reason: String) = withContext(dispatchers.io) {
        dao.updateState(item.localId, SyncOperationState.Pending.name, item.attempts, reason)
    }

    /** Deja visible una operacion rechazada por el servidor (R-06). */
    suspend fun markPermanentError(item: QueuedOperation, reason: String) = withContext(dispatchers.io) {
        dao.updateState(item.localId, SyncOperationState.PermanentError.name, item.attempts, reason)
    }

    /** Quita de la cola una operacion que el servidor ya confirmo. */
    suspend fun markDone(item: QueuedOperation) = withContext(dispatchers.io) {
        dao.delete(item.localId)
    }

    /** Reintento manual de una operacion con error permanente (R-10). */
    suspend fun retryManually(item: QueuedOperation) = withContext(dispatchers.io) {
        dao.updateState(item.localId, SyncOperationState.Pending.name, item.attempts, null)
    }

    /** Descarta una operacion con error permanente a pedido del usuario (R-06). */
    suspend fun discard(item: QueuedOperation) = withContext(dispatchers.io) {
        dao.delete(item.localId)
    }

    /**
     * Al arrancar, lo que quedo "enviando" porque el proceso murio vuelve a pendiente. Como lleva la misma
     * clave de idempotencia, reenviarlo no duplica. Devuelve cuantas operaciones se recuperaron.
     */
    suspend fun recoverInterrupted(): Int = withContext(dispatchers.io) {
        dao.moveAll(from = SyncOperationState.Sending.name, to = SyncOperationState.Pending.name)
    }
}

private fun SyncOperation.toEntity(idempotencyKey: String) = SyncOperationEntity(
    targetType = target.type.name,
    targetId = target.id,
    action = action.name,
    payload = payload,
    parentType = dependsOn?.type?.name,
    parentId = dependsOn?.id,
    idempotencyKey = idempotencyKey,
    state = SyncOperationState.Pending.name,
    attempts = 0,
    lastError = null,
    createdAtMillis = createdAtMillis,
)

private fun SyncOperationEntity.toQueued(): QueuedOperation {
    val parent = if (parentType != null && parentId != null) {
        SyncEntityRef(SyncEntityType.valueOf(parentType), parentId)
    } else {
        null
    }
    return QueuedOperation(
        localId = localId,
        operation = SyncOperation(
            target = SyncEntityRef(SyncEntityType.valueOf(targetType), targetId),
            action = SyncAction.valueOf(action),
            payload = payload,
            dependsOn = parent,
            createdAtMillis = createdAtMillis,
        ),
        idempotencyKey = idempotencyKey,
        state = SyncOperationState.valueOf(state),
        attempts = attempts,
        lastError = lastError,
    )
}
