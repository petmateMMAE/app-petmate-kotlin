package mx.petmate.app.ui.map

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.ui.components.IconButtonStyle
import mx.petmate.app.ui.components.OwnerTab
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.components.PetMateBottomBar
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateIconButton
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.components.PetMateSmallButton
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/**
 * Pantalla de inicio: mapa de negocios afiliados (spec 010, A-01 a A-04 y A-10).
 *
 * La navegacion sube como lambdas. Llamar, WhatsApp y "Como llegar" tambien
 * suben: abrir otras apps es trabajo de quien conecte la logica.
 */
@Composable
fun MapScreen(
    onTabSelected: (OwnerTab) -> Unit,
    onBusinessDetailsClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO(spec 010): sustituir los datos de ejemplo por MapViewModel
    //  (hiltViewModel + collectAsStateWithLifecycle) y conectar sus eventos.
    MapContent(
        uiState = MapSampleData.state,
        onQueryChange = {},
        onTypeToggle = {},
        onBusinessClick = {},
        onDismissBusiness = {},
        onCallClick = {},
        onWhatsAppClick = {},
        onDirectionsClick = {},
        onBusinessDetailsClick = onBusinessDetailsClick,
        onZoomInClick = {},
        onZoomOutClick = {},
        onMyLocationClick = {},
        onEnableLocationClick = {},
        onHelpClick = {},
        onRetryClick = {},
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

@Composable
private fun MapContent(
    uiState: MapUiState,
    onQueryChange: (String) -> Unit,
    onTypeToggle: (BusinessType) -> Unit,
    onBusinessClick: (String) -> Unit,
    onDismissBusiness: () -> Unit,
    onCallClick: (MapBusiness) -> Unit,
    onWhatsAppClick: (MapBusiness) -> Unit,
    onDirectionsClick: (MapBusiness) -> Unit,
    onBusinessDetailsClick: (String) -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    onEnableLocationClick: () -> Unit,
    onHelpClick: () -> Unit,
    onRetryClick: () -> Unit,
    onTabSelected: (OwnerTab) -> Unit,
    modifier: Modifier = Modifier,
    /** true solo en las previsualizaciones, que no pueden cargar el mapa real. */
    useIllustratedMap: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val businessesOnMap =
                if (uiState.isLoading) emptyList() else uiState.visibleBusinesses

            // En las previsualizaciones se usa el mapa ilustrado, porque el
            // real necesita red y ciclo de vida de Activity.
            if (useIllustratedMap) {
                IllustratedMap(
                    businesses = businessesOnMap,
                    selectedBusinessId = uiState.selectedBusiness?.id,
                    showUserLocation = uiState.isLocationAvailable,
                    onBusinessClick = onBusinessClick,
                )
            } else {
                OsmMap(
                    businesses = businessesOnMap,
                    selectedBusinessId = uiState.selectedBusiness?.id,
                    showUserLocation = uiState.isLocationAvailable,
                    onBusinessClick = onBusinessClick,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp),
            ) {
                SearchBar(query = uiState.query, onQueryChange = onQueryChange)
                TypeFilters(visibleTypes = uiState.visibleTypes, onTypeToggle = onTypeToggle)
                if (uiState.isLoading) LoadingPill()
                if (uiState.isHelpVisible) HelpTip()
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            ) {
                MapControls(
                    onHelpClick = onHelpClick,
                    onZoomInClick = onZoomInClick,
                    onZoomOutClick = onZoomOutClick,
                    onMyLocationClick = onMyLocationClick,
                    modifier = Modifier.padding(end = 2.dp, bottom = 8.dp),
                )
                val selected = uiState.selectedBusiness
                when {
                    uiState.error != null -> MessageBanner(
                        icon = R.drawable.ic_pm_alert,
                        iconBackground = MaterialTheme.petMateColors.alertSoft,
                        iconTint = MaterialTheme.petMateColors.alert,
                        title = uiState.error.asString(),
                        body = null,
                        action = stringResource(R.string.action_retry),
                        onActionClick = onRetryClick,
                    )
                    selected != null -> BusinessSheet(
                        business = selected,
                        onDismiss = onDismissBusiness,
                        onCallClick = { onCallClick(selected) },
                        onWhatsAppClick = { onWhatsAppClick(selected) },
                        onDirectionsClick = { onDirectionsClick(selected) },
                        onDetailsClick = { onBusinessDetailsClick(selected.id) },
                    )
                    uiState.isEmpty -> MessageBanner(
                        icon = R.drawable.ic_pm_search,
                        iconBackground = MaterialTheme.petMateColors.brandSoft,
                        iconTint = MaterialTheme.petMateColors.brandText,
                        title = stringResource(R.string.map_empty_title),
                        body = stringResource(R.string.map_empty_body),
                    )
                    !uiState.isLocationAvailable -> MessageBanner(
                        icon = R.drawable.ic_pm_pin,
                        iconBackground = MaterialTheme.petMateColors.sunSoft,
                        iconTint = MaterialTheme.petMateColors.sunInk,
                        title = stringResource(R.string.map_no_location_title),
                        body = stringResource(R.string.map_no_location_body),
                        action = stringResource(R.string.map_no_location_action),
                        onActionClick = onEnableLocationClick,
                    )
                }
            }
        }

        PetMateBottomBar(
            selected = OwnerTab.Map,
            onTabSelected = onTabSelected,
            remindersCount = uiState.remindersCount,
        )
    }
}

