package mx.petmate.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mx.petmate.app.data.local.entity.ProbeEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/** Base en memoria que escribe, lee y observa cambios (spec 001, T-011, C-05). */
@RunWith(AndroidJUnit4::class)
class PetMateDatabaseTest {

    private lateinit var database: PetMateDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, PetMateDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun escribe_lee_y_conserva_la_fecha() = runBlocking {
        val createdAt = Date(1_763_400_000_000L)
        val id = database.probeDao().insert(ProbeEntity(label = "uno", createdAt = createdAt))

        val stored = database.probeDao().getById(id)

        assertEquals("uno", stored?.label)
        assertEquals(createdAt, stored?.createdAt)
    }

    @Test
    fun el_flow_emite_cada_cambio() = runBlocking {
        val dao = database.probeDao()
        assertEquals(0, dao.observeAll().first().size)

        dao.insert(ProbeEntity(label = "uno", createdAt = Date(0)))
        dao.insert(ProbeEntity(label = "dos", createdAt = Date(0)))

        assertEquals(listOf("uno", "dos"), dao.observeAll().first().map { it.label })
    }
}
