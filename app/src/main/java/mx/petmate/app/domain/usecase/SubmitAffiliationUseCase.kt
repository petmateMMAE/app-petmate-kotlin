package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AffiliationDraft
import mx.petmate.app.domain.model.AffiliationRules
import mx.petmate.app.domain.model.DraftedService
import mx.petmate.app.domain.model.OwnBusiness
import mx.petmate.app.domain.repository.BusinessRepository

/**
 * Envia la afiliacion al backend (spec 011, T-004).
 *
 * Valida los tres pasos antes de salir a la red: un borrador a medias no se
 * manda. El negocio se crea primero y sus servicios despues, porque el backend
 * los cuelga del negocio (pm-107 y pm-111); si un servicio falla, el negocio ya
 * quedo creado y se informa cual no se pudo dar de alta en lugar de fingir que
 * todo salio bien.
 */
class SubmitAffiliationUseCase(private val repository: BusinessRepository) {

    sealed interface Submission {
        /** El negocio quedo afiliado. [servicesFailed] son los servicios que el backend no acepto. */
        data class Affiliated(
            val business: OwnBusiness,
            val servicesFailed: List<String> = emptyList(),
        ) : Submission

        /** Falta algo en alguno de los tres pasos: no se envio nada. */
        data class Incomplete(val issues: List<AffiliationRules.Issue>) : Submission
    }

    suspend operator fun invoke(draft: AffiliationDraft): AppResult<Submission> {
        val issues = AffiliationRules.allIssues(draft)
        if (issues.isNotEmpty()) return AppResult.Success(Submission.Incomplete(issues))

        // allIssues ya garantizo que el pin existe.
        val latitude = draft.latitude ?: return AppResult.Success(
            Submission.Incomplete(listOf(AffiliationRules.Issue(AffiliationRules.Field.Pin, AffiliationRules.Problem.Required))),
        )
        val longitude = draft.longitude ?: return AppResult.Success(
            Submission.Incomplete(listOf(AffiliationRules.Issue(AffiliationRules.Field.Pin, AffiliationRules.Problem.Required))),
        )

        val created = repository.createBusiness(draft.business, latitude, longitude)
        val business = when (created) {
            is AppResult.Failure -> return created
            is AppResult.Success -> created.data
        }

        val failed = draft.services
            .filter { it.name.isNotBlank() }
            .filter { repository.createService(it.name.trim(), null, it.price) is AppResult.Failure }
            .map(DraftedService::name)

        // El borrador ya no sirve: el negocio existe.
        repository.clearAffiliationDraft()

        return AppResult.Success(Submission.Affiliated(business, failed))
    }
}