// ---------- Parte superior ----------

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    val shape = MaterialTheme.shapes.medium
    val placeholder = stringResource(R.string.map_search_placeholder)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .defaultMinSize(minHeight = 50.dp)
            .padding(horizontal = 14.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pm_search),
            contentDescription = null,
            tint = extra.ink2,
            modifier = Modifier.size(20.dp),
        )
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extra.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = placeholder },
            )
        }
    }
}

/** Tres filtros de igual ancho; el apagado se ve gris y transparente. */
@Composable
private fun TypeFilters(
    visibleTypes: Set<BusinessType>,
    onTypeToggle: (BusinessType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = MaterialTheme.petMateColors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
        BusinessType.entries.forEach { type ->
            val style = type.style()
            val on = type in visibleTypes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (on) 1f else 0.55f)
                    .shadow(4.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .toggleable(value = on, role = Role.Switch, onValueChange = { onTypeToggle(type) })
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 6.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .background(if (on) style.pin else extra.ink3, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(style.icon),
                        contentDescription = null,
                        tint = extra.onPin,
                        modifier = Modifier.size(13.dp),
                    )
                }
                Text(
                    text = stringResource(style.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = extra.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LoadingPill(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .shadow(4.dp, CircleShape)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.map_loading),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Ayuda de la maqueta (.tip): fondo amarillo suave con icono. */
@Composable
private fun HelpTip(modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, MaterialTheme.shapes.medium)
            .background(extra.sunSoft, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pm_help),
            contentDescription = null,
            tint = extra.sunInk,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.map_help),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------- Controles ----------

@Composable
private fun MapControls(
    onHelpClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        PetMateIconButton(
            icon = R.drawable.ic_pm_help,
            contentDescription = stringResource(R.string.action_help),
            onClick = onHelpClick,
            style = IconButtonStyle.Help,
        )
        PetMateIconButton(
            icon = R.drawable.ic_pm_plus,
            contentDescription = stringResource(R.string.map_zoom_in),
            onClick = onZoomInClick,
        )
        PetMateIconButton(
            icon = R.drawable.ic_pm_minus,
            contentDescription = stringResource(R.string.map_zoom_out),
            onClick = onZoomOutClick,
        )
        PetMateIconButton(
            icon = R.drawable.ic_pm_locate,
            contentDescription = stringResource(R.string.map_my_location),
            onClick = onMyLocationClick,
            tint = MaterialTheme.petMateColors.userLocation,
        )
    }
}

// ---------- Paneles inferiores ----------

/** Aviso flotante (.loc-banner): sin ubicacion, sin resultados o error. */
@Composable
private fun MessageBanner(
    @DrawableRes icon: Int,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: () -> Unit = {},
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(iconBackground, RoundedCornerShape(12.dp)),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(title) }
                if (body != null) {
                    append(" ")
                    append(body)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            PetMateSmallButton(text = action, onClick = onActionClick)
        }
    }
}

