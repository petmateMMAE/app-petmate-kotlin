package mx.petmate.app.ui.business

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
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.repository.VeterinaryRepository
import mx.petmate.app.domain.usecase.PetDates
import javax.inject.Inject

data class VeterinaryActionUiState(
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
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
        val errors = mutableMapOf<String, String>()
        val trimmedReason = reason.trim()
        val trimmedDiagnosis = diagnosis.trim()

        if (trimmedReason.isBlank()) errors["reason"] = "El motivo es obligatorio."
        else if (trimmedReason.length > 500) errors["reason"] = "Máximo 500 caracteres."

        if (trimmedDiagnosis.isBlank()) errors["diagnosis"] = "El diagnóstico es obligatorio."
        else if (trimmedDiagnosis.length > 500) errors["diagnosis"] = "Máximo 500 caracteres."

        val weightKg = if (weightKgText.isNotBlank()) {
            val parsed = weightKgText.replace(",", ".").toDoubleOrNull()
            if (parsed == null || parsed < 0.1 || parsed > 200.0) {
                errors["weightKg"] = "El peso debe estar entre 0.1 y 200 kg."
                null
            } else parsed
        } else null

        if (medications.size > 10) {
            errors["medications"] = "Máximo 10 medicamentos por receta."
        }

        medications.forEachIndexed { index, med ->
            if (med.name.isBlank()) errors["med_${index}_name"] = "Nombre obligatorio."
            if (med.dose.isBlank()) errors["med_${index}_dose"] = "Dosis obligatoria."
            if (med.frequency.isBlank()) errors["med_${index}_frequency"] = "Frecuencia obligatoria."
            if (med.duration.isBlank()) errors["med_${index}_duration"] = "Duración obligatoria."
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = "Revisa los campos con error.") }
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
                    _state.update { it.copy(isSubmitting = false) }
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
        val errors = mutableMapOf<String, String>()
        val trimmedName = name.trim()
        val today = PetDates.today()

        if (trimmedName.isBlank()) errors["name"] = "El nombre de la vacuna es obligatorio."
        if (appliedOn.isBlank()) errors["appliedOn"] = "La fecha de aplicación es obligatoria."
        else if (appliedOn > today) errors["appliedOn"] = "La fecha de aplicación no puede ser futura."

        val next = nextDoseOn?.trim()?.ifEmpty { null }
        if (next != null && next <= appliedOn) {
            errors["nextDoseOn"] = "La próxima dosis debe ser posterior a la fecha de aplicación."
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = "Revisa los campos con error.") }
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
        val errors = mutableMapOf<String, String>()
        val trimmedProduct = product.trim()
        val today = PetDates.today()

        if (trimmedProduct.isBlank()) errors["product"] = "El nombre del producto es obligatorio."
        if (appliedOn.isBlank()) errors["appliedOn"] = "La fecha de aplicación es obligatoria."
        else if (appliedOn > today) errors["appliedOn"] = "La fecha de aplicación no puede ser futura."

        val next = nextOn?.trim()?.ifEmpty { null }
        if (next != null && next <= today) {
            errors["nextOn"] = "La próxima desparasitación debe ser futura."
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = "Revisa los campos con error.") }
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
                403 -> "La autorización venció o no incluye a tu negocio."
                400 -> "Revisa los datos: alguno no cumple el formato esperado."
                404 -> "No se encontró la mascota."
                409 -> "Ya existe un registro similar o en conflicto."
                else -> "Error en el servidor (${error.code}). Intenta más tarde."
            }
            AppError.Network -> "Sin conexión. Revisa tu internet e intenta de nuevo."
            AppError.Timeout -> "El servidor tardó en responder. Intenta de nuevo."
            AppError.Unauthorized -> "Tu sesión expiró. Vuelve a iniciar sesión."
            is AppError.Validation -> "Error en el campo ${error.field}."
            is AppError.Unknown -> "Ocurrió un error inesperado."
        }
        _state.update { it.copy(isSubmitting = false, error = message) }
    }
}
