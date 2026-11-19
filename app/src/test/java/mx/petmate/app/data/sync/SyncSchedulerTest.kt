package mx.petmate.app.data.sync

import mx.petmate.app.domain.sync.SyncAction
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncEntityType
import mx.petmate.app.domain.sync.SyncOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Orden por grafo, FIFO por nivel y bloqueos (spec 005, T-003, C-04). */
class SyncSchedulerTest {

    private val scheduler = SyncScheduler()
    private val pet = SyncEntityRef(SyncEntityType.Pet, "p-1")
    private val otherPet = SyncEntityRef(SyncEntityType.Pet, "p-2")
    private var nextId = 1L

    private fun queued(
        target: SyncEntityRef,
        at: Long,
        parent: SyncEntityRef? = null,
        action: SyncAction = SyncAction.Create,
        state: SyncOperationState = SyncOperationState.Pending,
    ) = QueuedOperation(
        localId = nextId++,
        operation = SyncOperation(target, action, "{}", parent, at),
        idempotencyKey = "k-$nextId",
        state = state,
        attempts = 0,
        lastError = null,
    )

    private fun vaccine(id: String) = SyncEntityRef(SyncEntityType.Vaccine, id)

    private fun List<QueuedOperation>.ids() = map { "${it.operation.target.id}:${it.operation.action}" }

    @Test
    fun `una vacuna no sale antes que su mascota aunque se haya encolado antes`() {
        val plan = scheduler.plan(listOf(queued(vaccine("v-1"), at = 1L, parent = pet), queued(pet, at = 5L)))

        assertEquals(listOf("p-1:Create", "v-1:Create"), plan.ids())
    }

    @Test
    fun `dentro del mismo nivel se respeta FIFO`() {
        val plan = scheduler.plan(
            listOf(
                queued(pet, at = 30L, action = SyncAction.Update),
                queued(otherPet, at = 10L),
                queued(pet, at = 20L),
            ),
        )

        assertEquals(listOf("p-2:Create", "p-1:Create", "p-1:Update"), plan.ids())
    }

    @Test
    fun `con la misma marca de tiempo decide el orden en que se encolo`() {
        val first = queued(pet, at = 1L)
        val second = queued(otherPet, at = 1L)

        assertEquals(listOf(first, second), scheduler.plan(listOf(second, first)))
    }

    @Test
    fun `si la mascota tiene error permanente, sus vacunas y sus cambios esperan`() {
        val plan = scheduler.plan(
            listOf(
                queued(pet, at = 1L, state = SyncOperationState.PermanentError),
                queued(pet, at = 2L, action = SyncAction.Update),
                queued(vaccine("v-1"), at = 3L, parent = pet),
                queued(otherPet, at = 4L),
                queued(vaccine("v-2"), at = 5L, parent = otherPet),
            ),
        )

        assertEquals(listOf("p-2:Create", "v-2:Create"), plan.ids())
    }

    @Test
    fun `si la mascota se esta enviando, sus vacunas esperan`() {
        val plan = scheduler.plan(
            listOf(
                queued(pet, at = 1L, state = SyncOperationState.Sending),
                queued(vaccine("v-1"), at = 2L, parent = pet),
            ),
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun `una cola vacia da un plan vacio`() {
        assertTrue(scheduler.plan(emptyList()).isEmpty())
    }
}
