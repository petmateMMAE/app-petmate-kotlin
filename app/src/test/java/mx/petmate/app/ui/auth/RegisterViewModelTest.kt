package mx.petmate.app.ui.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.usecase.RegisterUseCase
import mx.petmate.app.domain.usecase.ValidateCredentialsUseCase
import mx.petmate.app.util.AuthRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.usuarioFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pantalla de registro (spec 002, C-03).
 *
 * El validador es el de verdad, no un doble: lo que interesa comprobar es que el error del dominio
 * acabe **en el campo correcto** de la pantalla. Con un doble habria que programar ese mapeo a
 * mano y la prueba solo repetiria lo que ya dijimos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = AuthRepositoryFalso()

    private fun viewModel() = RegisterViewModel(
        register = RegisterUseCase(repository, ValidateCredentialsUseCase()),
    )

    /** Llena el formulario con datos que el validador acepta. */
    private fun RegisterViewModel.llenarBien(
        name: String = "Ana",
        email: String = "ana@petmate.mx",
        password: String = "contrasena-segura",
        confirmacion: String = "contrasena-segura",
        privacidad: Boolean = true,
    ) {
        onNameChanged(name)
        onEmailChanged(email)
        onPasswordChanged(password)
        onPasswordConfirmationChanged(confirmacion)
        onPrivacyChanged(privacidad)
    }

    // ---- Escribir en el formulario ----

    @Test
    fun `lo que se escribe queda en el estado`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.llenarBien()

        val s = vm.uiState.value
        assertEquals("Ana", s.name)
        assertEquals("ana@petmate.mx", s.email)
        assertEquals("contrasena-segura", s.password)
        assertTrue(s.privacyAccepted)
    }

    @Test
    fun `corregir un campo borra su error`() = runTest(mainDispatcher.dispatcher) {
        // Si el error se quedara, el usuario corrige y sigue viendo el reclamo: parece que no sirve.
        val vm = viewModel()
        vm.llenarBien(email = "no-es-un-correo")
        vm.onSubmit()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.emailError)

        vm.onEmailChanged("ana@petmate.mx")

        assertNull(vm.uiState.value.emailError)
    }

    @Test
    fun `aceptar el aviso de privacidad borra su error`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.llenarBien(privacidad = false)
        vm.onSubmit()
        assertNotNull(vm.uiState.value.privacyError)

        vm.onPrivacyChanged(true)

        assertNull(vm.uiState.value.privacyError)
    }

    // ---- El aviso de privacidad ----

    @Test
    fun `sin aceptar privacidad no se crea la cuenta`() = runTest(mainDispatcher.dispatcher) {
        // C-03: es requisito de la pantalla. Y lo importante es que NO salga a la red.
        val vm = viewModel()
        vm.llenarBien(privacidad = false)

        vm.onSubmit()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.privacyError)
        assertFalse(repository.seLlamo("register"))
        assertFalse(vm.uiState.value.isSubmitting)
    }

    // ---- Validacion de campos ----

    @Test
    fun `un correo invalido se reclama en el campo del correo`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.llenarBien(email = "ana@@petmate")

        vm.onSubmit()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.emailError)
        assertNull("No es un error general: es de ese campo", vm.uiState.value.generalError)
        assertFalse(repository.seLlamo("register"))
    }

    @Test
    fun `una confirmacion que no coincide se reclama en su propio campo`() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()
            vm.llenarBien(password = "contrasena-segura", confirmacion = "otra-cosa")

            vm.onSubmit()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.passwordConfirmationError)
            assertFalse(repository.seLlamo("register"))
        }

    @Test
    fun `un nombre vacio se reclama en el campo del nombre`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.llenarBien(name = "")

        vm.onSubmit()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.nameError)
    }

    // ---- Alta correcta ----

    @Test
    fun `con datos buenos se crea la cuenta y la pantalla lo sabe`() = runTest(mainDispatcher.dispatcher) {
        repository.responderRegistroCon(AppResult.Success(usuarioFalso(name = "Ana")))
        val vm = viewModel()
        vm.llenarBien()

        vm.onSubmit()
        advanceUntilIdle()

        assertTrue(repository.seLlamo("register"))
        assertTrue(vm.uiState.value.isSignedIn)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `el correo se manda sin espacios de sobra`() = runTest(mainDispatcher.dispatcher) {
        // Un espacio al final crearia una cuenta que luego no puede iniciar sesion.
        repository.responderRegistroCon(AppResult.Success(usuarioFalso()))
        val vm = viewModel()
        vm.llenarBien(email = "  ana@petmate.mx  ")

        vm.onSubmit()
        advanceUntilIdle()

        assertEquals("ana@petmate.mx", repository.ultimoRegistro?.email)
    }

    @Test
    fun `dos toques al boton crean una sola cuenta`() = runTest(mainDispatcher.dispatcher) {
        repository.responderRegistroCon(AppResult.Success(usuarioFalso()))
        val vm = viewModel()
        vm.llenarBien()

        vm.onSubmit()
        vm.onSubmit()
        advanceUntilIdle()

        assertEquals(1, repository.vecesQueSeLlamo("register"))
    }

    @Test
    fun `mientras crea la cuenta lo dice`() = runTest(mainDispatcher.dispatcher) {
        repository.responderRegistroCon(AppResult.Success(usuarioFalso()))
        val vm = viewModel()
        vm.llenarBien()

        vm.onSubmit()

        assertTrue(vm.uiState.value.isSubmitting)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
    }

    // ---- Fallos del servidor ----

    @Test
    fun `un correo ya registrado se ensena como error general`() = runTest(mainDispatcher.dispatcher) {
        // El servidor devuelve 409 sin decir en que campo: no se puede inventar que es del correo.
        repository.responderRegistroCon(AppResult.Failure(AppError.Server(409)))
        val vm = viewModel()
        vm.llenarBien()

        vm.onSubmit()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.isSignedIn)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `sin red se avisa y la cuenta no queda creada`() = runTest(mainDispatcher.dispatcher) {
        repository.responderRegistroCon(AppResult.Failure(AppError.Network))
        val vm = viewModel()
        vm.llenarBien()

        vm.onSubmit()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.isSignedIn)
    }

    @Test
    fun `reintentar limpia los errores anteriores`() = runTest(mainDispatcher.dispatcher) {
        repository.responderRegistroCon(AppResult.Failure(AppError.Network))
        val vm = viewModel()
        vm.llenarBien()
        vm.onSubmit()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.generalError)

        repository.responderRegistroCon(AppResult.Success(usuarioFalso()))
        vm.onSubmit()
        advanceUntilIdle()

        assertNull(vm.uiState.value.generalError)
        assertTrue(vm.uiState.value.isSignedIn)
    }
}
