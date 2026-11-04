package mx.petmate.app.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.usecase.ValidateCredentialsUseCase
import mx.petmate.app.ui.components.ErrorNotice
import mx.petmate.app.ui.components.FieldError
import mx.petmate.app.ui.components.PetMateBackBar
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/** Crear cuenta (spec 002, A-02). */
@Composable
fun RegisterScreen(
    onSignedIn: () -> Unit,
    onGoToLogin: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) onSignedIn()
    }

    RegisterContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChanged,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onPasswordConfirmationChange = viewModel::onPasswordConfirmationChanged,
        onPrivacyChange = viewModel::onPrivacyChanged,
        onSubmit = viewModel::onSubmit,
        onGoToLogin = onGoToLogin,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
    onPrivacyChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onGoToLogin: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                title = stringResource(R.string.register_title),
                lead = stringResource(R.string.register_lead),
            )

            PetMateTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.field_name),
                placeholder = stringResource(R.string.field_name_placeholder),
                leadingIcon = R.drawable.ic_pm_user,
                required = true,
                error = uiState.nameError,
                enabled = enabled,
            )
            PetMateTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.field_email),
                placeholder = stringResource(R.string.field_email_placeholder),
                leadingIcon = R.drawable.ic_pm_mail,
                required = true,
                hint = stringResource(R.string.register_email_hint),
                error = uiState.emailError,
                keyboardType = KeyboardType.Email,
                enabled = enabled,
            )
            PetMateTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.field_password),
                placeholder = stringResource(
                    R.string.register_password_placeholder,
                    ValidateCredentialsUseCase.MIN_PASSWORD_LENGTH,
                ),
                leadingIcon = R.drawable.ic_pm_lock,
                required = true,
                error = uiState.passwordError,
                isPassword = true,
                enabled = enabled,
                bottomSpacing = 8.dp,
            )
            PasswordStrength(password = uiState.password)

            PetMateTextField(
                value = uiState.passwordConfirmation,
                onValueChange = onPasswordConfirmationChange,
                label = stringResource(R.string.field_password_confirmation),
                placeholder = stringResource(R.string.register_password_confirmation_placeholder),
                leadingIcon = R.drawable.ic_pm_lock,
                required = true,
                error = uiState.passwordConfirmationError,
                isPassword = true,
                imeAction = ImeAction.Done,
                enabled = enabled,
            )

            PrivacyCheck(
                checked = uiState.privacyAccepted,
                onCheckedChange = onPrivacyChange,
                enabled = enabled,
            )
            uiState.privacyError?.let {
                FieldError(text = it.asString(), modifier = Modifier.padding(bottom = 12.dp))
            }

            uiState.generalError?.let { ErrorNotice(text = it.asString()) }

            PetMateButton(
                text = stringResource(R.string.register_submit),
                onClick = onSubmit,
                loading = uiState.isSubmitting,
            )

            AuthSwitchRow(
                question = stringResource(R.string.register_have_account),
                action = stringResource(R.string.register_go_login),
                onClick = onGoToLogin,
                enabled = enabled,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ---------- Fuerza de la contraseña ----------

/** Reglas que muestra la maqueta. Solo orientan: la que se valida es la longitud. */
private data class PasswordRules(
    val longEnough: Boolean,
    val hasDigit: Boolean,
    val hasUppercase: Boolean,
) {
    val score: Int = listOf(longEnough, hasDigit, hasUppercase).count { it }
}

private fun passwordRules(password: String) = PasswordRules(
    longEnough = password.length >= ValidateCredentialsUseCase.MIN_PASSWORD_LENGTH,
    hasDigit = password.any(Char::isDigit),
    hasUppercase = password.any(Char::isUpperCase),
)

/** Barra de fuerza (rojo, amarillo, verde) y lista de reglas con palomita. */
@Composable
private fun PasswordStrength(password: String, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    val rules = passwordRules(password)
    val fraction by animateFloatAsState(rules.score / 3f, label = "strength")
    val barColor by animateColorAsState(
        when (rules.score) {
            0, 1 -> extra.alert
            2 -> extra.sun
            else -> extra.ok
        },
        label = "strengthColor",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(extra.surface2, CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(barColor, CircleShape),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 10.dp, bottom = 14.dp),
        ) {
            RuleRow(
                text = stringResource(R.string.register_rule_length, ValidateCredentialsUseCase.MIN_PASSWORD_LENGTH),
                met = rules.longEnough,
            )
            RuleRow(text = stringResource(R.string.register_rule_digit), met = rules.hasDigit)
            RuleRow(text = stringResource(R.string.register_rule_uppercase), met = rules.hasUppercase)
        }
    }
}

