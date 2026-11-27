package mx.petmate.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mx.petmate.app.data.local.dao.AffiliationDraftDao
import mx.petmate.app.data.local.entity.AffiliationDraftEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Borrador de afiliacion en SQLite (spec 011, T-006, C-08).
 *
 * Dos cosas que solo se pueden probar con base de verdad: que el borrador
 * sobrevive a cerrar la app, y que el filtro por `ownerId` lo aplica SQLite y
 * no el repositorio.
 */
@RunWith(AndroidJUnit4::class)
class AffiliationDraftDaoTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: PetMateDatabase
    private lateinit var dao: AffiliationDraftDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PetMateDatabase::class.java).build()
        dao = database.affiliationDraftDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun draft(
        ownerId: String,
        name: String = "Estetica Luna",
        step: String = "Services",
    ) = AffiliationDraftEntity(
        ownerId = ownerId,
        kind = "Grooming",
        name = name,
        phone = "5512345678",
        whatsApp = "",
        address = "Calle 2, Centro",
        description = "Bano y corte",
        licenseNumber = "",
        scheduleJson = """{"monday":[{"open":"09:00","close":"18:00"}]}""",
        servicesJson = """[{"name":"Bano","price":250.0}]""",
        latitude = 19.43,
        longitude = -99.13,
        step = step,
        savedAtMillis = 1_763_400_000_000L,
    )

    // ---- Guardar y leer ----

    @Test
    fun guarda_y_lee_el_borrador_completo() = runBlocking {
        val original = draft("u1")
        dao.upsert(original)

        assertEquals(original, dao.get("u1"))
    }

    @Test
    fun sin_borrador_devuelve_nulo() = runBlocking {
        assertNull(dao.get("u1"))
    }

    @Test
    fun guardar_de_nuevo_reemplaza_y_no_duplica() = runBlocking {
        dao.upsert(draft("u1", name = "Primero"))
        dao.upsert(draft("u1", name = "Segundo", step = "Pin"))

        val stored = dao.get("u1")

        assertEquals("Segundo", stored?.name)
        assertEquals("Pin", stored?.step)
    }

    @Test
    fun el_json_del_horario_y_los_servicios_se_guarda_tal_cual() = runBlocking {
        val original = draft("u1")
        dao.upsert(original)

        val stored = dao.get("u1")

        assertEquals(original.scheduleJson, stored?.scheduleJson)
        assertEquals(original.servicesJson, stored?.servicesJson)
    }

    @Test
    fun las_coordenadas_nulas_se_conservan_nulas() = runBlocking {
        dao.upsert(draft("u1").copy(latitude = null, longitude = null))

        val stored = dao.get("u1")

        assertNotNull(stored)
        assertNull(stored?.latitude)
        assertNull(stored?.longitude)
    }

    // ---- C-08: el borrador sobrevive a cerrar la app ----

    @Test
    fun el_borrador_sobrevive_a_reabrir_la_base() = runBlocking {
        context.deleteDatabase(DISK_DB)
        try {
            val before = Room.databaseBuilder(context, PetMateDatabase::class.java, DISK_DB).build()
            before.affiliationDraftDao().upsert(draft("u1", name = "Estetica Luna", step = "Pin"))
            before.close()

            // Es lo que ve la app cuando el sistema la mata y la vuelve a lanzar.
            val after = Room.databaseBuilder(context, PetMateDatabase::class.java, DISK_DB).build()
            val stored = after.affiliationDraftDao().get("u1")
            after.close()

            assertEquals("Estetica Luna", stored?.name)
            assertEquals("Pin", stored?.step)
            assertEquals(19.43, stored?.latitude ?: 0.0, 0.0001)
        } finally {
            context.deleteDatabase(DISK_DB)
        }
    }

    // ---- Aislamiento por cuenta: lo aplica SQLite ----

    @Test
    fun cada_cuenta_tiene_su_propio_borrador() = runBlocking {
        dao.upsert(draft("u1", name = "De Ana"))
        dao.upsert(draft("u2", name = "De Beto"))

        assertEquals("De Ana", dao.get("u1")?.name)
        assertEquals("De Beto", dao.get("u2")?.name)
    }

    @Test
    fun una_cuenta_no_ve_el_borrador_de_otra() = runBlocking {
        dao.upsert(draft("u1"))

        assertNull(dao.get("u2"))
    }

    @Test
    fun borrar_el_de_una_cuenta_no_toca_el_de_la_otra() = runBlocking {
        dao.upsert(draft("u1"))
        dao.upsert(draft("u2"))

        dao.delete("u1")

        assertNull(dao.get("u1"))
        assertNotNull(dao.get("u2"))
    }

    @Test
    fun borrar_los_de_otras_cuentas_deja_solo_el_de_la_sesion() = runBlocking {
        dao.upsert(draft("u1"))
        dao.upsert(draft("u2"))
        dao.upsert(draft("u3"))

        dao.deleteOthers("u2")

        assertNull(dao.get("u1"))
        assertNotNull(dao.get("u2"))
        assertNull(dao.get("u3"))
    }

    @Test
    fun borrar_todo_no_deja_nada() = runBlocking {
        dao.upsert(draft("u1"))
        dao.upsert(draft("u2"))

        dao.deleteAll()

        assertNull(dao.get("u1"))
        assertNull(dao.get("u2"))
    }

    @Test
    fun borrar_lo_que_no_existe_no_truena() = runBlocking {
        dao.delete("fantasma")
        dao.deleteOthers("fantasma")

        assertTrue(true)
    }

    // ---- El Flow avisa de los cambios ----

    @Test
    fun el_flow_da_el_borrador_de_su_cuenta() = runBlocking {
        dao.upsert(draft("u1", name = "De Ana"))
        dao.upsert(draft("u2", name = "De Beto"))

        assertEquals("De Ana", dao.observe("u1").first()?.name)
    }

    @Test
    fun el_flow_da_nulo_cuando_no_hay_borrador() = runBlocking {
        assertNull(dao.observe("u1").first())
    }

    private companion object {
        const val DISK_DB = "affiliation-draft-dao-test.db"
    }
}
