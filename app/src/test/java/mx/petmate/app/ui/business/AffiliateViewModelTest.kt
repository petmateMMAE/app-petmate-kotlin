package mx.petmate.app.ui.business

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.R
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.util.AuthRepositoryFalso
import mx.petmate.app.util.BusinessRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.negocioPropioFalso
import mx.petmate.app.util.servicioFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Alta de un negocio: los tres pasos y el envio (spec 011).
 *
 * Lo que se cuida aqui no es que el formulario "funcione", sino que **no se pierda nada** por el
 * camino: que un paso no avance con datos malos, que solo se manden los servicios marcados, y que
 * un segundo toque en "Terminar" no cree el negocio dos veces.
 *
 * El dispatcher es [StandardTestDispatcher] a proposito: con el no confinado la corrutina de
 * `finish` termina antes de que la prueba pueda mirar el estado de "guardando", que es justo lo
 * que hay que comprobar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AffiliateViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = BusinessRepositoryFalso()
    private val auth = AuthRepositoryFalso()

    private fun viewModel() = AffiliateViewModel(repository, auth)

    /** Deja el paso 1 con datos que el dominio acepta: veterinaria, asi que lleva cedula. */
    private fun AffiliateViewModel.llenaDatosBuenos() {
        onKind(BusinessKind.Veterinary)
        onForm { form ->
            form.copy(
                name = "Veterinaria San Jorge",
                phone = "7711234567",
                address = "Calle 1 num 2, Pachuca",
                licenseNumber = "CED-123",
            )
        }
    }

    @Test
    fun `elegir el tipo trae los servicios sugeridos de ese tipo`() {
        val vm = viewModel()

        vm.onKind(BusinessKind.Grooming)

        assertEquals(BusinessKind.Grooming, vm.state.value.form.kind)
        assertEquals(listOf("Baño", "Corte de pelo", "Corte de uñas"), vm.state.value.services.map { it.name })
        assertTrue("El primero viene marcado para que el paso 2 no arranque vacio", vm.state.value.services.first().selected)
    }

    @Test
    fun `cambiar de tipo cambia la lista sugerida`() {
        val vm = viewModel()

        vm.onKind(BusinessKind.Veterinary)
        vm.onKind(BusinessKind.Store)

        assertEquals(listOf("Alimento", "Accesorios", "Juguetes"), vm.state.value.services.map { it.name })
    }

    @Test
    fun `el paso 1 no avanza con datos malos y dice que campo`() {
        val vm = viewModel()
        vm.onKind(BusinessKind.Veterinary)
        vm.onForm { it.copy(name = "V", phone = "123", address = "x") }

        vm.next()

        assertEquals("No puede pasar al paso 2 con el formulario mal", 1, vm.state.value.step)
        val errores = vm.state.value.form.errors
        assertTrue(errores.containsKey("name"))
        assertTrue(errores.containsKey("phone"))
        assertTrue(errores.containsKey("address"))
        assertEquals(R.string.affiliation_error_phone, errores["phone"])
    }

    @Test
    fun `una veterinaria sin cedula no pasa del paso 1`() {
        // Regla del backend (pm-107): la cedula solo es obligatoria para veterinarias.
        val vm = viewModel()
        vm.llenaDatosBuenos()
        vm.onForm { it.copy(licenseNumber = "") }

        vm.next()

        assertEquals(1, vm.state.value.step)
        assertEquals(R.string.affiliation_error_license, vm.state.value.form.errors["license"])
    }

    @Test
    fun `una tienda sin cedula si pasa del paso 1`() {
        val vm = viewModel()
        vm.onKind(BusinessKind.Store)
        vm.onForm { it.copy(name = "Croquetas del centro", phone = "7711234567", address = "Calle 1 num 2, Pachuca") }

        vm.next()

        assertEquals(2, vm.state.value.step)
    }

    @Test
    fun `escribir en el formulario borra los errores de antes`() {
        val vm = viewModel()
        vm.onKind(BusinessKind.Veterinary)
        vm.next()
        assertTrue(vm.state.value.form.errors.isNotEmpty())

        vm.onForm { it.copy(name = "Veterinaria San Jorge") }

        assertTrue("Un error viejo no puede quedarse pintado mientras el usuario corrige", vm.state.value.form.errors.isEmpty())
    }

    @Test
    fun `el paso 2 no avanza si no quedo ningun servicio marcado`() {
        val vm = viewModel()
        vm.llenaDatosBuenos()
        vm.next()
        // Se desmarca el unico que venia marcado.
        vm.toggleService(0)

        vm.next()

        assertEquals(2, vm.state.value.step)
        assertEquals(R.string.affiliation_error_services, vm.state.value.servicesError)
    }

    @Test
    fun `marcar un servicio limpia el aviso del paso 2`() {
        val vm = viewModel()
        vm.llenaDatosBuenos()
        vm.next()
        vm.toggleService(0)
        vm.next()
        assertNotNull(vm.state.value.servicesError)

        vm.toggleService(1)

        assertNull(vm.state.value.servicesError)
    }

    @Test
    fun `un servicio agregado a mano entra marcado y limpia los campos`() {
        val vm = viewModel()
        vm.onKind(BusinessKind.Store)
        vm.onNewService(name = "  Guarderia  ", price = "500")

        vm.addService()

        val agregado = vm.state.value.services.last()
        assertEquals("Guarderia", agregado.name)
        assertEquals("500", agregado.price)
        assertTrue(agregado.selected)
        assertEquals("", vm.state.value.newServiceName)
        assertEquals("", vm.state.value.newServicePrice)
    }

    @Test
    fun `no agrega un servicio sin nombre`() {
        val vm = viewModel()
        vm.onKind(BusinessKind.Store)
        val antes = vm.state.value.services.size

        vm.onNewService(name = "   ")
        vm.addService()

        assertEquals(antes, vm.state.value.services.size)
    }

    @Test
    fun `con datos y un servicio marcado llega al paso del mapa`() {
        val vm = viewModel()
        vm.llenaDatosBuenos()

        vm.next()
        assertEquals(2, vm.state.value.step)
        vm.next()

        assertEquals(3, vm.state.value.step)
        assertNull(vm.state.value.servicesError)
    }

    @Test
    fun `en el ultimo paso next ya no avanza`() {
        // El paso 3 se cierra con "Terminar", no con "Siguiente": si avanzara, quedaria en un paso
        // que no existe y la pantalla no sabria que pintar.
        val vm = viewModel()
        vm.llenaDatosBuenos()
        vm.next()
        vm.next()

        vm.next()

        assertEquals(3, vm.state.value.step)
    }

    @Test
    fun `regresar en el primer paso avisa que ya no hay a donde`() {
        // La pantalla usa el false para dejar que el boton de atras cierre la afiliacion.
        val vm = viewModel()

        assertFalse(vm.back())
        assertEquals(1, vm.state.value.step)
    }

    @Test
    fun `regresar desde el paso 2 vuelve al 1 sin perder lo escrito`() {
        val vm = viewModel()
        vm.llenaDatosBuenos()
        vm.next()

        assertTrue(vm.back())
        assertEquals(1, vm.state.value.step)
        assertEquals("Veterinaria San Jorge", vm.state.value.form.name)
    }

    @Test
    fun `la ubicacion solo se mueve si llegan las dos coordenadas`() {
        val vm = viewModel()
        val centro = vm.state.value.latitude to vm.state.value.longitude

        vm.onLocation(20.5, null)
        assertEquals("Media coordenada es peor que ninguna", centro, vm.state.value.latitude to vm.state.value.longitude)

        vm.onLocation(20.5, -98.9)
        assertEquals(20.5 to -98.9, vm.state.value.latitude to vm.state.value.longitude)
    }

    @Test
    fun `al terminar manda el negocio con el pin del mapa`() = runTest(mainDispatcher.dispatcher) {
        repository.altaDeNegocio = AppResult.Success(negocioPropioFalso())
        repository.altaDeServicio = AppResult.Success(servicioFalso())
        val vm = viewModel()
        vm.llenaDatosBuenos()

        vm.finish(20.11, -98.74)
        advanceUntilIdle()

        val alta = repository.ultimaAlta
        assertNotNull(alta)
        assertEquals("Veterinaria San Jorge", alta!!.draft.name)
        assertEquals(20.11, alta.latitude, 0.0)
        assertEquals(-98.74, alta.longitude, 0.0)
        assertTrue(vm.state.value.done)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun `solo se dan de alta los servicios marcados`() = runTest(mainDispatcher.dispatcher) {
        repository.altaDeNegocio = AppResult.Success(negocioPropioFalso())
        repository.altaDeServicio = AppResult.Success(servicioFalso())
        val vm = viewModel()
        vm.llenaDatosBuenos()
        // Sugeridos de veterinaria: consulta (marcada), vacunacion y desparasitacion (sin marcar).
        vm.toggleService(1)

        vm.finish(20.0, -98.0)
        advanceUntilIdle()

        assertEquals(
            "Un sugerido sin marcar no es parte del negocio",
            listOf("Consulta general", "Vacunación"),
            repository.serviciosCreados.map { it.name },
        )
        assertEquals(350.0, repository.serviciosCreados.first().price!!, 0.0)
    }

    @Test
    fun `al terminar renueva la sesion porque el tipo de negocio viaja en ella`() =
        runTest(mainDispatcher.dispatcher) {
            // pm-107: la sesion no sabe que la cuenta ya es afiliada hasta renovarla, y de eso
            // depende que la app abra el modo negocio.
            repository.altaDeNegocio = AppResult.Success(negocioPropioFalso())
            repository.altaDeServicio = AppResult.Success(servicioFalso())
            val vm = viewModel()
            vm.llenaDatosBuenos()

            vm.finish(20.0, -98.0)
            advanceUntilIdle()

            assertTrue(auth.seLlamo("refreshSession"))
        }

    @Test
    fun `si el alta falla lo dice y no marca el alta como hecha`() = runTest(mainDispatcher.dispatcher) {
        repository.altaDeNegocio = AppResult.Failure(AppError.Server(409))
        val vm = viewModel()
        vm.llenaDatosBuenos()

        vm.finish(20.0, -98.0)
        advanceUntilIdle()

        assertFalse(vm.state.value.done)
        assertFalse(vm.state.value.saving)
        assertEquals("Tu cuenta ya tiene un negocio afiliado.", vm.state.value.error)
        assertTrue("Sin negocio no se crean sus servicios", repository.serviciosCreados.isEmpty())
    }

    @Test
    fun `dos toques en terminar crean un solo negocio`() = runTest(mainDispatcher.dispatcher) {
        repository.altaDeNegocio = AppResult.Success(negocioPropioFalso())
        repository.altaDeServicio = AppResult.Success(servicioFalso())
        val vm = viewModel()
        vm.llenaDatosBuenos()

        vm.finish(20.0, -98.0)
        vm.finish(20.0, -98.0)
        advanceUntilIdle()

        assertEquals(1, repository.vecesQueSeLlamo("createBusiness"))
    }

    @Test
    fun `entrar al modo negocio lo pide a la sesion`() {
        val vm = viewModel()

        vm.enterBusinessMode()

        assertTrue(auth.seLlamo("setBusinessMode"))
        assertTrue(auth.isBusinessMode())
    }

    @Test
    fun `tocar un dia del horario lo quita y lo vuelve a poner`() {
        val vm = viewModel()
        assertTrue("monday" in vm.state.value.form.days)

        vm.toggleDay("monday")
        assertFalse("monday" in vm.state.value.form.days)

        vm.toggleDay("monday")
        assertTrue("monday" in vm.state.value.form.days)
    }

    @Test
    fun `el precio que se escribe se queda en ese servicio y no en los otros`() {
        val vm = viewModel()
        vm.onKind(BusinessKind.Veterinary)

        vm.onServicePrice(1, "199")

        assertEquals("199", vm.state.value.services[1].price)
        assertEquals("350", vm.state.value.services[0].price)
    }
}
