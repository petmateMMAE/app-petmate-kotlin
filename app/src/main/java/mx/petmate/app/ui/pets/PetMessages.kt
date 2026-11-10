package mx.petmate.app.ui.pets

import mx.petmate.app.R
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.usecase.PetRules
import mx.petmate.app.ui.auth.toUiText

/** Errores de mascotas y carnet con los codigos del contrato (pm-040 a pm-103). */
fun AppError.toPetUiText(): UiText = when {
    this is AppError.Server && code == 403 -> UiText.Resource(R.string.pets_error_business_record)
    this is AppError.Server && code == 404 -> UiText.Resource(R.string.pets_error_not_found)
    this is AppError.Server && code == 409 -> UiText.Resource(R.string.pets_error_limit)
    this is AppError.Server && code == 413 -> UiText.Resource(R.string.pets_error_too_large)
    this == AppError.Unauthorized -> UiText.Resource(R.string.pets_error_session)
    else -> toUiText()
}

/** Mensaje de cada regla que no se cumple en un formulario. */
fun PetRules.Issue.toUiText(): UiText = when (problem) {
    PetRules.Problem.Required -> UiText.Resource(R.string.error_required)
    PetRules.Problem.TooLong -> UiText.Resource(
        R.string.pets_error_too_long,
        listOf(if (field == PetRules.Field.Breed) PetRules.BREED_MAX else PetRules.NAME_MAX),
    )
    PetRules.Problem.Future -> UiText.Resource(R.string.pets_error_future)
    PetRules.Problem.TooOld -> UiText.Resource(R.string.pets_error_too_old, listOf(PetRules.MAX_AGE_YEARS))
    PetRules.Problem.NotAfterApplied -> UiText.Resource(R.string.pets_error_next_after_applied)
    PetRules.Problem.NotFuture -> UiText.Resource(R.string.pets_error_next_future)
    PetRules.Problem.OutOfRange -> UiText.Resource(R.string.pets_error_weight_range)
}

fun List<PetRules.Issue>.toFieldErrors(): Map<PetRules.Field, UiText> = associate { it.field to it.toUiText() }
