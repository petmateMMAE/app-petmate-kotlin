package mx.petmate.app.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.FollowUpStatus
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.repository.VeterinaryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El cambio de estado valida antes de tocar la red (spec 015, T-005). */
class ChangeFollowUpStatusUseCaseTest {

    private val repository: VeterinaryRepository = mockk()
    private val change = ChangeFollowUpStatusUseCase(repository)

    private fun followup(status: String) = Followup(
        id = "f-1",
        consultationId = "c-1",
        petId = "p-1",
        petName = "Luna",
        status = status,
        nextReviewOn = null,
        isOverdue = false,
        notes = emptyList(),
        conversationId = null,
        createdAt = null,
        updatedAt = null,
    )

    @Test
    fun `una transicion valida llama al servidor con el texto del backend`() = runTest {
        val updated = followup("Improving")
        coEvery { repository.updateFollowup("f-1", "Improving", 7) } returns AppResult.Success(updated)

        val result = change(followup("Treating"), FollowUpStatus.Improving, nextReviewInDays = 7)

        assertEquals(FollowUpChange.Done(updated), result)
    }

    @Test
    fun `desde dado de alta no se llama al servidor`() = runTest {
        val result = change(followup("Discharged"), FollowUpStatus.Treating, nextReviewInDays = null)

        assertEquals(FollowUpChange.InvalidTransition(FollowUpStatus.Discharged, FollowUpStatus.Treating), result)
        coVerify(exactly = 0) { repository.updateFollowup(any(), any(), any()) }
    }

    @Test
    fun `un estado desconocido del backend no se cambia`() = runTest {
        val result = change(followup("Cancelled"), FollowUpStatus.Improving, nextReviewInDays = null)

        assertTrue(result is FollowUpChange.InvalidTransition)
    }

    @Test
    fun `dias de revision fuera de rango no llegan al servidor`() = runTest {
        assertEquals(FollowUpChange.InvalidReviewDays, change(followup("Treating"), FollowUpStatus.Improving, 0))
        assertEquals(FollowUpChange.InvalidReviewDays, change(followup("Treating"), FollowUpStatus.Improving, 366))
        coVerify(exactly = 0) { repository.updateFollowup(any(), any(), any()) }
    }

    @Test
    fun `al dar de alta no se manda fecha de revision`() = runTest {
        coEvery { repository.updateFollowup("f-1", "Discharged", null) } returns
            AppResult.Success(followup("Discharged"))

        change(followup("Improving"), FollowUpStatus.Discharged, nextReviewInDays = 10)

        coVerify { repository.updateFollowup("f-1", "Discharged", null) }
    }

    @Test
    fun `un error del servidor se devuelve sin inventar exito`() = runTest {
        coEvery { repository.updateFollowup(any(), any(), any()) } returns AppResult.Failure(AppError.Network)

        val result = change(followup("Treating"), FollowUpStatus.Improving, nextReviewInDays = null)

        assertEquals(FollowUpChange.Failed(AppError.Network), result)
    }
}
