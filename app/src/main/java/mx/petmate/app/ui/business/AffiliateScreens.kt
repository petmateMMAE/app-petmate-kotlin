package mx.petmate.app.ui.business

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.ui.components.ErrorNotice
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.map.OsmMap
import mx.petmate.app.ui.map.OsmMapHandle
import mx.petmate.app.ui.map.rememberLocationRequest
import mx.petmate.app.ui.pets.PetBottomAction
import mx.petmate.app.ui.pets.PetFieldLabel
import mx.petmate.app.ui.pets.PetScaffoldColumn
import mx.petmate.app.ui.pets.PetSectionTitle
import mx.petmate.app.ui.pets.PetTopBar
import mx.petmate.app.ui.pets.VSpace
import mx.petmate.app.ui.theme.petMateColors

/* Afiliar negocio en tres pasos: datos, servicios y ubicacion (SCREENS.affiliate, affServices y affPin). */

@Composable
fun AffiliateScreen(
    viewModel: AffiliateViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val handle = remember { OsmMapHandle() }
    val requestLocation = rememberLocationRequest(viewModel::onLocation)
    LaunchedEffect(Unit) { requestLocation(true) }
    // Al terminar, la sesion ya trae el negocio: se entra al modo negocio y el grafo cambia solo.
    LaunchedEffect(state.done) { if (state.done) viewModel.enterBusinessMode() }
    BackHandler { if (!viewModel.back()) onBackClick() }

    val (title, subtitle) = when (state.step) {
        1 -> "Afilia tu negocio" to "Paso 1 de 3: tus datos"
        2 -> "Tus servicios" to "Paso 2 de 3: lo que ofreces"
        else -> "¿Dónde está tu negocio?" to "Paso 3 de 3: ubicación"
    }
    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = { PetTopBar(title = title, subtitle = subtitle, onBackClick = { if (!viewModel.back()) onBackClick() }) },
        bottom = {
            PetBottomAction {
                Column {
                    state.error?.let { ErrorNotice(it, modifier = Modifier.padding(bottom = 10.dp)) }
                    when (state.step) {
                        1 -> PetMateButton("Siguiente: tus servicios", viewModel::next, icon = R.drawable.ic_pm_next, modifier = Modifier.fillMaxWidth())
                        2 -> PetMateButton("Siguiente: ubicación", viewModel::next, icon = R.drawable.ic_pm_pin, modifier = Modifier.fillMaxWidth())
                        else -> PetMateButton(
                            "Confirmar y terminar",
                            onClick = {
                                val (lat, lng) = handle.center() ?: (state.latitude to state.longitude)
                                viewModel.finish(lat, lng)
                            },
                            icon = R.drawable.ic_pm_check,
                            loading = state.saving,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    ) {
        when (state.step) {
            1 -> BusinessDataForm(
                form = state.form,
                onForm = viewModel::onForm,
                onKind = viewModel::onKind,
                onToggleDay = viewModel::toggleDay,
                editing = false,
            )
            2 -> ServicesStep(state, viewModel)
            else -> PinPicker(
                latitude = state.latitude,
                longitude = state.longitude,
                address = state.form.address,
                handle = handle,
                hint = "Mueve el mapa hasta que el pin quede en la puerta de tu negocio. Así te encontrarán más fácil.",
            )
        }
    }
}

/** Formulario de datos (paso 1 y "Mi negocio"). Con [editing] no se cambian tipo, direccion ni cedula (pm-109). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessDataForm(
    form: BusinessForm,
    onForm: ((BusinessForm) -> BusinessForm) -> Unit,
    onToggleDay: (String) -> Unit,
    editing: Boolean,
    modifier: Modifier = Modifier,
    onKind: (BusinessKind) -> Unit = {},
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    val extra = MaterialTheme.petMateColors
    val err = { key: String -> form.errors[key]?.let { UiText.Dynamic(it) } }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        header()
        if (!editing) {
            PetFieldLabel("¿Qué ofreces?", required = true)
            Text("Elige uno. Para otro tipo, afilia otro negocio con otra cuenta.", color = extra.ink2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BusinessKind.entries.forEach { kind -> KindOption(kind, selected = form.kind == kind, onClick = { onKind(kind) }, modifier = Modifier.weight(1f)) }
            }
            form.errors["kind"]?.let { Text(it, color = extra.alert, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
            VSpace(14.dp)
        }
        PetMateTextField(form.name, { v -> onForm { it.copy(name = v.take(80)) } }, "Nombre del negocio", placeholder = "Ej. Veterinaria Los Ángeles", leadingIcon = R.drawable.ic_pm_store, required = true, error = err("name"))
        PetMateTextField(form.phone, { v -> onForm { it.copy(phone = v.take(13)) } }, "Teléfono", placeholder = "10 dígitos", leadingIcon = R.drawable.ic_pm_phone, required = true, keyboardType = KeyboardType.Phone, error = err("phone"))
        PetMateTextField(form.whatsApp, { v -> onForm { it.copy(whatsApp = v.take(13)) } }, "WhatsApp", placeholder = "Déjalo vacío si no tienes", leadingIcon = R.drawable.ic_pm_chat, keyboardType = KeyboardType.Phone, error = err("whatsApp"))
        if (!editing) {
            PetMateTextField(form.address, { v -> onForm { it.copy(address = v.take(200)) } }, "Dirección", placeholder = "Calle, número y colonia", leadingIcon = R.drawable.ic_pm_pin, required = true, error = err("address"))
        }
        PetMateTextField(form.description, { v -> onForm { it.copy(description = v.take(500)) } }, "Descripción", placeholder = "Opcional: qué te hace especial")
        if (!editing && form.kind == BusinessKind.Veterinary) {
            PetMateTextField(
                form.licenseNumber,
                { v -> onForm { it.copy(licenseNumber = v.take(20)) } },
                "Cédula profesional del veterinario",
                placeholder = "7 u 8 números",
                leadingIcon = R.drawable.ic_pm_file,
                required = true,
                keyboardType = KeyboardType.Number,
                hint = "Solo las veterinarias pueden registrar consultas y vacunas.",
                error = err("license"),
            )
        }
        PetFieldLabel("Horario")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ORDERED_DAYS.forEach { day -> SelectChip(DAY_LABELS.getValue(day), selected = day in form.days, onClick = { onToggleDay(day) }) }
        }
        VSpace(10.dp)
        if (form.days.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PetMateTextField(form.open, { v -> onForm { it.copy(open = v.take(5)) } }, "Abre", placeholder = "09:00", leadingIcon = R.drawable.ic_pm_calendar, modifier = Modifier.weight(1f), bottomSpacing = 4.dp)
                PetMateTextField(form.close, { v -> onForm { it.copy(close = v.take(5)) } }, "Cierra", placeholder = "18:00", modifier = Modifier.weight(1f), bottomSpacing = 4.dp)
            }
            Text(
                form.errors["hours"] ?: "Formato 24 h. Para cerrar a medianoche escribe 23:59.",
                color = if (form.errors["hours"] != null) extra.alert else extra.ink2,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        } else {
            Text("Sin días marcados el negocio aparece como cerrado.", color = extra.ink2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 14.dp))
        }
        if (!editing) {
            TipBox("La foto del local llega pronto: mientras tanto se muestra una imagen de tu tipo de negocio.", modifier = Modifier.padding(bottom = 24.dp))
        }
        footer()
    }
}

@Composable
private fun KindOption(kind: BusinessKind, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = kind.style()
    val extra = MaterialTheme.petMateColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) s.soft else MaterialTheme.colorScheme.surface)
            .border(2.dp, if (selected) s.ink else extra.line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(Modifier.size(40.dp).background(s.soft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(s.icon), contentDescription = null, tint = s.ink, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.heightIn(min = 6.dp))
        Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selected) s.ink else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ServicesStep(state: AffiliateUiState, viewModel: AffiliateViewModel) {
    val extra = MaterialTheme.petMateColors
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        TipBox("Elige al menos uno. Así aparecen en tu perfil del mapa. Sin precio dirá «Pregunta el precio».")
        state.form.kind?.let { PetSectionTitle(it.style().name) }
        state.services.forEachIndexed { index, s ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, if (s.selected) MaterialTheme.colorScheme.primary else extra.line, RoundedCornerShape(16.dp))
                    .clickable { viewModel.toggleService(index) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Box(
                    Modifier.size(26.dp).background(if (s.selected) MaterialTheme.colorScheme.primary else extra.surface2, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (s.selected) Icon(painterResource(R.drawable.ic_pm_check), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(s.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                PetMateTextField(
                    s.price,
                    { viewModel.onServicePrice(index, it.take(10)) },
                    label = "",
                    placeholder = "$",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.width(110.dp),
                    bottomSpacing = 0.dp,
                )
            }
        }
        state.servicesError?.let { Text(it, color = extra.alert, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        PetSectionTitle("Agregar otro")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetMateTextField(state.newServiceName, { viewModel.onNewService(name = it.take(80)) }, "Servicio", placeholder = "Ej. Hotel canino", modifier = Modifier.weight(1f))
            PetMateTextField(state.newServicePrice, { viewModel.onNewService(price = it.take(10)) }, "Precio", placeholder = "$", keyboardType = KeyboardType.Decimal, modifier = Modifier.width(110.dp))
        }
        PetMateButton("Agregar servicio", viewModel::addService, style = PetMateButtonStyle.Soft, icon = R.drawable.ic_pm_plus, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))
    }
}

/** Mapa con un pin fijo al centro: el usuario mueve el mapa (affPin). */
@Composable
fun PinPicker(
    latitude: Double,
    longitude: Double,
    address: String,
    handle: OsmMapHandle,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val extra = MaterialTheme.petMateColors
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        TipBox(hint, icon = R.drawable.ic_pm_pin, soft = extra.sunSoft, ink = extra.sunInk)
        VSpace(12.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, extra.line, RoundedCornerShape(24.dp)),
        ) {
            OsmMap(
                businesses = emptyList(),
                selectedBusinessId = null,
                showUserLocation = false,
                onBusinessClick = {},
                centerLatitude = latitude,
                centerLongitude = longitude,
                handle = handle,
            )
            // Pin al centro: la punta queda exactamente en el centro del mapa.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-22).dp)
                    .size(44.dp)
                    .shadow(6.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            ) {
                Icon(painterResource(R.drawable.ic_pm_store), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
            }
            Box(Modifier.align(Alignment.Center).size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
        if (address.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                Icon(painterResource(R.drawable.ic_pm_pin), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(address, fontWeight = FontWeight.Bold)
            }
        } else {
            VSpace(12.dp)
        }
    }
}
