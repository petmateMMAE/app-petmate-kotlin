package mx.petmate.app.ui.pets

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.domain.model.HistoryItem
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.theme.petMateColors

@Composable
fun PetHistoryScreen(
    petId: String,
    petName: String,
    onBackClick: () -> Unit,
    onConsultationClick: (id: String) -> Unit,
    viewModel: PetHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.historyState.collectAsStateWithLifecycle()
    val extra = MaterialTheme.petMateColors

    LaunchedEffect(petId) {
        viewModel.loadHistory(petId)
    }

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = {
            PetTopBar(
                title = "Historial médico",
                subtitle = petName,
                onBackClick = onBackClick,
            )
        },
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error.orEmpty(), color = extra.alert, fontWeight = FontWeight.SemiBold)
                }
            }
            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pm_calendar),
                            contentDescription = null,
                            tint = extra.ink2,
                            modifier = Modifier.size(54.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sin registros en el historial médico de $petName.",
                            color = extra.ink2,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.items.forEach { item ->
                        HistoryCard(
                            item = item,
                            onClick = {
                                if (item.kind.equals("Consultation", ignoreCase = true)) {
                                    onConsultationClick(item.id)
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    onClick: () -> Unit,
) {
    val extra = MaterialTheme.petMateColors
    val shape = RoundedCornerShape(18.dp)
    val isConsultation = item.kind.equals("Consultation", ignoreCase = true)

    val (iconRes, iconBg, iconTint, kindLabel) = when (item.kind) {
        "Consultation" -> Quad(R.drawable.ic_pm_edit, extra.brandSoft, MaterialTheme.colorScheme.primary, "Consulta")
        "Vaccine" -> Quad(R.drawable.ic_pm_syringe, extra.brandSoft, MaterialTheme.colorScheme.primary, "Vacuna")
        "Deworming" -> Quad(R.drawable.ic_pm_pill, extra.okSoft, extra.ok, "Desparasitación")
        "Weight" -> Quad(R.drawable.ic_pm_weight, extra.shopSoft, extra.shop, "Peso")
        else -> Quad(R.drawable.ic_pm_calendar, extra.surface2, MaterialTheme.colorScheme.onSurface, item.kind)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = MaterialTheme.colorScheme.primary)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = isConsultation, onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(kindLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iconTint)
                    Text(item.date, fontSize = 12.sp, color = extra.ink2)
                }
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1D1733),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.origin.byBusiness) {
                PetMateBadge(
                    text = "Registrado por: ${item.origin.businessName ?: "Veterinaria"}",
                    container = extra.brandSoft,
                    content = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text("Registrado por el dueño", fontSize = 12.sp, color = extra.ink2)
            }

            if (isConsultation) {
                Text(
                    "Ver receta ›",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// =====================================================================================================
// 2. Pantalla de Detalle de Consulta y Receta (solo lectura)
// =====================================================================================================

@Composable
fun ConsultationDetailScreen(
    consultationId: String,
    onBackClick: () -> Unit,
    viewModel: PetHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val extra = MaterialTheme.petMateColors

    LaunchedEffect(consultationId) {
        viewModel.loadConsultation(consultationId)
    }

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = {
            PetTopBar(
                title = "Detalle de consulta",
                subtitle = "Solo lectura",
                onBackClick = onBackClick,
            )
        },
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error.orEmpty(), color = extra.alert, fontWeight = FontWeight.SemiBold)
                }
            }
            state.consultation != null -> {
                val c = state.consultation!!
                val followup = state.followup
                val shape = RoundedCornerShape(20.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    // Tarjeta principal de la consulta
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, shape)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Fecha: ${c.consultedOn}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                            )
                            c.origin.businessName?.let {
                                PetMateBadge(it, extra.brandSoft, MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("Motivo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extra.ink2)
                        Text(c.reason, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D1733))

                        Spacer(Modifier.height(12.dp))
                        Text("Diagnóstico", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extra.ink2)
                        Text(c.diagnosis, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D1733))

                        c.weightKg?.let { w ->
                            Spacer(Modifier.height(12.dp))
                            Text("Peso registrado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extra.ink2)
                            Text("$w kg", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D1733))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    PetSectionTitle("Receta médica")

                    if (c.medications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(extra.surface2)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No se recetaron medicamentos en esta consulta.", color = extra.ink2, fontSize = 13.sp)
                        }
                    } else {
                        c.medications.forEachIndexed { i, med ->
                            val medShape = RoundedCornerShape(16.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(medShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, extra.line, medShape)
                                    .padding(14.dp),
                            ) {
                                Text(
                                    "${i + 1}. ${med.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Dosis: ${med.dose} · Frecuencia: ${med.frequency}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Duración: ${med.duration}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                med.instructions?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Indicaciones: $it", fontSize = 12.sp, color = extra.ink2)
                                }
                            }
                        }
                    }

                    // Seguimiento (si existe)
                    if (followup != null) {
                        Spacer(Modifier.height(16.dp))
                        PetSectionTitle("Seguimiento clínico")

                        val statusLabel = when (followup.status) {
                            "Treating" -> "En tratamiento"
                            "Improving" -> "En mejoría"
                            "Discharged" -> "Alta médica"
                            else -> followup.status
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(extra.sunSoft)
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Estado: $statusLabel", fontWeight = FontWeight.Bold, color = extra.sunInk)
                                followup.nextReviewOn?.let {
                                    Text("Próx. revisión: $it", fontSize = 12.sp, color = extra.sunInk)
                                }
                            }

                            if (followup.notes.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Notas de evolución:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = extra.sunInk)
                                followup.notes.forEach { n ->
                                    Text("• ${n.text}", fontSize = 13.sp, color = extra.sunInk, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
