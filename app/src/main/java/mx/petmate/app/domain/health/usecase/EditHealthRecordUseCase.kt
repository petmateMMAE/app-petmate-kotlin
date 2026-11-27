package mx.petmate.app.domain.health.usecase

import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.repository.PetRepository

/**
 * Edicion y borrado de registros del carnet (spec 008, T-003, R-11 y C-07).
 *
 * Lo que registro un negocio es de solo lectura para el dueno: aqui se rechaza
 * antes de salir a la red, para no gastar un viaje y recibir el 403 del backend.
 * El servidor vuelve a validarlo; esta regla no lo sustituye.
 */
class EditHealthRecordUseCase(private val repository: PetRepository) {

    /** Resultado de intentar editar o borrar un registro. */
    sealed interface Outcome {
        data object Done : Outcome

        /** Lo registro un negocio: no se edita ni se borra desde el carnet (R-11). */
        data object VeterinaryRecord : Outcome

        data class Failed(val error: AppError) : Outcome
    }

    /** Falso cuando el registro es de origen veterinario. */
    fun canModify(origin: RecordOrigin): Boolean = !origin.byBusiness

    suspend fun deleteVaccine(vaccine: Vaccine): Outcome =
        guard(vaccine.origin) { repository.deleteVaccine(vaccine.id) }

    suspend fun deleteDeworming(deworming: Deworming): Outcome =
        guard(deworming.origin) { repository.deleteDeworming(deworming.id) }

    suspend fun updateAllergy(allergy: Allergy, draft: AllergyDraft): Outcome =
        guard(allergy.origin) { repository.updateAllergy(allergy.id, draft) }

    private suspend fun guard(origin: RecordOrigin, call: suspend () -> AppResult<*>): Outcome {
        if (!canModify(origin)) return Outcome.VeterinaryRecord
        return when (val result = call()) {
            is AppResult.Success -> Outcome.Done
            is AppResult.Failure -> Outcome.Failed(result.error)
        }
    }
}
