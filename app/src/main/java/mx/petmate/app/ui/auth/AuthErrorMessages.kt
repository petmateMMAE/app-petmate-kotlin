package mx.petmate.app.ui.auth

import mx.petmate.app.R
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.ValidationReason
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.usecase.ValidateCredentialsUseCase

/** Traduce un error del dominio al texto que ve el usuario. */
fun AppError.toUiText(): UiText = when (this) {
    AppError.Network -> UiText.Resource(R.string.error_network)
    AppError.Timeout -> UiText.Resource(R.string.error_timeout)
    AppError.Unauthorized -> UiText.Resource(R.string.error_unauthorized)
    is AppError.Server -> when (code) {
        409 -> UiText.Resource(R.string.error_email_taken)
        429 -> UiText.Resource(R.string.error_too_many_attempts)
        503 -> UiText.Resource(R.string.error_service_unavailable)
        else -> UiText.Resource(R.string.error_server)
    }
    is AppError.Validation -> reason.toUiText()
    is AppError.Unknown -> UiText.Resource(R.string.error_unknown)
}

fun ValidationReason.toUiText(): UiText = when (this) {
    ValidationReason.REQUIRED -> UiText.Resource(R.string.error_required)
    ValidationReason.INVALID_EMAIL -> UiText.Resource(R.string.error_invalid_email)
    ValidationReason.PASSWORD_TOO_SHORT -> UiText.Resource(
        R.string.error_password_too_short,
        listOf(ValidateCredentialsUseCase.MIN_PASSWORD_LENGTH),
    )
    ValidationReason.PASSWORD_TOO_LONG -> UiText.Resource(
        R.string.error_password_too_long,
        listOf(ValidateCredentialsUseCase.MAX_PASSWORD_LENGTH),
    )
    ValidationReason.PASSWORDS_DO_NOT_MATCH -> UiText.Resource(R.string.error_passwords_do_not_match)
}
