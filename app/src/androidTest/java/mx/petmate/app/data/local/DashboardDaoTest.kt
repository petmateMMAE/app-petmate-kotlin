package mx.petmate.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mx.petmate.app.data.local.dao.DashboardDao
import mx.petmate.app.data.local.entity.AttendedPetEntity
import mx.petmate.app.data.local.entity.DashboardAlertEntity
import mx.petmate.app.data.local.entity.DashboardEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cache del panel en SQLite (spec 012, T-006).
 *
 * Aparte del guardado y el aislamiento por cuenta, aqui se revisa la decision
 * **D-05**: la cache guarda identificadores y fechas, nunca contenido clinico.
 * La revision se hace contra el esquema real de SQLite (`PRAGMA table_info`),
 * no contra las clases: asi la prueba falla si alguien agrega una columna de
 * diagnostico o receta aunque el modelo parezca inocente.
 */
@RunWith(AndroidJUnit4::class)
class DashboardDaoTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: PetMateDatabase
    private lateinit var dao: DashboardDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PetMateDatabase::class.java).build()
        dao = database.dashboardDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun dashboard(ownerId: String, name: String = "Veterinaria Centro") = DashboardEntity(
        ownerId = ownerId,
        businessId = "b-$ownerId",
        businessName = name,
        kind = "Veterinary",
        photoUrl = null,
        overdue = 1,
        soon = 2,
        unreadMessages = 3,
        syncedAtMillis = 1_763_400_000_000L,
    )

    private fun alert(ownerId: String, id: String, reviewOn: String?) = DashboardAlertEntity(
        ownerId = ownerId,
        followupId = id,
        petId = "p-$id",
        petName = "Nube",
        nextReviewOn = reviewOn,
        severity = "Urgent",
        unreadMessages = 0,
    )

    private fun attended(ownerId: String, name: String, at: String) = AttendedPetEntity(
        ownerId = ownerId,
        name = name,
        scannedAt = at,
        scannedOn = at.take(10),
        petId = null,
    )

    /** Columnas reales de una tabla, segun SQLite. */
    private fun columnsOf(table: String): List<String> {
        val columns = mutableListOf<String>()
        database.openHelper.readableDatabase
            .query("PRAGMA table_info(`$table`)")
            .use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
            }
        return columns
    }

    // ---- D-05: nada clinico en la cache ----

    @Test
    fun ninguna_tabla_del_panel_tiene_columnas_clinicas() {
        val forbidden = listOf(
            "diagnosis", "diagnostico", "prescription", "receta", "medication", "medicamento",
            "treatment", "tratamiento", "notes", "notas", "reason", "motivo", "weight", "peso",
            "allergy", "alergia", "vaccine", "vacuna",
        )

        listOf("dashboard", "dashboard_alerts", "dashboard_attended").forEach { table ->
            val columns = columnsOf(table).map(String::lowercase)
            forbidden.forEach { word ->
                assertTrue(
                    "la tabla $table no debe tener una columna con '$word': $columns",
                    columns.none { it.contains(word) },
                )
            }
        }
    }

    @Test
    fun las_columnas_del_panel_son_exactamente_las_esperadas() {
        assertEquals(
            listOf(
                "ownerId", "businessId", "businessName", "kind", "photoUrl",
                "overdue", "soon", "unreadMessages", "syncedAtMillis",
            ).sorted(),
            columnsOf("dashboard").sorted(),
        )
        assertEquals(
            listOf("ownerId", "followupId", "petId", "petName", "nextReviewOn", "severity", "unreadMessages").sorted(),
            columnsOf("dashboard_alerts").sorted(),
        )
        assertEquals(
            listOf("ownerId", "name", "scannedAt", "scannedOn", "petId").sorted(),
            columnsOf("dashboard_attended").sorted(),
        )
    }

    // ---- Guardar y observar ----

    @Test
    fun guarda_y_observa_el_panel() = runBlocking {
        dao.upsertDashboard(dashboard("u1"))

        val stored = dao.observeDashboard("u1").first()

        assertEquals("Veterinaria Centro", stored?.businessName)
        assertEquals(1, stored?.overdue)
        assertEquals(3, stored?.unreadMessages)
    }

    @Test
    fun sin_panel_guardado_el_flow_da_nulo() = runBlocking {
        assertNull(dao.observeDashboard("u1").first())
    }

    @Test
    fun las_alertas_salen_ordenadas_y_las_sin_fecha_al_final() = runBlocking {
        dao.upsertAlerts(
            listOf(
                alert("u1", "f1", "2026-12-01"),
                alert("u1", "f2", null),
                alert("u1", "f3", "2026-11-20"),
            ),
        )

        val ids = dao.observeAlerts("u1").first().map(DashboardAlertEntity::followupId)

        assertEquals(listOf("f3", "f1", "f2"), ids)
    }

    @Test
    fun las_atendidas_salen_de_la_mas_reciente_a_la_mas_vieja() = runBlocking {
        dao.upsertAttended(
            listOf(
                attended("u1", "Rocco", "2026-11-24T18:30:00Z"),
                attended("u1", "Nube", "2026-11-26T10:00:00Z"),
            ),
        )

        val names = dao.observeAttended("u1").first().map(AttendedPetEntity::name)

        assertEquals(listOf("Nube", "Rocco"), names)
    }

    // ---- Filtro por rango de dias ----

    @Test
    fun el_rango_incluye_sus_dos_extremos() = runBlocking {
        dao.upsertAttended(
            listOf(
                attended("u1", "Lunes", "2026-11-23T10:00:00Z"),
                attended("u1", "Martes", "2026-11-24T10:00:00Z"),
                attended("u1", "Miercoles", "2026-11-25T10:00:00Z"),
            ),
        )

        val names = dao.attendedBetween("u1", "2026-11-23", "2026-11-25").map(AttendedPetEntity::name)

        assertEquals(listOf("Miercoles", "Martes", "Lunes"), names)
    }

    @Test
    fun el_rango_deja_fuera_lo_que_no_le_toca() = runBlocking {
        dao.upsertAttended(
            listOf(
                attended("u1", "Antes", "2026-11-19T10:00:00Z"),
                attended("u1", "Dentro", "2026-11-24T10:00:00Z"),
                attended("u1", "Despues", "2026-11-30T10:00:00Z"),
            ),
        )

        val names = dao.attendedBetween("u1", "2026-11-20", "2026-11-26").map(AttendedPetEntity::name)

        assertEquals(listOf("Dentro"), names)
    }

    @Test
    fun una_atendida_sin_dia_legible_no_entra_en_ningun_rango() = runBlocking {
        dao.upsertAttended(listOf(attended("u1", "Rara", "fecha-mala").copy(scannedOn = null)))

        assertTrue(dao.attendedBetween("u1", "2000-01-01", "2100-01-01").isEmpty())
    }

    @Test
    fun un_rango_vacio_devuelve_lista_vacia() = runBlocking {
        assertTrue(dao.attendedBetween("u1", "2026-01-01", "2026-01-02").isEmpty())
    }

    // ---- Reemplazo completo en una transaccion ----

    @Test
    fun replace_deja_solo_lo_ultimo_que_mando_el_servidor() = runBlocking {
        dao.replace(
            dashboard("u1"),
            listOf(alert("u1", "f1", "2026-12-01"), alert("u1", "f2", "2026-12-02")),
            listOf(attended("u1", "Nube", "2026-11-26T10:00:00Z")),
        )

        dao.replace(
            dashboard("u1", name = "Veterinaria Centro Sur"),
            listOf(alert("u1", "f3", "2026-12-05")),
            emptyList(),
        )

        assertEquals("Veterinaria Centro Sur", dao.observeDashboard("u1").first()?.businessName)
        assertEquals(listOf("f3"), dao.observeAlerts("u1").first().map(DashboardAlertEntity::followupId))
        assertTrue("lo atendido viejo no debe quedarse pegado", dao.observeAttended("u1").first().isEmpty())
    }

    // ---- Aislamiento por cuenta (T-008, C-13) ----

    @Test
    fun cada_cuenta_ve_solo_su_panel() = runBlocking {
        dao.upsertDashboard(dashboard("u1", name = "De Ana"))
        dao.upsertDashboard(dashboard("u2", name = "De Beto"))
        dao.upsertAlerts(listOf(alert("u1", "f1", "2026-12-01"), alert("u2", "f2", "2026-12-02")))

        assertEquals("De Ana", dao.observeDashboard("u1").first()?.businessName)
        assertEquals(listOf("f1"), dao.observeAlerts("u1").first().map(DashboardAlertEntity::followupId))
        assertEquals(listOf("f2"), dao.observeAlerts("u2").first().map(DashboardAlertEntity::followupId))
    }

    @Test
    fun las_atendidas_no_se_cruzan_entre_cuentas() = runBlocking {
        dao.upsertAttended(
            listOf(
                attended("u1", "De Ana", "2026-11-26T10:00:00Z"),
                attended("u2", "De Beto", "2026-11-26T11:00:00Z"),
            ),
        )

        assertEquals(listOf("De Ana"), dao.attendedBetween("u1", "2026-11-26", "2026-11-26").map { it.name })
    }

    @Test
    fun clear_borra_solo_la_cuenta_indicada() = runBlocking {
        dao.replace(dashboard("u1"), listOf(alert("u1", "f1", null)), listOf(attended("u1", "Nube", "2026-11-26T10:00:00Z")))
        dao.replace(dashboard("u2"), listOf(alert("u2", "f2", null)), listOf(attended("u2", "Rocco", "2026-11-26T10:00:00Z")))

        dao.clear("u1")

        assertNull(dao.observeDashboard("u1").first())
        assertTrue(dao.observeAlerts("u1").first().isEmpty())
        assertTrue(dao.observeAttended("u1").first().isEmpty())
        assertNotNull("la otra cuenta no se toca", dao.observeDashboard("u2").first())
        assertEquals(1, dao.observeAlerts("u2").first().size)
    }

    @Test
    fun clearOthers_deja_solo_la_cuenta_de_la_sesion() = runBlocking {
        dao.replace(dashboard("u1"), listOf(alert("u1", "f1", null)), listOf(attended("u1", "Nube", "2026-11-26T10:00:00Z")))
        dao.replace(dashboard("u2"), listOf(alert("u2", "f2", null)), listOf(attended("u2", "Rocco", "2026-11-26T10:00:00Z")))

        dao.clearOthers("u2")

        assertNull(dao.observeDashboard("u1").first())
        assertTrue(dao.observeAlerts("u1").first().isEmpty())
        assertTrue(dao.observeAttended("u1").first().isEmpty())
        assertNotNull(dao.observeDashboard("u2").first())
    }

    @Test
    fun clearAll_no_deja_nada_de_nadie() = runBlocking {
        dao.replace(dashboard("u1"), listOf(alert("u1", "f1", null)), listOf(attended("u1", "Nube", "2026-11-26T10:00:00Z")))
        dao.replace(dashboard("u2"), listOf(alert("u2", "f2", null)), emptyList())

        dao.clearAll()

        assertNull(dao.observeDashboard("u1").first())
        assertNull(dao.observeDashboard("u2").first())
        assertTrue(dao.observeAlerts("u2").first().isEmpty())
    }
}
