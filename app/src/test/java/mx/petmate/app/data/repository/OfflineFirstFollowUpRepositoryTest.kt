package mx.petmate.app.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.local.FollowUpDao
import mx.petmate.app.data.local.FollowUpEntity
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.FollowupNote
import mx.petmate.app.domain.repository.VeterinaryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Seguimiento legible sin conexion y separado por sesion (spec 015, T-007 y T-008, R-10). */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstFollowUpRepositoryTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }
    private val remote: VeterinaryRepository = mockk()
    private val dao = InMemoryFollowUpDao()
    private var user: String? = "ana"
    private val repository = OfflineFirstFollowUpRepository(remote, dao, Json, { user }, dispatchers)

    private val followup = Followup(
        id = "f-1",
        consultationId = "c-1",
        petId = "p-1",
        petName = "Luna",
        status = "Improving",
        nextReviewOn = "2026-11-20",
        isOverdue = false,
        notes = listOf(FollowupNote("2026-11-17T10:00:00Z", "Come mejor")),
        conversationId = null,
        createdAt = null,
        updatedAt = null,
    )

    @Test
    fun `lo leido con red se puede leer despues sin red, con sus notas`() = runTest(dispatcher) {
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
        repository.getFollowupByConsultation("c-1")

        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Failure(AppError.Network)

        assertEquals(AppResult.Success(followup), repository.getFollowupByConsultation("c-1"))
    }

    @Test
    fun `sin red y sin nada guardado se devuelve el error`() = runTest(dispatcher) {
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Failure(AppError.Network)

        assertEquals(AppResult.Failure(AppError.Network), repository.getFollowupByConsultation("c-1"))
    }

    @Test
    fun `un cambio de estado confirmado queda guardado`() = runTest(dispatcher) {
        val discharged = followup.copy(status = "Discharged")
        coEvery { remote.updateFollowup("f-1", "Discharged", null) } returns AppResult.Success(discharged)
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Failure(AppError.Network)

        repository.updateFollowup("f-1", "Discharged", null)

        assertEquals(AppResult.Success(discharged), repository.getFollowupByConsultation("c-1"))
    }

    @Test
    fun `otra sesion no ve el seguimiento guardado`() = runTest(dispatcher) {
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
        repository.getFollowupByConsultation("c-1")

        user = "beto"
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Failure(AppError.Network)

        assertEquals(AppResult.Failure(AppError.Network), repository.getFollowupByConsultation("c-1"))
    }

    // ---- Lectura observable (spec 015, T-007 "el DAO expone Flow" y T-008) ----

    @Test
    fun `la lectura observable refleja lo que el servidor confirmo, sin volver a preguntar`() =
        runTest(dispatcher) {
            // Esto es lo que hace que la veterinaria vea su cambio aplicado: el repositorio escribe
            // en Room y el flujo reemite. Sin esto habria que recargar la pantalla a mano.
            coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
            repository.getFollowupByConsultation("c-1")
            assertEquals("Improving", repository.observeFollowup("c-1").first()?.status)

            val alta = followup.copy(status = "Discharged")
            coEvery { remote.updateFollowup("f-1", "Discharged", null) } returns AppResult.Success(alta)
            repository.updateFollowup("f-1", "Discharged", null)

            assertEquals("Discharged", repository.observeFollowup("c-1").first()?.status)
        }

    @Test
    fun `la lectura observable trae las notas, no solo el estado`() = runTest(dispatcher) {
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
        repository.getFollowupByConsultation("c-1")

        assertEquals(listOf("Come mejor"), repository.observeFollowup("c-1").first()?.notes?.map { it.text })
    }

    @Test
    fun `sin nada guardado la lectura observable emite null, no se queda callada`() =
        runTest(dispatcher) {
            // La pantalla necesita una emision para poder mostrar su estado vacio. Un flujo que no
            // emite deja el indicador de carga girando para siempre.
            assertNull(repository.observeFollowup("c-1").first())
        }

    @Test
    fun `sin sesion la lectura observable no devuelve datos de nadie`() = runTest(dispatcher) {
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
        repository.getFollowupByConsultation("c-1")

        user = null

        assertNull(repository.observeFollowup("c-1").first())
        assertEquals(emptyList<Followup>(), repository.observeFollowups().first())
    }

    @Test
    fun `otra sesion tampoco ve el seguimiento en la lectura observable`() = runTest(dispatcher) {
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
        repository.getFollowupByConsultation("c-1")

        user = "beto"

        assertNull(
            "El filtro por ownerId vale igual para el flujo que para el get",
            repository.observeFollowup("c-1").first(),
        )
    }

    @Test
    fun `la bandeja sale ordenada por fecha de revision y los sin fecha al final`() =
        runTest(dispatcher) {
            // El orden lo decide el DAO para que no se repita en cada pantalla: es el orden en que
            // hay que atender los seguimientos.
            guarda(followup.copy(consultationId = "c-lejos", id = "f-2", nextReviewOn = "2026-12-10"))
            guarda(followup.copy(consultationId = "c-sin", id = "f-3", nextReviewOn = null))
            guarda(followup.copy(consultationId = "c-cerca", id = "f-4", nextReviewOn = "2026-11-28"))

            val ids = repository.observeFollowups().first().map { it.consultationId }

            assertEquals(listOf("c-cerca", "c-lejos", "c-sin"), ids)
        }

    // ---- D-05: el cambio de estado exige conexion y NO se encola ----

    @Test
    fun `un cambio de estado sin red falla y no deja nada pendiente`() = runTest(dispatcher) {
        // D-05: el cambio de estado **no** entra al outbox de 005. Si se encolara, la veterinaria
        // daria de alta a una mascota, se iria sin red, y el alta se aplicaria sola horas despues
        // sin que nadie la confirme.
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
        repository.getFollowupByConsultation("c-1")
        coEvery { remote.updateFollowup("f-1", "Discharged", null) } returns AppResult.Failure(AppError.Network)

        val resultado = repository.updateFollowup("f-1", "Discharged", null)

        assertEquals(AppResult.Failure(AppError.Network), resultado)
        assertEquals(
            "Un cambio que no llego al servidor no puede verse como aplicado",
            "Improving",
            repository.observeFollowup("c-1").first()?.status,
        )
    }

    @Test
    fun `un cambio rechazado por el backend no pisa lo guardado`() = runTest(dispatcher) {
        // 409: el backend no acepta ese cambio de estado. Lo local sigue siendo lo ultimo cierto.
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
        repository.getFollowupByConsultation("c-1")
        coEvery { remote.updateFollowup("f-1", "Discharged", null) } returns
            AppResult.Failure(AppError.Server(409))

        repository.updateFollowup("f-1", "Discharged", null)

        assertEquals("Improving", repository.observeFollowup("c-1").first()?.status)
    }

    @Test
    fun `una nota que no llego al servidor no aparece en la bitacora local`() = runTest(dispatcher) {
        coEvery { remote.getFollowupByConsultation("c-1") } returns AppResult.Success(followup)
        repository.getFollowupByConsultation("c-1")
        coEvery { remote.addFollowupNote("f-1", "Sigue igual") } returns AppResult.Failure(AppError.Timeout)

        repository.addFollowupNote("f-1", "Sigue igual")

        assertEquals(
            "Una nota sin confirmar no es parte de la bitacora clinica",
            listOf("Come mejor"),
            repository.observeFollowup("c-1").first()?.notes?.map { it.text },
        )
    }

    @Test
    fun `este repositorio no conoce el outbox de sincronizacion`() {
        // Prueba estructural, no de comportamiento: lee el archivo real. Si alguien le inyecta el
        // outbox o lo importa, esta prueba falla sola el dia que pase, sin que nadie se acuerde de
        // D-05. Es mas fuerte que comprobar el comportamiento de hoy.
        val fuente = File("src/main/java/mx/petmate/app/data/repository/OfflineFirstFollowUpRepository.kt")
        assertTrue("No se encontro el archivo del repositorio; si se movio, actualiza esta prueba", fuente.exists())

        val texto = fuente.readText()
        listOf("SyncOutbox", "SyncScheduler", "outbox", "enqueue").forEach { marca ->
            assertFalse(
                "D-05: el cambio de estado del seguimiento exige conexion y no se encola. " +
                    "El repositorio no debe mencionar '$marca'.",
                texto.contains(marca),
            )
        }
    }

    /** Guarda un seguimiento en el almacen local pasando por el repositorio, como en la app. */
    private suspend fun guarda(followup: Followup) {
        coEvery { remote.getFollowupByConsultation(followup.consultationId) } returns AppResult.Success(followup)
        repository.getFollowupByConsultation(followup.consultationId)
    }
}

/**
 * Doble de [FollowUpDao] que **reemite**, igual que Room.
 *
 * Esto es lo que hace honesta la prueba de la lectura observable: si el doble solo devolviera un
 * valor y cerrara el flujo, la prueba pasaria sin comprobar lo unico que importa, que es que la
 * pantalla se entera de un cambio sin volver a preguntar.
 */
private class InMemoryFollowUpDao : FollowUpDao {
    private val rows = MutableStateFlow(emptyMap<Pair<String, String>, FollowUpEntity>())

    override suspend fun get(ownerId: String, consultationId: String) = rows.value[ownerId to consultationId]

    override fun observe(ownerId: String, consultationId: String): Flow<FollowUpEntity?> =
        rows.map { it[ownerId to consultationId] }

    override fun observeAll(ownerId: String): Flow<List<FollowUpEntity>> =
        rows.map { filas ->
            filas.values
                .filter { it.ownerId == ownerId }
                .sortedWith(compareBy({ it.nextReviewOn == null }, { it.nextReviewOn }))
        }

    override suspend fun upsert(followup: FollowUpEntity) {
        rows.value = rows.value + ((followup.ownerId to followup.consultationId) to followup)
    }
}
