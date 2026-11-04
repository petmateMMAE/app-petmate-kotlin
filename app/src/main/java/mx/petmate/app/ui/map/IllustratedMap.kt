package mx.petmate.app.ui.map

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.petmate.app.R
import mx.petmate.app.ui.theme.PetMateColors
import mx.petmate.app.ui.theme.petMateColors

private const val WORLD_WIDTH = 800f
private const val WORLD_HEIGHT = 1000f

/** Desplazamiento inicial del mapa en la maqueta (pan x = -220, y = -200). */
private const val PAN_X = -220f
private const val PAN_Y = -200f

/** Punto "estas aqui" de la maqueta. */
private const val ME_X = 400f
private const val ME_Y = 520f

// ---------- Estilo por tipo de negocio ----------

/** Textos, icono y colores de cada tipo de negocio. */
@Immutable
internal data class BusinessStyle(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    val pin: Color,
    val soft: Color,
    val text: Color,
)

@Composable
internal fun BusinessType.style(): BusinessStyle {
    val c = MaterialTheme.petMateColors
    return when (this) {
        BusinessType.Vet -> BusinessStyle(R.string.business_type_vet, R.drawable.ic_pm_cross, c.pinVet, c.vetSoft, c.vet)
        BusinessType.Shop -> BusinessStyle(R.string.business_type_shop, R.drawable.ic_pm_bag, c.pinShop, c.shopSoft, c.shop)
        BusinessType.Spa -> BusinessStyle(R.string.business_type_spa, R.drawable.ic_pm_scissors, c.pinSpa, c.spaSoft, c.spa)
    }
}

// ---------- Mapa ----------

/**
 * Mapa ilustrado de la maqueta con sus marcadores.
 *
 * Es un dibujo fijo para el diseño: no se arrastra ni hace zoom. Lo sustituye
 * el mapa real cuando la spec 010 fije el proveedor (requiere dependencia).
 */
@Composable
internal fun IllustratedMap(
    businesses: List<MapBusiness>,
    selectedBusinessId: String?,
    showUserLocation: Boolean,
    onBusinessClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.petMateColors.mapLand)
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(x = PAN_X.dp, y = PAN_Y.dp)
                .requiredSize(WORLD_WIDTH.dp, WORLD_HEIGHT.dp),
        ) {
            MapDrawing(modifier = Modifier.fillMaxSize())

            if (showUserLocation) {
                UserLocationDot(
                    modifier = Modifier.offset(x = (ME_X - 33).dp, y = (ME_Y - 33).dp),
                )
            }

            // El elegido se pinta al final para que quede encima de los demas.
            businesses
                .sortedBy { it.id == selectedBusinessId }
                .forEach { business ->
                    BusinessPin(
                        business = business,
                        selected = business.id == selectedBusinessId,
                        onClick = { onBusinessClick(business.id) },
                        modifier = Modifier.offset(
                            x = (business.previewX - PIN_BOX / 2).dp,
                            y = (business.previewY - PIN_BOX).dp,
                        ),
                    )
                }
        }
    }
}

/** Calles, manzanas, parque, rio y nombres, igual que el SVG de la maqueta. */
@Composable
private fun MapDrawing(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.petMateColors
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, color = colors.mapLabel)
    val labels = MapLabel.entries.map { it to stringResource(it.text) }

    Canvas(modifier = modifier) {
        val u = size.width / WORLD_WIDTH
        fun p(x: Float, y: Float) = Offset(x * u, y * u)

        BLOCKS.forEach { (x, y, w, h) ->
            drawRoundRect(colors.mapBlock, p(x, y), Size(w * u, h * u), CornerRadius(14 * u))
        }
        drawRoundRect(colors.mapPark, p(430f, 660f), Size(160 * u, 120 * u), CornerRadius(18 * u))
        TREES.forEach { (x, y) -> drawCircle(colors.mapTree, 13 * u, p(x, y)) }

        val river = Path().apply {
            moveTo(-20 * u, 590 * u)
            cubicTo(140 * u, 560 * u, 220 * u, 640 * u, 380 * u, 610 * u)
            cubicTo(540 * u, 580 * u, 640 * u, 560 * u, 820 * u, 600 * u)
        }
        drawPath(river, colors.mapWater, style = Stroke(width = 34 * u))

        ROADS_H.forEach { y -> road(colors, p(0f, y), p(WORLD_WIDTH, y), u) }
        ROADS_V.forEach { x -> road(colors, p(x, 0f), p(x, WORLD_HEIGHT), u) }

        val a = p(0f, 980f)
        val b = p(800f, 120f)
        drawLine(colors.mapRoadEdge, a, b, strokeWidth = 32 * u)
        drawLine(colors.mapRoad, a, b, strokeWidth = 24 * u)
        drawLine(
            color = colors.sun.copy(alpha = 0.7f),
            start = a,
            end = b,
            strokeWidth = 3 * u,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14 * u, 12 * u)),
        )

        labels.forEach { (label, text) ->
            val style = when (label.tint) {
                LabelTint.Park -> labelStyle.copy(color = colors.ok)
                LabelTint.River -> labelStyle.copy(color = colors.shop)
                LabelTint.Street -> labelStyle
            }
            mapText(measurer, text, style, p(label.x, label.y), label.angle)
        }
    }
}

