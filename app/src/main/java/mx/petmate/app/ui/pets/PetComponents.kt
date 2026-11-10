package mx.petmate.app.ui.pets

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.usecase.PetDates
import mx.petmate.app.ui.components.PetMateIconButton
import mx.petmate.app.ui.theme.petMateColors

/* Piezas compartidas de mascotas y carnet, copiadas de las clases de la maqueta (PetMate_App.html). */

/** .topbar con regresar, titulo (22/700) y subtitulo. */
@Composable
fun PetTopBar(
    title: String,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (onBackClick != null) 10.dp else 20.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
    ) {
        if (onBackClick != null) {
            PetMateIconButton(
                icon = R.drawable.ic_pm_back,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBackClick,
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.petMateColors.ink2, maxLines = 1)
            }
        }
        actions()
    }
}

/** Pantalla con barra superior fija, contenido y pie. */
@Composable
fun PetScaffoldColumn(
    top: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottom: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        top()
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
        bottom()
    }
}

/** Retrato de la mascota: aun no hay fotos (pm-079), asi que se usa su inicial sobre el color de su especie. */
@Composable
fun PetAvatar(name: String, species: PetSpecies, size: Dp, modifier: Modifier = Modifier, radius: Dp = 20.dp) {
    val extra = MaterialTheme.petMateColors
    val (bg, fg) = when (species) {
        PetSpecies.Dog -> extra.sunSoft to extra.sunInk
        PetSpecies.Cat -> extra.spaSoft to extra.spa
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(bg),
    ) {
        Icon(
            painterResource(R.drawable.ic_pm_paw),
            contentDescription = null,
            tint = fg.copy(alpha = 0.18f),
            modifier = Modifier.size(size * 0.8f),
        )
        Text(
            text = name.trim().take(1).uppercase().ifEmpty { "?" },
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.ExtraBold),
            color = fg,
        )
    }
}

/** Fondo del encabezado del carnet segun la especie (.hero). */
@Composable
fun heroColor(species: PetSpecies): Color = when (species) {
    PetSpecies.Dog -> MaterialTheme.petMateColors.sunSoft
    PetSpecies.Cat -> MaterialTheme.petMateColors.spaSoft
}

/** .empty: icono, titulo y texto centrados. */
@Composable
fun PetEmptyState(
    @DrawableRes icon: Int,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    iconBackground: Color = MaterialTheme.petMateColors.brandSoft,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    action: @Composable () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 26.dp, bottom = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(84.dp).background(iconBackground, RoundedCornerShape(26.dp)),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(40.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
        )
        Text(text, color = MaterialTheme.petMateColors.ink2, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 18.dp))
        action()
    }
}

@Composable
fun PetLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
    }
}

@Composable
fun PetError(message: UiText, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    PetEmptyState(
        icon = R.drawable.ic_pm_alert,
        title = stringResource(R.string.pets_error_title),
        text = message.asString(),
        iconBackground = MaterialTheme.petMateColors.alertSoft,
        iconTint = MaterialTheme.petMateColors.alert,
        modifier = modifier,
        action = { TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) } },
    )
}

/** .sticky-foot / .fab-row: boton al pie con degradado sobre el contenido. */
@Composable
fun PetBottomAction(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to bg.copy(alpha = 0f), 0.35f to bg, 1f to bg))
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 10.dp),
    ) { content() }
}

/** .sec-title */
@Composable
fun PetSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
        modifier = modifier.padding(top = 20.dp, bottom = 10.dp),
    )
}

/** .segc: control segmentado. */
@Composable
fun <T> PetSegmented(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = MaterialTheme.petMateColors
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(extra.surface2, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(value) },
            ) {
                Text(label, fontWeight = FontWeight.Bold, color = if (on) MaterialTheme.colorScheme.onSurface else extra.ink2)
            }
        }
    }
}

/** Etiqueta de campo como la de PetMateTextField (.fl). */
@Composable
fun PetFieldLabel(text: String, required: Boolean = false) {
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (required) Text(" *", fontWeight = FontWeight.Bold, color = MaterialTheme.petMateColors.alert)
    }
}

