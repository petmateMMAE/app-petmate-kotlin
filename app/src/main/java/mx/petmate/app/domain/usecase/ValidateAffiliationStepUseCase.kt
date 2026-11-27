package mx.petmate.app.domain.usecase

import mx.petmate.app.domain.model.AffiliationDraft
import mx.petmate.app.domain.model.AffiliationRules
import mx.petmate.app.domain.model.AffiliationStep
import mx.petmate.app.domain.model.BusinessDraft
import mx.petmate.app.domain.model.DraftedService

/**
 * Valida un paso del formulario de afiliacion (spec 011, T-003).
 *
 * Las reglas estan en [AffiliationRules] y aqui **no se duplican**: este caso de
 * uso solo decide que reglas aplican segun el paso y segun si se esta editando
 * un negocio que ya existe.
 *
 * Devuelve problemas con nombre, nunca texto: el mensaje lo elige la interfaz
 * desde `strings.xml`. Antes esta validacion vivia en `BusinessForm.validate()`,
 * dentro de `ui`, con los textos en espanol incrustados; eso rompia P3 (la
 * interfaz no decide reglas de negocio) y dejaba los mensajes fuera de
 * `strings.xml`.
 */
class ValidateAffiliationStepUseCase {

    /**
     * Problemas de [step]; lista vacia si ese paso esta bien.
     *
     * Con [editing] en cierto no se revisan el tipo, la direccion ni la cedula:
     * el backend no deja cambiarlos despues de afiliarse (pm-107), asi que el
     * formulario de "Mi negocio" ni los muestra y no tiene sentido exigirlos.
     */
    operator fun invoke(
        draft: AffiliationDraft,
        step: AffiliationStep,
        editing: Boolean = false,
    ): List<AffiliationRules.Issue> {
        val issues = AffiliationRules.issues(draft, step)
        return if (editing) issues.filterNot { it.field in NOT_EDITABLE } else issues
    }

    /** Problemas de los tres pasos juntos, para decidir si ya se puede enviar. */
    fun all(draft: AffiliationDraft, editing: Boolean = false): List<AffiliationRules.Issue> =
        AffiliationStep.entries.flatMap { invoke(draft, it, editing) }

    /** Atajo para validar solo los datos del negocio, sin armar un borrador completo. */
    fun data(business: BusinessDraft, editing: Boolean = false): List<AffiliationRules.Issue> =
        invoke(AffiliationDraft(business = business), AffiliationStep.Data, editing)

    /** Atajo para validar solo los servicios. */
    fun services(services: List<DraftedService>): List<AffiliationRules.Issue> =
        invoke(AffiliationDraft(services = services), AffiliationStep.Services)

    /** Si [step] esta completo. */
    fun isValid(draft: AffiliationDraft, step: AffiliationStep, editing: Boolean = false): Boolean =
        invoke(draft, step, editing).isEmpty()

    private companion object {
        /** Lo que no se puede cambiar una vez afiliado. */
        val NOT_EDITABLE = setOf(
            AffiliationRules.Field.Kind,
            AffiliationRules.Field.Address,
            AffiliationRules.Field.License,
        )
    }
}
