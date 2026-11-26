package mx.petmate.app.data.sync

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.domain.sync.SyncAction
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncEntityType
import mx.petmate.app.domain.sync.SyncOperation
import mx.petmate.app.domain.sync.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** Estado por elemento a partir de la cola y reintento manual (spec 005, T-008, C-11). */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncStatusRepositoryImplTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }
    private val outbox = SyncOutbox(InMemorySyncOperationDao(), dispatchers)
    private val repository = SyncStatusRepositoryImpl(outbox)

    private val pet = SyncEntityRef(SyncEntityType.Pet, "p-1")
    private val otherPet = SyncEntityRef(SyncEntityType.Pet, "p-2")

    private suspend fun enqueue(target: SyncEntityRef, at: Long = 1L) =
        outbox.enqueue(SyncOperation(target, SyncAction.Create, "{}", null, at))

    private suspend fun queuedOf(target: SyncEntityRef) =
        outbox.observe().first().first { it.operation.target == target }

    @Test
    fun `sin cambios en la cola el elemento esta sincronizado`() = runTest(dispatcher) {
        assertEquals(SyncStatus.Synced, repository.observeStatus(pet).first())
    }

    @Test
    fun `recorre pendiente, sincronizando y sincronizado`() = runTest(dispatcher) {
        repository.observeStatus(pet).test {
            assertEquals(SyncStatus.Synced, awaitItem())

            enqueue(pet)
            assertEquals(SyncStatus.Pending, awaitItem())

            outbox.markSending(queuedOf(pet))
            assertEquals(SyncStatus.Syncing, awaitItem())

            outbox.markDone(queuedOf(pet))
            assertEquals(SyncStatus.Synced, awaitItem())
        }
    }

    @Test
    fun `el error manda sobre los demas cambios del mismo elemento`() = runTest(dispatcher) {
        enqueue(pet, at = 1L)
        enqueue(pet, at = 2L)
        outbox.markPermanentError(outbox.pending().first(), "400")

        assertEquals(SyncStatus.Error, repository.observeStatus(pet).first())
    }

    @Test
    fun `los cambios de otra mascota no cambian el estado`() = runTest(dispatcher) {
        enqueue(otherPet)

        assertEquals(SyncStatus.Synced, repository.observeStatus(pet).first())
    }

    @Test
    fun `reintentar devuelve a pendiente solo los errores de ese elemento`() = runTest(dispatcher) {
        enqueue(pet)
        enqueue(otherPet)
        outbox.pending().forEach { outbox.markPermanentError(it, "400") }

        repository.retry(pet)

        assertEquals(SyncStatus.Pending, repository.observeStatus(pet).first())
        assertEquals(SyncStatus.Error, repository.observeStatus(otherPet).first())
    }
}
