package mx.petmate.app.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.petmate.app.R
import mx.petmate.app.ui.components.DemoStrip
import mx.petmate.app.ui.components.PetMateBackBar
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/** Encabezado de las pantallas secundarias de Perfil: regresar + titulo (.topbar). */
@Composable
private fun SecondaryHeader(
    @StringRes title: Int,
    onBackClick: () -> Unit,
    isGuest: Boolean,
    onCreateAccountClick: () -> Unit,
) {
    Column(modifier = Modifier.statusBarsPadding()) {
        if (isGuest) DemoStrip(onCreateAccountClick = onCreateAccountClick)
        Box {
            PetMateBackBar(onBackClick = onBackClick)
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 72.dp),
            )
        }
    }
}

/** Privacidad y terminos (pantalla "legal" de la maqueta). */
@Composable
fun LegalScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGuest: Boolean = false,
    onCreateAccountClick: () -> Unit = {},
) {
    val extra = MaterialTheme.petMateColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SecondaryHeader(R.string.legal_title, onBackClick, isGuest, onCreateAccountClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            // .tip: aviso de la version web.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extra.brandSoft, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Icon(painterResource(R.drawable.ic_pm_info), contentDescription = null, tint = extra.brandText, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.legal_tip), fontSize = 14.sp, color = extra.brandText, fontWeight = FontWeight.SemiBold)
            }
            LegalSection(R.string.legal_data_title, R.string.legal_data_body)
            LegalSection(R.string.legal_card_title, R.string.legal_card_body)
            LegalSection(R.string.legal_location_title, R.string.legal_location_body)
            LegalSection(R.string.legal_delete_title, R.string.legal_delete_body)
            LegalSection(R.string.legal_terms_title, R.string.legal_terms_body)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LegalSection(@StringRes title: Int, @StringRes body: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
    Text(text = stringResource(body), fontSize = 15.sp, color = MaterialTheme.petMateColors.ink2, lineHeight = 22.sp)
}

/**
 * "Eliminar mi cuenta" de la maqueta. El backend todavia no tiene el endpoint de borrado, asi
 * que al confirmar se explica como pedirlo (petmate.mx/eliminar-cuenta) en lugar de fingir que
 * se borro.
 */
@Composable
fun DeleteAccountScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    var password by rememberSaveable { mutableStateOf("") }
    var showPending by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SecondaryHeader(R.string.profile_delete_account, onBackClick, isGuest = false, onCreateAccountClick = {})
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(extra.alertSoft, RoundedCornerShape(20.dp)),
            ) {
                Icon(painterResource(R.drawable.ic_pm_trash), contentDescription = null, tint = extra.alert, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.delete_title), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp))
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.delete_lead), fontSize = 15.sp, color = extra.ink2)
            Spacer(Modifier.height(10.dp))
            listOf(R.string.delete_item_account, R.string.delete_item_pets, R.string.delete_item_qr, R.string.delete_item_business)
                .forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(Modifier.size(8.dp).background(extra.alert, CircleShape))
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(item), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            Spacer(Modifier.height(18.dp))
            PetMateTextField(
                value = password,
                onValueChange = {
                    password = it
                    showPending = false
                },
                label = stringResource(R.string.delete_password_label),
                leadingIcon = R.drawable.ic_pm_lock,
                isPassword = true,
                imeAction = ImeAction.Done,
            )
            if (showPending) {
                Text(
                    text = stringResource(R.string.delete_pending),
                    fontSize = 14.sp,
                    color = extra.sunInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(extra.sunSoft, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = { showPending = true },
                enabled = password.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = extra.alert,
                    contentColor = Color.White,
                    disabledContainerColor = extra.alertSoft,
                    disabledContentColor = extra.alert,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.delete_confirm), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            PetMateButton(
                text = stringResource(R.string.delete_cancel),
                onClick = onBackClick,
                style = PetMateButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LegalPreview() {
    PetMateEdelTheme { LegalScreen(onBackClick = {}) }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun DeleteAccountPreview() {
    PetMateEdelTheme { DeleteAccountScreen(onBackClick = {}) }
}
