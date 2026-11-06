package mx.petmate.app.data.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAuthRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val sessionStore: SessionStore = mockk(relaxed = true)

    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }

    private fun repository() = FakeAuthRepository(sessionStore, dispatchers)

    @Test
    fun `la cuenta de ejemplo permite iniciar sesion`() = runTest(dispatcher) {
        val result = repository().login(
            LoginCredentials(email = "demo@petmate.mx", password = "PetMateDemo2026"),
        )
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `una contrasena incorrecta devuelve no autorizado`() = runTest(dispatcher) {
        val result = repository().login(
            LoginCredentials(email = "demo@petmate.mx", password = "incorrecta"),
        )
        assertEquals(AppError.Unauthorized, (result as AppResult.Failure).error)
    }

    @Test
    fun `un correo inexistente devuelve el mismo error que una contrasena mala`() =
        runTest(dispatcher) {
            val repo = repository()
            val inexistente = repo.login(
                LoginCredentials(email = "nadie@petmate.mx", password = "petmate123"),
            )
            val malaClave = repo.login(
                LoginCredentials(email = "demo@petmate.mx", password = "incorrecta"),
            )
            // No se debe poder deducir que correos existen (spec 002, C-06).
            assertEquals(
                (inexistente as AppResult.Failure).error,
                (malaClave as AppResult.Failure).error,
            )
        }

    @Test
    fun `registrar una cuenta nueva deja la sesion iniciada`() = runTest(dispatcher) {
        val repo = repository()
        val result = repo.register(nuevaCuenta())
        assertTrue(result is AppResult.Success)
        assertTrue(repo.sessionState.first() is SessionState.SignedIn)
    }

    @Test
    fun `registrar un correo repetido falla`() = runTest(dispatcher) {
        val repo = repository()
        repo.register(nuevaCuenta())
        val result = repo.register(nuevaCuenta())
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `el registro guarda la sesion cifrada`() = runTest(dispatcher) {
        val userSlot = slot<User>()
        repository().register(nuevaCuenta())
        verify { sessionStore.save(capture(userSlot), any(), any()) }
        assertEquals("nuevo@petmate.mx", userSlot.captured.email)
    }

    @Test
    fun `cerrar sesion borra el almacenamiento y deja el estado fuera`() = runTest(dispatcher) {
        val repo = repository()
        repo.register(nuevaCuenta())
        repo.logout()
        verify { sessionStore.clear() }
        assertEquals(SessionState.SignedOut, repo.sessionState.first())
    }

    @Test
    fun `restaurar sin sesion guardada deja el estado fuera`() = runTest(dispatcher) {
        every { sessionStore.readUser() } returns null
        val repo = repository()
        repo.restoreSession()
        assertEquals(SessionState.SignedOut, repo.sessionState.first())
    }

    @Test
    fun `restaurar con sesion guardada la recupera`() = runTest(dispatcher) {
        val guardado = User(id = "1", name = "Adrian", email = "demo@petmate.mx")
        every { sessionStore.readUser() } returns guardado
        val repo = repository()
        repo.restoreSession()
        assertEquals(SessionState.SignedIn(guardado), repo.sessionState.first())
    }

    private fun nuevaCuenta() = RegisterData(
        name = "Nuevo",
        email = "nuevo@petmate.mx",
        password = "petmate123",
        passwordConfirmation = "petmate123",
        privacyAccepted = true,
    )
}
