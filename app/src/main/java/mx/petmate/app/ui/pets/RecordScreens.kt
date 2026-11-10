package mx.petmate.app.ui.pets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.model.WeightRecord
import mx.petmate.app.domain.usecase.PetDates
import mx.petmate.app.domain.usecase.PetRules
import mx.petmate.app.ui.components.DemoStrip
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.theme.petMateColors

/** Estructura comun de las listas del carnet: barra, franja demo, contenido y boton al pie. */
@Composable
private fun RecordListScaffold(
    title: String,
    petName: String,
    isGuest: Boolean,
    onBackClick: () -> Unit,
    onNeedAccount: () -> Unit,
    bottom: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    PetScaffoldColumn(
        modifier = Modifier.fillMaxSize(),
        top = {
            if (isGuest) DemoStrip(onCreateAccountClick = onNeedAccount)
            PetTopBar(title = title, subtitle = petName, onBackClick = onBackClick)
        },
        bottom = { PetBottomAction { bottom() } },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            content()
            Box(Modifier.height(16.dp))
        }
    }
}

/** .rec: tarjeta de un registro del carnet con quien lo registro. */
@Composable
private fun RecordCard(
    title: String,
    subtitle: String,
    meta: List<Pair<String, String>>,
    next: String?,
    origin: RecordOrigin,
    onDelete: (() -> Unit)?,
) {
    val extra = MaterialTheme.petMateColors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .shadow(2.dp, shape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(subtitle, color = extra.ink2, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            if (next != null) {
                val days = PetDates.daysBetween(PetDates.today(), next) ?: 99
                PetMateBadge(
                    stringResource(R.string.record_next, formatDate(next)),
                    if (days in 0..30) extra.sunSoft else extra.surface2,
                    if (days in 0..30) extra.sunInk else extra.ink2,
                    icon = R.drawable.ic_pm_calendar,
                )
            }
        }
        if (meta.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                meta.forEach { (k, v) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(k, color = extra.ink2, fontSize = 12.sp)
                        Text(v, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
        // .by: separador punteado y origen del registro
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp).background(extra.line))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            if (origin.byBusiness) {
                PetMateBadge(origin.businessName ?: stringResource(R.string.record_by_business), extra.vetSoft, extra.vet, icon = R.drawable.ic_pm_cross)
                Box(Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_pm_lock), contentDescription = null, tint = extra.ink2, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.record_locked), fontSize = 13.sp, color = extra.ink2)
            } else {
                PetMateBadge(stringResource(R.string.record_me), extra.brandSoft, extra.brandText, icon = R.drawable.ic_pm_user)
                Text(stringResource(R.string.record_by_you), fontSize = 13.sp, color = extra.ink2, modifier = Modifier.weight(1f))
                if (onDelete != null) {
                    Text(
                        stringResource(R.string.record_delete),
                        color = extra.alert,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onDelete).padding(4.dp),
                    )
                }
            }
        }
    }
}

// =====================================================================================================
// Vacunas (SCREENS.vaccines y vaccineForm; pm-097, pm-098)
// =====================================================================================================

@Composable
fun VaccinesScreen(
    viewModel: PetCarnetViewModel,
    isGuest: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onNeedAccount: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    ShowMessages(state.message, viewModel::messageShown)
    var toDelete by rememberSaveable { mutableStateOf<String?>(null) }

    RecordListScaffold(
        title = stringResource(R.string.vaccines_title),
        petName = state.pet?.name.orEmpty(),
        isGuest = isGuest,
        onBackClick = onBackClick,
        onNeedAccount = onNeedAccount,
        bottom = {
            PetMateButton(stringResource(R.string.vaccines_add), if (isGuest) onNeedAccount else onAddClick, icon = R.drawable.ic_pm_plus, modifier = Modifier.fillMaxWidth())
        },
    ) {
        val list = state.carnet.vaccines
        when {
            state.isLoading -> PetLoading()
            list.isEmpty() -> PetEmptyState(R.drawable.ic_pm_syringe, stringResource(R.string.vaccines_empty_title), stringResource(R.string.vaccines_empty_text))
            else -> list.forEach { v ->
                RecordCard(
                    title = v.name,
                    subtitle = stringResource(R.string.record_applied_on, formatDate(v.appliedOn)),
                    meta = listOf(
                        stringResource(R.string.vaccine_batch) to (v.batch ?: "—"),
                        stringResource(R.string.vaccine_next) to (v.nextDoseOn?.let { whenText(it) } ?: "—"),
                    ),
                    next = v.nextDoseOn,
                    origin = v.origin,
                    onDelete = if (isGuest) null else ({ toDelete = v.id }),
                )
            }
        }
    }
    toDelete?.let { id ->
        PetConfirmDialog(
            title = stringResource(R.string.record_delete_title),
            text = stringResource(R.string.record_delete_text),
            confirm = stringResource(R.string.record_delete),
            danger = true,
            icon = R.drawable.ic_pm_trash,
            onConfirm = {
                toDelete = null
                viewModel.deleteVaccine(id)
            },
            onDismiss = { toDelete = null },
        )
    }
}

