package mx.petmate.app.ui.map

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.util.AuthRepositoryFalso
import mx.petmate.app.util.BusinessRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.negocioCercanoFalso
import mx.petmate.app.util.usuarioFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Mapa de inicio (spec 010).
 *
 * Dos cosas se cuidan aqui por encima del resto. Una: con cuenta el mapa consulta el directorio y
 * en modo invitado usa los negocios de ejemplo **sin tocar la red**, porque un invitado no tiene
 * token y cada llamada seria un 401. Otra: un negocio sin coordenadas se descarta, porque
 * `get-nearby` no siempre las trae (pm-111) y un marcador sin posicion no se puede pintar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val businesses = BusinessRepositoryFalso()
    private val auth = AuthRepositoryFalso()

    private fun conSesion(): MapViewModel {
        auth.emitir(SessionState.SignedIn(usuarioFalso()))
        return MapViewModel(businesses, auth)
    }

    private fun conNegocio(
        id: String = "b-1",
        name: String = "Veterinaria San Jorge",
        kind: BusinessKind = BusinessKind.Veterinary,
        isOpen: Boolean = true,
        distanceMeters: Double = 450.0,
    ) = negocioCercanoFalso(
        id = id,
        name = name,
        kind = kind,
        isOpen = isOpen,
        distanceMeters = distanceMeters,
        latitude = 20.05,
        longitude = -99.34,
    )

    @Test
    fun `espera a que la sesion se resuelva antes de pedir nada`() = runTest(mainDispatcher.dispatcher) {
        // La sesion arranca en Checking: si consultara ahi, pediria sin saber si hay token.
        val vm = MapViewModel(businesses, auth)
        advanceUntilIdle()

        assertFalse(businesses.seLlamo("getNearby"))

        auth.emitir(SessionState.SignedIn(usuarioFalso()))
        businesses.cercanos = AppResult.Success(listOf(conNegocio()))
        advanceUntilIdle()

        assertTrue(businesses.seLlamo("getNearby"))
        assertEquals(1, vm.state.value.businesses.size)
    }

    @Test
    fun `sin ubicacion arranca centrado en Tula`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(emptyList())
        val vm = conSesion()
        advanceUntilIdle()

        assertEquals(MapViewModel.DEFAULT_CENTER, vm.center.value)
        assertEquals(MapViewModel.DEFAULT_CENTER, businesses.ultimasCoordenadas)
    }

    @Test
    fun `un invitado ve los negocios de ejemplo y no consulta el directorio`() =
        runTest(mainDispatcher.dispatcher) {
            auth.emitir(SessionState.Guest)
            val vm = MapViewModel(businesses, auth)
            advanceUntilIdle()

            assertFalse("Un invitado no tiene token: consultar seria un 401 seguro", businesses.seLlamo("getNearby"))
            assertTrue(vm.state.value.businesses.isNotEmpty())
            assertFalse(vm.state.value.isLoading)
        }

    @Test
    fun `un invitado tampoco consulta al dar ubicacion ni al reintentar`() =
        runTest(mainDispatcher.dispatcher) {
            auth.emitir(SessionState.Guest)
            val vm = MapViewModel(businesses, auth)
            advanceUntilIdle()

            vm.onLocation(20.9, -99.9)
            vm.retry()
            vm.onNearMeClick()
            advanceUntilIdle()

            assertFalse(businesses.seLlamo("getNearby"))
        }

    @Test
    fun `los negocios del directorio llegan al mapa ya formateados`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(listOf(conNegocio(distanceMeters = 456.0)))
        val vm = conSesion()
        advanceUntilIdle()

        val negocio = vm.state.value.businesses.single()
        assertEquals("Veterinaria San Jorge", negocio.name)
        assertEquals(BusinessType.Vet, negocio.type)
        assertEquals("450 m", negocio.distance)
        assertNotNull("La maqueta pone una foto por tipo mientras el backend no las guarda", negocio.photo)
    }

    @Test
    fun `un negocio sin coordenadas se descarta en lugar de pintarse en el centro`() =
        runTest(mainDispatcher.dispatcher) {
            // get-nearby no siempre trae lat y lng (pm-111). Pintarlo en 0,0 lo mandaria al golfo
            // de Guinea; pintarlo en el centro lo haria parecer el negocio mas cercano.
            businesses.cercanos = AppResult.Success(
                listOf(negocioCercanoFalso(id = "sin-pin"), conNegocio(id = "con-pin")),
            )
            val vm = conSesion()
            advanceUntilIdle()

            assertEquals(listOf("con-pin"), vm.state.value.businesses.map { it.id })
        }

    @Test
    fun `al llegar la ubicacion recentra y vuelve a consultar`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(emptyList())
        val vm = conSesion()
        advanceUntilIdle()

        vm.onLocation(20.11, -99.11)
        advanceUntilIdle()

        assertEquals(20.11 to -99.11, vm.center.value)
        assertEquals(20.11 to -99.11, businesses.ultimasCoordenadas)
        assertTrue(vm.state.value.isLocationAvailable)
    }

    @Test
    fun `la misma ubicacion no dispara otra consulta`() = runTest(mainDispatcher.dispatcher) {
        // El proveedor de ubicacion repite la misma lectura varias veces por segundo.
        businesses.cercanos = AppResult.Success(emptyList())
        val vm = conSesion()
        advanceUntilIdle()
        vm.onLocation(20.11, -99.11)
        advanceUntilIdle()
        val consultas = businesses.vecesQueSeLlamo("getNearby")

        vm.onLocation(20.11, -99.11)
        vm.onLocation(20.11, -99.11)
        advanceUntilIdle()

        assertEquals(consultas, businesses.vecesQueSeLlamo("getNearby"))
    }

    @Test
    fun `sin permiso de ubicacion lo dice y se queda donde esta`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(emptyList())
        val vm = conSesion()
        advanceUntilIdle()

        vm.onLocation(null, null)
        advanceUntilIdle()

        assertFalse(vm.state.value.isLocationAvailable)
        assertEquals(MapViewModel.DEFAULT_CENTER, vm.center.value)
    }

    @Test
    fun `cerca de mi consulta de nuevo sin reordenar la lista`() = runTest(mainDispatcher.dispatcher) {
        // Decision del 27/11/2026 (Q-09): el orden por distancia lo da el backend y no se repite
        // aqui con otro criterio.
        businesses.cercanos = AppResult.Success(
            listOf(conNegocio(id = "lejos", distanceMeters = 900.0), conNegocio(id = "cerca", distanceMeters = 100.0)),
        )
        val vm = conSesion()
        advanceUntilIdle()

        vm.onNearMeClick()
        advanceUntilIdle()

        assertEquals(listOf("lejos", "cerca"), vm.state.value.businesses.map { it.id })
        assertEquals(2, businesses.vecesQueSeLlamo("getNearby"))
    }

    @Test
    fun `si el directorio falla deja mensaje y boton de reintentar`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Failure(AppError.Network)
        val vm = conSesion()
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
        assertFalse("Con error no es la pantalla de vacio, es la de error (A-10)", vm.state.value.isEmpty)
    }

    @Test
    fun `reintentar despues de un fallo limpia el error`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Failure(AppError.Timeout)
        val vm = conSesion()
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        businesses.cercanos = AppResult.Success(listOf(conNegocio()))
        vm.retry()
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertEquals(1, vm.state.value.businesses.size)
    }

    @Test
    fun `sin negocios es la pantalla de vacio, no la de error`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(emptyList())
        val vm = conSesion()
        advanceUntilIdle()

        assertTrue(vm.state.value.isEmpty)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `apagar un tipo lo esconde del mapa`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(
            listOf(
                conNegocio(id = "vet", kind = BusinessKind.Veterinary),
                conNegocio(id = "tienda", name = "Croquetas", kind = BusinessKind.Store),
            ),
        )
        val vm = conSesion()
        advanceUntilIdle()

        vm.onTypeToggle(BusinessType.Vet)

        assertEquals(listOf("tienda"), vm.state.value.visibleBusinesses.map { it.id })
        assertEquals("El filtro esconde, no borra", 2, vm.state.value.businesses.size)

        vm.onTypeToggle(BusinessType.Vet)
        assertEquals(2, vm.state.value.visibleBusinesses.size)
    }

    @Test
    fun `buscar filtra por nombre sin importar mayusculas`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(
            listOf(
                conNegocio(id = "vet", name = "Veterinaria San Jorge"),
                conNegocio(id = "tienda", name = "Croquetas del centro", kind = BusinessKind.Store),
            ),
        )
        val vm = conSesion()
        advanceUntilIdle()

        vm.onQueryChange("  croQUEtas ")

        assertEquals(listOf("tienda"), vm.state.value.visibleBusinesses.map { it.id })
    }

    @Test
    fun `buscar o filtrar cierra la tarjeta del negocio abierto`() = runTest(mainDispatcher.dispatcher) {
        // Si no se cerrara, quedaria abierta la tarjeta de un negocio que el filtro ya escondio.
        businesses.cercanos = AppResult.Success(listOf(conNegocio(id = "vet")))
        val vm = conSesion()
        advanceUntilIdle()

        vm.onBusinessClick("vet")
        assertNotNull(vm.state.value.selectedBusiness)

        vm.onQueryChange("otra cosa")
        assertNull(vm.state.value.selectedBusinessId)

        vm.onBusinessClick("vet")
        vm.onTypeToggle(BusinessType.Shop)
        assertNull(vm.state.value.selectedBusinessId)
    }

    @Test
    fun `abrir un negocio cierra la ayuda`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(listOf(conNegocio(id = "vet")))
        val vm = conSesion()
        advanceUntilIdle()
        vm.onHelpClick()
        assertTrue(vm.state.value.isHelpVisible)

        vm.onBusinessClick("vet")

        assertFalse("Dos paneles encima del mapa a la vez tapan el mapa entero", vm.state.value.isHelpVisible)
        assertEquals("vet", vm.state.value.selectedBusiness?.id)
    }

    @Test
    fun `cerrar la tarjeta la quita`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(listOf(conNegocio(id = "vet")))
        val vm = conSesion()
        advanceUntilIdle()
        vm.onBusinessClick("vet")

        vm.onDismissBusiness()

        assertNull(vm.state.value.selectedBusinessId)
    }

    @Test
    fun `la ayuda se abre y se cierra con el mismo boton`() = runTest(mainDispatcher.dispatcher) {
        businesses.cercanos = AppResult.Success(emptyList())
        val vm = conSesion()
        advanceUntilIdle()

        vm.onHelpClick()
        assertTrue(vm.state.value.isHelpVisible)
        vm.onHelpClick()
        assertFalse(vm.state.value.isHelpVisible)
    }
}
