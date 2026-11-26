package mx.petmate.app.ui.seguimiento

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mx.petmate.app.R
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.FollowUpStatus
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.FollowupNote
import mx.petmate.app.domain.repository.VeterinaryRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pantalla de seguimiento: abrir, cambiar estado, notas y errores (spec 015, T-009, C-01 a C-07). */
@OptIn(ExperimentalCoroutinesApi::class)
class FollowUpViewModelTest {

    private val repository: VeterinaryRepository = mockk(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: FollowUpViewModel

    private fun followup(status: String, notes: List<FollowupNote> = emptyList()) = Followup(
        id = "f-1",
        consultationId = "c-1",
        petId = "p-1",
        petName = "Luna",
        status = status,
        nextReviewOn = null,
        isOverdue = false,
        notes = notes,
        conversationId = null,
        createdAt = null,
        updatedAt = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = FollowUpViewModel(repository)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `sin seguimiento ofrece abrirlo`() {
        coEvery { repository.getFollowupByConsultation("c-1") } returns AppResult.Success(null)

        viewModel.load("c-1")

        assertNull(viewModel.state.value.followup)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `abrir manda los dias escritos y muestra el seguimiento en tratamiento`() {
        coEvery { repository.getFollowupByConsultation("c-1") } returns AppResult.Success(null)
        coEvery { repository.createFollowup("c-1", 7) } returns AppResult.Success(followup("Treating"))
        viewModel.load("c-1")

        viewModel.onReviewDays("7")
        viewModel.open()

        assertEquals(FollowUpStatus.Treating, viewModel.state.value.status)
        assertEquals(listOf(FollowUpStatus.Improving, FollowUpStatus.Discharged), viewModel.state.value.nextOptions)
    }

    @Test
    fun `dias fuera de rango no llegan al servidor`() {
        coEvery { repository.getFollowupByConsultation("c-1") } returns AppResult.Success(null)
        viewModel.load("c-1")

        viewModel.onReviewDays("400")
        viewModel.open()

        assertEquals(R.string.followup_error_days, viewModel.state.value.error)
        coVerify(exactly = 0) { repository.createFollowup(any(), any()) }
    }

    @Test
    fun `dar de alta cierra el seguimiento y ya no ofrece cambios`() {
        coEvery { repository.getFollowupByConsultation("c-1") } returns AppResult.Success(followup("Improving"))
        coEvery { repository.updateFollowup("f-1", "Discharged", null) } returns AppResult.Success(followup("Discharged"))
        viewModel.load("c-1")

        viewModel.changeTo(FollowUpStatus.Discharged)

        assertTrue(viewModel.state.value.isClosed)
        assertTrue(viewModel.state.value.nextOptions.isEmpty())
    }

    @Test
    fun `una nota vacia no se manda`() {
        coEvery { repository.getFollowupByConsultation("c-1") } returns AppResult.Success(followup("Treating"))
        viewModel.load("c-1")

        viewModel.onNote("   ")
        viewModel.addNote()

        assertEquals(R.string.followup_error_note, viewModel.state.value.error)
        coVerify(exactly = 0) { repository.addFollowupNote(any(), any()) }
    }

    @Test
    fun `una nota se agrega y el campo se limpia`() {
        val withNote = followup("Treating", listOf(FollowupNote("2026-11-17T10:00:00Z", "Come mejor")))
        coEvery { repository.getFollowupByConsultation("c-1") } returns AppResult.Success(followup("Treating"))
        coEvery { repository.addFollowupNote("f-1", "Come mejor") } returns AppResult.Success(withNote)
        viewModel.load("c-1")

        viewModel.onNote("  Come mejor ")
        viewModel.addNote()

        assertEquals("", viewModel.state.value.note)
        assertEquals(1, viewModel.state.value.followup?.notes?.size)
    }

    @Test
    fun `un error de red se muestra sin perder lo escrito`() {
        coEvery { repository.getFollowupByConsultation("c-1") } returns AppResult.Success(followup("Treating"))
        coEvery { repository.addFollowupNote(any(), any()) } returns AppResult.Failure(AppError.Network)
        viewModel.load("c-1")

        viewModel.onNote("Come mejor")
        viewModel.addNote()

        assertEquals(R.string.followup_error_network, viewModel.state.value.error)
        assertEquals("Come mejor", viewModel.state.value.note)
    }
}
