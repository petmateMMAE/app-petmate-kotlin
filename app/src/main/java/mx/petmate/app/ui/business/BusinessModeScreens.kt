package mx.petmate.app.ui.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import mx.petmate.app.R
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.ScanFicha
import mx.petmate.app.ui.components.ErrorNotice
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.map.OsmMapHandle
import mx.petmate.app.ui.pets.PetBottomAction
import mx.petmate.app.ui.pets.PetConfirmDialog
import mx.petmate.app.ui.pets.PetEmptyState
import mx.petmate.app.ui.pets.PetLoading
import mx.petmate.app.ui.pets.PetScaffoldColumn
import mx.petmate.app.ui.pets.PetSectionTitle
import mx.petmate.app.ui.pets.PetTopBar
import mx.petmate.app.ui.pets.VSpace
import mx.petmate.app.ui.pets.formatDate
import mx.petmate.app.ui.theme.petMateColors

/* Modo negocio: panel, mi negocio, servicios, escanear, ficha y atendidas (PetMate_App.html). */

// ---------- Panel ----------

@Composable
fun BusinessPanelScreen(
    viewModel: MyBusinessViewModel,
    onEditClick: () -> Unit,
    onServicesClick: () -> Unit,
    onScanClick: () -> Unit,
    onAttendedClick: () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.reload() }
    val extra = MaterialTheme.petMateColors
    val b = state.business
    val kind = b?.kind ?: state.kind ?: BusinessKind.Store

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Encabezado con la foto y el nombre, como el panel de la maqueta.
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                BusinessPhoto(kind, modifier = Modifier.fillMaxSize(), radius = 0.dp)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp).statusBarsPadding()) {
                    Text("Mi negocio", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        b?.name ?: "Tu negocio",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        KindBadge(kind)
                        b?.let { Text(scheduleSummary(it.schedule), color = Color.White, fontSize = 13.sp, maxLines = 1) }
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                VSpace(16.dp)
                if (b == null) {
                    TipBox(
                        "Este teléfono aún no tiene los datos de tu negocio. Toca «Mi negocio» y guárdalos para verlos aquí.",
                        icon = R.drawable.ic_pm_info,
                        soft = extra.sunSoft,
                        ink = extra.sunInk,
                    )
                    VSpace(12.dp)
                }
                // Escanear es la accion principal (.scan-big).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(extra.sun, RoundedCornerShape(28.dp))
                        .clickable(onClick = onScanClick)
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                ) {
                    Box(Modifier.size(60.dp).background(extra.onSun, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_pm_qr), contentDescription = null, tint = extra.sun, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Escanear QR de mascota", color = extra.onSun, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp))
                        Text(
                            if (kind == BusinessKind.Veterinary) "Ve su historial y alergias" else "Ve sus datos básicos y alergias",
                            color = extra.onSun,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                PetSectionTitle("Accesos")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccessRow(R.drawable.ic_pm_store, "Mi negocio", "Horario, teléfonos y ubicación", onEditClick)
                    AccessRow(
                        R.drawable.ic_pm_bag,
                        "Servicios",
                        if (state.loadingServices) "Cargando…" else "${state.services.size} publicados",
                        onServicesClick,
                        tint = extra.shop,
                        soft = extra.shopSoft,
                    )
                    AccessRow(R.drawable.ic_pm_paw, "Mascotas atendidas", "Las que escaneaste hoy y esta semana", onAttendedClick, tint = extra.spa, soft = extra.spaSoft)
                }
                VSpace(24.dp)
            }
        }
        bottomBar()
    }
}

// ---------- Mi negocio (bizEdit) ----------

