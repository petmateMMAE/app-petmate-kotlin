package mx.petmate.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import mx.petmate.app.data.local.entity.GeneratedTextEntity
import mx.petmate.app.domain.model.ContentFingerprint
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** La cache solo sirve el texto si el original no cambio (spec 019, T-004, D-04). */
@RunWith(AndroidJUnit4::class)
class GeneratedTextDaoTest {

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

    private val receta = "Amoxicilina 250 mg cada 12 horas"

    @Test
    fun mismo_contenido_sirve_la_cache_y_contenido_cambiado_no() = runBlocking {
        val dao = database.generatedTextDao()
        val huella = ContentFingerprint.of(receta)
        dao.upsert(GeneratedTextEntity("ana", "prescription:c-1", huella, "Dale una pastilla...", 1L))

        assertEquals("Dale una pastilla...", dao.get("ana", "prescription:c-1", huella)?.text)
        assertNull(dao.get("ana", "prescription:c-1", ContentFingerprint.of("$receta por 7 dias")))
    }

    @Test
    fun otra_cuenta_no_ve_el_texto() = runBlocking {
        val dao = database.generatedTextDao()
        val huella = ContentFingerprint.of(receta)
        dao.upsert(GeneratedTextEntity("ana", "prescription:c-1", huella, "texto", 1L))

        assertNull(dao.get("beto", "prescription:c-1", huella))
    }
}
