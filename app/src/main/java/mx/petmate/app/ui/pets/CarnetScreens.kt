package mx.petmate.app.ui.pets

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.usecase.PetDates
import mx.petmate.app.ui.components.DemoStrip
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateIconButton
import mx.petmate.app.ui.theme.petMateColors

/** Muestra los avisos de una sola vez del ViewModel como Toast. */
@Composable
fun ShowMessages(message: UiText?, onShown: () -> Unit) {
    val context = LocalContext.current
    message?.let {
        val text = it.asString()
        LaunchedEffect(it) {
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
            onShown()
        }
    }
}

// =====================================================================================================
// Carnet (SCREENS.carnet de la maqueta)
// =====================================================================================================

/** Secciones del carnet a las que se puede ir desde el resumen. */
enum class CarnetSection { Vaccines, Deworm, Weight, Allergies, Qr, History }

@Composable
fun CarnetScreen(
    viewModel: PetCarnetViewModel,
    isGuest: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onOpen: (CarnetSection) -> Unit,
    onNeedAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    ShowMessages(state.message, viewModel::messageShown)
    val extra = MaterialTheme.petMateColors
    val pet = state.pet

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            pet == null && state.isLoading -> Box(Modifier.statusBarsPadding()) { PetLoading() }
            pet == null -> Column(Modifier.statusBarsPadding()) {
                PetTopBar(title = "", onBackClick = onBackClick)
                PetError(state.error ?: UiText.Resource(R.string.error_unknown), onRetry = viewModel::refresh)
            }
            else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // .hero
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(heroColor(pet.species))
                        .statusBarsPadding()
                        .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 40.dp),
                ) {
                    if (isGuest) DemoStrip(onCreateAccountClick = onNeedAccount, modifier = Modifier.padding(bottom = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PetMateIconButton(R.drawable.ic_pm_back, stringResource(R.string.action_back), onBackClick)
                        PetMateIconButton(R.drawable.ic_pm_edit, stringResource(R.string.carnet_edit), if (isGuest) onNeedAccount else onEditClick)
                    }
                    PetAvatar(
                        name = pet.name,
                        species = pet.species,
                        petId = pet.id,
                        size = 120.dp,
                        radius = 34.dp,
                        modifier = Modifier.border(4.dp, Color.White, RoundedCornerShape(34.dp)),
                    )
                    Text(
                        pet.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF1D1733),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    val sub = listOfNotNull(pet.breed, ageText(pet.birthDate)).joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, color = Color(0xFF4A4460), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                        HeroChip(speciesLabel(pet.species))
                        sexLabel(pet.sex)?.let { HeroChip(it) }
                        pet.birthDate?.let { HeroChip(formatDate(it), R.drawable.ic_pm_calendar) }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    // "Mostrar QR" montado sobre el borde del encabezado.
                    PetMateButton(
                        text = stringResource(R.string.carnet_show_qr),
                        onClick = { onOpen(CarnetSection.Qr) },
                        icon = R.drawable.ic_pm_qr,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-26).dp),
                    )
                    val today = PetDates.today()
                    val c = state.carnet
                    val nextVaccine = c.vaccines.mapNotNull { v -> v.nextDoseOn?.let { v to it } }.filter { it.second >= today }.minByOrNull { it.second }
                    val nextDeworm = c.dewormings.mapNotNull { it.nextOn }.filter { it >= today }.minOrNull()
                    val lastWeight = c.weights.maxByOrNull { it.measuredOn }
                    val allergies = c.allergies.filter { it.active }
                    // .sum
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryCard(
                                icon = R.drawable.ic_pm_syringe,
                                label = stringResource(R.string.carnet_next_vaccine),
                                value = nextVaccine?.second?.let { formatDate(it) } ?: stringResource(R.string.carnet_none),
                                sub = nextVaccine?.let { "${it.first.name} · ${whenText(it.second)}" } ?: stringResource(R.string.carnet_all_ok),
                                background = if (nextVaccine != null && (PetDates.daysBetween(today, nextVaccine.second) ?: 99) <= 30) extra.sunSoft else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpen(CarnetSection.Vaccines) },
                            )
                            SummaryCard(
                                icon = R.drawable.ic_pm_pill,
                                label = stringResource(R.string.carnet_deworm),
                                value = nextDeworm?.let { formatDate(it) } ?: stringResource(R.string.carnet_none),
                                sub = nextDeworm?.let { whenText(it) } ?: stringResource(R.string.carnet_all_ok),
                                modifier = Modifier.weight(1f),
                                onClick = { onOpen(CarnetSection.Deworm) },
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryCard(
                                icon = R.drawable.ic_pm_weight,
                                label = stringResource(R.string.carnet_last_weight),
                                value = lastWeight?.let { stringResource(R.string.weight_kg, it.kilograms) } ?: "—",
                                sub = lastWeight?.let { formatDate(it.measuredOn) } ?: stringResource(R.string.carnet_no_record),
                                modifier = Modifier.weight(1f),
                                onClick = { onOpen(CarnetSection.Weight) },
                            )
                            SummaryCard(
                                icon = R.drawable.ic_pm_alert,
                                label = stringResource(R.string.carnet_allergies),
                                value = if (allergies.isEmpty()) stringResource(R.string.carnet_none) else allergies.joinToString(", ") { it.name },
                                sub = stringResource(if (allergies.isEmpty()) R.string.carnet_no_active_allergies else R.string.allergy_active),
                                background = if (allergies.isNotEmpty()) extra.alertSoft else MaterialTheme.colorScheme.surface,
                                labelColor = if (allergies.isNotEmpty()) extra.alert else extra.ink2,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpen(CarnetSection.Allergies) },
                            )
                        }
                    }
                    PetSectionTitle(stringResource(R.string.carnet_sections))
                    // .sections (Historial clinico llega con la consulta veterinaria, spec 014).
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionButton(R.drawable.ic_pm_syringe, stringResource(R.string.vaccines_title), c.vaccines.size, extra.brandSoft, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { onOpen(CarnetSection.Vaccines) }
                            SectionButton(R.drawable.ic_pm_pill, stringResource(R.string.deworm_title), c.dewormings.size, extra.okSoft, extra.ok, Modifier.weight(1f)) { onOpen(CarnetSection.Deworm) }
                            SectionButton(R.drawable.ic_pm_weight, stringResource(R.string.weight_title), c.weights.size, extra.shopSoft, extra.shop, Modifier.weight(1f)) { onOpen(CarnetSection.Weight) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionButton(R.drawable.ic_pm_alert, stringResource(R.string.carnet_allergies), c.allergies.size, extra.alertSoft, extra.alert, Modifier.weight(1f)) { onOpen(CarnetSection.Allergies) }
                            SectionButton(R.drawable.ic_pm_qr, stringResource(R.string.carnet_qr), null, extra.sunSoft, extra.sunInk, Modifier.weight(1f)) { onOpen(CarnetSection.Qr) }
                            SectionButton(R.drawable.ic_pm_calendar, "Historial", null, extra.brandSoft, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { onOpen(CarnetSection.History) }
                        }
                    }
                    VSpace(24.dp)
                }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String, @DrawableRes icon: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        if (icon != null) Icon(painterResource(icon), contentDescription = null, tint = Color(0xFF1D1733), modifier = Modifier.size(14.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1D1733))
    }
}

