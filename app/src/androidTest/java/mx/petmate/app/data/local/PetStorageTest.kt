package mx.petmate.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Consultas reales de Room para mascotas y carnet (spec 007, T-002, C-10; spec 008, T-002, C-01).
 *
 * La spec pide probar la migracion desde la version anterior. La version publicada (versionCode 3) no trae base:
 * Room crea las tablas al instalar, asi que aqui se prueba la creacion y los filtros. La primera migracion real
 * llegara con el primer cambio de esquema despues de publicar esta base (constitucion §4).
 */
@RunWith(AndroidJUnit4::class)
class PetStorageTest {

    private lateinit var database: PetMateDatabase
    private val pets get() = database.petDao()
    private val health get() = database.healthDao()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, PetMateDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun pet(owner: String, id: String, name: String, active: Boolean = true) =
        PetEntity(owner, id, name, "Perro", null, null, "Unknown", active, syncedAtMillis = 1L)

    private fun vaccine(owner: String, pet: String, id: String) =
        VaccineEntity(owner, pet, id, "Rabia", "2026-10-01", null, null, null, byBusiness = false, businessName = null)

    private fun only(vararg vaccines: VaccineEntity) =
        CarnetRows(vaccines.toList(), emptyList(), emptyList(), emptyList())

    @Test
    fun la_lista_filtra_por_dueno_y_por_activa() = runBlocking {
        pets.upsertAll(
            listOf(pet("ana", "p-1", "Luna"), pet("ana", "p-2", "Rocky", active = false), pet("beto", "p-3", "Michi")),
        )

        assertEquals(listOf("Luna"), pets.observeActive("ana").first().map { it.name })
        assertEquals(listOf("Michi"), pets.getActive("beto").map { it.name })
        assertNull(pets.get("beto", "p-1"))
    }

    @Test
    fun el_mismo_id_en_dos_cuentas_son_dos_filas() = runBlocking {
        pets.upsertAll(listOf(pet("ana", "p-1", "Luna"), pet("beto", "p-1", "Otra")))

        assertEquals("Luna", pets.get("ana", "p-1")?.name)
        assertEquals("Otra", pets.get("beto", "p-1")?.name)
    }

    @Test
    fun reemplazar_la_lista_no_toca_a_otro_dueno() = runBlocking {
        pets.upsertAll(listOf(pet("ana", "p-1", "Luna"), pet("beto", "p-3", "Michi")))

        pets.replaceAll("ana", listOf(pet("ana", "p-2", "Rocky")))

        assertEquals(listOf("Rocky"), pets.getActive("ana").map { it.name })
        assertEquals(listOf("Michi"), pets.getActive("beto").map { it.name })
    }

    @Test
    fun el_carnet_se_reemplaza_completo_y_queda_marcado() = runBlocking {
        assertNull(health.syncedAt("ana", "p-1"))

        health.replaceCarnet("ana", "p-1", only(vaccine("ana", "p-1", "v-1")), 5L)
        health.replaceCarnet("ana", "p-1", only(vaccine("ana", "p-1", "v-2")), 6L)

        assertEquals(listOf("v-2"), health.carnet("ana", "p-1").vaccines.map { it.id })
        assertEquals(6L, health.syncedAt("ana", "p-1"))
    }

    @Test
    fun borrar_el_carnet_de_una_mascota_no_toca_otra() = runBlocking {
        health.replaceCarnet("ana", "p-1", only(vaccine("ana", "p-1", "v-1")), 1L)
        health.replaceCarnet("ana", "p-2", only(vaccine("ana", "p-2", "v-2")), 1L)

        health.clearPet("ana", "p-1")

        assertTrue(health.carnet("ana", "p-1").vaccines.isEmpty())
        assertNull(health.syncedAt("ana", "p-1"))
        assertEquals(listOf("v-2"), health.carnet("ana", "p-2").vaccines.map { it.id })
    }
}
