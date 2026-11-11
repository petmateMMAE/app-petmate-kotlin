package mx.petmate.app.ui.map

import androidx.annotation.DrawableRes
import mx.petmate.app.core.ui.UiText

/** Tipos de negocio afiliado que filtra el mapa (spec 010, A-02 y A-03). */
enum class BusinessType { Vet, Shop, Spa }

/**
 * Negocio tal como lo pinta el mapa.
 *
 * [latitude] y [longitude] son coordenadas reales; el mapa las traduce a
 * pixeles en cada movimiento. Antes eran una posicion fija sobre el mapa
 * ilustrado de la maqueta.
 */
data class MapBusiness(
    val id: String,
    val name: String,
    val type: BusinessType,
    val latitude: Double,
    val longitude: Double,
    /** Distancia ya formateada, por ejemplo "600 m" o "1.1 km". */
    val distance: String,
    val isOpen: Boolean,
    /** Hora de cierre si esta abierto, o de apertura si esta cerrado; null = 24 horas. */
    val scheduleTime: String?,
    val phone: String,
    val hasWhatsApp: Boolean,
    /** Foto provisional desde recursos; con el backend sera una URL. */
    @DrawableRes val photo: Int? = null,
    /** Numero de WhatsApp si es distinto del telefono. */
    val whatsApp: String? = null,
) {
    /**
     * Posicion sobre el mapa ilustrado de la maqueta (800 × 1000 dp).
     *
     * Solo la usan las previsualizaciones, que no pueden cargar el mapa real.
     * El mapa de verdad proyecta [latitude] y [longitude] a pixeles.
     */
    val previewX: Float get() = (PREVIEW_CENTER_X + (longitude - PREVIEW_LON) / PREVIEW_SCALE).toFloat()
    val previewY: Float get() = (PREVIEW_CENTER_Y - (latitude - PREVIEW_LAT) / PREVIEW_SCALE).toFloat()

    private companion object {
        const val PREVIEW_CENTER_X = 400.0
        const val PREVIEW_CENTER_Y = 450.0
        const val PREVIEW_LAT = 19.4326
        const val PREVIEW_LON = -99.1332
        const val PREVIEW_SCALE = 0.00018
    }
}

/**
 * Estado de la pantalla de inicio con el mapa (spec 010).
 *
 * - Cargando: [isLoading] con el mapa de fondo y los marcadores ocultos.
 * - Vacio: sin error, sin carga y sin negocios visibles (A-10).
 * - Error: [error] distinto de null, con opcion de reintentar (A-10).
 */
data class MapUiState(
    val isLoading: Boolean = false,
    val businesses: List<MapBusiness> = emptyList(),
    val visibleTypes: Set<BusinessType> = BusinessType.entries.toSet(),
    val query: String = "",
    val selectedBusinessId: String? = null,
    /** false cuando el permiso de ubicacion se nego o no se ha dado (A-07). */
    val isLocationAvailable: Boolean = true,
    val isHelpVisible: Boolean = false,
    val remindersCount: Int = 0,
    val error: UiText? = null,
) {
    val visibleBusinesses: List<MapBusiness> =
        businesses.filter { business ->
            business.type in visibleTypes &&
                (query.isBlank() || business.name.contains(query.trim(), ignoreCase = true))
        }

    val selectedBusiness: MapBusiness? =
        visibleBusinesses.firstOrNull { it.id == selectedBusinessId }

    val isEmpty: Boolean = !isLoading && error == null && visibleBusinesses.isEmpty()
}
