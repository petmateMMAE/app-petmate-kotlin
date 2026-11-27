package mx.petmate.app.ui.business

import androidx.annotation.StringRes
import mx.petmate.app.R
import mx.petmate.app.domain.model.AffiliationRules

/**
 * Mensaje visible de cada problema de la afiliacion (spec 011, T-003).
 *
 * El dominio dice **que** esta mal; aqui se decide **como se dice**, y el texto
 * sale de `strings.xml`. Mismo reparto que `AuthErrorMessages` en 002.
 */
@StringRes
fun AffiliationRules.Issue.messageRes(): Int = when (field) {
    AffiliationRules.Field.Kind -> R.string.affiliation_error_kind
    AffiliationRules.Field.Name -> R.string.affiliation_error_name
    AffiliationRules.Field.Phone -> R.string.affiliation_error_phone
    AffiliationRules.Field.WhatsApp -> R.string.affiliation_error_whatsapp
    AffiliationRules.Field.Address -> R.string.affiliation_error_address
    AffiliationRules.Field.License -> R.string.affiliation_error_license
    AffiliationRules.Field.Hours -> when (problem) {
        AffiliationRules.Problem.CloseBeforeOpen -> R.string.affiliation_error_hours_order
        else -> R.string.affiliation_error_hours_format
    }
    AffiliationRules.Field.Services -> when (problem) {
        AffiliationRules.Problem.TooLong -> R.string.affiliation_error_service_name
        AffiliationRules.Problem.NegativePrice -> R.string.affiliation_error_service_price
        else -> R.string.affiliation_error_services
    }
    AffiliationRules.Field.Pin -> R.string.affiliation_error_pin
}

/**
 * Clave con la que el formulario busca el error de cada campo.
 *
 * Son las mismas que ya usaban las pantallas, para no tocarlas mas de lo
 * necesario.
 */
fun AffiliationRules.Field.formKey(): String = when (this) {
    AffiliationRules.Field.Kind -> "kind"
    AffiliationRules.Field.Name -> "name"
    AffiliationRules.Field.Phone -> "phone"
    AffiliationRules.Field.WhatsApp -> "whatsApp"
    AffiliationRules.Field.Address -> "address"
    AffiliationRules.Field.License -> "license"
    AffiliationRules.Field.Hours -> "hours"
    AffiliationRules.Field.Services -> "services"
    AffiliationRules.Field.Pin -> "pin"
}

/** Errores por campo, listos para la pantalla: clave del campo y recurso de texto. */
fun List<AffiliationRules.Issue>.toFormErrors(): Map<String, Int> =
    associate { it.field.formKey() to it.messageRes() }
