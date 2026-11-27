package mx.petmate.app.ui.business

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.util.AuthRepositoryFalso
import mx.petmate.app.util.BusinessRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.fichaFalsa
import mx.petmate.app.util.usuarioFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Escaneo del QR y ficha de la mascota (spec 013).
 *
 * Aqui lo delicado es la lectura del codigo: el QR trae un token de 43 caracteres base64url (lleva
 * `-` y `_`) y el codigo corto son 8 caracteres que alguien teclea. Si el saneado se come los
 * guiones del token, el escaneo falla con un codigo que estaba bien, y el negocio culpa al QR.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = BusinessRepositoryFalso()
    private val auth = AuthRepositoryFalso()

    private fun viewModel() = ScanViewModel(repository, auth)

    /** Token como el que trae el QR: 43 caracteres base64url. */
    private val token = "ab-cd_ef" + "g".repeat(35)

    @Test
    fun `el codigo escrito se recorta a 64 caracteres`() {
        // Tope defensivo: lo mas largo valido son 43, asi que nada razonable se pierde.
        val vm = viewModel()

        vm.onCode("a".repeat(100))

        assertEquals(64, vm.state.value.code.length)
    }

    @Test
    fun `escribir limpia el error anterior`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.resolve("corto")
        assertNotNull(vm.state.value.error)

        vm.onCode("ABCD1234")

        assertNull(vm.state.value.error)
    }

    @Test
    fun `un codigo que no mide 8 ni 43 no llega al servidor`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.resolve("ABC12")
        advanceUntilIdle()

        assertFalse(repository.seLlamo("resolveScan"))
        assertEquals("El código tiene 8 caracteres (o escanea el QR).", vm.state.value.error)
    }

    @Test
    fun `el codigo corto se manda en mayusculas y sin signos`() = runTest(mainDispatcher.dispatcher) {
        repository.ficha = AppResult.Success(fichaFalsa())
        val vm = viewModel()

        vm.resolve("ab-cd 12*34")
        advanceUntilIdle()

        assertEquals("ABCD1234", repository.ultimoCodigo)
    }

    @Test
    fun `el token del QR se manda tal cual, con guiones y guion bajo`() = runTest(mainDispatcher.dispatcher) {
        // Es lo que rompe si alguien "limpia" el token: base64url usa - y _ como datos.
        repository.ficha = AppResult.Success(fichaFalsa())
        val vm = viewModel()

        vm.resolve(token)
        advanceUntilIdle()

        assertEquals(token, repository.ultimoCodigo)
    }

    @Test
    fun `si el QR trae una URL se queda con lo que va despues de la ultima diagonal`() =
        runTest(mainDispatcher.dispatcher) {
            repository.ficha = AppResult.Success(fichaFalsa())
            val vm = viewModel()

            vm.resolve("https://petmate.mx/s/" + token)
            advanceUntilIdle()

            assertEquals(token, repository.ultimoCodigo)
        }

    @Test
    fun `la ficha llega y el campo del codigo se vacia`() = runTest(mainDispatcher.dispatcher) {
        // Se vacia para que el siguiente escaneo no arrastre el codigo del anterior.
        repository.ficha = AppResult.Success(fichaFalsa(name = "Firulais"))
        val vm = viewModel()
        vm.onCode("ABCD1234")

        vm.resolve()
        advanceUntilIdle()

        assertEquals("Firulais", vm.state.value.ficha?.name)
        assertEquals("", vm.state.value.code)
        assertFalse(vm.state.value.resolving)
    }

    @Test
    fun `una veterinaria queda marcada como tal y una tienda no`() = runTest(mainDispatcher.dispatcher) {
        // De esto depende que se ofrezca registrar consulta, que solo puede una veterinaria.
        repository.ficha = AppResult.Success(fichaFalsa())
        auth.emitir(SessionState.SignedIn(usuarioFalso(isAffiliate = true, businessType = "Veterinary")))
        val vet = viewModel()

        vet.resolve("ABCD1234")
        advanceUntilIdle()
        assertTrue(vet.state.value.isVet)

        auth.emitir(SessionState.SignedIn(usuarioFalso(isAffiliate = true, businessType = "Store")))
        val tienda = viewModel()
        tienda.resolve("ABCD1234")
        advanceUntilIdle()
        assertFalse(tienda.state.value.isVet)
    }

    @Test
    fun `un 403 explica que el dueno no compartio, no que el codigo este mal`() =
        runTest(mainDispatcher.dispatcher) {
            // Es la diferencia entre que el negocio pida el QR otra vez o que acuse al dueno de
            // haberlo escrito mal.
            repository.ficha = AppResult.Failure(AppError.Server(403))
            val vm = viewModel()

            vm.resolve("ABCD1234")
            advanceUntilIdle()

            assertTrue(vm.state.value.error.orEmpty().contains("no ha compartido"))
            assertNull(vm.state.value.ficha)
        }

    @Test
    fun `un 404 habla del codigo`() = runTest(mainDispatcher.dispatcher) {
        repository.ficha = AppResult.Failure(AppError.Server(404))
        val vm = viewModel()

        vm.resolve("ABCD1234")
        advanceUntilIdle()

        assertTrue(vm.state.value.error.orEmpty().contains("No encontramos ese código"))
    }

    @Test
    fun `otro error cae en el mensaje general`() = runTest(mainDispatcher.dispatcher) {
        repository.ficha = AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.resolve("ABCD1234")
        advanceUntilIdle()

        assertEquals("Sin conexión. Revisa tu internet e intenta de nuevo.", vm.state.value.error)
        assertFalse(vm.state.value.resolving)
    }

    @Test
    fun `cerrar la ficha deja la pantalla como nueva`() = runTest(mainDispatcher.dispatcher) {
        repository.ficha = AppResult.Success(fichaFalsa())
        val vm = viewModel()
        vm.resolve("ABCD1234")
        advanceUntilIdle()
        assertNotNull(vm.state.value.ficha)

        vm.closeFicha()

        assertNull(vm.state.value.ficha)
        assertNull(vm.state.value.lastCode)
        assertEquals("", vm.state.value.code)
    }

    @Test
    fun `refrescar vuelve a pedir el mismo codigo`() = runTest(mainDispatcher.dispatcher) {
        // Lo usa la pantalla despues de registrar una vacuna: la ficha tiene que traer el dato nuevo.
        repository.ficha = AppResult.Success(fichaFalsa())
        val vm = viewModel()
        vm.resolve("ABCD1234")
        advanceUntilIdle()

        vm.refreshFicha()
        advanceUntilIdle()

        assertEquals(2, repository.vecesQueSeLlamo("resolveScan"))
        assertEquals("ABCD1234", repository.ultimoCodigo)
    }

    @Test
    fun `refrescar sin haber escaneado nada no hace nada`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.refreshFicha()
        advanceUntilIdle()

        assertFalse(repository.seLlamo("resolveScan"))
    }

    @Test
    fun `mientras resuelve lo dice`() = runTest(mainDispatcher.dispatcher) {
        repository.ficha = AppResult.Success(fichaFalsa())
        val vm = viewModel()

        vm.resolve("ABCD1234")

        assertTrue(vm.state.value.resolving)
        advanceUntilIdle()
        assertFalse(vm.state.value.resolving)
    }

    @Test
    fun `lo que el dueno no compartio llega en nulo, no en lista vacia`() =
        runTest(mainDispatcher.dispatcher) {
            // pm-123: "no compartido" y "no tiene registros" se ven distinto en la pantalla, y la
            // diferencia tiene que sobrevivir el viaje hasta aqui.
            repository.ficha = AppResult.Success(fichaFalsa(vaccines = null, dewormings = emptyList()))
            val vm = viewModel()

            vm.resolve("ABCD1234")
            advanceUntilIdle()

            assertNull(vm.state.value.ficha?.vaccines)
            assertEquals(emptyList<Any>(), vm.state.value.ficha?.dewormings)
        }
}