/** Campo de fecha que abre el calendario. [value] en `yyyy-MM-dd`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDateField(
    label: String,
    value: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    hint: String? = null,
    error: UiText? = null,
    clearable: Boolean = !required,
) {
    val extra = MaterialTheme.petMateColors
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        PetFieldLabel(label, required)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (error != null) extra.alertSoft else MaterialTheme.colorScheme.surface)
                .border(2.dp, if (error != null) extra.alert else extra.line, RoundedCornerShape(16.dp))
                .clickable { open = true }
                .padding(horizontal = 14.dp),
        ) {
            Icon(painterResource(R.drawable.ic_pm_calendar), contentDescription = null, tint = extra.ink2, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = value?.let { formatDate(it) } ?: stringResource(R.string.pets_pick_date),
                color = if (value != null) MaterialTheme.colorScheme.onSurface else extra.ink3,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            if (clearable && value != null) {
                Text(
                    stringResource(R.string.pets_clear),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onValueChange(null) }.padding(6.dp),
                )
            }
        }
        when {
            error != null -> Text(error.asString(), color = extra.alert, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            hint != null -> Text(hint, color = extra.ink2, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (value ?: PetDates.today()).let { PetDates.toMillisUtc(it) })
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onValueChange(PetDates.fromMillisUtc(it)) }
                    open = false
                }) { Text(stringResource(R.string.pets_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.profile_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** Dialogo de confirmacion (.dialog de la maqueta). */
@Composable
fun PetConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    @DrawableRes icon: Int = R.drawable.ic_pm_info,
) {
    val extra = MaterialTheme.petMateColors
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).background(if (danger) extra.alertSoft else extra.brandSoft, RoundedCornerShape(18.dp)),
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = if (danger) extra.alert else MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) },
        text = { Text(text, color = extra.ink2, textAlign = TextAlign.Center) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = if (danger) extra.alert else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
    )
}

/** Codigo QR dibujado con ZXing (oscuro sobre blanco para que se pueda escanear). */
@Composable
fun PetQrCode(content: String, size: Dp, modifier: Modifier = Modifier) {
    val matrix = remember(content) {
        runCatching {
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0),
            )
        }.getOrNull()
    }
    val ink = MaterialTheme.petMateColors.qrInk
    Canvas(modifier = modifier.size(size)) {
        val m = matrix ?: return@Canvas
        val cell = this.size.width / m.width
        for (y in 0 until m.height) {
            for (x in 0 until m.width) {
                if (m.get(x, y)) drawRect(ink, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

/** Separador vertical pequeno. */
@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))

// ---------- Textos de fechas ----------

/** "2026-11-16" -> "16/11/2026", como fd() de la maqueta. */
fun formatDate(iso: String): String {
    val parts = iso.take(10).split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
}

/** "hoy", "manana", "en 3 dias", "hace 2 dias" (whenTxt de la maqueta). */
@Composable
fun whenText(iso: String, today: String = PetDates.today()): String {
    val days = PetDates.daysBetween(today, iso) ?: return formatDate(iso)
    return when {
        days == 0 -> stringResource(R.string.pets_when_today)
        days == 1 -> stringResource(R.string.pets_when_tomorrow)
        days > 1 -> pluralStringResource(R.plurals.pets_when_in_days, days, days)
        else -> pluralStringResource(R.plurals.pets_when_days_ago, -days, -days)
    }
}

/** Edad: "3 anos", "5 meses" o "Recien nacido". */
@Composable
fun ageText(birthDate: String?, today: String = PetDates.today()): String? {
    val (years, months) = birthDate?.let { PetDates.age(it, today) } ?: return null
    return when {
        years > 0 -> pluralStringResource(R.plurals.pets_age_years, years, years)
        months > 0 -> pluralStringResource(R.plurals.pets_age_months, months, months)
        else -> stringResource(R.string.pets_age_newborn)
    }
}
