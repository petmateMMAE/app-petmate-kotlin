package mx.petmate.app.ui.auth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.petmate.app.R
import mx.petmate.app.ui.components.DecorativeQr
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/**
 * Bienvenida (spec 002, A-01): tres slides que explican la app y los botones
 * para crear cuenta o entrar.
 *
 * "Probar sin cuenta" solo aparece si se pasa [onDemoClick]; el modo demo
 * llega con la spec 010.
 */
@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDemoClick: (() -> Unit)? = null,
) {
    WelcomeContent(
        onLoginClick = onLoginClick,
        onRegisterClick = onRegisterClick,
        onDemoClick = onDemoClick,
        modifier = modifier,
    )
}

private enum class WelcomeSlide(@StringRes val title: Int, @StringRes val body: Int) {
    Carnet(R.string.welcome_slide1_title, R.string.welcome_slide1_body),
    Qr(R.string.welcome_slide2_title, R.string.welcome_slide2_body),
    Map(R.string.welcome_slide3_title, R.string.welcome_slide3_body),
}

@Composable
private fun WelcomeContent(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onDemoClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    initialSlide: Int = 0,
) {
    val slides = WelcomeSlide.entries
    val pagerState = rememberPagerState(initialPage = initialSlide) { slides.size }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 24.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val slide = slides[page]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    when (slide) {
                        WelcomeSlide.Carnet -> CarnetArt()
                        WelcomeSlide.Qr -> QrArt()
                        WelcomeSlide.Map -> MapArt()
                    }
                }
            }
        }

        PagerDots(pagerState = pagerState)

        val slide = slides[pagerState.currentPage]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 118.dp),
        ) {
            Text(
                text = stringResource(slide.title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )
            Text(
                text = stringResource(slide.body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.petMateColors.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }

        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PetMateButton(
                text = stringResource(R.string.welcome_register),
                onClick = onRegisterClick,
                icon = R.drawable.ic_pm_user,
            )
            PetMateButton(
                text = stringResource(R.string.welcome_login),
                onClick = onLoginClick,
                style = PetMateButtonStyle.Outline,
            )
            if (onDemoClick != null) {
                PetMateButton(
                    text = stringResource(R.string.welcome_demo),
                    onClick = onDemoClick,
                    style = PetMateButtonStyle.Ghost,
                    icon = R.drawable.ic_pm_play,
                )
            }
        }
    }
}

/** Puntos del carrusel: el activo es una pastilla morada de 28 dp. */
@Composable
private fun PagerDots(pagerState: PagerState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val extra = MaterialTheme.petMateColors
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 4.dp),
    ) {
        repeat(pagerState.pageCount) { index ->
            val active = index == pagerState.currentPage
            val width by animateDpAsState(if (active) 28.dp else 10.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary else extra.line,
                label = "dotColor",
            )
            val description = stringResource(R.string.welcome_step, index + 1)
            // Cada punto tiene un area tactil de 48 dp aunque se dibuje chico.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = if (active) 40.dp else 24.dp, height = 48.dp)
                    .clickable(role = Role.Tab) { scope.launch { pagerState.animateScrollToPage(index) } }
                    .semantics {
                        contentDescription = description
                        selected = active
                        role = Role.Tab
                    },
            ) {
                Box(
                    Modifier
                        .width(width)
                        .height(10.dp)
                        .background(color, CircleShape),
                )
            }
        }
    }
}

// ---------- Ilustraciones de cada slide ----------

