package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.FollowUpStatus
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.statusKind
import mx.petmate.app.domain.repository.VeterinaryRepository

/** Resultado de cambiar el estado de un seguimiento. */
sealed interface FollowUpChange {
    data class Done(val followup: Followup) : FollowUpChange

    /** La tabla de transiciones no lo permite; no se llamo al servidor (spec 015, C-02 a C-05). */
    data class InvalidTransition(val from: FollowUpStatus?, val to: FollowUpStatus) : FollowUpChange

    /** Dias de revision fuera de rango; no se llamo al servidor. */
    data object InvalidReviewDays : FollowUpChange

    data class Failed(val error: AppError) : FollowUpChange
}

/**
 * Cambia el estado de un seguimiento validando la transicion antes de tocar la red (spec 015, T-005).
 *
 * El contrato del repositorio (T-004) ya existia en `VeterinaryRepository` (unidad 014) y se reutiliza en vez
 * de declarar otro igual. Q-03: la fecha de revision es opcional (el backend acepta `null`).
 * La moderacion de notas (R-09) depende de la unidad 004, que aun no existe.
 */
class ChangeFollowUpStatusUseCase(private val repository: VeterinaryRepository) {

    suspend operator fun invoke(followup: Followup, to: FollowUpStatus, nextReviewInDays: Int?): FollowUpChange {
        val from = followup.statusKind
        if (from == null || !FollowUpTransition.isAllowed(from, to)) return FollowUpChange.InvalidTransition(from, to)
        if (nextReviewInDays != null && nextReviewInDays !in REVIEW_DAYS) return FollowUpChange.InvalidReviewDays
        // Al dar de alta no queda revision pendiente.
        val days = if (to == FollowUpStatus.Discharged) null else nextReviewInDays
        return when (val result = repository.updateFollowup(followup.id, to.apiValue, days)) {
            is AppResult.Success -> FollowUpChange.Done(result.data)
            is AppResult.Failure -> FollowUpChange.Failed(result.error)
        }
    }

    companion object {
        /** Dias validos para la proxima revision: de manana a un ano. */
        val REVIEW_DAYS = 1..365
    }
}