/** Nombres sugeridos de la maqueta; "Otra" deja escribir el nombre. */
private val VACCINE_NAMES = listOf("Rabia", "Múltiple", "Bordetella", "Triple felina")

@Composable
fun VaccineFormScreen(viewModel: PetCarnetViewModel, onBackClick: () -> Unit, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (state.pet == null) viewModel.refresh() }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    ShowMessages(state.message, viewModel::messageShown)
    val other = stringResource(R.string.vaccine_other)
    var choice by rememberSaveable { mutableStateOf<String?>(null) }
    var otherName by rememberSaveable { mutableStateOf("") }
    var applied by rememberSaveable { mutableStateOf<String?>(PetDates.today()) }
    var batch by rememberSaveable { mutableStateOf("") }
    var by by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf<String?>(null) }
    val errors = state.formErrors

    FormScaffold(
        title = stringResource(R.string.vaccine_form_title),
        petName = state.pet?.name.orEmpty(),
        onBackClick = onBackClick,
        saveLabel = stringResource(R.string.vaccine_save),
        saving = state.isBusy,
        onSave = {
            val name = if (choice == other) otherName else choice.orEmpty()
            viewModel.saveVaccine(VaccineDraft(name, applied ?: PetDates.today(), batch, by, next))
        },
    ) {
        PetFieldLabel(stringResource(R.string.vaccine_which), required = true)
        ChipGroup(options = VACCINE_NAMES + other, selected = choice, onSelect = {
            choice = it
            viewModel.clearFieldError(PetRules.Field.Name)
        })
        errors[PetRules.Field.Name]?.let { FieldErrorText(it.asString()) }
        VSpace(14.dp)
        if (choice == other) {
            PetMateTextField(otherName, { otherName = it }, stringResource(R.string.vaccine_other_name), placeholder = stringResource(R.string.vaccine_other_placeholder))
        }
        PetDateField(stringResource(R.string.vaccine_applied_on), applied, { applied = it; viewModel.clearFieldError(PetRules.Field.AppliedOn) }, required = true, error = errors[PetRules.Field.AppliedOn])
        PetMateTextField(batch, { batch = it }, stringResource(R.string.vaccine_batch), placeholder = stringResource(R.string.vaccine_batch_placeholder), hint = stringResource(R.string.pets_optional))
        PetMateTextField(by, { by = it }, stringResource(R.string.vaccine_by), placeholder = stringResource(R.string.vaccine_by_placeholder))
        PetDateField(stringResource(R.string.vaccine_next), next, { next = it; viewModel.clearFieldError(PetRules.Field.NextDate) }, hint = stringResource(R.string.record_next_hint), error = errors[PetRules.Field.NextDate])
    }
}

// =====================================================================================================
// Desparasitacion (SCREENS.deworm y dewormForm; pm-099, pm-100)
// =====================================================================================================