@Composable
private fun RuleRow(text: String, met: Boolean) {
    val color = if (met) MaterialTheme.petMateColors.ok else MaterialTheme.petMateColors.ink3
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            painter = painterResource(if (met) R.drawable.ic_pm_check else R.drawable.ic_pm_minus),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

// ---------- Aviso de privacidad ----------

/** Casilla cuadrada de la maqueta; todo el renglon se puede tocar. */
@Composable
private fun PrivacyCheck(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.petMateColors
    val boxShape = RoundedCornerShape(8.dp)
    val boxColor by animateColorAsState(if (checked) colors.primary else colors.surface, label = "checkBox")
    val borderColor = if (checked) colors.primary else extra.line

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(bottom = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .background(boxColor, boxShape)
                .border(2.dp, borderColor, boxShape),
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_pm_check),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        val prefix = stringResource(R.string.register_privacy_prefix)
        val document = stringResource(R.string.register_privacy_document)
        val suffix = stringResource(R.string.register_privacy_suffix)
        Text(
            text = buildAnnotatedString {
                append(prefix)
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = extra.brandText)) { append(document) }
                append(suffix)
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
            color = extra.ink2,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------- Previews ----------

@Composable
private fun RegisterPreview(uiState: RegisterUiState) {
    PetMateEdelTheme {
        RegisterContent(
            uiState = uiState,
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordConfirmationChange = {},
            onPrivacyChange = {},
            onSubmit = {},
            onGoToLogin = {},
            onBackClick = {},
        )
    }
}

@PetMatePreviews
@Composable
private fun RegisterEmptyPreview() = RegisterPreview(RegisterUiState())

@PetMatePreviews
@Composable
private fun RegisterFilledPreview() = RegisterPreview(
    RegisterUiState(
        name = "Mariana Torres",
        email = "mariana@correo.com",
        password = "Firulais2026",
        passwordConfirmation = "Firulais2026",
        privacyAccepted = true,
    ),
)

@PetMatePreviews
@Composable
private fun RegisterSubmittingPreview() = RegisterPreview(
    RegisterUiState(
        name = "Mariana Torres",
        email = "mariana@correo.com",
        password = "Firulais2026",
        passwordConfirmation = "Firulais2026",
        privacyAccepted = true,
        isSubmitting = true,
    ),
)

@PetMatePreviews
@Composable
private fun RegisterErrorsPreview() = RegisterPreview(
    RegisterUiState(
        name = "Mariana",
        email = "existe@correo.com",
        password = "perro1",
        passwordConfirmation = "perro2",
        emailError = UiText.Resource(R.string.error_email_taken),
        passwordError = UiText.Resource(
            R.string.error_password_too_short,
            listOf(ValidateCredentialsUseCase.MIN_PASSWORD_LENGTH),
        ),
        passwordConfirmationError = UiText.Resource(R.string.error_passwords_do_not_match),
        privacyError = UiText.Resource(R.string.error_privacy_required),
    ),
)

@PetMatePreviews
@Composable
private fun RegisterNetworkErrorPreview() = RegisterPreview(
    RegisterUiState(
        name = "Mariana Torres",
        email = "mariana@correo.com",
        password = "Firulais2026",
        passwordConfirmation = "Firulais2026",
        privacyAccepted = true,
        generalError = UiText.Resource(R.string.error_network),
    ),
)
