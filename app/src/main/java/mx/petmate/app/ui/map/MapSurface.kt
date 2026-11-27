package mx.petmate.app.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Todo lo que un mapa necesita para dibujarse (spec 010, T-006 y T-007).
 *
 * No menciona ningún SDK: lleva los negocios del mapa, que son un modelo
 * nuestro, y la cámara y los avisos del contrato. Cada adaptador traduce esto
 * a lo que su librería entienda.
 */
data class MapSurfaceState(
    val businesses: List<MapBusiness>,
    val selectedBusinessId: String?,
    val showUserLocation: Boolean,
    val camera: MapCamera = MapCamera.Default,
    val callbacks: MapCallbacks = MapCallbacks(),
    val commands: MapCommands? = null,
) {
    /** Los negocios como marcadores del contrato. */
    val markers: List<MapMarker> get() = businesses.toMarkers(selectedBusinessId)
}

/**
 * Quién dibuja el mapa.
 *
 * La pantalla de inicio recibe uno de estos y no sabe cuál es: con eso deja de
 * nombrar al proveedor. Hoy hay tres —osmdroid, el mapa ilustrado de la maqueta
 * y el falso de las pruebas— y cambiar de proveedor es escribir un cuarto.
 */
typealias MapSurface = @Composable (MapSurfaceState, Modifier) -> Unit

/**
 * Adaptador de osmdroid (spec 010, T-007).
 *
 * Es el **único** lugar, junto con `OsmMap` y `OsmMapConfig`, donde el proveedor
 * aparece. Se eligió osmdroid porque no pide llave de API ni tarjeta y cumple
 * el costo cero del proyecto (decisión del 04/11/2026, que cerró la Q-01 de
 * esta spec).
 */
@Composable
fun OsmMapSurface(state: MapSurfaceState, modifier: Modifier = Modifier) {
    OsmMap(
        businesses = state.businesses,
        selectedBusinessId = state.selectedBusinessId,
        showUserLocation = state.showUserLocation,
        onBusinessClick = state.callbacks.onMarkerClick,
        modifier = modifier,
        centerLatitude = state.camera.latitude,
        centerLongitude = state.camera.longitude,
        handle = state.commands as? OsmMapHandle,
    )
}

/**
 * Adaptador del mapa ilustrado de la maqueta.
 *
 * Lo usan las previsualizaciones, que no pueden cargar teselas de red ni tienen
 * ciclo de vida de Activity.
 */
@Composable
fun IllustratedMapSurface(state: MapSurfaceState, modifier: Modifier = Modifier) {
    IllustratedMap(
        businesses = state.businesses,
        selectedBusinessId = state.selectedBusinessId,
        showUserLocation = state.showUserLocation,
        onBusinessClick = state.callbacks.onMarkerClick,
    )
}

/**
 * Adaptador falso: pinta la lista de marcadores (spec 010, T-006).
 *
 * Con esto la pantalla de inicio se puede probar sin red, sin teselas y sin
 * proveedor.
 */
@Composable
fun FakeMapSurface(state: MapSurfaceState, modifier: Modifier = Modifier) {
    val controller = remember { FakeMapController(state.camera) }
    controller.showMarkers(state.markers)
    state.selectedBusinessId?.let(controller::select)
    FakeMapView(controller = controller, callbacks = state.callbacks, modifier = modifier)
}

/**
 * Comandos del mapa para la pantalla que lo muestra.
 *
 * Devuelve el tipo del contrato, no el del proveedor: las pantallas de
 * afiliación y de «mi negocio» mueven el pin sin importar osmdroid.
 */
@Composable
fun rememberMapCommands(): MapCommands = remember { OsmMapHandle() }
