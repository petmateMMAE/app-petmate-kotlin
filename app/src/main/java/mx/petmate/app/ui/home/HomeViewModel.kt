package mx.petmate.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.UserProfile
import mx.petmate.app.domain.usecase.GetProfileUseCase
import mx.petmate.app.domain.usecase.LogoutUseCase
import mx.petmate.app.ui.auth.toUiText
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isLoggingOut: Boolean = false,
)

/** Perfil propio (pm-037 del backend): datos de la cuenta, estado de la sesion y cierre de sesion. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProfile: GetProfileUseCase,
    private val logout: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Se llama al abrir Perfil. En modo demo ("Probar sin cuenta") no hay cuenta que pedir al
     * backend, asi que solo con sesion iniciada se carga el perfil.
     */
    fun start(userName: String, isGuest: Boolean) {
        _uiState.update { it.copy(userName = it.profile?.name ?: userName) }
        if (!isGuest && _uiState.value.profile == null) refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getProfile()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, profile = result.data, userName = result.data.name)
                }
                // Si la sesion ya no es valida, el repositorio la cierra y el grafo vuelve al login.
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    fun onLogoutClicked() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            logout()
            // La navegacion la decide el grafo al observar el estado de sesion:
            // esta pantalla no navega por su cuenta.
            _uiState.update { it.copy(isLoggingOut = false) }
        }
    }
}