@Composable
fun BizEditScreen(
    viewModel: MyBusinessViewModel,
    onBackClick: () -> Unit,
    onEditLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val extra = MaterialTheme.petMateColors
    val kind = state.form.kind ?: state.kind ?: BusinessKind.Store

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = { PetTopBar(title = "Mi negocio", subtitle = "Edita cómo te ven en el mapa", onBackClick = onBackClick) },
        bottom = {
            PetBottomAction {
                Column {
                    state.error?.let { ErrorNotice(it, modifier = Modifier.padding(bottom = 10.dp)) }
                    PetMateButton("Guardar cambios", { viewModel.save(onBackClick) }, icon = R.drawable.ic_pm_check, loading = state.saving, modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) {
        BusinessDataForm(
            form = state.form,
            onForm = viewModel::onForm,
            onToggleDay = viewModel::toggleDay,
            editing = true,
            header = {
                // Vista previa en el mapa.
                BizCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BusinessPhoto(kind, size = 64.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            KindBadge(kind)
                            Text(state.form.name.ifBlank { "Nombre del negocio" }, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 4.dp))
                            Text(scheduleSummary(state.form.schedule()), color = extra.ink2, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            },
            footer = {
                PetMateButton(
                    "Cambiar ubicación en el mapa",
                    onEditLocationClick,
                    style = PetMateButtonStyle.Outline,
                    icon = R.drawable.ic_pm_pin,
                    modifier = Modifier.fillMaxWidth(),
                )
                PetSectionTitle("Zona de cuidado")
                PetMateButton(
                    "Dar de baja mi negocio",
                    { confirmDelete = true },
                    style = PetMateButtonStyle.Ghost,
                    icon = R.drawable.ic_pm_trash,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                )
            },
        )
    }
    if (confirmDelete) {
        PetConfirmDialog(
            title = "¿Dar de baja tu negocio?",
            text = "Se quita del mapa junto con sus servicios. Tu cuenta y tus mascotas siguen igual.",
            confirm = "Dar de baja",
            danger = true,
            icon = R.drawable.ic_pm_trash,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun EditLocationScreen(viewModel: MyBusinessViewModel, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val handle = remember { OsmMapHandle() }
    val b = state.business
    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = { PetTopBar(title = "Cambiar ubicación", subtitle = "Mueve el mapa", onBackClick = onBackClick) },
        bottom = {
            PetBottomAction {
                Column {
                    state.error?.let { ErrorNotice(it, modifier = Modifier.padding(bottom = 10.dp)) }
                    PetMateButton(
                        "Guardar ubicación",
                        onClick = {
                            val (lat, lng) = handle.center() ?: return@PetMateButton
                            viewModel.saveLocation(lat, lng, onBackClick)
                        },
                        icon = R.drawable.ic_pm_check,
                        loading = state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        PinPicker(
            latitude = b?.latitude ?: mx.petmate.app.ui.map.MapViewModel.DEFAULT_CENTER.first,
            longitude = b?.longitude ?: mx.petmate.app.ui.map.MapViewModel.DEFAULT_CENTER.second,
            address = b?.address.orEmpty(),
            handle = handle,
            hint = "Mueve el mapa hasta que el pin quede en la puerta de tu negocio.",
        )
    }
}

// ---------- Servicios ----------

@Composable
fun ServicesScreen(
    viewModel: MyBusinessViewModel,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadServices() }
    val extra = MaterialTheme.petMateColors
    var toDelete by remember { mutableStateOf<String?>(null) }
    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = { PetTopBar(title = "Servicios", subtitle = state.business?.name, onBackClick = onBackClick) },
        bottom = { PetBottomAction { PetMateButton("Agregar servicio", onAddClick, icon = R.drawable.ic_pm_plus, modifier = Modifier.fillMaxWidth()) } },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text("Así aparecen en tu perfil del mapa.", color = extra.ink2, modifier = Modifier.padding(bottom = 12.dp))
            state.error?.let { ErrorNotice(it, modifier = Modifier.padding(bottom = 10.dp)) }
            when {
                state.loadingServices -> PetLoading()
                state.services.isEmpty() -> PetEmptyState(R.drawable.ic_pm_bag, "Sin servicios", "Agrega lo que ofreces para que aparezca en el mapa.")
                else -> state.services.forEach { s ->
                    BizCard(modifier = Modifier.padding(bottom = 10.dp), onClick = { onEditClick(s.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                s.description?.let { Text(it, color = extra.ink2, fontSize = 13.sp, maxLines = 2) }
                                Text(priceText(s.price), color = if (s.price != null) MaterialTheme.colorScheme.primary else extra.ink3, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Icon(
                                painterResource(R.drawable.ic_pm_trash),
                                contentDescription = "Eliminar ${s.name}",
                                tint = extra.alert,
                                modifier = Modifier.size(40.dp).clickable { toDelete = s.id }.padding(10.dp),
                            )
                        }
                    }
                }
            }
            VSpace(24.dp)
        }
    }
    toDelete?.let { id ->
        PetConfirmDialog(
            title = "¿Eliminar servicio?",
            text = "Deja de aparecer en tu perfil del mapa.",
            confirm = "Eliminar",
            danger = true,
            icon = R.drawable.ic_pm_trash,
            onConfirm = {
                toDelete = null
                viewModel.deleteService(id)
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
fun ServiceFormScreen(viewModel: MyBusinessViewModel, serviceId: String?, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val existing = state.services.firstOrNull { it.id == serviceId }
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var price by rememberSaveable(existing?.id) { mutableStateOf(existing?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    LaunchedEffect(Unit) { viewModel.clearError() }
    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = { PetTopBar(title = if (serviceId == null) "Nuevo servicio" else "Editar servicio", onBackClick = onBackClick) },
        bottom = {
            PetBottomAction {
                Column {
                    state.error?.let { ErrorNotice(it, modifier = Modifier.padding(bottom = 10.dp)) }
                    PetMateButton(
                        "Guardar servicio",
                        { viewModel.saveService(serviceId, name, description, price, onBackClick) },
                        icon = R.drawable.ic_pm_check,
                        loading = state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            PetMateTextField(name, { name = it.take(80) }, "Nombre del servicio", placeholder = "Ej. Baño y corte", required = true)
            PetMateTextField(description, { description = it.take(300) }, "Descripción", placeholder = "Opcional")
            PetMateTextField(
                price,
                { price = it.take(12) },
                "Precio",
                placeholder = "Ej. 300",
                keyboardType = KeyboardType.Decimal,
                hint = "Si lo dejas vacío dirá «Pregunta el precio».",
            )
        }
    }
}

// ---------- Escanear y ficha ----------

@Composable
fun ScannerScreen(viewModel: ScanViewModel, bottomBar: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ficha = state.ficha
    if (ficha != null) {
        FichaScreen(ficha = ficha, isVet = state.isVet, onClose = viewModel::closeFicha, modifier = modifier)
        return
    }
    val extra = MaterialTheme.petMateColors
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result -> result.contents?.let { viewModel.resolve(it) } }
    val openCamera = {
        launcher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Pon el QR de la mascota dentro del cuadro")
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }
    Column(modifier = modifier.fillMaxSize().background(Color(0xFF15131C))) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        ) {
            VSpace(28.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .border(4.dp, extra.sun, RoundedCornerShape(32.dp))
                    .clickable(onClick = openCamera),
            ) {
                Icon(painterResource(R.drawable.ic_pm_qr), contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(96.dp))
            }
            Text(
                "Escanea el QR de la mascota",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
            )
            Text(
                "Solo verás su ficha si el dueño la compartió con tu negocio.",
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 18.dp),
            )
            PetMateButton("Abrir cámara", openCamera, icon = R.drawable.ic_pm_qr, loading = state.resolving, modifier = Modifier.fillMaxWidth())
            VSpace(18.dp)
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(14.dp)) {
                Column {
                    PetMateTextField(state.code, viewModel::onCode, "O escribe el código", placeholder = "8 caracteres, por ejemplo K7Q2M9XA")
                    PetMateButton("Ver ficha", { viewModel.resolve() }, style = PetMateButtonStyle.Outline, enabled = !state.resolving, modifier = Modifier.fillMaxWidth())
                }
            }
            state.error?.let { ErrorNotice(it, modifier = Modifier.padding(top = 12.dp)) }
            VSpace(24.dp)
        }
        bottomBar()
    }
}

@Composable
fun FichaScreen(ficha: ScanFicha, isVet: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    androidx.activity.compose.BackHandler(onBack = onClose)
    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = { PetTopBar(title = "Ficha de ${ficha.name}", subtitle = "Solo lectura", onBackClick = onClose) },
        bottom = { PetBottomAction { PetMateButton("Terminar y salir", onClose, style = PetMateButtonStyle.Ghost, icon = R.drawable.ic_pm_logout, modifier = Modifier.fillMaxWidth()) } },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            BizCard {
                Column {
                    Text(ficha.name, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold))
                    val age = listOfNotNull(ficha.ageYears?.takeIf { it > 0 }?.let { "$it ${if (it == 1) "año" else "años"}" }, ficha.ageMonths?.takeIf { it > 0 }?.let { "$it ${if (it == 1) "mes" else "meses"}" })
                    Text(listOfNotNull(ficha.species, ficha.breed, age.joinToString(" ").ifBlank { null }).joinToString(" · "), color = extra.ink2)
                }
            }
            VSpace(12.dp)
            if (ficha.allergies.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().background(extra.alertSoft, RoundedCornerShape(20.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_pm_alert), contentDescription = null, tint = extra.alert, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("¡Cuidado! Es alérgico a:", color = extra.alert, fontWeight = FontWeight.ExtraBold)
                    }
                    ficha.allergies.forEach { a ->
                        Text("• ${a.name}" + (a.notes?.let { " — $it" } ?: ""), color = extra.alert, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                TipBox("Sin alergias registradas.", icon = R.drawable.ic_pm_shield, soft = extra.okSoft, ink = extra.ok)
            }
            if (!isVet) {
                VSpace(12.dp)
                TipBox("Como estética o tienda solo ves datos básicos y alergias. Vacunas, consultas y recetas son solo para veterinarias.")
            } else {
                FichaSection("Vacunas", ficha.vaccines, empty = "Sin vacunas registradas") { v ->
                    FichaRow(v.name, "${formatDate(v.appliedOn)}" + (v.appliedBy?.let { " · $it" } ?: ""), v.nextDoseOn?.let { "Próx. ${formatDate(it)}" })
                }
                FichaSection("Desparasitaciones", ficha.dewormings, empty = "Sin desparasitaciones") { d ->
                    FichaRow(d.product, formatDate(d.appliedOn), d.nextOn?.let { "Próx. ${formatDate(it)}" })
                }
                FichaSection("Peso", ficha.weights, empty = "Sin registros de peso") { w ->
                    FichaRow("${w.kilograms} kg", formatDate(w.measuredOn), null)
                }
                FichaSection("Consultas anteriores", ficha.consultations, empty = "Sin consultas") { c ->
                    FichaRow(c.reason, formatDate(c.date) + " · " + c.diagnosis, c.medications.takeIf { it.isNotEmpty() }?.let { "Receta" })
                }
            }
            VSpace(24.dp)
        }
    }
}

/** Seccion de la ficha; null = el dueno no la compartio. */
@Composable
private fun <T> FichaSection(title: String, items: List<T>?, empty: String, row: @Composable (T) -> Unit) {
    PetSectionTitle(title)
    when {
        items == null -> Text("El dueño no compartió esta parte.", color = MaterialTheme.petMateColors.ink3)
        items.isEmpty() -> Text(empty, color = MaterialTheme.petMateColors.ink3)
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { items.forEach { row(it) } }
    }
}

@Composable
private fun FichaRow(title: String, subtitle: String, badge: String?) {
    val extra = MaterialTheme.petMateColors
    BizCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = extra.ink2, fontSize = 13.sp)
            }
            badge?.let {
                Text(it, color = extra.brandText, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.background(extra.brandSoft, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

// ---------- Atendidas ----------

@Composable
fun AttendedScreen(viewModel: AttendedViewModel, bottomBar: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extra = MaterialTheme.petMateColors
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        PetTopBar(title = "Mascotas atendidas", subtitle = "De la más reciente a la más antigua", onBackClick = null)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("Hoy", state.period == "day", { viewModel.load("day") })
                SelectChip("Esta semana", state.period == "week", { viewModel.load("week") })
            }
            VSpace(12.dp)
            TipBox("Para volver a ver su ficha necesitas escanear su QR otra vez. Así cuidamos los datos del dueño.", icon = R.drawable.ic_pm_lock)
            VSpace(12.dp)
            when {
                state.loading -> PetLoading()
                state.error != null -> ErrorNotice(state.error!!)
                state.items.isEmpty() -> PetEmptyState(R.drawable.ic_pm_paw, "Nadie por aquí", if (state.period == "day") "Hoy aún no escaneas ninguna mascota." else "Esta semana no escaneaste mascotas.")
                else -> state.items.forEach { a ->
                    BizCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).background(extra.spaSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_pm_paw), contentDescription = null, tint = extra.spa, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(a.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(scannedText(a.scannedAt), color = extra.ink2, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            VSpace(24.dp)
        }
        bottomBar()
    }
}

/** "2026-11-11T18:20:00Z" en hora local: "11/11/2026 · 12:20". */
internal fun scannedText(iso: String): String = runCatching {
    // minSdk 24 sin desugaring: java.time no esta disponible, se usa SimpleDateFormat.
    val clean = iso.substringBefore('.').removeSuffix("Z")
    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    val date = requireNotNull(parser.parse(clean))
    java.text.SimpleDateFormat("dd/MM/yyyy · HH:mm", java.util.Locale.US).format(date)
}.getOrDefault(iso)
