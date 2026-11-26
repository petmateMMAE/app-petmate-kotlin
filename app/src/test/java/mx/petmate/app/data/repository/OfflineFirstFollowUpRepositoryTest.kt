package mx.petmate.app.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Test

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
}

private class InMemoryFollowUpDao : FollowUpDao {
    private val rows = mutableMapOf<Pair<String, String>, FollowUpEntity>()

    override suspend fun get(ownerId: String, consultationId: String) = rows[ownerId to consultationId]

    override suspend fun upsert(followup: FollowUpEntity) {
        rows[followup.ownerId to followup.consultationId] = followup
    }
}
