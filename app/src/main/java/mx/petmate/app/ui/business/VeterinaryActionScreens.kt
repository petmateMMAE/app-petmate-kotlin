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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.usecase.PetDates
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateIconButton
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.pets.PetBottomAction
import mx.petmate.app.ui.pets.PetScaffoldColumn
import mx.petmate.app.ui.pets.PetSectionTitle
import mx.petmate.app.ui.pets.PetTopBar
import mx.petmate.app.ui.theme.petMateColors

// =====================================================================================================
// 1. Pantalla de Nueva Consulta y Receta (SCREENS.consultForm)
// =====================================================================================================

@Composable
fun ConsultationFormScreen(
    petId: String,
    petName: String,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: VeterinaryActionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extra = MaterialTheme.petMateColors

    var reason by rememberSaveable { mutableStateOf("") }
    var diagnosis by rememberSaveable { mutableStateOf("") }
    var weightKg by rememberSaveable { mutableStateOf("") }

    val medications = remember { mutableStateListOf<MedicationDraft>() }

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = {
            PetTopBar(
                title = "Nueva consulta",
                subtitle = "Mascota: $petName",
                onBackClick = onBackClick,
            )
        },
        bottom = {
            PetBottomAction {
                PetMateButton(
                    text = if (state.isSubmitting) "Guardando..." else "Guardar consulta y receta",
                    onClick = {
                        viewModel.createConsultation(
                            petId = petId,
                            reason = reason,
                            diagnosis = diagnosis,
                            weightKgText = weightKg,
                            medications = medications.toList(),
                            onSuccess = onSaved,
                        )
                    },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            state.error?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(extra.alertSoft)
                        .padding(14.dp),
                ) {
                    Text(err, color = extra.alert, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
            }

            PetMateTextField(
                value = reason,
                onValueChange = { reason = it; viewModel.clearError() },
                label = "Motivo de la consulta",
                placeholder = "Ej. Revisión anual, decaimiento, tos...",
                required = true,
                error = state.fieldErrors["reason"]?.let { UiText.Dynamic(it) },
            )

            PetMateTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it; viewModel.clearError() },
                label = "Diagnóstico clínico",
                placeholder = "Ej. Faringitis leve, buen estado general...",
                required = true,
                error = state.fieldErrors["diagnosis"]?.let { UiText.Dynamic(it) },
            )

            PetMateTextField(
                value = weightKg,
                onValueChange = { weightKg = it; viewModel.clearError() },
                label = "Peso en kg (opcional)",
                placeholder = "Ej. 14.5",
                keyboardType = KeyboardType.Decimal,
                error = state.fieldErrors["weightKg"]?.let { UiText.Dynamic(it) },
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PetSectionTitle("Receta médica")
                if (medications.size < 10) {
                    PetMateButton(
                        text = "+ Medicamento",
                        onClick = { medications.add(MedicationDraft()) },
                        style = PetMateButtonStyle.Outline,
                    )
                }
            }

            if (medications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(extra.surface2)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Sin medicamentos en la receta. Pulsa '+ Medicamento' para agregar.",
                        color = extra.ink2,
                        fontSize = 13.sp,
                    )
                }
            } else {
                medications.forEachIndexed { index, med ->
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, extra.line, shape)
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Medicamento #${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                            )
                            Text(
                                "Quitar",
                                color = extra.alert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { medications.removeAt(index) },
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        PetMateTextField(
                            value = med.name,
                            onValueChange = { medications[index] = med.copy(name = it); viewModel.clearError() },
                            label = "Nombre del medicamento",
                            placeholder = "Ej. Amoxicilina 250mg",
                            required = true,
                            error = state.fieldErrors["med_${index}_name"]?.let { UiText.Dynamic(it) },
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                PetMateTextField(
                                    value = med.dose,
                                    onValueChange = { medications[index] = med.copy(dose = it); viewModel.clearError() },
                                    label = "Dosis",
                                    placeholder = "Ej. 1 tableta",
                                    required = true,
                                    error = state.fieldErrors["med_${index}_dose"]?.let { UiText.Dynamic(it) },
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PetMateTextField(
                                    value = med.frequency,
                                    onValueChange = { medications[index] = med.copy(frequency = it); viewModel.clearError() },
                                    label = "Frecuencia",
                                    placeholder = "Ej. Cada 8 horas",
                                    required = true,
                                    error = state.fieldErrors["med_${index}_frequency"]?.let { UiText.Dynamic(it) },
                                )
                            }
                        }

                        PetMateTextField(
                            value = med.duration,
                            onValueChange = { medications[index] = med.copy(duration = it); viewModel.clearError() },
                            label = "Duración",
                            placeholder = "Ej. 7 días",
                            required = true,
                            error = state.fieldErrors["med_${index}_duration"]?.let { UiText.Dynamic(it) },
                        )

                        PetMateTextField(
                            value = med.instructions.orEmpty(),
                            onValueChange = { medications[index] = med.copy(instructions = it.ifEmpty { null }); viewModel.clearError() },
                            label = "Indicaciones adicionales (opcional)",
                            placeholder = "Ej. Administrar con comida",
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// =====================================================================================================
// 2. Pantalla de Aplicar Vacuna por Veterinaria
// =====================================================================================================

@Composable
fun VaccineByBusinessScreen(
    petId: String,
    petName: String,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: VeterinaryActionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extra = MaterialTheme.petMateColors

    var name by rememberSaveable { mutableStateOf("") }
    var appliedOn by rememberSaveable { mutableStateOf(PetDates.today()) }
    var batch by rememberSaveable { mutableStateOf("") }
    var nextDoseOn by rememberSaveable { mutableStateOf("") }

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = {
            PetTopBar(
                title = "Aplicar vacuna",
                subtitle = "Mascota: $petName",
                onBackClick = onBackClick,
            )
        },
        bottom = {
            PetBottomAction {
                PetMateButton(
                    text = if (state.isSubmitting) "Guardando..." else "Guardar vacuna",
                    onClick = {
                        viewModel.createVaccine(
                            petId = petId,
                            name = name,
                            appliedOn = appliedOn,
                            batch = batch,
                            nextDoseOn = nextDoseOn.ifBlank { null },
                            onSuccess = onSaved,
                        )
                    },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            state.error?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(extra.alertSoft)
                        .padding(14.dp),
                ) {
                    Text(err, color = extra.alert, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
            }

            PetMateTextField(
                value = name,
                onValueChange = { name = it; viewModel.clearError() },
                label = "Nombre de la vacuna",
                placeholder = "Ej. Rabia, Séxtuple, Desparasitante...",
                required = true,
                error = state.fieldErrors["name"]?.let { UiText.Dynamic(it) },
            )

            PetMateTextField(
                value = appliedOn,
                onValueChange = { appliedOn = it; viewModel.clearError() },
                label = "Fecha de aplicación (AAAA-MM-DD)",
                placeholder = "AAAA-MM-DD",
                required = true,
                hint = "No puede ser una fecha futura",
                error = state.fieldErrors["appliedOn"]?.let { UiText.Dynamic(it) },
            )

            PetMateTextField(
                value = batch,
                onValueChange = { batch = it; viewModel.clearError() },
                label = "Lote (opcional)",
                placeholder = "Ej. L-2026-X4",
            )

            PetMateTextField(
                value = nextDoseOn,
                onValueChange = { nextDoseOn = it; viewModel.clearError() },
                label = "Próxima dosis (AAAA-MM-DD, opcional)",
                placeholder = "AAAA-MM-DD",
                hint = "Debe ser posterior a la fecha de aplicación",
                error = state.fieldErrors["nextDoseOn"]?.let { UiText.Dynamic(it) },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// =====================================================================================================
// 3. Pantalla de Aplicar Desparasitación por Veterinaria
// =====================================================================================================

@Composable
fun DewormByBusinessScreen(
    petId: String,
    petName: String,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: VeterinaryActionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extra = MaterialTheme.petMateColors

    var product by rememberSaveable { mutableStateOf("") }
    var appliedOn by rememberSaveable { mutableStateOf(PetDates.today()) }
    var type by rememberSaveable { mutableStateOf(DewormingType.Internal) }
    var nextOn by rememberSaveable { mutableStateOf("") }

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = {
            PetTopBar(
                title = "Aplicar desparasitación",
                subtitle = "Mascota: $petName",
                onBackClick = onBackClick,
            )
        },
        bottom = {
            PetBottomAction {
                PetMateButton(
                    text = if (state.isSubmitting) "Guardando..." else "Guardar desparasitación",
                    onClick = {
                        viewModel.createDeworming(
                            petId = petId,
                            product = product,
                            appliedOn = appliedOn,
                            type = type,
                            nextOn = nextOn.ifBlank { null },
                            onSuccess = onSaved,
                        )
                    },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            state.error?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(extra.alertSoft)
                        .padding(14.dp),
                ) {
                    Text(err, color = extra.alert, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
            }

            PetMateTextField(
                value = product,
                onValueChange = { product = it; viewModel.clearError() },
                label = "Producto desparasitante",
                placeholder = "Ej. Nexgard, Bravecto, Drontal...",
                required = true,
                error = state.fieldErrors["product"]?.let { UiText.Dynamic(it) },
            )

            Text("Tipo de desparasitación", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val isInternal = type == DewormingType.Internal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isInternal) MaterialTheme.colorScheme.primary else extra.surface2)
                        .clickable { type = DewormingType.Internal }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Interna",
                        color = if (isInternal) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                val isExternal = type == DewormingType.External
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExternal) MaterialTheme.colorScheme.primary else extra.surface2)
                        .clickable { type = DewormingType.External }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Externa",
                        color = if (isExternal) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            PetMateTextField(
                value = appliedOn,
                onValueChange = { appliedOn = it; viewModel.clearError() },
                label = "Fecha de aplicación (AAAA-MM-DD)",
                placeholder = "AAAA-MM-DD",
                required = true,
                hint = "No puede ser una fecha futura",
                error = state.fieldErrors["appliedOn"]?.let { UiText.Dynamic(it) },
            )

            PetMateTextField(
                value = nextOn,
                onValueChange = { nextOn = it; viewModel.clearError() },
                label = "Próxima desparasitación (AAAA-MM-DD, opcional)",
                placeholder = "AAAA-MM-DD",
                hint = "Debe ser una fecha futura",
                error = state.fieldErrors["nextOn"]?.let { UiText.Dynamic(it) },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
