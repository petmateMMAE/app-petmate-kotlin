package mx.petmate.app.ui.map

/**
 * Dónde está mirando el mapa (spec 010, T-006).
 *
 * [zoom] es el nivel de las teselas de OpenStreetMap, que es el estándar que
 * usa casi cualquier proveedor: 0 es el mundo entero y 19 es nivel de calle.
 * Se expresa así, y no en metros, porque todos los proveedores candidatos
 * hablan este lenguaje; aun así es un número, no un tipo de ningún SDK.
 */
data class MapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double = DEFAULT_ZOOM,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitud fuera de rango: $latitude" }
        require(longitude in -180.0..180.0) { "Longitud fuera de rango: $longitude" }
        require(zoom in MIN_ZOOM..MAX_ZOOM) { "Zoom fuera de rango: $zoom" }
    }

    companion object {
        const val MIN_ZOOM = 0.0
        const val MAX_ZOOM = 19.0
        const val DEFAULT_ZOOM = 15.0

        /**
         * Dónde se abre el mapa cuando no hay ubicación.
         *
         * Las constantes viven aquí y no en la configuración del proveedor: el
         * contrato no puede depender del adaptador, o dejaría de ser un
         * contrato. El adaptador tiene las suyas para configurar su librería.
         */
        const val DEFAULT_LATITUDE = 19.4326
        const val DEFAULT_LONGITUDE = -99.1332

        val Default = MapCamera(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    }
}

/**
 * Lo que la pantalla puede pedirle al mapa (spec 010, T-006).
 *
 * **No menciona ningún SDK a propósito.** La pantalla de inicio habla con esta
 * interfaz; quien la implementa es el adaptador del proveedor (T-007) o el
 * adaptador falso de las pruebas. Si mañana se cambia osmdroid por MapLibre
 * —el riesgo que `memoria.md` registra, porque osmdroid está archivado desde
 * noviembre de 2024— se escribe otro adaptador y la pantalla no se toca.
 */
interface MapController {

    /** Dónde está mirando el mapa ahora. */
    val camera: MapCamera

    /** Los marcadores que se están pintando. */
    val markers: List<MapMarker>

    /** Identificador del marcador seleccionado, o null. */
    val selectedMarkerId: String?

    /** Mueve la cámara. [animate] en falso salta sin animación. */
    fun moveTo(camera: MapCamera, animate: Boolean = true)

    /** Reemplaza los marcadores por [markers]. */
    fun showMarkers(markers: List<MapMarker>)

    /**
     * Selecciona un marcador, o deselecciona con null.
     *
     * Un marcador que no se puede seleccionar (la posición del usuario) o que
     * no está en el mapa se ignora: devuelve falso y no cambia nada.
     */
    fun select(markerId: String?): Boolean

    /** Centra la cámara en un marcador. Falso si ese marcador no está. */
    fun centerOn(markerId: String, zoom: Double = camera.zoom): Boolean
}

/**
 * Lo que el mapa le avisa a la pantalla (spec 010, T-006).
 *
 * Va aparte de [MapController] porque las direcciones son distintas: la
 * pantalla manda con el controlador y escucha con esto.
 */
data class MapCallbacks(
    val onMarkerClick: (String) -> Unit = {},
    val onCameraMove: (MapCamera) -> Unit = {},

    /** Un toque en el mapa, fuera de cualquier marcador: deselecciona. */
    val onMapClick: () -> Unit = {},
)

/**
 * Lo que una pantalla le puede ordenar a un mapa ya dibujado (spec 010, T-006 y T-007).
 *
 * Es lo que antes era `OsmMapHandle`, pero sin el nombre ni los tipos del
 * proveedor: las pantallas de afiliación y de «mi negocio» lo usan para mover
 * el pin, y así no importan nada de osmdroid.
 */
interface MapCommands {
    fun zoomIn()

    fun zoomOut()

    /** Punto que está al centro de la pantalla, o null si el mapa no está listo. */
    fun center(): Pair<Double, Double>?

    fun centerOn(latitude: Double, longitude: Double)
}
