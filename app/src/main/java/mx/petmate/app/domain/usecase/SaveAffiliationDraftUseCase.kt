package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AffiliationDraft
import mx.petmate.app.domain.model.AffiliationStep
import mx.petmate.app.domain.repository.BusinessRepository

/**
 * Guarda y recupera el avance del formulario de afiliacion (spec 011, T-004, R-06).
 *
 * El borrador es del usuario de la sesion activa; el repositorio se encarga de
 * no mezclar cuentas (T-008).
 */
class SaveAffiliationDraftUseCase(private val repository: BusinessRepository) {

    /** Guarda el borrador tal cual. */
    suspend operator fun invoke(draft: AffiliationDraft): AppResult<Unit> =
        repository.saveAffiliationDraft(draft)

    /** Guarda el borrador anotando en que paso se quedo el usuario. */
    suspend operator fun invoke(draft: AffiliationDraft, step: AffiliationStep): AppResult<Unit> =
        repository.saveAffiliationDraft(draft.copy(step = step))

    /** Borrador guardado, o null si no hay ninguno de esta cuenta. */
    suspend fun read(): AppResult<AffiliationDraft?> = repository.readAffiliationDraft()

    /** Tira el borrador. Se llama al terminar la afiliacion o al abandonarla. */
    suspend fun clear(): AppResult<Unit> = repository.clearAffiliationDraft()
}