private fun DrawScope.road(colors: PetMateColors, start: Offset, end: Offset, u: Float) {
    drawLine(colors.mapRoadEdge, start, end, strokeWidth = 24 * u)
    drawLine(colors.mapRoad, start, end, strokeWidth = 18 * u)
}

/** Escribe un nombre de calle; como en SVG, [at] es la linea base del texto. */
private fun DrawScope.mapText(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    at: Offset,
    angle: Float,
) {
    val layout = measurer.measure(text, style)
    val topLeft = Offset(at.x, at.y - layout.firstBaseline)
    rotate(degrees = angle, pivot = at) {
        drawText(layout, topLeft = topLeft)
    }
}

private enum class LabelTint { Street, Park, River }

private enum class MapLabel(
    @StringRes val text: Int,
    val x: Float,
    val y: Float,
    val angle: Float = 0f,
    val tint: LabelTint = LabelTint.Street,
) {
    Insurgentes(R.string.map_street_insurgentes, 30f, 200f),
    Hidalgo(R.string.map_street_hidalgo, 450f, 380f),
    Zaragoza(R.string.map_street_zaragoza, 640f, 815f),
    Juarez(R.string.map_street_juarez, 422f, 150f, angle = 90f),
    Morelos(R.string.map_street_morelos, 612f, 470f, angle = 90f),
    Boulevard(R.string.map_street_boulevard, 120f, 828f, angle = -47f),
    Park(R.string.map_park_name, 452f, 725f, tint = LabelTint.Park),
    River(R.string.map_river_name, 40f, 580f, tint = LabelTint.River),
}

private data class Block(val x: Float, val y: Float, val w: Float, val h: Float)

private val BLOCKS = listOf(
    Block(40f, 40f, 180f, 150f), Block(260f, 40f, 150f, 120f), Block(450f, 40f, 140f, 140f),
    Block(630f, 80f, 140f, 110f), Block(40f, 240f, 140f, 120f), Block(230f, 200f, 150f, 150f),
    Block(430f, 230f, 150f, 120f), Block(620f, 240f, 150f, 140f), Block(40f, 420f, 120f, 170f),
    Block(200f, 410f, 180f, 140f), Block(420f, 390f, 130f, 130f), Block(600f, 420f, 170f, 150f),
    Block(40f, 650f, 160f, 140f), Block(240f, 640f, 150f, 170f), Block(430f, 660f, 160f, 120f),
    Block(640f, 640f, 130f, 120f), Block(60f, 850f, 200f, 120f), Block(300f, 860f, 160f, 110f),
    Block(500f, 840f, 240f, 130f),
)

private val TREES = listOf(460f to 690f, 500f to 720f, 550f to 690f, 470f to 750f, 560f to 750f, 520f to 690f)
private val ROADS_H = listOf(205f, 385f, 615f, 820f)
private val ROADS_V = listOf(215f, 410f, 600f)

// ---------- Marcadores ----------

/** Caja tactil del marcador (48 dp); la punta queda en el centro inferior. */
internal const val PIN_BOX = 48f

/**
 * Marcador en forma de gota con el icono del tipo. El elegido crece 25 % y
 * muestra su nombre; los cerrados se ven deslavados.
 */
@Composable
internal fun BusinessPin(
    business: MapBusiness,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = business.type.style()
    val extra = MaterialTheme.petMateColors
    val color = if (business.isOpen) style.pin else lerp(style.pin, extra.ink3, 0.55f)
    val scale by animateFloatAsState(if (selected) 1.25f else 1f, label = "pinScale")
    val description = stringResource(R.string.map_pin_description, business.name, stringResource(style.label))
    // Esquinas de la gota: redondas salvo la inferior izquierda, que al girar
    // -45° queda como punta hacia abajo.
    val drop = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 50, bottomStartPercent = 10)

    Box(modifier = modifier.size(PIN_BOX.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = description
                    this.selected = selected
                },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .rotate(-45f)
                    .shadow(6.dp, drop)
                    .background(color, drop)
                    .border(3.dp, extra.onPin, drop),
            ) {
                Icon(
                    painter = painterResource(style.icon),
                    contentDescription = null,
                    tint = extra.onPin,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(45f),
                )
            }
        }
        if (selected) {
            // El nombre cuelga bajo la punta sin mover el marcador de su sitio.
            val labelShape = RoundedCornerShape(8.dp)
            Text(
                text = business.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (PIN_BOX + 12).dp)
                    .wrapContentSize(Alignment.TopCenter, unbounded = true)
                    .shadow(2.dp, labelShape)
                    .background(MaterialTheme.colorScheme.surface, labelShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Punto azul con halo que late: la ubicacion del usuario. */
@Composable
internal fun UserLocationDot(modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    val pulse = rememberInfiniteTransition(label = "me")
    val halo by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "halo",
    )
    val description = stringResource(R.string.map_you_are_here)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(66.dp)
            .semantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .size(66.dp)
                .scale(halo)
                .background(extra.userLocation.copy(alpha = 0.22f * (1.4f - halo)), CircleShape),
        )
        Box(
            Modifier
                .size(22.dp)
                .shadow(4.dp, CircleShape)
                .background(extra.userLocation, CircleShape)
                .border(4.dp, extra.onPin, CircleShape),
        )
    }
}
