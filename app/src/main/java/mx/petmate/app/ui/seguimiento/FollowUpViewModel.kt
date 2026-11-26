package mx.petmate.app.ui.seguimiento

import androidx.annotation.StringRes
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
import mx.petmate.app.domain.model.FollowUpStatus
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.statusKind
import mx.petmate.app.domain.repository.VeterinaryRepository
import mx.petmate.app.domain.usecase.ChangeFollowUpStatusUseCase
import mx.petmate.app.domain.usecase.ClassifyReviewDateUseCase
import mx.petmate.app.domain.usecase.FollowUpChange
import mx.petmate.app.domain.usecase.FollowUpTransition
import mx.petmate.app.domain.usecase.ReviewDateClass
import javax.inject.Inject

/** Estado de la pantalla de seguimiento (spec 015, T-009). */
data class FollowUpUiState(
    val isLoading: Boolean = true,
    val consultationId: String? = null,
    val followup: Followup? = null,
    val status: FollowUpStatus? = null,
    /** Estados a los que se puede pasar; vacio si esta cerrado. La pantalla solo ofrece estos. */
    val nextOptions: List<FollowUpStatus> = emptyList(),
    val isClosed: Boolean = false,
    val reviewClass: ReviewDateClass = ReviewDateClass.None,
    val reviewDays: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    @param:StringRes val error: Int? = null,
)

/**
 * Seguimiento de una consulta para la veterinaria que atiende (spec 015, T-009, R-01 a R-06, R-08).
 *
 * Toda la E/S va por el repositorio, que ya corre en `Dispatchers.IO` (R-13).
 */
@HiltViewModel
class FollowUpViewModel @Inject constructor(
    private val repository: VeterinaryRepository,
) : ViewModel() {

    private val changeStatus = ChangeFollowUpStatusUseCase(repository)
    private val classify = ClassifyReviewDateUseCase()

    private val _state = MutableStateFlow(FollowUpUiState())
    val state: StateFlow<FollowUpUiState> = _state.asStateFlow()

    /** Carga el seguimiento de [consultationId]. Si ya se cargo esa consulta, no repite la llamada. */
    fun load(consultationId: String) {
        if (_state.value.consultationId == consultationId && !_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, consultationId = consultationId, error = null) }
        viewModelScope.launch {
            when (val result = repository.getFollowupByConsultation(consultationId)) {
                is AppResult.Success -> show(result.data)
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = R.string.followup_error_network)
                }
            }
        }
    }

    fun onReviewDays(value: String) =
        _state.update { it.copy(reviewDays = value.filter(Char::isDigit).take(3), error = null) }

    fun onNote(value: String) = _state.update { it.copy(note = value, error = null) }

    /** Abre el seguimiento de la consulta; arranca "En tratamiento" (R-01). */
    fun open() {
        val consultationId = _state.value.consultationId ?: return
        val days = parsedDays() ?: return
        save { repository.createFollowup(consultationId, days.value) }
    }

    fun changeTo(target: FollowUpStatus) {
        val followup = _state.value.followup ?: return
        val days = parsedDays() ?: return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val change = changeStatus(followup, target, days.value)) {
                is FollowUpChange.Done -> show(change.followup, keepInputs = false)
                is FollowUpChange.InvalidTransition -> fail(R.string.followup_error_transition)
                FollowUpChange.InvalidReviewDays -> fail(R.string.followup_error_days)
                is FollowUpChange.Failed -> fail(R.string.followup_error_network)
            }
        }
    }

    fun addNote() {
        val followup = _state.value.followup ?: return
        val text = _state.value.note.trim()
        if (text.isEmpty() || text.length > MAX_NOTE) {
            return _state.update { it.copy(error = R.string.followup_error_note) }
        }
        save { repository.addFollowupNote(followup.id, text) }
    }

    private fun save(call: suspend () -> AppResult<Followup>) {
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is AppResult.Success -> show(result.data, keepInputs = false)
                is AppResult.Failure -> fail(R.string.followup_error_network)
            }
        }
    }

    private fun show(followup: Followup?, keepInputs: Boolean = true) {
        val status = followup?.statusKind
        val next = status?.let { FollowUpTransition.nextOf(it).sortedBy(FollowUpStatus::ordinal) }.orEmpty()
        _state.update {
            it.copy(
                isLoading = false,
                isSaving = false,
                followup = followup,
                status = status,
                nextOptions = next,
                isClosed = status != null && FollowUpTransition.isClosed(status),
                reviewClass = classify(followup?.nextReviewOn),
                reviewDays = if (keepInputs) it.reviewDays else "",
                note = if (keepInputs) it.note else "",
                error = null,
            )
        }
    }

    private fun fail(@StringRes message: Int) = _state.update { it.copy(isSaving = false, error = message) }

    /** Dias escritos: vacio es "sin fecha"; fuera de rango marca error y devuelve null para no guardar. */
    private fun parsedDays(): ParsedDays? {
        val text = _state.value.reviewDays
        if (text.isBlank()) return ParsedDays(null)
        val days = text.toIntOrNull()
        if (days == null || days !in ChangeFollowUpStatusUseCase.REVIEW_DAYS) {
            fail(R.string.followup_error_days)
            return null
        }
        return ParsedDays(days)
    }

    private data class ParsedDays(val value: Int?)

    companion object {
        const val MAX_NOTE = 500
    }
}