/** .sum .card */
@Composable
private fun SummaryCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surface,
    labelColor: Color = MaterialTheme.petMateColors.ink2,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .shadow(2.dp, shape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = labelColor, modifier = Modifier.size(15.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = labelColor)
        }
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, lineHeight = 21.sp), maxLines = 2)
        Text(sub, fontSize = 12.sp, color = MaterialTheme.petMateColors.ink2, maxLines = 2)
    }
}

/** .sections button */
@Composable
private fun SectionButton(
    @DrawableRes icon: Int,
    label: String,
    count: Int?,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .shadow(2.dp, shape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(46.dp).background(background, RoundedCornerShape(14.dp))) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1)
        if (count != null) PetMateBadge(count.toString(), MaterialTheme.petMateColors.surface2, MaterialTheme.petMateColors.ink2)
    }
}

// =====================================================================================================
// Codigo QR (SCREENS.qr de la maqueta; pm-094 y pm-095)
// =====================================================================================================

@Composable
fun QrScreen(
    viewModel: PetCarnetViewModel,
    isGuest: Boolean,
    onBackClick: () -> Unit,
    onNeedAccount: () -> Unit,
    modifier: Modifier = Modifier,
    onShareClick: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.refresh()
        viewModel.loadQr()
    }
    ShowMessages(state.message, viewModel::messageShown)
    var confirm by rememberSaveable { mutableStateOf(false) }
    val extra = MaterialTheme.petMateColors
    val pet = state.pet

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = {
            if (isGuest) DemoStrip(onCreateAccountClick = onNeedAccount)
            PetTopBar(title = stringResource(R.string.qr_title, pet?.name.orEmpty()), onBackClick = onBackClick)
        },
    ) {
        val qr = state.qr
        if (qr == null || pet == null) {
            PetLoading()
            return@PetScaffoldColumn
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Text(stringResource(R.string.qr_lead), color = extra.ink2, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
            // .qr-card: siempre blanco, el retrato al centro (corrección de errores alta).
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(28.dp))
                    .background(extra.qrBackground, RoundedCornerShape(28.dp))
                    .padding(20.dp),
            ) {
                PetQrCode(content = qr.token, size = 240.dp)
                PetAvatar(
                    name = pet.name,
                    species = pet.species,
                    petId = pet.id,
                    size = 62.dp,
                    radius = 18.dp,
                    modifier = Modifier.border(4.dp, Color.White, RoundedCornerShape(18.dp)),
                )
            }
            Text(
                qr.shortCode,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(stringResource(R.string.qr_code_hint), color = extra.ink2, fontSize = 14.sp, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)) {
                PetMateBadge(stringResource(R.string.qr_brightness), extra.sunSoft, extra.sunInk, icon = R.drawable.ic_pm_info)
                PetMateBadge(stringResource(R.string.qr_only_yours), extra.okSoft, extra.ok, icon = R.drawable.ic_pm_shield)
            }
            // Compartir historial (pm-123): el dueno decide con quien, que y por cuanto tiempo.
            PetMateButton(
                text = "Compartir historial",
                onClick = { if (isGuest) onNeedAccount() else onShareClick() },
                icon = R.drawable.ic_pm_shield,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            )
            PetMateButton(
                text = stringResource(R.string.qr_regenerate),
                onClick = { if (isGuest) onNeedAccount() else confirm = true },
                style = PetMateButtonStyle.Outline,
                icon = R.drawable.ic_pm_refresh,
                loading = state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.qr_regenerate_note),
                fontSize = 13.sp,
                color = extra.ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
            )
        }
    }

    if (confirm) {
        PetConfirmDialog(
            title = stringResource(R.string.qr_regenerate_title),
            text = stringResource(R.string.qr_regenerate_text),
            confirm = stringResource(R.string.qr_regenerate_confirm),
            icon = R.drawable.ic_pm_refresh,
            onConfirm = {
                confirm = false
                viewModel.regenerateQr()
            },
            onDismiss = { confirm = false },
        )
    }
}
