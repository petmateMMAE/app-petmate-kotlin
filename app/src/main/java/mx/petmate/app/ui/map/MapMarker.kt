package mx.petmate.app.ui.map

/**
 * Qué representa un marcador del mapa (spec 010, T-006, A-02 y A-03).
 *
 * Es el vocabulario del mapa, no el del backend: `BusinessType` dice qué clase
 * de negocio es, y esto dice qué se pinta. Separarlos permite marcadores que no
 * son negocios, como la posición del usuario.
 */
enum class MarkerKind {
    Vet,
    Shop,
    Spa,

    /** Dónde está el usuario. No se puede seleccionar. */
    UserLocation,
    ;

    val isSelectable: Boolean get() = this != UserLocation
}

/**
 * Un marcador sobre el mapa (spec 010, T-006).
 *
 * Coordenadas reales, sin una sola referencia a ningún SDK de mapas: el
 * adaptador del proveedor (T-007) traduce esto a lo que su librería entienda.
 * Por eso aquí no hay píxeles, `GeoPoint`, `LatLng` ni `Marker`.
 */
data class MapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val kind: MarkerKind,
    val isSelected: Boolean = false,
) {
    companion object {
        /** Identificador reservado del marcador de la posición del usuario. */
        const val USER_LOCATION_ID = "user-location"

        fun userLocation(latitude: Double, longitude: Double) = MapMarker(
            id = USER_LOCATION_ID,
            latitude = latitude,
            longitude = longitude,
            kind = MarkerKind.UserLocation,
        )
    }
}

/** El tipo de negocio del mapa como clase de marcador. */
fun BusinessType.toMarkerKind(): MarkerKind = when (this) {
    BusinessType.Vet -> MarkerKind.Vet
    BusinessType.Shop -> MarkerKind.Shop
    BusinessType.Spa -> MarkerKind.Spa
}

/** Marcador de un negocio del mapa. */
fun MapBusiness.toMarker(isSelected: Boolean = false) = MapMarker(
    id = id,
    latitude = latitude,
    longitude = longitude,
    kind = type.toMarkerKind(),
    isSelected = isSelected,
)

/** Marcadores de una lista de negocios, con el seleccionado marcado. */
fun List<MapBusiness>.toMarkers(selectedId: String? = null): List<MapMarker> =
    map { it.toMarker(isSelected = it.id == selectedId) }
