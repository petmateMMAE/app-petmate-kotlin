package mx.petmate.app.ui.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.domain.model.SCHEDULE_DAYS
import mx.petmate.app.domain.model.ShareDuration
import mx.petmate.app.domain.model.SharedData
import mx.petmate.app.data.repository.label
import mx.petmate.app.ui.components.ErrorNotice
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateIconButton
import mx.petmate.app.ui.map.dial
import mx.petmate.app.ui.map.formatDistance
import mx.petmate.app.ui.map.openDirections
import mx.petmate.app.ui.map.openWhatsApp
import mx.petmate.app.ui.map.rememberLocationRequest
import mx.petmate.app.ui.pets.PetBottomAction
import mx.petmate.app.ui.pets.PetConfirmDialog
import mx.petmate.app.ui.pets.PetError
import mx.petmate.app.ui.pets.PetLoading
import mx.petmate.app.ui.pets.PetScaffoldColumn
import mx.petmate.app.ui.pets.PetSectionTitle
import mx.petmate.app.ui.pets.PetTopBar
import mx.petmate.app.ui.pets.VSpace
import mx.petmate.app.ui.theme.petMateColors
import mx.petmate.app.core.ui.UiText

/* Lo que ve el dueno: perfil publico del negocio (bizProfile) y "Compartir historial" (share). */

@Composable
fun BizProfileScreen(viewModel: BizProfileViewModel, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extra = MaterialTheme.petMateColors
    val context = LocalContext.current
    val b = state.business
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.loading -> Column(Modifier.statusBarsPadding()) {
                PetTopBar(title = "", onBackClick = onBackClick)
                PetLoading()
            }
            b == null -> Column(Modifier.statusBarsPadding()) {
                PetTopBar(title = "", onBackClick = onBackClick)
                PetError(UiText.Dynamic(state.error ?: "No se pudo cargar"), onRetry = viewModel::load)
            }
            else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(230.dp)) {
                    BusinessPhoto(b.kind, modifier = Modifier.fillMaxSize(), radius = 0.dp)
                    PetMateIconButton(
                        icon = R.drawable.ic_pm_back,
                        contentDescription = "Regresar",
                        onClick = onBackClick,
                        modifier = Modifier.statusBarsPadding().padding(12.dp),
                    )
                }
                Column(Modifier.padding(horizontal = 18.dp).offset(y = (-26).dp)) {
                    BizCard {
                        Column {
                            KindBadge(b.kind)
                            Text(
                                b.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                            )
                            Text(
                                todayText(b.isOpen, b.todayHours),
                                color = if (b.isOpen) extra.ok else extra.alert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                                b.phone?.let { phone -> ContactAction("Llamar", R.drawable.ic_pm_phone, extra.okSoft, extra.ok, Modifier.weight(1f)) { context.dial(phone) } }
                                b.whatsApp?.let { wa -> ContactAction("WhatsApp", R.drawable.ic_pm_chat, extra.whatsApp, extra.whatsAppInk, Modifier.weight(1f)) { context.openWhatsApp(wa) } }
                                ContactAction("Cómo llegar", R.drawable.ic_pm_navigation, extra.brandSoft, extra.brandText, Modifier.weight(1f)) {
                                    context.openDirections(b.latitude, b.longitude, b.name)
                                }
                            }
                        }
                    }
                    b.description?.let { Text(it, color = extra.ink2, modifier = Modifier.padding(top = 16.dp)) }
                    if (b.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                            Icon(painterResource(R.drawable.ic_pm_pin), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(b.address, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    PetSectionTitle("Horario")
                    BizCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SCHEDULE_DAYS.forEach { day ->
                                val ranges = b.schedule[day].orEmpty()
                                Row {
                                    Text(DAY_LABELS.getValue(day), fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
                                    Text(
                                        if (ranges.isEmpty()) "Cerrado" else ranges.joinToString(", ") { "${it.open}–${it.close}" },
                                        color = if (ranges.isEmpty()) extra.ink3 else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    PetSectionTitle("Servicios")
                    if (b.services.isEmpty()) {
                        Text("Este negocio aún no publica servicios.", color = extra.ink3)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            b.services.forEach { s ->
                                BizCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(s.name, fontWeight = FontWeight.Bold)
                                            s.description?.let { Text(it, color = extra.ink2, fontSize = 13.sp) }
                                        }
                                        Text(priceText(s.price), fontWeight = FontWeight.Bold, color = if (s.price != null) MaterialTheme.colorScheme.primary else extra.ink3)
                                    }
                                }
                            }
                        }
                    }
                    VSpace(24.dp)
                }
            }
        }
    }
}

