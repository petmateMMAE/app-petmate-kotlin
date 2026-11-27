package mx.petmate.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mx.petmate.app.ui.theme.petMateColors

/** Etiquetas para las pruebas de interfaz. */
object FakeMapViewTags {
    const val MAP = "fake_map"

    fun marker(id: String) = "fake_map_marker_$id"
}

/**
 * Mapa de mentira para probar la pantalla de inicio (spec 010, T-006).
 *
 * No dibuja un mapa: pinta la lista de marcadores que recibió. Eso es justo lo
 * que hace falta para probar la pantalla sin red, sin teselas y sin dispositivo
 * —las teselas de OpenStreetMap no se pueden cargar en una prueba— y de paso
 * sirve a las previsualizaciones.
 *
 * Es lógica pura con una capa de Compose encima: [FakeMapController] no sabe
 * nada de Compose y se prueba en la JVM.
 */
class FakeMapController(initial: MapCamera = MapCamera.Default) : MapController {

    override var camera: MapCamera = initial
        private set

    override var markers: List<MapMarker> = emptyList()
        private set

    override var selectedMarkerId: String? = null
        private set

    /** Cada movimiento de cámara, en orden. Para afirmar sobre lo que pidió la pantalla. */
    val cameraMoves = mutableListOf<MapCamera>()

    /** Cuántas veces se reemplazaron los marcadores. */
    var markerUpdates: Int = 0
        private set

    /** Si el último movimiento se pidió animado. */
    var lastMoveAnimated: Boolean? = null
        private set

    override fun moveTo(camera: MapCamera, animate: Boolean) {
        this.camera = camera
        cameraMoves += camera
        lastMoveAnimated = animate
    }

    override fun showMarkers(markers: List<MapMarker>) {
        this.markers = markers
        markerUpdates += 1
        // Un marcador que ya no está deja de estar seleccionado: si no, la
        // pantalla mostraría la tarjeta de un negocio que el filtro quitó.
        if (markers.none { it.id == selectedMarkerId }) selectedMarkerId = null
    }

    override fun select(markerId: String?): Boolean {
        if (markerId == null) {
            selectedMarkerId = null
            return true
        }
        val marker = markers.firstOrNull { it.id == markerId } ?: return false
        if (!marker.kind.isSelectable) return false
        selectedMarkerId = markerId
        return true
    }

    override fun centerOn(markerId: String, zoom: Double): Boolean {
        val marker = markers.firstOrNull { it.id == markerId } ?: return false
        moveTo(MapCamera(marker.latitude, marker.longitude, zoom))
        return true
    }

    /** Deja el controlador como recién creado. Útil entre casos de prueba. */
    fun reset(camera: MapCamera = MapCamera.Default) {
        this.camera = camera
        markers = emptyList()
        selectedMarkerId = null
        cameraMoves.clear()
        markerUpdates = 0
        lastMoveAnimated = null
    }
}

/**
 * Pinta los marcadores del [controller] como una lista.
 *
 * Tocar uno avisa por [callbacks] igual que haría el mapa real.
 */
@Composable
fun FakeMapView(
    controller: FakeMapController,
    callbacks: MapCallbacks = MapCallbacks(),
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.petMateColors
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface2)
            .testTag(FakeMapViewTags.MAP)
            .clickable { callbacks.onMapClick() }
            .padding(8.dp),
    ) {
        controller.markers.forEach { marker ->
            Text(
                text = marker.id,
                style = MaterialTheme.typography.labelSmall,
                color = if (marker.isSelected) colors.brandText else colors.ink2,
                modifier = Modifier
                    .testTag(FakeMapViewTags.marker(marker.id))
                    .clickable(enabled = marker.kind.isSelectable) {
                        callbacks.onMarkerClick(marker.id)
                    },
            )
        }
    }
}
