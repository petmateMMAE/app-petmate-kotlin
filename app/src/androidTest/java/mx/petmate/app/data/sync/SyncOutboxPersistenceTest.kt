package mx.petmate.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.local.PetMateDatabase
import mx.petmate.app.domain.sync.SyncAction
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncEntityType
import mx.petmate.app.domain.sync.SyncOperation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La cola sobrevive a la muerte del proceso (spec 005, T-002, C-09).
 *
 * Se simula cerrando la base en disco y abriendo una nueva instancia sobre el mismo archivo, que es lo que
 * ve la app cuando el sistema la mata y la vuelve a lanzar.
 */
@RunWith(AndroidJUnit4::class)
class SyncOutboxPersistenceTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dispatchers = object : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
        override val main = Dispatchers.Main
    }

    private fun open() = Room.databaseBuilder(context, PetMateDatabase::class.java, DB_NAME).build()

    @Before
    fun setUp() {
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun la_cola_y_su_clave_sobreviven_a_reabrir_la_base() = runBlocking {
        val before = open()
        val outbox = SyncOutbox(before.syncOperationDao(), dispatchers)
        val pet = SyncEntityRef(SyncEntityType.Pet, "p-1")
        outbox.enqueue(SyncOperation(pet, SyncAction.Create, "{}", null, 1L))
        outbox.enqueue(SyncOperation(SyncEntityRef(SyncEntityType.Vaccine, "v-1"), SyncAction.Create, "{}", pet, 2L))
        val key = outbox.pending().first().idempotencyKey
        outbox.markSending(outbox.pending().first())
        before.close()

        val after = open()
        val revived = SyncOutbox(after.syncOperationDao(), dispatchers)
        assertEquals(1, revived.recoverInterrupted())
        val pending = revived.pending()
        after.close()

        assertEquals(listOf("p-1", "v-1"), pending.map { it.operation.target.id })
        assertEquals(key, pending.first().idempotencyKey)
        assertEquals(pet, pending[1].operation.dependsOn)
    }

    private companion object {
        const val DB_NAME = "sync-outbox-persistence-test.db"
    }
}
