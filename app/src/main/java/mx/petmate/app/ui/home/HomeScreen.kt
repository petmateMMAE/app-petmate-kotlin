package mx.petmate.app.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.ui.components.DemoStrip
import mx.petmate.app.ui.components.OwnerTab
import mx.petmate.app.ui.components.PetMateBottomBar
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.ThemeSettings
import mx.petmate.app.ui.theme.petMateColors

/** Acciones de la pantalla Perfil; las rutas las resuelve el grafo de navegacion. */
data class ProfileActions(
    val onTabSelected: (OwnerTab) -> Unit = {},
    val onCreateAccountClick: () -> Unit = {},
    val onLoginClick: () -> Unit = {},
    val onExitDemoClick: () -> Unit = {},
    val onRemindersClick: () -> Unit = {},
    val onPrivacyClick: () -> Unit = {},
    val onAffiliateClick: () -> Unit = {},
    val onDeleteAccountClick: () -> Unit = {},
)

/**
 * Perfil (pestana Perfil), igual a la maqueta (SCREENS.profile de PetMate_App.html).
 *
 * - Modo demo: tarjeta amarilla "Estas usando la demo" con crear cuenta / ya tengo cuenta,
 *   y "Salir de la demo" al final.
 * - Con sesion: tarjeta con iniciales, nombre y correo (GET users/me/get, pm-037), la invitacion
 *   a afiliar un negocio, "Cerrar sesion" con confirmacion y "Eliminar mi cuenta".
 * - En los dos: Ajustes con modo oscuro, avisos y privacidad.
 */
@Composable
fun HomeScreen(
    isGuest: Boolean,
    userName: String,
    userEmail: String,
    isAffiliate: Boolean,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(isGuest, userName) { viewModel.start(userName, isGuest) }

    ProfileContent(
        isGuest = isGuest,
        name = uiState.profile?.name ?: uiState.userName.ifBlank { userName },
        email = uiState.profile?.email ?: userEmail,
        isAffiliate = uiState.profile?.isAffiliate ?: isAffiliate,
        isLoggingOut = uiState.isLoggingOut,
        actions = actions,
        onConfirmLogout = viewModel::onLogoutClicked,
        modifier = modifier,
    )
}