/** Foto girada 2° con esquinas de 36 dp y las tarjetas flotando encima. */
@Composable
private fun SlideArt(
    @DrawableRes photo: Int,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(36.dp)
    Box(modifier = Modifier.size(width = 216.dp, height = 270.dp)) {
        Image(
            painter = painterResource(photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .rotate(-2f)
                .shadow(18.dp, shape)
                .clip(shape),
        )
        content()
    }
}

@Composable
private fun FloatingCard(
    @DrawableRes icon: Int,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    @DrawableRes subtitleIcon: Int? = null,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .shadow(12.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .background(iconBackground, RoundedCornerShape(11.dp)),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (subtitleIcon != null) {
                    Icon(
                        painterResource(subtitleIcon),
                        contentDescription = null,
                        tint = MaterialTheme.petMateColors.ink2,
                        modifier = Modifier.size(11.dp),
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.petMateColors.ink2,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Posicion de una tarjeta respecto a la foto, como `left/top/right/bottom` del CSS. */
private fun Modifier.place(
    scope: BoxScope,
    alignment: Alignment,
    x: Dp,
    y: Dp,
): Modifier = with(scope) { this@place.align(alignment).offset(x = x, y = y) }

@Composable
private fun CarnetArt() {
    val extra = MaterialTheme.petMateColors
    SlideArt(photo = R.drawable.welcome_slide_carnet) {
        FloatingCard(
            icon = R.drawable.ic_pm_alert,
            iconBackground = extra.alertSoft,
            iconTint = extra.alert,
            title = stringResource(R.string.welcome_art_allergy),
            subtitle = stringResource(R.string.welcome_art_allergy_status),
            modifier = Modifier.place(this, Alignment.TopEnd, x = 18.dp, y = 34.dp),
        )
        FloatingCard(
            icon = R.drawable.ic_pm_syringe,
            iconBackground = extra.sun,
            iconTint = extra.onSun,
            title = stringResource(R.string.welcome_art_vaccine),
            subtitle = stringResource(R.string.welcome_art_vaccine_when),
            modifier = Modifier.place(this, Alignment.TopStart, x = (-14).dp, y = 150.dp),
        )
        FloatingCard(
            icon = R.drawable.ic_pm_weight,
            iconBackground = extra.shopSoft,
            iconTint = extra.shop,
            title = stringResource(R.string.welcome_art_weight),
            subtitle = stringResource(R.string.welcome_art_weight_label),
            modifier = Modifier.place(this, Alignment.BottomEnd, x = 6.dp, y = (-18).dp),
        )
    }
}

@Composable
private fun QrArt() {
    val extra = MaterialTheme.petMateColors
    SlideArt(photo = R.drawable.welcome_slide_qr) {
        FloatingCard(
            icon = R.drawable.ic_pm_cross,
            iconBackground = extra.pinVet,
            iconTint = extra.onSplash,
            title = stringResource(R.string.business_type_vet),
            subtitle = stringResource(R.string.welcome_art_vet_sees),
            modifier = Modifier.place(this, Alignment.TopStart, x = (-16).dp, y = 40.dp),
        )
        val shape = RoundedCornerShape(20.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .place(this, Alignment.BottomEnd, x = 22.dp, y = 6.dp)
                .shadow(14.dp, shape)
                .background(extra.qrBackground, shape)
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
        ) {
            val code = stringResource(R.string.welcome_art_qr_code)
            DecorativeQr(seed = code, modifier = Modifier.size(104.dp))
            Text(
                text = code,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                color = extra.qrInk,
            )
        }
    }
}

@Composable
private fun MapArt() {
    val extra = MaterialTheme.petMateColors
    SlideArt(photo = R.drawable.welcome_slide_map) {
        FloatingCard(
            icon = R.drawable.ic_pm_cross,
            iconBackground = extra.pinVet,
            iconTint = extra.onSplash,
            title = stringResource(R.string.welcome_art_biz_vet),
            subtitle = stringResource(R.string.welcome_art_distance, stringResource(R.string.welcome_art_biz_vet_km)),
            subtitleIcon = R.drawable.ic_pm_navigation,
            modifier = Modifier.place(this, Alignment.TopStart, x = (-12).dp, y = 60.dp),
        )
        FloatingCard(
            icon = R.drawable.ic_pm_scissors,
            iconBackground = extra.pinSpa,
            iconTint = extra.onSplash,
            title = stringResource(R.string.welcome_art_biz_spa),
            subtitle = stringResource(R.string.welcome_art_distance, stringResource(R.string.welcome_art_biz_spa_km)),
            subtitleIcon = R.drawable.ic_pm_navigation,
            modifier = Modifier.place(this, Alignment.TopEnd, x = 16.dp, y = 140.dp),
        )
        FloatingCard(
            icon = R.drawable.ic_pm_bag,
            iconBackground = extra.pinShop,
            iconTint = extra.onSplash,
            title = stringResource(R.string.welcome_art_biz_shop),
            subtitle = stringResource(R.string.welcome_art_distance, stringResource(R.string.welcome_art_biz_shop_km)),
            subtitleIcon = R.drawable.ic_pm_navigation,
            modifier = Modifier.place(this, Alignment.BottomStart, x = (-6).dp, y = (-30).dp),
        )
    }
}

// ---------- Previews ----------

@PetMatePreviews
@Composable
private fun WelcomeCarnetPreview() {
    PetMateEdelTheme {
        WelcomeContent(onLoginClick = {}, onRegisterClick = {}, onDemoClick = {})
    }
}

@PetMatePreviews
@Composable
private fun WelcomeQrPreview() {
    PetMateEdelTheme {
        WelcomeContent(onLoginClick = {}, onRegisterClick = {}, onDemoClick = {}, initialSlide = 1)
    }
}

@PetMatePreviews
@Composable
private fun WelcomeMapPreview() {
    PetMateEdelTheme {
        WelcomeContent(onLoginClick = {}, onRegisterClick = {}, onDemoClick = {}, initialSlide = 2)
    }
}

@PetMatePreviews
@Composable
private fun WelcomeWithoutDemoPreview() {
    PetMateEdelTheme {
        WelcomeContent(onLoginClick = {}, onRegisterClick = {}, onDemoClick = null)
    }
}
