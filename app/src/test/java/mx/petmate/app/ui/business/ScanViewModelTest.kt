package mx.petmate.app.ui.business

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.ScanFicha
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.repository.AuthRepository
import mx.petmate.app.domain.repository.BusinessRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository: BusinessRepository = mockk(relaxed = true)
    private val auth: AuthRepository = mockk(relaxed = true)
    private val dummyFicha: ScanFicha = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { auth.sessionState } returns flowOf(
            SessionState.SignedIn(
                user = User(id = "1", name = "Vet Clinica", email = "vet@petmate.mx", businessType = "Veterinary"),
                businessMode = true,
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `token de 43 caracteres con guiones y guiones bajos se envia tal cual`() = runTest(dispatcher) {
        val token43 = "abcd-efgh_ijkl-mnop_qrst-uvwx_yz01-2345_678"
        assertEquals(43, token43.length)
        coEvery { repository.resolveScan(token43) } returns AppResult.Success(dummyFicha)

        val vm = ScanViewModel(repository, auth)
        vm.resolve(token43)

        coVerify(exactly = 1) { repository.resolveScan(token43) }
        assertEquals(dummyFicha, vm.state.value.ficha)
    }

    @Test
    fun `codigo corto con espacios y guion se normaliza a mayusculas sin guion`() = runTest(dispatcher) {
        val raw = " k7q2-m9xa "
        val expected = "K7Q2M9XA"
        coEvery { repository.resolveScan(expected) } returns AppResult.Success(dummyFicha)

        val vm = ScanViewModel(repository, auth)
        vm.resolve(raw)

        coVerify(exactly = 1) { repository.resolveScan(expected) }
        assertEquals(dummyFicha, vm.state.value.ficha)
    }

    @Test
    fun `isVet se calcula con el tipo de negocio de la sesion`() = runTest(dispatcher) {
        coEvery { repository.resolveScan(any()) } returns AppResult.Success(dummyFicha)

        // Sesión de veterinaria
        every { auth.sessionState } returns flowOf(
            SessionState.SignedIn(
                user = User(id = "1", name = "Vet Clinica", email = "vet@petmate.mx", businessType = "Veterinary"),
                businessMode = true,
            ),
        )
        val vmVet = ScanViewModel(repository, auth)
        vmVet.resolve("K7Q2M9XA")
        assertTrue(vmVet.state.value.isVet)

        // Sesión de tienda
        every { auth.sessionState } returns flowOf(
            SessionState.SignedIn(
                user = User(id = "2", name = "Tienda Pet", email = "shop@petmate.mx", businessType = "Store"),
                businessMode = true,
            ),
        )
        val vmShop = ScanViewModel(repository, auth)
        vmShop.resolve("K7Q2M9XA")
        assertFalse(vmShop.state.value.isVet)
    }
}
