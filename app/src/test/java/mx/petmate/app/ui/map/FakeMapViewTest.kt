package mx.petmate.app.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contrato del mapa y adaptador falso (spec 010, T-006). */
class FakeMapViewTest {

    private val controller = FakeMapController()

    private fun business(id: String, type: BusinessType, lat: Double = 19.43, lon: Double = -99.13) =
        MapBusiness(
            id = id,
            name = "Negocio $id",
            type = type,
            latitude = lat,
            longitude = lon,
            distance = "600 m",
            isOpen = true,
            scheduleTime = "18:00",
            phone = "5512345678",
            hasWhatsApp = false,
        )

    private val negocios = listOf(
        business("b1", BusinessType.Vet, 19.43, -99.13),
        business("b2", BusinessType.Shop, 19.44, -99.14),
        business("b3", BusinessType.Spa, 19.45, -99.15),
    )

    // ---- Marcadores ----

    @Test
    fun `cada tipo de negocio tiene su clase de marcador`() {
        assertEquals(MarkerKind.Vet, BusinessType.Vet.toMarkerKind())
        assertEquals(MarkerKind.Shop, BusinessType.Shop.toMarkerKind())
        assertEquals(MarkerKind.Spa, BusinessType.Spa.toMarkerKind())
    }

    @Test
    fun `un negocio se convierte en marcador conservando sus coordenadas`() {
        val marker = negocios.first().toMarker()

        assertEquals("b1", marker.id)
        assertEquals(19.43, marker.latitude, 0.00001)
        assertEquals(-99.13, marker.longitude, 0.00001)
        assertEquals(MarkerKind.Vet, marker.kind)
        assertFalse(marker.isSelected)
    }

    @Test
    fun `al convertir la lista solo el seleccionado queda marcado`() {
        val markers = negocios.toMarkers(selectedId = "b2")

        assertEquals(listOf(false, true, false), markers.map(MapMarker::isSelected))
    }

    @Test
    fun `sin seleccion ninguno queda marcado`() {
        assertTrue(negocios.toMarkers().none(MapMarker::isSelected))
    }

    @Test
    fun `el marcador del usuario no se puede seleccionar`() {
        val user = MapMarker.userLocation(19.43, -99.13)

        assertEquals(MapMarker.USER_LOCATION_ID, user.id)
        assertFalse(user.kind.isSelectable)
        assertTrue(MarkerKind.Vet.isSelectable)
    }

    // ---- Camara ----

    @Test
    fun `la camara por defecto es el centro configurado`() {
        assertEquals(MapCamera.DEFAULT_LATITUDE, MapCamera.Default.latitude, 0.00001)
        assertEquals(MapCamera.DEFAULT_LONGITUDE, MapCamera.Default.longitude, 0.00001)
        assertEquals(MapCamera.DEFAULT_ZOOM, MapCamera.Default.zoom, 0.00001)
    }

    @Test
    fun `el contrato no depende de la configuracion del proveedor`() {
        // Si MapCamera volviera a leer OsmMapConfig, el contrato dependeria del
        // adaptador y dejaria de ser un contrato.
        assertEquals(19.4326, MapCamera.DEFAULT_LATITUDE, 0.00001)
        assertEquals(-99.1332, MapCamera.DEFAULT_LONGITUDE, 0.00001)
    }

    // ---- Estado del adaptador (T-007) ----

    @Test
    fun `el estado del adaptador deriva los marcadores de los negocios`() {
        val state = MapSurfaceState(
            businesses = negocios,
            selectedBusinessId = "b2",
            showUserLocation = true,
        )

        assertEquals(listOf("b1", "b2", "b3"), state.markers.map(MapMarker::id))
        assertEquals(listOf(false, true, false), state.markers.map(MapMarker::isSelected))
    }

    @Test
    fun `sin negocios el estado no tiene marcadores`() {
        val state = MapSurfaceState(emptyList(), null, showUserLocation = false)

        assertTrue(state.markers.isEmpty())
    }

    @Test
    fun `el estado del adaptador usa la camara por defecto si no se le da otra`() {
        val state = MapSurfaceState(negocios, null, showUserLocation = false)

        assertEquals(MapCamera.Default, state.camera)
    }

