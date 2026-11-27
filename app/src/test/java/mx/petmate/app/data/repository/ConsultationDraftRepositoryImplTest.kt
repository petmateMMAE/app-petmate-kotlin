package mx.petmate.app.data.repository

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.local.dao.ConsultationDraftDao
import mx.petmate.app.data.local.entity.ConsultationDraftEntity
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.util.RecordingLogger
import mx.petmate.app.util.jsonDePrueba
import mx.petmate.app.util.testDispatcherProvider
import mx.petmate.app.util.usuarioFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Borrador de consulta guardado en el telefono (spec 014, T-004, D-05).
 *
 * El DAO se reemplaza por uno en memoria: lo que se prueba aqui es la regla de aislamiento por
 * cuenta y el mapeo sin perdida, no SQLite. La prueba que mas importa es la de dos cuentas: esa
 * regla no se negocia.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConsultationDraftRepositoryImplTest {

    /** DAO en memoria con la misma llave compuesta que la tabla real. */
    private class FakeConsultationDraftDao : ConsultationDraftDao {
        private val filas = MutableStateFlow<Map<Pair<String, String>, ConsultationDraftEntity>>(emptyMap())

        val guardadas: List<ConsultationDraftEntity> get() = filas.value.values.toList()

        override suspend fun get(ownerId: String, petId: String) = filas.value[ownerId to petId]

        override fun observe(ownerId: String, petId: String): Flow<ConsultationDraftEntity?> =
            filas.map { it[ownerId to petId] }

        override suspend fun getAll(ownerId: String): List<ConsultationDraftEntity> =
            filas.value.values.filter { it.ownerId == ownerId }.sortedByDescending { it.savedAtMillis }

        override suspend fun upsert(draft: ConsultationDraftEntity) {
            filas.value = filas.value + ((draft.ownerId to draft.petId) to draft)
        }

        override suspend fun delete(ownerId: String, petId: String) {
            filas.value = filas.value - (ownerId to petId)
        }

        override suspend fun deleteOthers(ownerId: String) {
            filas.value = filas.value.filterKeys { it.first == ownerId }
        }

        override suspend fun deleteAll() {
            filas.value = emptyMap()
        }
    }

    private val dao = FakeConsultationDraftDao()
    private val sessionStore: SessionStore = mockk()
    private val logger = RecordingLogger()
    private val dispatcher = UnconfinedTestDispatcher()
    private var reloj = 1_000L

    private fun repositorio() = ConsultationDraftRepositoryImpl(
        dao = dao,
        sessionStore = sessionStore,
        dispatchers = testDispatcherProvider(dispatcher),
        json = jsonDePrueba,
        logger = logger,
        now = { reloj },
    )

    private fun conSesion(id: String = "u-1") {
        every { sessionStore.readUser() } returns usuarioFalso(id = id)
    }

    private fun sinSesion() {
        every { sessionStore.readUser() } returns null
    }

    private val borrador = ConsultationDraft(
        petId = "p-1",
        reason = "Cojea de la pata trasera",
        diagnosis = "Posible esguince",
        weightKg = 12.4,
        medications = listOf(
            MedicationDraft("Meloxicam", "0.1 mg/kg", "cada 24 h", "5 dias", "con comida"),
            MedicationDraft("Omeprazol", "10 mg", "cada 24 h", "5 dias", null),
        ),
    )

    // ---- Guardar y leer ----

    @Test
    fun `lo que se guarda se lee igual, con todo y medicamentos`() = runTest(dispatcher) {
        conSesion()
        val repo = repositorio()

        repo.save(borrador)
        val leido = repo.get("p-1")

        assertEquals(AppResult.Success(borrador), leido)
    }

    @Test
    fun `un borrador que no existe devuelve null y no es un error`() = runTest(dispatcher) {
        conSesion()

        val leido = repositorio().get("p-9")

        assertEquals(AppResult.Success(null), leido)
    }

    @Test
    fun `guardar dos veces la misma mascota reemplaza, no duplica`() = runTest(dispatcher) {
        conSesion()
        val repo = repositorio()

        repo.save(borrador)
        repo.save(borrador.copy(diagnosis = "Esguince confirmado"))

        assertEquals(1, dao.guardadas.size)
        assertEquals(
            "Esguince confirmado",
            (repo.get("p-1") as AppResult.Success).data?.diagnosis,
        )
    }

    @Test
    fun `cada mascota conserva su propio avance`() = runTest(dispatcher) {
        // Es el motivo de que la llave sea (ownerId, petId): una veterinaria atiende varias
        // mascotas la misma manana y empezar la segunda no puede borrar la primera.
        conSesion()
        val repo = repositorio()

        repo.save(borrador)
        repo.save(borrador.copy(petId = "p-2", reason = "Vacunacion anual"))

        assertEquals("Cojea de la pata trasera", (repo.get("p-1") as AppResult.Success).data?.reason)
        assertEquals("Vacunacion anual", (repo.get("p-2") as AppResult.Success).data?.reason)
    }

    // ---- Aislamiento por cuenta: la regla que no se negocia ----

    @Test
    fun `dos cuentas en el mismo telefono no se ven el borrador`() = runTest(dispatcher) {
        conSesion("u-1")
        repositorio().save(borrador)

        conSesion("u-2")
        val desdeLaOtraCuenta = repositorio().get("p-1")

        assertEquals(AppResult.Success(null), desdeLaOtraCuenta)
        assertEquals("El de la primera cuenta sigue ahi", 1, dao.guardadas.size)
    }

    @Test
    fun `getAll solo trae los de la cuenta activa`() = runTest(dispatcher) {
        conSesion("u-1")
        val deUno = repositorio()
        deUno.save(borrador)
        reloj = 2_000L
        deUno.save(borrador.copy(petId = "p-2"))

        conSesion("u-2")
        repositorio().save(borrador.copy(petId = "p-3"))

        conSesion("u-1")
        val todos = (repositorio().getAll() as AppResult.Success).data

        assertEquals(2, todos.size)
        assertEquals("El mas reciente primero", "p-2", todos.first().petId)
    }

    // ---- Sin sesion ----

    @Test
    fun `sin sesion no se guarda nada y se devuelve Unauthorized`() = runTest(dispatcher) {
        // Un borrador sin dueno lo veria la siguiente cuenta que entrara al telefono.
        sinSesion()

        val resultado = repositorio().save(borrador)

        assertEquals(AppResult.Failure(AppError.Unauthorized), resultado)
        assertTrue(dao.guardadas.isEmpty())
    }

    @Test
    fun `sin sesion leer tampoco revela nada`() = runTest(dispatcher) {
        conSesion("u-1")
        repositorio().save(borrador)

        sinSesion()
        val leido = repositorio().get("p-1")

        assertEquals(AppResult.Failure(AppError.Unauthorized), leido)
    }

    @Test
    fun `sin sesion observar no emite`() = runTest(dispatcher) {
        sinSesion()

        val emisiones = mutableListOf<ConsultationDraft?>()
        repositorio().observe("p-1").collect { emisiones += it }

        assertTrue("Un flujo vacio es mejor que emitir el borrador de otro", emisiones.isEmpty())
    }

    // ---- Borrar ----

    @Test
    fun `borrar quita el borrador de esa mascota y deja las demas`() = runTest(dispatcher) {
        conSesion()
        val repo = repositorio()
        repo.save(borrador)
        repo.save(borrador.copy(petId = "p-2"))

        repo.delete("p-1")

        assertNull((repo.get("p-1") as AppResult.Success).data)
        assertEquals("p-2", (repo.get("p-2") as AppResult.Success).data?.petId)
    }

    @Test
    fun `borrar un borrador que ya no esta no es un error`() = runTest(dispatcher) {
        // D-05 borra al cerrar la consulta; si se cierra dos veces no debe tronar.
        conSesion()

        assertEquals(AppResult.Success(Unit), repositorio().delete("p-9"))
    }

    // ---- Observar ----

    @Test
    fun `observar emite el borrador guardado`() = runTest(dispatcher) {
        conSesion()
        val repo = repositorio()
        repo.save(borrador)

        val primero = repo.observe("p-1").first()

        assertEquals("Posible esguince", primero?.diagnosis)
    }

    // ---- Datos corruptos ----

    @Test
    fun `un JSON de medicamentos ilegible no impide abrir el borrador`() = runTest(dispatcher) {
        // Puede pasar si una version anterior guardo otro formato. Perder los medicamentos es
        // molesto; que la pantalla no abra es peor.
        conSesion()
        dao.upsert(
            ConsultationDraftEntity(
                ownerId = "u-1",
                petId = "p-1",
                reason = "Cojea",
                diagnosis = "Esguince",
                weightKg = 12.4,
                medicationsJson = "{esto no es una lista}",
                savedAtMillis = 1_000L,
            ),
        )

        val leido = (repositorio().get("p-1") as AppResult.Success).data

        assertEquals("Esguince", leido?.diagnosis)
        assertTrue("Los medicamentos se pierden, pero el borrador abre", leido?.medications?.isEmpty() == true)
    }

    // ---- P6: el registro no lleva datos clinicos ----

    @Test
    fun `el registro no trae el diagnostico ni el peso`() = runTest(dispatcher) {
        conSesion()
        dao.upsert(
            ConsultationDraftEntity(
                ownerId = "u-1",
                petId = "p-1",
                reason = "Cojea",
                diagnosis = "DIAGNOSTICO-CONFIDENCIAL",
                weightKg = 12.4,
                medicationsJson = "roto",
                savedAtMillis = 1_000L,
            ),
        )

        repositorio().get("p-1")

        assertTrue(logger.noRegistro("DIAGNOSTICO-CONFIDENCIAL"))
        assertTrue(logger.noRegistro("12.4"))
    }

    // ---- T-009: tras guardar o descartar no queda NADA clinico en el almacenamiento (D-05) ----

    private fun borradorClinico(pet: String = "p-1") = ConsultationDraftEntity(
        ownerId = "u-1",
        petId = pet,
        reason = "Cojea de la pata trasera",
        diagnosis = "DIAGNOSTICO-CONFIDENCIAL",
        weightKg = 12.4,
        medicationsJson = """[{"name":"RECETA-CONFIDENCIAL","dose":"5mg"}]""",
        savedAtMillis = 1_000L,
    )

    @Test
    fun `tras descartar no queda diagnostico, receta ni peso en el almacenamiento`() = runTest(dispatcher) {
        conSesion()
        dao.upsert(borradorClinico())

        repositorio().delete("p-1")

        // No basta con que el borrador "desaparezca" de la consulta: se revisa TODO lo que queda guardado.
        // Una fila que sobreviva con el diagnostico dentro es una historia clinica en el telefono que el
        // veterinario cree que ya cerro.
        val todo = dao.guardadas.joinToString(" ") { it.toString() }
        assertTrue("Quedo el diagnostico en el almacenamiento.", !todo.contains("DIAGNOSTICO-CONFIDENCIAL"))
        assertTrue("Quedo la receta en el almacenamiento.", !todo.contains("RECETA-CONFIDENCIAL"))
        assertTrue("Quedo el peso en el almacenamiento.", !todo.contains("12.4"))
        assertTrue("No debe quedar ninguna fila de esa mascota.", dao.guardadas.none { it.petId == "p-1" })
    }

    @Test
    fun `tras guardar con exito tampoco queda nada clinico`() = runTest(dispatcher) {
        conSesion()
        val repo = repositorio()
        repo.save(
            ConsultationDraft(
                petId = "p-1",
                reason = "Cojea",
                diagnosis = "DIAGNOSTICO-CONFIDENCIAL",
                weightKg = 12.4,
                medications = listOf(MedicationDraft("RECETA-CONFIDENCIAL", "5mg", "cada 8h")),
            ),
        )

        // El camino de exito borra igual que el de descarte: son los dos finales de la consulta.
        repo.delete("p-1")

        val todo = dao.guardadas.joinToString(" ") { it.toString() }
        assertTrue(todo.isEmpty() || !todo.contains("DIAGNOSTICO-CONFIDENCIAL"))
        assertTrue(todo.isEmpty() || !todo.contains("RECETA-CONFIDENCIAL"))
    }

    @Test
    fun `borrar un borrador no toca el de otra mascota`() = runTest(dispatcher) {
        conSesion()
        dao.upsert(borradorClinico("p-1"))
        dao.upsert(borradorClinico("p-2"))

        repositorio().delete("p-1")

        // Cerrar una consulta no puede llevarse la de la mascota de al lado, que puede estar a medias.
        assertEquals(1, dao.guardadas.size)
        assertEquals("p-2", dao.guardadas.single().petId)
    }
}
