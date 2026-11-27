package mx.petmate.app.ui.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.petmate.app.R
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.repository.VeterinaryRepository
import mx.petmate.app.domain.usecase.PetDates
import javax.inject.Inject

data class VeterinaryActionUiState(
    val isSubmitting: Boolean = false,
    /** Aviso general. Es [UiText] y no texto: el mensaje sale de `strings.xml` (spec 014, T-003). */
    val error: UiText? = null,
    /** Error de cada campo, por su clave. Tambien [UiText]. */
    val fieldErrors: Map<String, UiText> = emptyMap(),
    /** Id de la ultima consulta creada, para ofrecer abrir su seguimiento (spec 015, R-01). */
    val lastConsultationId: String? = null,
)

@HiltViewModel
class VeterinaryActionViewModel @Inject constructor(
    private val repository: VeterinaryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VeterinaryActionUiState())
    val state: StateFlow<VeterinaryActionUiState> = _state.asStateFlow()

    fun clearError() = _state.update { it.copy(error = null, fieldErrors = emptyMap()) }

    fun createConsultation(
        petId: String,
        reason: String,
        diagnosis: String,
        weightKgText: String,
        medications: List<MedicationDraft>,
        onSuccess: () -> Unit,
    ) {
        val errors = mutableMapOf<String, UiText>()
        val trimmedReason = reason.trim()
        val trimmedDiagnosis = diagnosis.trim()

        if (trimmedReason.isBlank()) errors["reason"] = res(R.string.veterinary_error_reason_required)
        else if (trimmedReason.length > TEXT_MAX) errors["reason"] = res(R.string.veterinary_error_too_long, TEXT_MAX)

        if (trimmedDiagnosis.isBlank()) errors["diagnosis"] = res(R.string.veterinary_error_diagnosis_required)
        else if (trimmedDiagnosis.length > TEXT_MAX) errors["diagnosis"] = res(R.string.veterinary_error_too_long, TEXT_MAX)

        val weightKg = if (weightKgText.isNotBlank()) {
            val parsed = weightKgText.replace(",", ".").toDoubleOrNull()
            if (parsed == null || parsed < WEIGHT_MIN || parsed > WEIGHT_MAX) {
                errors["weightKg"] = res(R.string.veterinary_error_weight_range, WEIGHT_MIN.toString(), WEIGHT_MAX.toString())
                null
            } else parsed
        } else null

        if (medications.size > MEDICATIONS_MAX) {
            errors["medications"] = res(R.string.veterinary_error_medications_max, MEDICATIONS_MAX)
        }

        medications.forEachIndexed { index, med ->
            if (med.name.isBlank()) errors["med_${index}_name"] = res(R.string.veterinary_error_med_name)
            if (med.dose.isBlank()) errors["med_${index}_dose"] = res(R.string.veterinary_error_med_dose)
            if (med.frequency.isBlank()) errors["med_${index}_frequency"] = res(R.string.veterinary_error_med_frequency)
            if (med.duration.isBlank()) errors["med_${index}_duration"] = res(R.string.veterinary_error_med_duration)
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = res(R.string.veterinary_error_check_fields)) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val draft = ConsultationDraft(
                petId = petId,
                reason = trimmedReason,
                diagnosis = trimmedDiagnosis,
                weightKg = weightKg,
                medications = medications,
            )
            when (val r = repository.createConsultation(draft)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, lastConsultationId = r.data.id) }
                    onSuccess()
                }
                is AppResult.Failure -> handleFailure(r.error)
            }
        }
    }

    fun createVaccine(
        petId: String,
        name: String,
        appliedOn: String,
        batch: String,
        nextDoseOn: String?,
        onSuccess: () -> Unit,
    ) {
        val errors = mutableMapOf<String, UiText>()
        val trimmedName = name.trim()
        val today = PetDates.today()

        if (trimmedName.isBlank()) errors["name"] = res(R.string.veterinary_error_vaccine_name)
        if (appliedOn.isBlank()) errors["appliedOn"] = res(R.string.veterinary_error_applied_required)
        else if (appliedOn > today) errors["appliedOn"] = res(R.string.veterinary_error_applied_future)

        val next = nextDoseOn?.trim()?.ifEmpty { null }
        if (next != null && next <= appliedOn) {
            errors["nextDoseOn"] = res(R.string.veterinary_error_next_dose_after)
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = res(R.string.veterinary_error_check_fields)) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            when (val r = repository.createVaccineByBusiness(petId, trimmedName, appliedOn, batch.trim().ifEmpty { null }, next)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    onSuccess()
                }
                is AppResult.Failure -> handleFailure(r.error)
            }
        }
    }

    fun createDeworming(
        petId: String,
        product: String,
        appliedOn: String,
        type: DewormingType,
        nextOn: String?,
        onSuccess: () -> Unit,
    ) {
        val errors = mutableMapOf<String, UiText>()
        val trimmedProduct = product.trim()
        val today = PetDates.today()

        if (trimmedProduct.isBlank()) errors["product"] = res(R.string.veterinary_error_product_required)
        if (appliedOn.isBlank()) errors["appliedOn"] = res(R.string.veterinary_error_applied_required)
        else if (appliedOn > today) errors["appliedOn"] = res(R.string.veterinary_error_applied_future)

        val next = nextOn?.trim()?.ifEmpty { null }
        if (next != null && next <= today) {
            errors["nextOn"] = res(R.string.veterinary_error_next_deworming_future)
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = res(R.string.veterinary_error_check_fields)) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            when (val r = repository.createDewormingByBusiness(petId, trimmedProduct, appliedOn, type, next)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    onSuccess()
                }
                is AppResult.Failure -> handleFailure(r.error)
            }
        }
    }

    private fun handleFailure(error: AppError) {
        val message = when (error) {
            is AppError.Server -> when (error.code) {
                403 -> res(R.string.veterinary_error_grant_expired)
                400 -> res(R.string.veterinary_error_bad_request)
                404 -> res(R.string.veterinary_error_pet_not_found)
                409 -> res(R.string.veterinary_error_conflict)
                else -> res(R.string.veterinary_error_server, error.code)
            }
            AppError.Network -> res(R.string.veterinary_error_network)
            AppError.Timeout -> res(R.string.veterinary_error_timeout)
            AppError.Unauthorized -> res(R.string.veterinary_error_session)
            is AppError.Validation -> res(R.string.veterinary_error_field, error.field)
            is AppError.Unknown -> res(R.string.veterinary_error_unknown)
        }
        _state.update { it.copy(isSubmitting = false, error = message) }
    }

    private companion object {
        /** Limites del contrato del backend (pm-130 a pm-142). */
        const val TEXT_MAX = 500
        const val MEDICATIONS_MAX = 10
        const val WEIGHT_MIN = 0.1
        const val WEIGHT_MAX = 200.0

        /** Atajo para no repetir `UiText.Resource(...)` en cada regla. */
        fun res(id: Int, vararg args: Any): UiText = UiText.Resource(id, args.toList())
    }
}
