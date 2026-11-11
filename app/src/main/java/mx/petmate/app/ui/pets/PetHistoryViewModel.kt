package mx.petmate.app.ui.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Consultation
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.HistoryItem
import mx.petmate.app.domain.repository.VeterinaryRepository
import javax.inject.Inject

data class PetHistoryUiState(
    val isLoading: Boolean = false,
    val items: List<HistoryItem> = emptyList(),
    val error: String? = null,
)

data class ConsultationDetailUiState(
    val isLoading: Boolean = false,
    val consultation: Consultation? = null,
    val followup: Followup? = null,
    val error: String? = null,
)

@HiltViewModel
class PetHistoryViewModel @Inject constructor(
    private val repository: VeterinaryRepository,
) : ViewModel() {

    private val _historyState = MutableStateFlow(PetHistoryUiState())
    val historyState: StateFlow<PetHistoryUiState> = _historyState.asStateFlow()

    private val _detailState = MutableStateFlow(ConsultationDetailUiState())
    val detailState: StateFlow<ConsultationDetailUiState> = _detailState.asStateFlow()

    fun loadHistory(petId: String) {
        _historyState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.getHistory(petId)) {
                is AppResult.Success -> _historyState.update {
                    it.copy(isLoading = false, items = r.data, error = null)
                }
                is AppResult.Failure -> _historyState.update {
                    it.copy(isLoading = false, error = r.error.toMessage())
                }
            }
        }
    }

    fun loadConsultation(id: String) {
        _detailState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.getConsultationById(id)) {
                is AppResult.Success -> {
                    val followup = repository.getFollowupByConsultation(id).let {
                        if (it is AppResult.Success) it.data else null
                    }
                    _detailState.update {
                        it.copy(isLoading = false, consultation = r.data, followup = followup, error = null)
                    }
                }
                is AppResult.Failure -> _detailState.update {
                    it.copy(isLoading = false, error = r.error.toMessage())
                }
            }
        }
    }

    private fun AppError.toMessage(): String = when (this) {
        AppError.Network -> "Sin conexión a internet."
        AppError.Timeout -> "El servidor tardó en responder."
        AppError.Unauthorized -> "Sesión expirada."
        is AppError.Server -> "Error del servidor ($code)."
        else -> "Ocurrió un error al cargar los datos."
    }
}
