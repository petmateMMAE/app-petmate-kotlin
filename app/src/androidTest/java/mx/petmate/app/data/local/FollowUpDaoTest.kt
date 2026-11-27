package mx.petmate.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Seguimientos guardados para leerse sin conexion (spec 015, T-007, R-10).
 *
 * Lo que mas importa aqui es la decision **D-06**: el seguimiento guarda estado, fechas, autor y notas, pero
 * NUNCA diagnostico ni receta. Eso se revisa contra el esquema real de SQLite con `PRAGMA table_info`, no
 * contra la clase de Kotlin: asi la prueba falla si alguien agrega una columna clinica aunque el modelo
 * parezca inocente.
 */
@RunWith(AndroidJUnit4::class)
class FollowUpDaoTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: PetMateDatabase
    private lateinit var dao: FollowUpDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PetMateDatabase::class.java).build()
        dao = database.followUpDao()
    }

    @After
    fun tearDown() = database.close()

    private fun followUp(
        owner: String = "u1",
        consultation: String = "c1",
        status: String = "Pending",
        notes: String = """[{"author":"Vet","text":"Revisar la herida"}]""",
    ) = FollowUpEntity(
        ownerId = owner,
        consultationId = consultation,
        id = "f-$consultation",
        petId = "p1",
        petName = "Firulais",
        status = status,
        nextReviewOn = "2026-12-01",
        isOverdue = false,
        notesJson = notes,
        conversationId = "conv-1",
        createdAt = "2026-11-20T10:00:00Z",
        updatedAt = "2026-11-20T10:00:00Z",
    )

    @Test
    fun guarda_y_devuelve_el_seguimiento() = runBlocking {
        dao.upsert(followUp())

        val stored = dao.get("u1", "c1")

        assertNotNull("El seguimiento guardado debe poder leerse sin conexion.", stored)
        assertEquals("Firulais", stored!!.petName)
        assertEquals("Pending", stored.status)
        assertEquals("2026-12-01", stored.nextReviewOn)
    }

    @Test
    fun upsert_reemplaza_en_vez_de_duplicar() = runBlocking {
        dao.upsert(followUp(status = "Pending"))
        dao.upsert(followUp(status = "Done"))

        val stored = dao.get("u1", "c1")

        // La clave primaria es (ownerId, consultationId): refrescar desde la red actualiza, no acumula.
        assertEquals("Done", stored!!.status)
    }

    @Test
    fun una_cuenta_no_ve_el_seguimiento_de_otra() = runBlocking {
        dao.upsert(followUp(owner = "u1"))

        // Dos cuentas en el mismo telefono no comparten datos: el filtro lo aplica SQLite, no el repositorio.
        assertNull("La cuenta u2 no debe ver el seguimiento de u1.", dao.get("u2", "c1"))
        assertNotNull(dao.get("u1", "c1"))
    }

    @Test
    fun el_mismo_consultationId_en_dos_cuentas_no_se_pisa() = runBlocking {
        dao.upsert(followUp(owner = "u1", status = "Pending"))
        dao.upsert(followUp(owner = "u2", status = "Done"))

        assertEquals("Pending", dao.get("u1", "c1")!!.status)
        assertEquals("Done", dao.get("u2", "c1")!!.status)
    }

    @Test
    fun las_notas_se_guardan_tal_cual() = runBlocking {
        val json = """[{"author":"Vet","text":"Revisar la herida"},{"author":"Dueno","text":"Ya cicatrizo"}]"""
        dao.upsert(followUp(notes = json))

        // El backend manda las notas completas; guardarlas tal cual evita perder una al volver a serializar.
        assertEquals(json, dao.get("u1", "c1")!!.notesJson)
    }

    @Test
    fun la_fecha_de_revision_puede_faltar() = runBlocking {
        dao.upsert(followUp().copy(nextReviewOn = null, conversationId = null))

        val stored = dao.get("u1", "c1")!!

        // Un seguimiento sin fecha de revision es valido: no se inventa una por conveniencia del esquema.
        assertNull(stored.nextReviewOn)
        assertNull(stored.conversationId)
    }

    @Test
    fun la_tabla_no_guarda_contenido_clinico() {
        // D-06: el seguimiento no es el carnet. Diagnostico y receta viven en 014 y no se copian aqui.
        // Se revisa el esquema real de SQLite para que agregar una columna clinica rompa esta prueba.
        val prohibidas = listOf(
            "diagnosis", "diagnostico", "prescription", "receta", "medication", "medicamento",
            "dosage", "dosis", "treatment", "tratamiento", "weight", "peso", "allergy", "alergia",
            "vaccine", "vacuna", "symptom", "sintoma",
        )

        val columnas = mutableListOf<String>()
        database.query("PRAGMA table_info(followups)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                columnas += cursor.getString(nameIndex)
            }
        }

        assertTrue("No se leyo el esquema de followups.", columnas.isNotEmpty())
        val ofensoras = columnas.filter { columna ->
            prohibidas.any { columna.lowercase().contains(it) }
        }
        assertTrue(
            "La tabla followups no puede guardar contenido clinico (D-06). Columnas: $ofensoras",
            ofensoras.isEmpty(),
        )
    }

    @Test
    fun el_esquema_es_exactamente_el_acordado() {
        // Fija la lista completa. Si alguien agrega una columna, esta prueba lo obliga a pasar por aqui
        // a proposito y a justificarla, en vez de que entre sin que nadie la vea.
        val esperadas = setOf(
            "ownerId", "consultationId", "id", "petId", "petName", "status",
            "nextReviewOn", "isOverdue", "notesJson", "conversationId", "createdAt", "updatedAt",
        )

        val columnas = mutableSetOf<String>()
        database.query("PRAGMA table_info(followups)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                columnas += cursor.getString(nameIndex)
            }
        }

        assertEquals("El esquema de followups cambio. Si es a proposito, actualiza esta prueba.", esperadas, columnas)
    }
}
