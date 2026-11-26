package mx.petmate.app.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncStatus
import mx.petmate.app.domain.sync.SyncStatusRepository

/**
 * Traduce la cola a un estado por elemento (spec 005, T-008; plan SY-09, D-08).
 *
 * Si un elemento tiene varios cambios en la cola, manda el mas grave: error, luego enviando, luego pendiente.
 */
class SyncStatusRepositoryImpl(private val outbox: SyncOutbox) : SyncStatusRepository {

    override fun observeStatus(ref: SyncEntityRef): Flow<SyncStatus> =
        outbox.observe()
            .map { queue -> statusOf(queue.filter { it.operation.target == ref }) }
            .distinctUntilChanged()

    override suspend fun retry(ref: SyncEntityRef) {
        outbox.observe().first()
            .filter { it.operation.target == ref && it.state == SyncOperationState.PermanentError }
            .forEach { outbox.retryManually(it) }
    }

    private fun statusOf(items: List<QueuedOperation>): SyncStatus = when {
        items.any { it.state == SyncOperationState.PermanentError } -> SyncStatus.Error
        items.any { it.state == SyncOperationState.Sending } -> SyncStatus.Syncing
        items.isNotEmpty() -> SyncStatus.Pending
        else -> SyncStatus.Synced
    }
}