@Composable
private fun ContactAction(text: String, icon: Int, soft: Color, ink: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.heightIn(min = 58.dp).clip(RoundedCornerShape(16.dp)).background(soft).clickable(onClick = onClick).padding(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        Text(text, color = ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ---------- Compartir historial ----------

private val SHARE_ITEMS = listOf(
    SharedData.Vaccines to ("Vacunas" to R.drawable.ic_pm_syringe),
    SharedData.Dewormings to ("Desparasitación" to R.drawable.ic_pm_pill),
    SharedData.Weights to ("Peso" to R.drawable.ic_pm_weight),
    SharedData.Consultations to ("Consultas y recetas" to R.drawable.ic_pm_file),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareScreen(viewModel: ShareViewModel, petName: String, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extra = MaterialTheme.petMateColors
    val requestLocation = rememberLocationRequest(viewModel::loadVets)
    LaunchedEffect(Unit) { requestLocation(false) }

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = { PetTopBar(title = "Compartir historial", subtitle = petName.ifBlank { null }, onBackClick = onBackClick) },
        bottom = {
            PetBottomAction {
                Column {
                    state.error?.let { ErrorNotice(it, modifier = Modifier.padding(bottom = 10.dp)) }
                    PetMateButton("Compartir", viewModel::share, icon = R.drawable.ic_pm_shield, loading = state.saving, modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            TipBox("Tú decides con quién y qué compartir. Después el negocio escanea el QR de tu mascota.")
            StepTitle(1, "¿Con quién?")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.loadingVets) PetLoading()
                state.vets.forEach { v ->
                    ShareOption(v.name, "Veterinaria · a ${formatDistance(v.distanceMeters)}${if (v.isOpen) " · abierta" else ""}", state.businessId == v.id) { viewModel.selectBusiness(v.id) }
                }
                ShareOption("Cualquier negocio que escanee", "Si vas a uno que no está en la lista", state.businessId == null) { viewModel.selectBusiness(null) }
            }
            StepTitle(2, "¿Qué puede ver?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("Alergias", selected = true, onClick = {}, icon = R.drawable.ic_pm_lock, enabled = false)
                SHARE_ITEMS.forEach { (item, meta) ->
                    SelectChip(meta.first, selected = item in state.data, onClick = { viewModel.toggle(item) }, icon = if (item in state.data) R.drawable.ic_pm_check else meta.second)
                }
            }
            Text("Las alergias siempre se comparten para cuidar a ${petName.ifBlank { "tu mascota" }}.", color = extra.ink2, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            StepTitle(3, "¿Por cuánto tiempo?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareDuration.entries.forEach { d -> SelectChip(d.label(), selected = state.duration == d, onClick = { viewModel.duration(d) }, modifier = Modifier.weight(1f)) }
            }
            VSpace(24.dp)
        }
    }

    state.sharedWith?.let { who ->
        PetConfirmDialog(
            title = "¡Listo!",
            text = "Compartiste el historial con $who por ${state.duration.label()}. Que escaneen el QR de ${petName.ifBlank { "tu mascota" }}.",
            confirm = "Ver el QR",
            icon = R.drawable.ic_pm_check,
            onConfirm = {
                viewModel.dismissShared()
                onBackClick()
            },
            onDismiss = viewModel::dismissShared,
        )
    }
}

@Composable
private fun ShareOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) extra.brandSoft else MaterialTheme.colorScheme.surface)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else extra.line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = extra.ink2, fontSize = 13.sp)
        }
        if (selected) Icon(painterResource(R.drawable.ic_pm_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}
