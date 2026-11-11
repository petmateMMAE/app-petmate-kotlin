package mx.petmate.app.ui.business

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.petmate.app.R
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.SCHEDULE_DAYS
import mx.petmate.app.domain.model.TimeRange
import mx.petmate.app.ui.theme.petMateColors
import java.util.Locale

/* Piezas del modo negocio (pantallas panel, scanner, attended, bizEdit y services de PetMate_App.html). */

/** Pestanas de la barra inferior en modo negocio. */
enum class BusinessTab(val label: String, @DrawableRes val icon: Int) {
    Panel("Panel", R.drawable.ic_pm_store),
    Scanner("Escanear", R.drawable.ic_pm_qr),
    Attended("Atendidas", R.drawable.ic_pm_paw),
    Profile("Perfil", R.drawable.ic_pm_user),
}

/** Misma barra que la del dueno, con las pestanas del negocio. */
@Composable
fun BusinessBottomBar(selected: BusinessTab, onTabSelected: (BusinessTab) -> Unit, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = extra.line)
        Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 2.dp).selectableGroup()) {
            BusinessTab.entries.forEach { tab ->
                val on = tab == selected
                val tint = if (on) extra.brandText else extra.ink3
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .selectable(selected = on, onClick = { onTabSelected(tab) }, role = Role.Tab)
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.width(58.dp).height(32.dp).background(if (on) extra.brandSoft else Color.Transparent, CircleShape),
                    ) {
                        Icon(painterResource(tab.icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Text(tab.label, style = MaterialTheme.typography.labelMedium, color = tint)
                }
            }
        }
    }
}

/** Nombre, icono y colores de cada tipo, como TYPE de la maqueta. */
data class KindStyle(val name: String, @DrawableRes val icon: Int, val soft: Color, val ink: Color, @DrawableRes val photo: Int)

@Composable
fun BusinessKind.style(): KindStyle {
    val c = MaterialTheme.petMateColors
    return when (this) {
        BusinessKind.Veterinary -> KindStyle("Veterinaria", R.drawable.ic_pm_cross, c.vetSoft, c.vet, R.drawable.sample_business_vet)
        BusinessKind.Store -> KindStyle("Tienda", R.drawable.ic_pm_bag, c.shopSoft, c.shop, R.drawable.sample_business_shop)
        BusinessKind.Grooming -> KindStyle("Estética", R.drawable.ic_pm_scissors, c.spaSoft, c.spa, R.drawable.sample_business_spa)
    }
}

@Composable
fun KindBadge(kind: BusinessKind, modifier: Modifier = Modifier) {
    val s = kind.style()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.background(s.soft, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(painterResource(s.icon), contentDescription = null, tint = s.ink, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(s.name, color = s.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Foto del negocio: la del tipo mientras no exista la carga de imagenes (pm-111). */
@Composable
fun BusinessPhoto(kind: BusinessKind, modifier: Modifier = Modifier, size: Dp? = null, radius: Dp = 18.dp) {
    Image(
        painter = painterResource(kind.style().photo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = (if (size != null) modifier.size(size) else modifier).clip(RoundedCornerShape(radius)),
    )
}

/** .card */
@Composable
fun BizCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val extra = MaterialTheme.petMateColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, extra.line, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) { content() }
}

/** Fila de acceso del panel: icono, titulo, subtitulo y flecha. */
@Composable
fun AccessRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    soft: Color = MaterialTheme.petMateColors.brandSoft,
) {
    val extra = MaterialTheme.petMateColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, extra.line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(Modifier.size(44.dp).background(soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = extra.ink2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(painterResource(R.drawable.ic_pm_chevron), contentDescription = null, tint = extra.ink3, modifier = Modifier.size(18.dp))
    }
}

/** Chip de seleccion (dias del horario, que compartir, duracion). */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val extra = MaterialTheme.petMateColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(if (selected) extra.brandSoft else MaterialTheme.colorScheme.surface)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else extra.line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = if (selected) extra.brandText else extra.ink2, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selected) extra.brandText else MaterialTheme.colorScheme.onSurface)
    }
}

/** Aviso suave (.tip). */
@Composable
fun TipBox(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_pm_info,
    soft: Color = MaterialTheme.petMateColors.brandSoft,
    ink: Color = MaterialTheme.petMateColors.brandText,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().background(soft, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = ink, fontSize = 14.sp)
    }
}

/** Numero de paso de "Compartir historial". */
@Composable
fun StepTitle(number: Int, text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 18.dp, bottom = 10.dp)) {
        Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Text("$number", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold))
    }
}

// ---------- Textos ----------

val DAY_LABELS = mapOf(
    "monday" to "Lun", "tuesday" to "Mar", "wednesday" to "Mié", "thursday" to "Jue",
    "friday" to "Vie", "saturday" to "Sáb", "sunday" to "Dom",
)

/** "Lun a Vie 09:00–18:00 · Sáb 10:00–14:00" */
fun scheduleSummary(schedule: Map<String, List<TimeRange>>): String {
    val groups = mutableListOf<Pair<MutableList<String>, String>>()
    SCHEDULE_DAYS.forEachIndexed { index, day ->
        val ranges = schedule[day].orEmpty()
        if (ranges.isEmpty()) return@forEachIndexed
        val text = ranges.joinToString(", ") { "${it.open}–${it.close}" }
        val last = groups.lastOrNull()
        if (last != null && last.second == text && last.first.last() == SCHEDULE_DAYS.getOrNull(index - 1)) {
            last.first.add(day)
        } else {
            groups.add(mutableListOf(day) to text)
        }
    }
    if (groups.isEmpty()) return "Sin horario"
    return groups.joinToString(" · ") { (days, text) ->
        val label = if (days.size == 1) DAY_LABELS.getValue(days.first()) else "${DAY_LABELS.getValue(days.first())} a ${DAY_LABELS.getValue(days.last())}"
        "$label $text"
    }
}

fun todayText(isOpen: Boolean, today: List<TimeRange>): String = when {
    isOpen && today.isNotEmpty() -> "Abierto ahora · cierra ${today.last().close}"
    isOpen -> "Abierto ahora"
    today.isNotEmpty() -> "Cerrado · hoy ${today.joinToString(", ") { "${it.open}–${it.close}" }}"
    else -> "Cerrado hoy"
}

fun priceText(price: Double?): String = price?.let { "$" + String.format(Locale.US, "%,.2f", it) } ?: "Pregunta el precio"