@Composable
fun DewormScreen(
    viewModel: PetCarnetViewModel,
    isGuest: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onNeedAccount: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    ShowMessages(state.message, viewModel::messageShown)
    var toDelete by rememberSaveable { mutableStateOf<String?>(null) }

    RecordListScaffold(
        title = stringResource(R.string.deworm_title),
        petName = state.pet?.name.orEmpty(),
        isGuest = isGuest,
        onBackClick = onBackClick,
        onNeedAccount = onNeedAccount,
        bottom = {
            PetMateButton(stringResource(R.string.deworm_add), if (isGuest) onNeedAccount else onAddClick, icon = R.drawable.ic_pm_plus, modifier = Modifier.fillMaxWidth())
        },
    ) {
        val list = state.carnet.dewormings
        when {
            state.isLoading -> PetLoading()
            list.isEmpty() -> PetEmptyState(R.drawable.ic_pm_pill, stringResource(R.string.deworm_empty_title), stringResource(R.string.deworm_empty_text))
            else -> list.forEach { d ->
                RecordCard(
                    title = d.product,
                    subtitle = stringResource(R.string.record_applied_on, formatDate(d.appliedOn)),
                    meta = listOf(
                        stringResource(R.string.deworm_type) to (d.type?.let { dewormTypeLabel(it) } ?: "—"),
                        stringResource(R.string.deworm_next) to (d.nextOn?.let { whenText(it) } ?: "—"),
                    ),
                    next = d.nextOn,
                    origin = d.origin,
                    onDelete = if (isGuest) null else ({ toDelete = d.id }),
                )
            }
        }
    }
    toDelete?.let { id ->
        PetConfirmDialog(
            title = stringResource(R.string.record_delete_title),
            text = stringResource(R.string.record_delete_text),
            confirm = stringResource(R.string.record_delete),
            danger = true,
            icon = R.drawable.ic_pm_trash,
            onConfirm = {
                toDelete = null
                viewModel.deleteDeworming(id)
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun dewormTypeLabel(type: DewormingType) =
    stringResource(if (type == DewormingType.Internal) R.string.deworm_internal else R.string.deworm_external)

@Composable
fun DewormFormScreen(viewModel: PetCarnetViewModel, onBackClick: () -> Unit, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (state.pet == null) viewModel.refresh() }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    ShowMessages(state.message, viewModel::messageShown)
    var type by rememberSaveable { mutableStateOf(DewormingType.Internal) }
    var product by rememberSaveable { mutableStateOf("") }
    var applied by rememberSaveable { mutableStateOf<String?>(PetDates.today()) }
    var by by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf<String?>(null) }
    val errors = state.formErrors

    FormScaffold(
        title = stringResource(R.string.deworm_form_title),
        petName = state.pet?.name.orEmpty(),
        onBackClick = onBackClick,
        saveLabel = stringResource(R.string.deworm_save),
        saving = state.isBusy,
        onSave = { viewModel.saveDeworming(DewormingDraft(product, applied ?: PetDates.today(), type, by, next)) },
    ) {
        PetFieldLabel(stringResource(R.string.deworm_type))
        PetSegmented(
            options = listOf(DewormingType.Internal to stringResource(R.string.deworm_internal), DewormingType.External to stringResource(R.string.deworm_external)),
            selected = type,
            onSelect = { type = it },
        )
        Text(stringResource(R.string.deworm_type_hint), fontSize = 13.sp, color = MaterialTheme.petMateColors.ink2, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
        PetMateTextField(
            product,
            { product = it; viewModel.clearFieldError(PetRules.Field.Product) },
            stringResource(R.string.deworm_product),
            placeholder = stringResource(R.string.deworm_product_placeholder),
            required = true,
            error = errors[PetRules.Field.Product],
        )
        PetDateField(stringResource(R.string.deworm_date), applied, { applied = it; viewModel.clearFieldError(PetRules.Field.AppliedOn) }, required = true, error = errors[PetRules.Field.AppliedOn])
        PetMateTextField(by, { by = it }, stringResource(R.string.vaccine_by), placeholder = stringResource(R.string.vaccine_by_placeholder))
        PetDateField(stringResource(R.string.deworm_next), next, { next = it; viewModel.clearFieldError(PetRules.Field.NextDate) }, hint = stringResource(R.string.record_next_hint), error = errors[PetRules.Field.NextDate])
    }
}

// =====================================================================================================
// Peso (SCREENS.weight; pm-101)
// =====================================================================================================

@Composable
fun WeightScreen(
    viewModel: PetCarnetViewModel,
    isGuest: Boolean,
    onBackClick: () -> Unit,
    onNeedAccount: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    ShowMessages(state.message, viewModel::messageShown)
    val extra = MaterialTheme.petMateColors
    val weights = state.carnet.weights.sortedByDescending { it.measuredOn }
    var kg by rememberSaveable { mutableDoubleStateOf(-1.0) }
    if (kg < 0 && !state.isLoading) kg = weights.firstOrNull()?.kilograms ?: 1.0

    PetScaffoldColumn(
        modifier = Modifier.fillMaxSize(),
        top = {
            if (isGuest) DemoStrip(onCreateAccountClick = onNeedAccount)
            PetTopBar(title = stringResource(R.string.weight_title), subtitle = state.pet?.name, onBackClick = onBackClick)
        },
    ) {
        if (state.isLoading) {
            PetLoading()
            return@PetScaffoldColumn
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            weights.firstOrNull()?.let { last ->
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)) {
                    Text(
                        stringResource(R.string.weight_kg, last.kilograms),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 46.sp),
                    )
                    weights.getOrNull(1)?.let { prev ->
                        val delta = last.kilograms - prev.kilograms
                        PetMateBadge(
                            stringResource(R.string.weight_delta, (if (delta >= 0) "+" else "") + "%.1f".format(delta), formatDate(prev.measuredOn).take(5)),
                            if (delta > 0) extra.sunSoft else extra.okSoft,
                            if (delta > 0) extra.sunInk else extra.ok,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                if (weights.size > 1) WeightChart(weights.reversed())
            } ?: PetEmptyState(R.drawable.ic_pm_weight, stringResource(R.string.weight_empty_title), stringResource(R.string.weight_empty_text))

            PetSectionTitle(stringResource(R.string.weight_today))
            // .stepper
            val shape = RoundedCornerShape(20.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, shape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
                    .background(MaterialTheme.colorScheme.surface, shape)
                    .padding(10.dp),
            ) {
                StepperButton(R.drawable.ic_pm_minus, stringResource(R.string.weight_less)) { kg = PetRules.roundKilograms((kg - 0.1).coerceAtLeast(0.1)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("%.1f".format(kg.coerceAtLeast(0.0)), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold))
                    Text(stringResource(R.string.weight_kilos), fontSize = 13.sp, color = extra.ink2, fontWeight = FontWeight.SemiBold)
                }
                StepperButton(R.drawable.ic_pm_plus, stringResource(R.string.weight_more)) { kg = PetRules.roundKilograms((kg + 0.1).coerceAtMost(PetRules.MAX_KILOGRAMS)) }
            }
            PetMateButton(
                text = stringResource(R.string.weight_save),
                onClick = { if (isGuest) onNeedAccount() else viewModel.saveWeight(kg) },
                icon = R.drawable.ic_pm_check,
                loading = state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            if (weights.isNotEmpty()) {
                PetSectionTitle(stringResource(R.string.weight_history))
                weights.forEachIndexed { i, w -> WeightRow(w, latest = i == 0) }
            }
            VSpace(24.dp)
        }
    }
}

@Composable
private fun StepperButton(icon: Int, description: String, onClick: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(extra.brandSoft)
            .clickable(onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
    }
}

/** .chart: linea del peso con area suave, de la mas antigua a la mas reciente. */
@Composable
private fun WeightChart(points: List<WeightRecord>) {
    val extra = MaterialTheme.petMateColors
    val brand = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = brand, spotColor = brand)
            .background(surface, shape)
            .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 8.dp),
    ) {
        val min = points.minOf { it.kilograms } - 0.5
        val max = points.maxOf { it.kilograms } + 0.5
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val w = size.width
            val h = size.height
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
            for (i in 0..2) {
                val y = 8f + i * (h - 16f) / 2
                drawLine(extra.line, Offset(0f, y), Offset(w, y), strokeWidth = 2f, pathEffect = dash)
            }
            val pts = points.mapIndexed { i, p ->
                val x = 16f + i * (w - 32f) / (points.size - 1).coerceAtLeast(1)
                val y = 8f + (h - 16f) * (1f - ((p.kilograms - min) / (max - min)).toFloat())
                Offset(x, y)
            }
            val area = Path().apply {
                moveTo(pts.first().x, h)
                pts.forEach { lineTo(it.x, it.y) }
                lineTo(pts.last().x, h)
                close()
            }
            drawPath(area, extra.brandSoft.copy(alpha = 0.7f))
            val line = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(line, brand, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            pts.forEachIndexed { i, p ->
                val last = i == pts.lastIndex
                drawCircle(if (last) brand else surface, radius = if (last) 12f else 8f, center = p)
                drawCircle(brand, radius = if (last) 12f else 8f, center = p, style = Stroke(width = 5f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { Text(formatDate(it.measuredOn).take(5), fontSize = 11.sp, color = extra.ink2) }
        }
    }
}

/** .list-item del historial de peso. */
@Composable
private fun WeightRow(record: WeightRecord, latest: Boolean) {
    val extra = MaterialTheme.petMateColors
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .shadow(2.dp, shape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(46.dp).background(extra.shopSoft, RoundedCornerShape(14.dp))) {
            Icon(painterResource(R.drawable.ic_pm_weight), contentDescription = null, tint = extra.shop, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.weight_kg, record.kilograms), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(formatDate(record.measuredOn), fontSize = 13.sp, color = extra.ink2)
        }
        if (latest) PetMateBadge(stringResource(R.string.weight_latest), extra.brandSoft, extra.brandText)
    }
}

// =====================================================================================================
// Alergias y condiciones (SCREENS.allergies y allergyForm; pm-102, pm-103)
// =====================================================================================================

@Composable
fun AllergiesScreen(
    viewModel: PetCarnetViewModel,
    isGuest: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onNeedAccount: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    ShowMessages(state.message, viewModel::messageShown)
    var activeTab by rememberSaveable { mutableStateOf(true) }
    val all = state.carnet.allergies
    val petName = state.pet?.name.orEmpty()

    RecordListScaffold(
        title = stringResource(R.string.allergies_title),
        petName = petName,
        isGuest = isGuest,
        onBackClick = onBackClick,
        onNeedAccount = onNeedAccount,
        bottom = {
            PetMateButton(stringResource(R.string.allergies_add), if (isGuest) onNeedAccount else onAddClick, icon = R.drawable.ic_pm_plus, modifier = Modifier.fillMaxWidth())
        },
    ) {
        PetSegmented(
            options = listOf(
                true to stringResource(R.string.allergies_tab_active, all.count { it.active }),
                false to stringResource(R.string.allergies_tab_resolved, all.count { !it.active }),
            ),
            selected = activeTab,
            onSelect = { activeTab = it },
            modifier = Modifier.padding(bottom = 14.dp),
        )
        val list = all.filter { it.active == activeTab }
        when {
            state.isLoading -> PetLoading()
            list.isEmpty() -> PetEmptyState(
                icon = R.drawable.ic_pm_check,
                title = stringResource(if (activeTab) R.string.allergies_empty_active_title else R.string.allergies_empty_resolved_title),
                text = if (activeTab) stringResource(R.string.allergies_empty_active_text, petName) else stringResource(R.string.allergies_empty_resolved_text),
                iconBackground = MaterialTheme.petMateColors.okSoft,
                iconTint = MaterialTheme.petMateColors.ok,
            )
            else -> list.forEach { a ->
                AllergyCard(
                    allergy = a,
                    onToggle = { if (isGuest) onNeedAccount() else viewModel.toggleAllergy(a) },
                    onEdit = { if (isGuest) onNeedAccount() else onEditClick(a.id) },
                )
            }
        }
    }
}

@Composable
private fun AllergyCard(allergy: Allergy, onToggle: () -> Unit, onEdit: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .shadow(2.dp, shape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .height(IntrinsicSize.Min),
    ) {
        if (allergy.active) Box(Modifier.width(6.dp).fillMaxHeight().background(extra.alert))
        Column(modifier = Modifier.weight(1f).padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(allergy.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    PetMateBadge(
                        allergyKindLabel(allergy.kind),
                        if (allergy.active) extra.alertSoft else extra.surface2,
                        if (allergy.active) extra.alert else extra.ink2,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                PetMateBadge(
                    stringResource(if (allergy.active) R.string.allergy_active else R.string.allergy_resolved),
                    if (allergy.active) extra.alertSoft else extra.okSoft,
                    if (allergy.active) extra.alert else extra.ok,
                    icon = if (allergy.active) R.drawable.ic_pm_alert else R.drawable.ic_pm_check,
                )
            }
            allergy.notes?.let { Text(it, color = extra.ink2, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp)) }
            if (allergy.origin.byBusiness) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Icon(painterResource(R.drawable.ic_pm_lock), contentDescription = null, tint = extra.ink2, modifier = Modifier.size(14.dp))
                    Text(stringResource(R.string.record_locked_by, allergy.origin.businessName ?: stringResource(R.string.record_by_business)), fontSize = 13.sp, color = extra.ink2)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    PetMateButton(
                        text = stringResource(if (allergy.active) R.string.allergy_mark_resolved else R.string.allergy_reactivate),
                        onClick = onToggle,
                        style = PetMateButtonStyle.Soft,
                        icon = if (allergy.active) R.drawable.ic_pm_check else R.drawable.ic_pm_refresh,
                        modifier = Modifier.weight(1f),
                    )
                    PetMateButton(
                        text = stringResource(R.string.allergy_edit),
                        onClick = onEdit,
                        style = PetMateButtonStyle.Ghost,
                        icon = R.drawable.ic_pm_edit,
                    )
                }
            }
        }
    }
}

@Composable
private fun allergyKindLabel(kind: AllergyKind) =
    stringResource(if (kind == AllergyKind.Allergy) R.string.allergy_kind_allergy else R.string.allergy_kind_condition)

@Composable
fun AllergyFormScreen(viewModel: PetCarnetViewModel, allergyId: String?, onBackClick: () -> Unit, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (state.pet == null) viewModel.refresh() }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    ShowMessages(state.message, viewModel::messageShown)
    val existing = allergyId?.let { id -> state.carnet.allergies.firstOrNull { it.id == id } }
    var loaded by rememberSaveable { mutableStateOf(allergyId == null) }
    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(AllergyKind.Allergy) }
    var notes by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }
    if (!loaded && existing != null) {
        name = existing.name
        kind = existing.kind
        notes = existing.notes.orEmpty()
        active = existing.active
        loaded = true
    }

    FormScaffold(
        title = stringResource(if (allergyId == null) R.string.allergy_form_new else R.string.allergy_form_edit),
        petName = state.pet?.name.orEmpty(),
        onBackClick = onBackClick,
        saveLabel = stringResource(R.string.allergy_save),
        saving = state.isBusy,
        onSave = { viewModel.saveAllergy(allergyId, AllergyDraft(name, kind, notes, active)) },
    ) {
        PetMateTextField(
            name,
            { name = it; viewModel.clearFieldError(PetRules.Field.Name) },
            stringResource(R.string.allergy_name),
            placeholder = stringResource(R.string.allergy_name_placeholder),
            required = true,
            error = state.formErrors[PetRules.Field.Name],
        )
        PetFieldLabel(stringResource(R.string.allergy_kind))
        PetSegmented(
            options = listOf(AllergyKind.Allergy to stringResource(R.string.allergy_kind_allergy), AllergyKind.Condition to stringResource(R.string.allergy_kind_condition)),
            selected = kind,
            onSelect = { kind = it },
            modifier = Modifier.padding(bottom = 14.dp),
        )
        PetMateTextField(notes, { notes = it }, stringResource(R.string.allergy_notes), placeholder = stringResource(R.string.allergy_notes_placeholder))
        PetFieldLabel(stringResource(R.string.allergy_state))
        PetSegmented(
            options = listOf(true to stringResource(R.string.allergy_active), false to stringResource(R.string.allergy_resolved)),
            selected = active,
            onSelect = { active = it },
        )
    }
}

// ---------- Piezas de formulario ----------

@Composable
private fun FormScaffold(
    title: String,
    petName: String,
    onBackClick: () -> Unit,
    saveLabel: String,
    saving: Boolean,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    PetScaffoldColumn(
        modifier = Modifier.fillMaxSize(),
        top = { PetTopBar(title = title, subtitle = petName, onBackClick = onBackClick) },
        bottom = {
            PetBottomAction {
                PetMateButton(saveLabel, onSave, icon = R.drawable.ic_pm_check, loading = saving, modifier = Modifier.fillMaxWidth())
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            content()
            VSpace(24.dp)
        }
    }
}

/** .chip seleccionable (lista de vacunas). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    val extra = MaterialTheme.petMateColors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Text(
                option,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .border(2.dp, if (on) MaterialTheme.colorScheme.primary else extra.line, RoundedCornerShape(999.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun FieldErrorText(text: String) {
    Text(text, color = MaterialTheme.petMateColors.alert, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
}
