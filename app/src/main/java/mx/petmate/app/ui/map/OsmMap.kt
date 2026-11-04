package mx.petmate.app.ui.map

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Mapa real de OpenStreetMap con los marcadores de PetMate encima.
 *
 * El mapa se arrastra y hace zoom; los marcadores son los mismos Composables
 * de la maqueta ([BusinessPin] y [UserLocationDot]), reposicionados en cada
 * movimiento segun la proyeccion del mapa.
 *
 * Se eligio osmdroid con teselas de OpenStreetMap porque no requiere llave de
 * API, registro ni tarjeta, y cumple el requisito de costo cero del proyecto
 * (spec 010, Q-07).
 *
 * La atribucion de OpenStreetMap se pinta siempre y no se puede ocultar: es
 * obligatoria por la politica de uso de las teselas.
 */
@Composable
internal fun OsmMap(
    businesses: List<MapBusiness>,
    selectedBusinessId: String?,
    showUserLocation: Boolean,
    onBusinessClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    centerLatitude: Double = OsmMapConfig.DEFAULT_LATITUDE,
    centerLongitude: Double = OsmMapConfig.DEFAULT_LONGITUDE,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Posicion en pixeles de cada marcador. Se recalcula al mover el mapa.
    var screenPositions by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var userPosition by remember { mutableStateOf<Offset?>(null) }

    val mapView = remember {
        OsmMapConfig.initialize(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // La atribucion la pinta Compose encima, con el estilo de la app.
            setUseDataConnection(true)
            minZoomLevel = OsmMapConfig.MIN_ZOOM
            maxZoomLevel = OsmMapConfig.MAX_ZOOM
            controller.setZoom(OsmMapConfig.DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
            // El logo y el boton de zoom propios de osmdroid se ocultan: la
            // pantalla ya trae los suyos con el estilo de la maqueta.
            zoomController.setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER,
            )
        }
    }

    /** Traduce cada negocio a su posicion actual en pantalla. */
    fun recomputePositions() {
        val projection = mapView.projection
        screenPositions = businesses.associate { business ->
            val point = projection.toPixels(
                GeoPoint(business.latitude, business.longitude),
                null,
            )
            business.id to Offset(point.x.toFloat(), point.y.toFloat())
        }
        userPosition = if (showUserLocation) {
            val point = projection.toPixels(
                GeoPoint(centerLatitude, centerLongitude),
                null,
            )
            Offset(point.x.toFloat(), point.y.toFloat())
        } else {
            null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.addMapListener(
                    object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            recomputePositions()
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            recomputePositions()
                            return false
                        }
                    },
                )
                view.post { recomputePositions() }
            },
        )

        MapOverlays(
            businesses = businesses,
            positions = screenPositions,
            userPosition = userPosition,
            selectedBusinessId = selectedBusinessId,
            onBusinessClick = onBusinessClick,
        )

        OsmAttribution(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

/** Marcadores y punto del usuario, posicionados sobre el mapa. */
@Composable
private fun BoxScope.MapOverlays(
    businesses: List<MapBusiness>,
    positions: Map<String, Offset>,
    userPosition: Offset?,
    selectedBusinessId: String?,
    onBusinessClick: (String) -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    userPosition?.let { position ->
        with(density) {
            UserLocationDot(
                modifier = Modifier.offset(
                    x = position.x.toDp() - USER_DOT_RADIUS.dp,
                    y = position.y.toDp() - USER_DOT_RADIUS.dp,
                ),
            )
        }
    }

    // El elegido se pinta al final para que quede encima de los demas.
    businesses
        .sortedBy { it.id == selectedBusinessId }
        .forEach { business ->
            val position = positions[business.id] ?: return@forEach
            with(density) {
                BusinessPin(
                    business = business,
                    selected = business.id == selectedBusinessId,
                    onClick = { onBusinessClick(business.id) },
                    modifier = Modifier.offset(
                        x = position.x.toDp() - (PIN_BOX / 2).dp,
                        y = position.y.toDp() - PIN_BOX.dp,
                    ),
                )
            }
        }
}

/**
 * Atribucion de OpenStreetMap.
 *
 * Obligatoria por la politica de uso de las teselas: debe verse siempre y no
 * puede quedar detras de un boton ni fuera de la pantalla.
 */
@Composable
private fun OsmAttribution(modifier: Modifier = Modifier) {
    Text(
        text = OsmMapConfig.ATTRIBUTION,
        style = MaterialTheme.typography.labelSmall,
        color = Color.Black.copy(alpha = 0.7f),
        modifier = modifier
            .padding(4.dp)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private const val USER_DOT_RADIUS = 33
