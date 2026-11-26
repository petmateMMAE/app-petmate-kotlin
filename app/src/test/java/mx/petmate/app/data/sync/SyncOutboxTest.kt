package mx.petmate.app.data.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.domain.sync.SyncAction
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncEntityType
import mx.petmate.app.domain.sync.SyncOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Estados, clave de idempotencia y recuperacion de la cola (spec 005, T-002). La persistencia va en androidTest. */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncOutboxTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }
    private val dao = InMemorySyncOperationDao()
    private var keys = 0
    private val outbox = SyncOutbox(dao, dispatchers, newKey = { "key-${++keys}" })

    private val pet = SyncEntityRef(SyncEntityType.Pet, "p-1")
    private fun operation(
        target: SyncEntityRef = pet,
        parent: SyncEntityRef? = null,
        at: Long = 1L,
    ) = SyncOperation(target, SyncAction.Create, "{}", parent, at)

    @Test
    fun `encolar deja la operacion pendiente con su padre y una clave propia`() = runTest(dispatcher) {
        val vaccine = SyncEntityRef(SyncEntityType.Vaccine, "v-1")
        outbox.enqueue(operation(target = vaccine, parent = pet))

        val queued = outbox.pending().single()

        assertEquals(SyncOperationState.Pending, queued.state)
        assertEquals(vaccine, queued.operation.target)
        assertEquals(pet, queued.operation.dependsOn)
        assertEquals("key-1", queued.idempotencyKey)
        assertEquals(0, queued.attempts)
    }

    @Test
    fun `pendientes salen en orden FIFO`() = runTest(dispatcher) {
        outbox.enqueue(operation(target = SyncEntityRef(SyncEntityType.Pet, "b"), at = 20L))
        outbox.enqueue(operation(target = SyncEntityRef(SyncEntityType.Pet, "a"), at = 10L))

        assertEquals(listOf("a", "b"), outbox.pending().map { it.operation.target.id })
    }

    @Test
    fun `un reintento conserva la misma clave y suma el intento`() = runTest(dispatcher) {
        outbox.enqueue(operation())
        val first = outbox.pending().single()

        outbox.markSending(first)
        outbox.markRetry(outbox.observe().first().single(), "timeout")
        val again = outbox.pending().single()

        assertEquals(first.idempotencyKey, again.idempotencyKey)
        assertEquals(1, again.attempts)
        assertEquals("timeout", again.lastError)
    }

    @Test
    fun `un error permanente sale de pendientes, queda visible y se puede reintentar a mano`() = runTest(dispatcher) {
        outbox.enqueue(operation())
        outbox.markPermanentError(outbox.pending().single(), "400")

        assertTrue(outbox.pending().isEmpty())
        val failed = outbox.observe().first().single()
        assertEquals(SyncOperationState.PermanentError, failed.state)

        outbox.retryManually(failed)
        assertNull(outbox.pending().single().lastError)
    }

    @Test
    fun `lo confirmado y lo descartado salen de la cola`() = runTest(dispatcher) {
        outbox.enqueue(operation(target = SyncEntityRef(SyncEntityType.Pet, "a")))
        outbox.enqueue(operation(target = SyncEntityRef(SyncEntityType.Pet, "b")))
        val (a, b) = outbox.pending()

        outbox.markDone(a)
        outbox.discard(b)

        assertTrue(outbox.observe().first().isEmpty())
    }

    @Test
    fun `al arrancar, lo que quedo enviando vuelve a pendiente`() = runTest(dispatcher) {
        outbox.enqueue(operation())
        outbox.markSending(outbox.pending().single())

        assertEquals(1, outbox.recoverInterrupted())
        assertEquals(SyncOperationState.Pending, outbox.pending().single().state)
    }
}

/** DAO en memoria con el mismo orden que las consultas de Room. */
internal class InMemorySyncOperationDao : SyncOperationDao {
    private val rows = MutableStateFlow<List<SyncOperationEntity>>(emptyList())
    private var nextId = 1L

    private fun sorted(list: List<SyncOperationEntity>) =
        list.sortedWith(compareBy({ it.createdAtMillis }, { it.localId }))

    override suspend fun insert(operation: SyncOperationEntity): Long {
        val id = nextId++
        rows.value = rows.value + operation.copy(localId = id)
        return id
    }

    override fun observeAll(): Flow<List<SyncOperationEntity>> = rows.map { sorted(it) }

    override suspend fun getByState(state: String) = sorted(rows.value.filter { it.state == state })

    override suspend fun updateState(localId: Long, state: String, attempts: Int, lastError: String?) {
        rows.value = rows.value.map {
            if (it.localId == localId) it.copy(state = state, attempts = attempts, lastError = lastError) else it
        }
    }

    override suspend fun moveAll(from: String, to: String): Int {
        val moved = rows.value.count { it.state == from }
        rows.value = rows.value.map { if (it.state == from) it.copy(state = to) else it }
        return moved
    }

    override suspend fun delete(localId: Long) {
        rows.value = rows.value.filterNot { it.localId == localId }
    }
}