@Composable
private fun ProfileContent(
    isGuest: Boolean,
    name: String,
    email: String,
    isAffiliate: Boolean,
    isLoggingOut: Boolean,
    actions: ProfileActions,
    onConfirmLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            if (isGuest) DemoStrip(onCreateAccountClick = actions.onCreateAccountClick)
            // .topbar h1: 22 / 700, sin boton de regresar.
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp),
        ) {
            if (isGuest) {
                DemoCard(onCreateAccountClick = actions.onCreateAccountClick, onLoginClick = actions.onLoginClick)
            } else {
                UserCard(name = name, email = email)
                if (!isAffiliate) {
                    Spacer(Modifier.height(14.dp))
                    AffiliateCard(onClick = actions.onAffiliateClick)
                }
            }

            SectionTitle(stringResource(R.string.profile_settings))
            Menu {
                val darkNow = ThemeSettings.darkMode ?: isSystemInDarkTheme()
                MenuItem(
                    icon = R.drawable.ic_pm_moon,
                    label = stringResource(R.string.profile_dark_mode),
                    onClick = { ThemeSettings.darkMode = !darkNow },
                    trailing = {
                        Switch(
                            checked = darkNow,
                            onCheckedChange = { ThemeSettings.darkMode = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedThumbColor = Color.White,
                                checkedBorderColor = Color.Transparent,
                                uncheckedTrackColor = MaterialTheme.petMateColors.line,
                                uncheckedThumbColor = Color.White,
                                uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    },
                )
                MenuDivider()
                MenuItem(R.drawable.ic_pm_bell, stringResource(R.string.profile_reminders), actions.onRemindersClick)
                MenuDivider()
                MenuItem(R.drawable.ic_pm_shield, stringResource(R.string.profile_privacy), actions.onPrivacyClick)
            }

            Spacer(Modifier.height(14.dp))
            Menu {
                if (isGuest) {
                    MenuItem(R.drawable.ic_pm_logout, stringResource(R.string.profile_exit_demo), actions.onExitDemoClick)
                } else {
                    MenuItem(
                        icon = R.drawable.ic_pm_logout,
                        label = stringResource(R.string.home_logout),
                        onClick = { if (!isLoggingOut) showLogoutDialog = true },
                    )
                    MenuDivider()
                    MenuItem(
                        icon = R.drawable.ic_pm_trash,
                        label = stringResource(R.string.profile_delete_account),
                        onClick = actions.onDeleteAccountClick,
                        danger = true,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.profile_footer),
                fontSize = 12.sp,
                color = MaterialTheme.petMateColors.ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PetMateBottomBar(selected = OwnerTab.Profile, onTabSelected = actions.onTabSelected)
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                onConfirmLogout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

/** .card con fondo sun-soft: aviso de que se esta usando la demo. */
@Composable
private fun DemoCard(onCreateAccountClick: () -> Unit, onLoginClick: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow()
            .background(extra.sunSoft, CardShape)
            .padding(20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .background(extra.sun, RoundedCornerShape(20.dp)),
        ) {
            Icon(painterResource(R.drawable.ic_pm_user), contentDescription = null, tint = extra.onSun, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.profile_demo_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.profile_demo_body),
            fontSize = 14.sp,
            color = extra.ink2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PetMateButton(
            text = stringResource(R.string.welcome_register),
            onClick = onCreateAccountClick,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        PetMateButton(
            text = stringResource(R.string.welcome_login),
            onClick = onLoginClick,
            style = PetMateButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** .card con avatar de iniciales (62, radio 20, color de marca), nombre y correo. */
@Composable
private fun UserCard(name: String, email: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow()
            .background(colors.surface, CardShape)
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(62.dp)
                .background(colors.primary, RoundedCornerShape(20.dp)),
        ) {
            Text(
                text = initials(name),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.onPrimary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    fontSize = 14.sp,
                    color = MaterialTheme.petMateColors.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** .scan-big amarilla: invitacion a afiliar un negocio. */
@Composable
private fun AffiliateCard(onClick: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    val shape = RoundedCornerShape(28.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = extra.sun, spotColor = extra.sun)
            .background(extra.sun, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .background(extra.onSun, RoundedCornerShape(20.dp)),
        ) {
            Icon(painterResource(R.drawable.ic_pm_store), contentDescription = null, tint = extra.sun, modifier = Modifier.size(32.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.profile_affiliate_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 24.sp),
                color = extra.onSun,
            )
            Text(
                text = stringResource(R.string.profile_affiliate_body),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = extra.onSun,
                modifier = Modifier.alpha(0.85f),
            )
        }
    }
}

/** .sec-title: 19 / 700, margen 20 arriba y 10 abajo. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
    )
}

/** .menu: superficie con radio 20 y sombra; cada fila es un .mi. */
@Composable
private fun Menu(content: @Composable () -> Unit) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun MenuDivider() = HorizontalDivider(color = MaterialTheme.petMateColors.line, thickness = 1.dp)

/** .mi: icono en caja 38 (radio 12), texto 15 / 600 y flecha o control al final. */
@Composable
private fun MenuItem(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val extra = MaterialTheme.petMateColors
    val tint = if (danger) extra.alert else extra.ink2
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .background(if (danger) extra.alertSoft else extra.surface2, RoundedCornerShape(12.dp)),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (danger) extra.alert else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        } else {
            Icon(painterResource(R.drawable.ic_pm_chevron), contentDescription = null, tint = extra.ink3, modifier = Modifier.size(20.dp))
        }
    }
}

/** Confirmacion de la maqueta antes de cerrar sesion. */
@Composable
private fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(extra.brandSoft, RoundedCornerShape(18.dp)),
            ) {
                Icon(
                    painterResource(R.drawable.ic_pm_logout),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(stringResource(R.string.profile_logout_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        },
        text = {
            Text(stringResource(R.string.profile_logout_body), fontSize = 14.sp, color = extra.ink2, textAlign = TextAlign.Center)
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PetMateButton(text = stringResource(R.string.home_logout), onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                PetMateButton(
                    text = stringResource(R.string.profile_cancel),
                    onClick = onDismiss,
                    style = PetMateButtonStyle.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
    )
}

private val CardShape = RoundedCornerShape(20.dp)

@Composable
private fun Modifier.cardShadow(): Modifier =
    shadow(2.dp, CardShape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)

internal fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ProfileSignedInPreview() {
    PetMateEdelTheme {
        ProfileContent(
            isGuest = false,
            name = "Ana Lopez",
            email = "ana@correo.mx",
            isAffiliate = false,
            isLoggingOut = false,
            actions = ProfileActions(),
            onConfirmLogout = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ProfileDemoPreview() {
    PetMateEdelTheme {
        ProfileContent(
            isGuest = true,
            name = "",
            email = "",
            isAffiliate = false,
            isLoggingOut = false,
            actions = ProfileActions(),
            onConfirmLogout = {},
        )
    }
}
