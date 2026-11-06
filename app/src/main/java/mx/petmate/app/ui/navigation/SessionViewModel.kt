package mx.petmate.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.repository.AuthRepository
import mx.petmate.app.domain.usecase.ObserveSessionUseCase
import javax.inject.Inject

/**
 * Observa la sesion para decidir que grafo se muestra.
 *
 * Arranca en Checking y restaura la sesion guardada antes de pintar, para que
 * no se vea el login y luego un salto al contenido (spec 002, C-07).
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val repository: AuthRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = observeSession().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SessionState.Checking,
    )

    init {
        viewModelScope.launch { repository.restoreSession() }
    }

    fun enterGuestMode() = repository.enterGuestMode()

    fun exitGuestMode() = repository.exitGuestMode()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
