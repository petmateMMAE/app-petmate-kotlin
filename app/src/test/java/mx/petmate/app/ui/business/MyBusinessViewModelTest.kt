package mx.petmate.app.ui.business

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.R
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.util.AuthRepositoryFalso
import mx.petmate.app.util.BusinessRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.negocioPropioFalso
import mx.petmate.app.util.servicioFalso
import mx.petmate.app.util.usuarioFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * "Mi negocio": panel, edicion de datos, ubicacion y servicios (spec 012).
 *
 * El hilo de todas estas pruebas es el mismo: el afiliado esta editando algo que **ya existe**, asi
 * que lo caro no es fallar al guardar, es guardar a medias o dejar la pantalla mostrando datos que
 * ya no son los del servidor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyBusinessViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = BusinessRepositoryFalso()
    private val auth = AuthRepositoryFalso()

    private fun viewModel() = MyBusinessViewModel(repository, auth)

    @Test
    fun `al abrir llena el formulario con el negocio en cache`() = runTest(mainDispatcher.dispatcher) {
        // El panel abre sin red: el negocio ya esta guardado de la sesion anterior.
        repository.negocioEnCache = negocioPropioFalso(name = "Veterinaria San Jorge", phone = "7711234567")

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("Veterinaria San Jorge", vm.state.value.form.name)
        assertEquals("7711234567", vm.state.value.form.phone)
        assertEquals(BusinessKind.Veterinary, vm.state.value.kind)
        assertNotNull(vm.state.value.business)
    }

    @Test
    fun `el formulario se queda solo con los dias que traen horario`() = runTest(mainDispatcher.dispatcher) {
        // Un negocio que cierra fin de semana no debe abrir el formulario con sabado marcado.
        repository.negocioEnCache = negocioPropioFalso()

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(setOf("monday", "tuesday", "wednesday"), vm.state.value.form.days)
        assertEquals("09:00", vm.state.value.form.open)
        assertEquals("18:00", vm.state.value.form.close)
    }

    @Test
    fun `sin negocio en cache el tipo sale de la sesion`() = runTest(mainDispatcher.dispatcher) {
        // Recien afiliado: el negocio aun no esta en cache, pero la sesion renovada ya trae el tipo.
        auth.emitir(SessionState.SignedIn(usuarioFalso(isAffiliate = true, businessType = "Grooming")))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(BusinessKind.Grooming, vm.state.value.kind)
        assertEquals(BusinessKind.Grooming, vm.state.value.form.kind)
    }

    @Test
    fun `los servicios llegan y apagan el indicador de carga`() = runTest(mainDispatcher.dispatcher) {
        repository.servicios = AppResult.Success(listOf(servicioFalso(name = "Consulta general"), servicioFalso(id = "s-2", name = "Vacunación")))

        val vm = viewModel()
        assertTrue(vm.state.value.loadingServices)
        advanceUntilIdle()

        assertEquals(listOf("Consulta general", "Vacunación"), vm.state.value.services.map { it.name })
        assertFalse(vm.state.value.loadingServices)
    }

    @Test
    fun `si los servicios fallan lo dice y no deja la pantalla girando`() = runTest(mainDispatcher.dispatcher) {
        repository.servicios = AppResult.Failure(AppError.Network)

        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.loadingServices)
        assertEquals("Sin conexión. Revisa tu internet e intenta de nuevo.", vm.state.value.error)
    }

    @Test
    fun `guardar con un campo mal no llama al servidor`() = runTest(mainDispatcher.dispatcher) {
        repository.negocioEnCache = negocioPropioFalso()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onForm { it.copy(phone = "123") }
        var aviso = false
        vm.save { aviso = true }
        advanceUntilIdle()

        assertFalse("No tiene sentido mandar al servidor datos que ya sabemos que no acepta", repository.seLlamo("updateBusiness"))
        assertFalse(aviso)
        assertEquals(R.string.affiliation_error_phone, vm.state.value.form.errors["phone"])
    }

    @Test
    fun `al editar no exige el tipo, la direccion ni la cedula`() = runTest(mainDispatcher.dispatcher) {
        // pm-107: el backend no deja cambiarlos, asi que el formulario de "Mi negocio" ni los pide.
        repository.negocioEnCache = negocioPropioFalso()
        repository.edicionDeNegocio = AppResult.Success(negocioPropioFalso(name = "Veterinaria Jorge"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onForm { it.copy(kind = null, address = "", licenseNumber = "") }
        vm.save {}
        advanceUntilIdle()

        assertTrue(repository.seLlamo("updateBusiness"))
        assertTrue(vm.state.value.form.errors.isEmpty())
    }

    @Test
    fun `al guardar bien avisa a la pantalla y deja el negocio nuevo`() = runTest(mainDispatcher.dispatcher) {
        repository.negocioEnCache = negocioPropioFalso(name = "Veterinaria San Jorge")
        repository.edicionDeNegocio = AppResult.Success(negocioPropioFalso(name = "Veterinaria Jorge"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onForm { it.copy(name = "Veterinaria Jorge") }
        var guardado = 0
        vm.save { guardado++ }
        advanceUntilIdle()

        assertEquals(1, guardado)
        assertEquals("Veterinaria Jorge", vm.state.value.business?.name)
        assertEquals("Cambios guardados", vm.state.value.message)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `si guardar falla no avisa que se guardo`() = runTest(mainDispatcher.dispatcher) {
        repository.negocioEnCache = negocioPropioFalso()
        repository.edicionDeNegocio = AppResult.Failure(AppError.Timeout)
        val vm = viewModel()
        advanceUntilIdle()

        var guardado = false
        vm.save { guardado = true }
        advanceUntilIdle()

        assertFalse("Avisar de un guardado que no ocurrio hace que el usuario salga creyendo que si", guardado)
        assertEquals("El servidor tardó en responder. Intenta de nuevo.", vm.state.value.error)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `mover el pin manda las coordenadas y avisa`() = runTest(mainDispatcher.dispatcher) {
        repository.negocioEnCache = negocioPropioFalso()
        repository.cambioDeUbicacion = AppResult.Success(negocioPropioFalso())
        val vm = viewModel()
        advanceUntilIdle()

        var listo = false
        vm.saveLocation(20.33, -98.44) { listo = true }
        advanceUntilIdle()

        assertEquals(20.33 to -98.44, repository.ultimaUbicacion)
        assertTrue(listo)
    }

    @Test
    fun `si mover el pin falla lo dice y no avisa`() = runTest(mainDispatcher.dispatcher) {
        repository.negocioEnCache = negocioPropioFalso()
        repository.cambioDeUbicacion = AppResult.Failure(AppError.Server(500))
        val vm = viewModel()
        advanceUntilIdle()

        var listo = false
        vm.saveLocation(20.33, -98.44) { listo = true }
        advanceUntilIdle()

        assertFalse(listo)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `dar de baja el negocio sale del modo negocio y renueva la sesion`() = runTest(mainDispatcher.dispatcher) {
        // Si la app se quedara en modo negocio sin negocio, el afiliado veria un panel vacio sin
        // forma de salir.
        repository.negocioEnCache = negocioPropioFalso()
        repository.bajaDeNegocio = AppResult.Success(Unit)
        val vm = viewModel()
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertTrue(vm.state.value.deleted)
        assertTrue(auth.seLlamo("setBusinessMode"))
        assertTrue(auth.seLlamo("refreshSession"))
        assertFalse(auth.isBusinessMode())
    }

    @Test
    fun `si la baja falla no marca el negocio como borrado`() = runTest(mainDispatcher.dispatcher) {
        repository.negocioEnCache = negocioPropioFalso()
        repository.bajaDeNegocio = AppResult.Failure(AppError.Unauthorized)
        val vm = viewModel()
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertFalse(vm.state.value.deleted)
        assertEquals("Tu sesión expiró. Vuelve a iniciar sesión.", vm.state.value.error)
    }

    @Test
    fun `un servicio sin nombre no se manda`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.saveService(id = null, name = "   ", description = "", price = "300") {}
        advanceUntilIdle()

        assertFalse(repository.seLlamo("createService"))
        assertEquals("Escribe el nombre del servicio.", vm.state.value.error)
    }

    @Test
    fun `un precio que no es numero no se manda`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.saveService(id = null, name = "Consulta", description = "", price = "caro") {}
        advanceUntilIdle()

        assertFalse(repository.seLlamo("createService"))
        assertEquals("El precio debe ser un número, por ejemplo 300.", vm.state.value.error)
    }

    @Test
    fun `un servicio sin precio si se manda, con precio nulo`() = runTest(mainDispatcher.dispatcher) {
        // Una tienda puede publicar un servicio sin precio: "sin precio" no es un error.
        repository.altaDeServicio = AppResult.Success(servicioFalso())
        val vm = viewModel()
        advanceUntilIdle()

        vm.saveService(id = null, name = "Alimento", description = "", price = "") {}
        advanceUntilIdle()

        assertEquals(1, repository.serviciosCreados.size)
        assertNull(repository.serviciosCreados.first().price)
    }

    @Test
    fun `editar un servicio usa el id y no crea otro`() = runTest(mainDispatcher.dispatcher) {
        repository.edicionDeServicio = AppResult.Success(servicioFalso())
        val vm = viewModel()
        advanceUntilIdle()

        var listo = false
        vm.saveService(id = "s-1", name = "Consulta", description = "con cita", price = "400") { listo = true }
        advanceUntilIdle()

        assertTrue(repository.serviciosCreados.isEmpty())
        assertEquals(listOf("s-1"), repository.serviciosEditados.map { it.id })
        assertEquals(400.0, repository.serviciosEditados.first().price!!, 0.0)
        assertTrue(listo)
    }

    @Test
    fun `al guardar un servicio vuelve a pedir la lista`() = runTest(mainDispatcher.dispatcher) {
        // Si no recargara, la pantalla seguiria mostrando la lista de antes del cambio.
        repository.altaDeServicio = AppResult.Success(servicioFalso())
        val vm = viewModel()
        advanceUntilIdle()
        val antes = repository.vecesQueSeLlamo("getServices")

        vm.saveService(id = null, name = "Consulta", description = "", price = "300") {}
        advanceUntilIdle()

        assertEquals(antes + 1, repository.vecesQueSeLlamo("getServices"))
    }

    @Test
    fun `borrar un servicio recarga la lista`() = runTest(mainDispatcher.dispatcher) {
        repository.bajaDeServicio = AppResult.Success(Unit)
        val vm = viewModel()
        advanceUntilIdle()
        val antes = repository.vecesQueSeLlamo("getServices")

        vm.deleteService("s-1")
        advanceUntilIdle()

        assertEquals(listOf("s-1"), repository.serviciosBorrados)
        assertEquals(antes + 1, repository.vecesQueSeLlamo("getServices"))
    }

    @Test
    fun `si borrar un servicio falla lo dice y no recarga`() = runTest(mainDispatcher.dispatcher) {
        repository.bajaDeServicio = AppResult.Failure(AppError.Server(404))
        val vm = viewModel()
        advanceUntilIdle()
        val antes = repository.vecesQueSeLlamo("getServices")

        vm.deleteService("s-1")
        advanceUntilIdle()

        assertEquals("No lo encontramos. Puede que ya no exista.", vm.state.value.error)
        assertEquals(antes, repository.vecesQueSeLlamo("getServices"))
    }

    @Test
    fun `tocar un dia del horario lo quita y lo vuelve a poner`() = runTest(mainDispatcher.dispatcher) {
        repository.negocioEnCache = negocioPropioFalso()
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue("monday" in vm.state.value.form.days)

        vm.toggleDay("monday")
        assertFalse("monday" in vm.state.value.form.days)

        vm.toggleDay("saturday")
        assertTrue("saturday" in vm.state.value.form.days)
        assertEquals(setOf("tuesday", "wednesday", "saturday"), vm.state.value.form.days)
    }

    @Test
    fun `cerrar el aviso de error lo quita`() = runTest(mainDispatcher.dispatcher) {
        repository.servicios = AppResult.Failure(AppError.Network)
        val vm = viewModel()
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        vm.clearError()

        assertNull(vm.state.value.error)
    }

    @Test
    fun `volver al modo dueno lo pide a la sesion`() = runTest(mainDispatcher.dispatcher) {
        auth.emitir(SessionState.SignedIn(usuarioFalso(isAffiliate = true), businessMode = true))
        val vm = viewModel()
        advanceUntilIdle()

        vm.switchToOwner()

        assertTrue(auth.seLlamo("setBusinessMode"))
        assertFalse(auth.isBusinessMode())
        assertEquals(
            "La sesion tiene que dejar de estar en modo negocio, no solo el repositorio",
            SessionState.SignedIn(usuarioFalso(isAffiliate = true), businessMode = false),
            auth.sessionState.first(),
        )
    }
}
