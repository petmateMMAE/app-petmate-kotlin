package mx.petmate.app.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.petmate.app.R
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.LegalLinks
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.repository.SettingsRepository
import mx.petmate.app.domain.usecase.LogoutUseCase
import javax.inject.Inject

/**
 * Ajustes del usuario (spec 017, T-006 y T-008).
 *
 * Los interruptores se leen del repositorio, que lee del disco: se ven con o sin conexion (T-005). Cerrar
 * sesion reusa el caso de uso de 002 en vez de reimplementarlo (T-008).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val logout: LogoutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.observeNotificationPreferences().collect { preferences ->
                _state.update {
                    it.copy(isLoading = false, toggles = SettingsUiState.togglesOf(preferences))
                }
            }
        }
    }

    /**
     * Enciende o apaga una categoria. Seguridad no se puede apagar (A-09): se ignora en vez de fallar, porque
     * la pantalla ya muestra ese interruptor deshabilitado y llegar aqui solo pasaria por una carrera.
     */
    fun toggle(category: NotificationCategory, on: Boolean) {
        if (!on && !mx.petmate.app.domain.push.NotificationPreferences.canBeDisabled(category)) return
        viewModelScope.launch {
            // No se escribe el estado a mano: el Flow del repositorio emite el valor guardado y la pantalla
            // se actualiza desde ahi. Asi lo que se ve es siempre lo que quedo en disco.
            settings.setNotification(category, on)
        }
    }

    fun logOut() {
        if (_state.value.isLoggingOut) return
        _state.update { it.copy(isLoggingOut = true, error = null) }
        viewModelScope.launch {
            when (logout()) {
                is AppResult.Success -> _state.update { it.copy(isLoggingOut = false, loggedOut = true) }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoggingOut = false, error = R.string.settings_error_logout)
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    /** Enlaces legales; sin URLs reales (Q-02) la pantalla no ofrece el enlace. */
    val legalLinks: LegalLinks get() = _state.value.legalLinks
}
