package mx.petmate.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.petmate.app.R
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.NearbyBusiness
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.repository.AuthRepository
import mx.petmate.app.domain.repository.BusinessRepository
import java.util.Locale
import javax.inject.Inject

/**
 * Mapa de inicio (spec 010). Con cuenta usa `businesses/get-nearby` del backend; en la demo, los negocios de ejemplo.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val businesses: BusinessRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState(isLoading = true))
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private val _center = MutableStateFlow(DEFAULT_CENTER)

    /** Centro del mapa: la ubicacion del usuario o, sin permiso, Tula de Allende. */
    val center: StateFlow<Pair<Double, Double>> = _center.asStateFlow()

    private var isGuest = false

    init {
        viewModelScope.launch {
            val session = auth.sessionState.first { it != SessionState.Checking }
            isGuest = session == SessionState.Guest
            if (isGuest) {
                _center.value = MapSampleData.state.businesses.first().let { it.latitude to it.longitude }
                _state.value = MapSampleData.state
            } else {
                load()
            }
        }
    }

    /** La pantalla avisa si hay ubicacion (permiso concedido y lectura disponible). */
    fun onLocation(latitude: Double?, longitude: Double?) {
        if (isGuest) return
        val available = latitude != null && longitude != null
        _state.update { it.copy(isLocationAvailable = available) }
        if (available) {
            val new = latitude!! to longitude!!
            if (new != _center.value) {
                _center.value = new
                load()
            }
        }
    }

    fun retry() = load()

    fun onQueryChange(query: String) = _state.update { it.copy(query = query, selectedBusinessId = null) }

    fun onTypeToggle(type: BusinessType) = _state.update {
        val types = if (type in it.visibleTypes) it.visibleTypes - type else it.visibleTypes + type
        it.copy(visibleTypes = types, selectedBusinessId = null)
    }

    fun onBusinessClick(id: String) = _state.update { it.copy(selectedBusinessId = id, isHelpVisible = false) }

    fun onDismissBusiness() = _state.update { it.copy(selectedBusinessId = null) }

    fun onHelpClick() = _state.update { it.copy(isHelpVisible = !it.isHelpVisible) }

    private fun load() {
        if (isGuest) return
        val (lat, lng) = _center.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = businesses.getNearby(lat, lng)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, businesses = result.data.mapNotNull { b -> b.toMapBusiness() })
                }
                is AppResult.Failure -> _state.update { it.copy(isLoading = false, error = UiText.Resource(R.string.map_error)) }
            }
        }
    }

    companion object {
        /** UTTT, Tula de Allende: donde estan los negocios de prueba. */
        val DEFAULT_CENTER = 20.0539 to -99.3417
    }
}

internal fun BusinessKind.toMapType(): BusinessType = when (this) {
    BusinessKind.Veterinary -> BusinessType.Vet
    BusinessKind.Store -> BusinessType.Shop
    BusinessKind.Grooming -> BusinessType.Spa
}

/** Foto por defecto de la maqueta mientras el backend no guarda fotos (pm-111). */
internal fun BusinessType.defaultPhoto(): Int = when (this) {
    BusinessType.Vet -> R.drawable.sample_business_vet
    BusinessType.Shop -> R.drawable.sample_business_shop
    BusinessType.Spa -> R.drawable.sample_business_spa
}

internal fun formatDistance(meters: Double): String =
    if (meters < 1000) "${(meters / 10).toInt() * 10} m" else String.format(Locale.US, "%.1f km", meters / 1000)

private fun NearbyBusiness.toMapBusiness(): MapBusiness? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    val type = kind.toMapType()
    val time = if (isOpen) todayHours.lastOrNull()?.close else todayHours.firstOrNull()?.open
    return MapBusiness(
        id = id,
        name = name,
        type = type,
        latitude = lat,
        longitude = lng,
        distance = formatDistance(distanceMeters),
        isOpen = isOpen,
        // Abierto todo el dia = sin hora; cerrado sin horario hoy tambien queda en null.
        scheduleTime = time?.takeUnless { it == "23:59" && todayHours.firstOrNull()?.open == "00:00" },
        phone = phone.orEmpty(),
        hasWhatsApp = !whatsApp.isNullOrBlank(),
        photo = type.defaultPhoto(),
        whatsApp = whatsApp,
    )
}