/** Tarjeta del negocio elegido (.biz-sheet de la maqueta). */
@Composable
private fun BusinessSheet(
    business: MapBusiness,
    onDismiss: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onDirectionsClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = MaterialTheme.petMateColors
    val style = business.type.style()
    val shape = MaterialTheme.shapes.large

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(12.dp),
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BusinessThumb(business = business)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 30.dp),
                ) {
                    PetMateBadge(
                        text = stringResource(style.label),
                        container = style.soft,
                        content = style.text,
                        icon = style.icon,
                    )
                    Text(
                        text = business.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                    )
                    OpenStatus(business = business)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pm_navigation),
                            contentDescription = null,
                            tint = extra.ink2,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = stringResource(R.string.map_distance_from_you, business.distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = extra.ink2,
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                SheetAction(
                    text = stringResource(R.string.map_action_call),
                    icon = R.drawable.ic_pm_phone,
                    container = extra.okSoft,
                    content = extra.ok,
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f),
                )
                if (business.hasWhatsApp) {
                    SheetAction(
                        text = stringResource(R.string.map_action_whatsapp),
                        icon = R.drawable.ic_pm_chat,
                        container = extra.whatsApp,
                        content = extra.whatsAppInk,
                        onClick = onWhatsAppClick,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    SheetAction(
                        text = stringResource(R.string.map_action_no_whatsapp),
                        icon = R.drawable.ic_pm_chat,
                        container = extra.surface2,
                        content = extra.ink3,
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                SheetAction(
                    text = stringResource(R.string.map_action_directions),
                    icon = R.drawable.ic_pm_navigation,
                    container = extra.brandSoft,
                    content = extra.brandText,
                    onClick = onDirectionsClick,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(6.dp))
            PetMateButton(
                text = stringResource(R.string.map_action_details),
                onClick = onDetailsClick,
                style = PetMateButtonStyle.Ghost,
                trailingIcon = R.drawable.ic_pm_next,
            )
        }

        // La "x" se dibuja de 32 dp, pero su area tactil es de 48 dp.
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(extra.surface2, RoundedCornerShape(10.dp)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pm_close),
                    contentDescription = stringResource(R.string.action_close),
                    tint = extra.ink2,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun BusinessThumb(business: MapBusiness, modifier: Modifier = Modifier) {
    val style = business.type.style()
    val shape = MaterialTheme.shapes.medium
    val photo = business.photo
    if (photo != null) {
        Image(
            painter = painterResource(photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(84.dp)
                .clip(shape),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(84.dp)
                .background(style.soft, shape),
        ) {
            Icon(
                painter = painterResource(style.icon),
                contentDescription = null,
                tint = style.text,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun OpenStatus(business: MapBusiness) {
    val extra = MaterialTheme.petMateColors
    val status = stringResource(if (business.isOpen) R.string.map_open_now else R.string.map_closed)
    val time = business.scheduleTime
    val schedule = when {
        time == null -> stringResource(R.string.map_open_24h)
        business.isOpen -> stringResource(R.string.map_closes_at, time)
        else -> stringResource(R.string.map_opens_tomorrow_at, time)
    }
    val scheduleText = stringResource(R.string.map_schedule_separator, schedule)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (business.isOpen) extra.ok else extra.alert)) {
                append(status)
            }
            append(" ")
            withStyle(SpanStyle(color = extra.ink2)) { append(scheduleText) }
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

/** Boton de accion de la tarjeta: icono arriba y texto abajo, 56 dp de alto. */
@Composable
private fun SheetAction(
    text: String,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = content,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

// ---------- Previews ----------

@Composable
private fun MapPreview(uiState: MapUiState) {
    PetMateEdelTheme {
        MapContent(
            uiState = uiState,
            onQueryChange = {},
            onTypeToggle = {},
            onBusinessClick = {},
            onDismissBusiness = {},
            onCallClick = {},
            onWhatsAppClick = {},
            onDirectionsClick = {},
            onBusinessDetailsClick = {},
            onZoomInClick = {},
            onZoomOutClick = {},
            onMyLocationClick = {},
            onEnableLocationClick = {},
            onHelpClick = {},
            onRetryClick = {},
            onTabSelected = {},
            useIllustratedMap = true,
        )
    }
}

@PetMatePreviews
@Composable
private fun MapWithDataPreview() = MapPreview(MapSampleData.state)

@PetMatePreviews
@Composable
private fun MapBusinessSelectedPreview() = MapPreview(MapSampleData.state.copy(selectedBusinessId = "b1"))

@PetMatePreviews
@Composable
private fun MapClosedBusinessPreview() = MapPreview(MapSampleData.state.copy(selectedBusinessId = "b5"))

@PetMatePreviews
@Composable
private fun MapOnlySpasPreview() = MapPreview(
    MapSampleData.state.copy(visibleTypes = setOf(BusinessType.Spa), selectedBusinessId = "b2"),
)

@PetMatePreviews
@Composable
private fun MapWithoutLocationPreview() = MapPreview(MapSampleData.state.copy(isLocationAvailable = false))

@PetMatePreviews
@Composable
private fun MapHelpPreview() = MapPreview(MapSampleData.state.copy(isHelpVisible = true))

@PetMatePreviews
@Composable
private fun MapLoadingPreview() = MapPreview(MapUiState(isLoading = true, remindersCount = 3))

@PetMatePreviews
@Composable
private fun MapEmptyPreview() = MapPreview(MapSampleData.state.copy(query = "Acuario"))

@PetMatePreviews
@Composable
private fun MapErrorPreview() = MapPreview(
    MapUiState(error = UiText.Resource(R.string.map_error), remindersCount = 3),
)