    @Test
    fun `una coordenada o un zoom imposible no se puede construir`() {
        assertThrows(IllegalArgumentException::class.java) { MapCamera(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { MapCamera(0.0, 181.0) }
        assertThrows(IllegalArgumentException::class.java) { MapCamera(0.0, 0.0, zoom = 25.0) }
        assertThrows(IllegalArgumentException::class.java) { MapCamera(0.0, 0.0, zoom = -1.0) }
    }

    @Test
    fun `mover la camara queda registrado en orden`() {
        controller.moveTo(MapCamera(19.0, -99.0))
        controller.moveTo(MapCamera(20.0, -98.0), animate = false)

        assertEquals(2, controller.cameraMoves.size)
        assertEquals(20.0, controller.camera.latitude, 0.00001)
        assertEquals(false, controller.lastMoveAnimated)
    }

    // ---- Marcadores en el controlador ----

    @Test
    fun `mostrar marcadores los reemplaza y no los acumula`() {
        controller.showMarkers(negocios.toMarkers())
        controller.showMarkers(negocios.take(1).toMarkers())

        assertEquals(listOf("b1"), controller.markers.map(MapMarker::id))
        assertEquals(2, controller.markerUpdates)
    }

    @Test
    fun `sin marcadores el mapa queda vacio y no truena`() {
        controller.showMarkers(emptyList())

        assertTrue(controller.markers.isEmpty())
        assertNull(controller.selectedMarkerId)
    }

    // ---- Seleccion ----

    @Test
    fun `seleccionar un marcador que existe funciona`() {
        controller.showMarkers(negocios.toMarkers())

        assertTrue(controller.select("b2"))
        assertEquals("b2", controller.selectedMarkerId)
    }

    @Test
    fun `seleccionar un marcador que no esta no cambia nada`() {
        controller.showMarkers(negocios.toMarkers())
        controller.select("b1")

        assertFalse(controller.select("fantasma"))
        assertEquals("b1", controller.selectedMarkerId)
    }

    @Test
    fun `el marcador del usuario no se selecciona ni estando en el mapa`() {
        controller.showMarkers(negocios.toMarkers() + MapMarker.userLocation(19.43, -99.13))

        assertFalse(controller.select(MapMarker.USER_LOCATION_ID))
        assertNull(controller.selectedMarkerId)
    }

    @Test
    fun `nulo deselecciona`() {
        controller.showMarkers(negocios.toMarkers())
        controller.select("b1")

        assertTrue(controller.select(null))
        assertNull(controller.selectedMarkerId)
    }

    @Test
    fun `si el seleccionado desaparece de la lista deja de estar seleccionado`() {
        controller.showMarkers(negocios.toMarkers())
        controller.select("b3")

        // El filtro del mapa quita las estéticas.
        controller.showMarkers(negocios.filter { it.type != BusinessType.Spa }.toMarkers())

        assertNull("no debe quedar seleccionado un negocio que ya no se ve", controller.selectedMarkerId)
    }

    @Test
    fun `si el seleccionado sigue en la lista se conserva`() {
        controller.showMarkers(negocios.toMarkers())
        controller.select("b1")

        controller.showMarkers(negocios.toMarkers(selectedId = "b1"))

        assertEquals("b1", controller.selectedMarkerId)
    }

    // ---- Centrar ----

    @Test
    fun `centrar en un marcador mueve la camara a sus coordenadas`() {
        controller.showMarkers(negocios.toMarkers())

        assertTrue(controller.centerOn("b3"))
        assertEquals(19.45, controller.camera.latitude, 0.00001)
        assertEquals(-99.15, controller.camera.longitude, 0.00001)
    }

    @Test
    fun `centrar conserva el zoom salvo que se pida otro`() {
        controller.showMarkers(negocios.toMarkers())
        controller.moveTo(MapCamera(19.0, -99.0, zoom = 12.0))

        controller.centerOn("b1")
        assertEquals(12.0, controller.camera.zoom, 0.00001)

        controller.centerOn("b1", zoom = 18.0)
        assertEquals(18.0, controller.camera.zoom, 0.00001)
    }

    @Test
    fun `centrar en un marcador que no esta no mueve la camara`() {
        controller.showMarkers(negocios.toMarkers())
        val before = controller.camera

        assertFalse(controller.centerOn("fantasma"))
        assertEquals(before, controller.camera)
    }

    // ---- Reinicio ----

    @Test
    fun `reset deja el controlador como nuevo`() {
        controller.showMarkers(negocios.toMarkers())
        controller.select("b1")
        controller.moveTo(MapCamera(19.0, -99.0))

        controller.reset()

        assertTrue(controller.markers.isEmpty())
        assertNull(controller.selectedMarkerId)
        assertTrue(controller.cameraMoves.isEmpty())
        assertEquals(0, controller.markerUpdates)
        assertEquals(MapCamera.Default, controller.camera)
    }

    // ---- Los avisos van en la otra direccion ----

    @Test
    fun `los callbacks traen por defecto algo que no hace nada`() {
        val callbacks = MapCallbacks()

        // No deben tronar: la pantalla puede ignorar los avisos que no le importan.
        callbacks.onMarkerClick("b1")
        callbacks.onCameraMove(MapCamera.Default)
        callbacks.onMapClick()
    }

    @Test
    fun `los callbacks avisan lo que paso`() {
        var clicked: String? = null
        var moved: MapCamera? = null
        var tapped = false
        val callbacks = MapCallbacks(
            onMarkerClick = { clicked = it },
            onCameraMove = { moved = it },
            onMapClick = { tapped = true },
        )

        callbacks.onMarkerClick("b2")
        callbacks.onCameraMove(MapCamera(19.0, -99.0))
        callbacks.onMapClick()

        assertEquals("b2", clicked)
        assertEquals(19.0, moved?.latitude ?: 0.0, 0.00001)
        assertTrue(tapped)
    }
}
