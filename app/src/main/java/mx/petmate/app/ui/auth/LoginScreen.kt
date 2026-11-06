package mx.petmate.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.BuildConfig
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.ui.components.ErrorNotice
import mx.petmate.app.ui.components.PetMateBackBar
import mx.petmate.app.domain.model.DemoAccount
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.theme.PetMateEdelTheme

/**
 * Iniciar sesion (spec 002, A-04).
 *
 * "¿Olvidaste tu contraseña?" solo aparece si se pasa [onForgotPasswordClick]:
 * la recuperacion (A-07) todavia no tiene pantalla.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onGoToRegister: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onForgotPasswordClick: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) onSignedIn()
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onSubmit = viewModel::onSubmit,
        onGoToRegister = onGoToRegister,
        onBackClick = onBackClick,
        onForgotPasswordClick = onForgotPasswordClick,
        showDemoAccount = BuildConfig.SHOW_DEMO_ACCOUNT,
        onUseDemoAccount = {
            viewModel.onEmailChanged(DemoAccount.EMAIL)
            viewModel.onPasswordChanged(DemoAccount.PASSWORD)
        },
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoToRegister: () -> Unit,
    onBackClick: () -> Unit,
    onForgotPasswordClick: (() -> Unit)?,
    showDemoAccount: Boolean,
    modifier: Modifier = Modifier,
    onUseDemoAccount: () -> Unit = {},
) {
    val enabled = !uiState.isSubmitting
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        PetMateBackBar(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
        ) {
            AuthHeader(
                title = stringResource(R.string.login_title),
                lead = stringResource(R.string.login_lead),
            )

            uiState.generalError?.let { ErrorNotice(text = it.asString()) }

            PetMateTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.field_email),
                placeholder = stringResource(R.string.field_email_placeholder),
                leadingIcon = R.drawable.ic_pm_mail,
                error = uiState.emailError,
                keyboardType = KeyboardType.Email,
                enabled = enabled,
            )
            PetMateTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.field_password),
                placeholder = stringResource(R.string.login_password_placeholder),
                leadingIcon = R.drawable.ic_pm_lock,
                error = uiState.passwordError,
                isPassword = true,
                imeAction = ImeAction.Done,
                enabled = enabled,
                bottomSpacing = 0.dp,
            )

            if (onForgotPasswordClick != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    AuthLink(
                        text = stringResource(R.string.login_forgot_password),
                        onClick = onForgotPasswordClick,
                        enabled = enabled,
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }

            PetMateButton(
                text = stringResource(R.string.login_submit),
                onClick = onSubmit,
                loading = uiState.isSubmitting,
            )

            if (showDemoAccount) {
                DemoAccountsNotice(modifier = Modifier.padding(top = 16.dp), onClick = onUseDemoAccount)
            }

            AuthSwitchRow(
                question = stringResource(R.string.login_no_account),
                action = stringResource(R.string.login_go_register),
                onClick = onGoToRegister,
                enabled = enabled,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

// ---------- Previews ----------

@Composable
private fun LoginPreview(uiState: LoginUiState) {
    PetMateEdelTheme {
        LoginContent(
            uiState = uiState,
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onGoToRegister = {},
            onBackClick = {},
            onForgotPasswordClick = {},
            showDemoAccount = true,
        )
    }
}

@PetMatePreviews
@Composable
private fun LoginEmptyPreview() = LoginPreview(LoginUiState())

@PetMatePreviews
@Composable
private fun LoginFilledPreview() = LoginPreview(
    LoginUiState(email = "mariana@correo.com", password = "petmate123"),
)

@PetMatePreviews
@Composable
private fun LoginSubmittingPreview() = LoginPreview(
    LoginUiState(email = "mariana@correo.com", password = "petmate123", isSubmitting = true),
)

@PetMatePreviews
@Composable
private fun LoginFieldErrorsPreview() = LoginPreview(
    LoginUiState(
        email = "mariana@correo",
        emailError = UiText.Resource(R.string.error_invalid_email),
        passwordError = UiText.Resource(R.string.error_required),
    ),
)

@PetMatePreviews
@Composable
private fun LoginWrongCredentialsPreview() = LoginPreview(
    LoginUiState(
        email = "mariana@correo.com",
        password = "otra-clave",
        generalError = UiText.Resource(R.string.error_unauthorized),
    ),
)
