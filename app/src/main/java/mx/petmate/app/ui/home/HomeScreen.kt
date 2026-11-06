package mx.petmate.app.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.domain.model.UserProfile
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Perfil del usuario (pestana Perfil del mapa).
 *
 * Muestra los datos que devuelve el backend (`GET users/me/get`, pm-037): nombre, correo, tipo de
 * cuenta y fecha de alta, el estado de la sesion y el cierre de sesion.
 */
@Composable
fun HomeScreen(
    userName: String,
    modifier: Modifier = Modifier,
    onBackToMap: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userName) { if (uiState.profile == null) viewModel.setUserName(userName) }

    HomeContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onLogoutClick = viewModel::onLogoutClicked,
        onBackToMap = onBackToMap,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onLogoutClick: () -> Unit,
    onBackToMap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val extra = MaterialTheme.petMateColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onBackToMap != null) {
            TextButton(onClick = onBackToMap, modifier = Modifier.align(Alignment.Start)) {
                Icon(painterResource(R.drawable.ic_pm_back), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.profile_back_to_map))
            }
        }

        Spacer(Modifier.height(8.dp))
        val name = uiState.profile?.name ?: uiState.userName
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .background(extra.brandSoft, CircleShape),
        ) {
            Text(
                text = initials(name),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = extra.brandText,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        uiState.profile?.let {
            Text(
                text = it.email,
                style = MaterialTheme.typography.bodyMedium,
                color = extra.ink2,
            )
        }

        Spacer(Modifier.height(16.dp))
        SessionBadge()
        Spacer(Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.profile_account_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                when {
                    uiState.profile != null -> ProfileRows(uiState.profile)
                    uiState.isLoading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                    else -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(
                            text = uiState.error?.asString() ?: stringResource(R.string.error_unknown),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extra.ink2,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.profile_retry)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        OutlinedButton(
            onClick = onLogoutClick,
            enabled = !uiState.isLoggingOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (uiState.isLoggingOut) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.home_logout))
            }
        }
    }
}

@Composable
private fun SessionBadge() {
    val extra = MaterialTheme.petMateColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(extra.okSoft, MaterialTheme.shapes.large)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(painterResource(R.drawable.ic_pm_check), contentDescription = null, tint = extra.ok, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.home_signed_in),
            style = MaterialTheme.typography.labelLarge,
            color = extra.ok,
        )
    }
}

@Composable
private fun ProfileRows(profile: UserProfile) {
    ProfileRow(R.drawable.ic_pm_user, stringResource(R.string.profile_name), profile.name)
    HorizontalDivider()
    ProfileRow(R.drawable.ic_pm_mail, stringResource(R.string.profile_email), profile.email)
    HorizontalDivider()
    ProfileRow(
        R.drawable.ic_pm_paw,
        stringResource(R.string.profile_account_type),
        stringResource(if (profile.isAffiliate) R.string.profile_type_business else R.string.profile_type_owner),
    )
    formatDate(profile.memberSince)?.let {
        HorizontalDivider()
        ProfileRow(R.drawable.ic_pm_info, stringResource(R.string.profile_member_since), it)
    }
}

@Composable
private fun ProfileRow(@DrawableRes icon: Int, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.petMateColors.ink2,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.petMateColors.ink2)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/**
 * "2026-11-06T17:34:07.123Z" -> "6 nov 2026" en la zona del telefono. Nulo si no se puede leer.
 * Usa SimpleDateFormat porque java.time exige Android 8 y la app soporta desde Android 7 (minSdk 24).
 */
private fun formatDate(iso: String?): String? = iso?.takeIf { it.length >= 19 }?.let {
    runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val date = parser.parse(it.substring(0, 19)) ?: return@runCatching null
        SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-MX")).format(date)
    }.getOrNull()
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    PetMateEdelTheme {
        HomeContent(
            uiState = HomeUiState(
                userName = "Usuario Demo",
                profile = UserProfile("1", "demo@petmate.mx", "Usuario Demo", false, "2026-11-06T17:34:07Z"),
            ),
            onRetry = {},
            onLogoutClick = {},
            onBackToMap = {},
        )
    }
}
