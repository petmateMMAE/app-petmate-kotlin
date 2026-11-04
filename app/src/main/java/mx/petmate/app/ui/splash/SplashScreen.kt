package mx.petmate.app.ui.splash

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.petmate.app.R
import mx.petmate.app.ui.components.PetMateLogo
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/**
 * Splash de la maqueta: fondo morado, logo que flota, nombre, lema y tres
 * fotos de mascotas.
 *
 * Con [onStartClick] muestra la pastilla "Toca para empezar"; sin ella sirve
 * como pantalla de espera mientras se revisa la sesion.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onStartClick: (() -> Unit)? = null,
) {
    SplashContent(onStartClick = onStartClick, modifier = modifier)
}

@Composable
private fun SplashContent(
    onStartClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val extra = MaterialTheme.petMateColors
    val onSplash = extra.onSplash

    val bob = rememberInfiniteTransition(label = "logo")
    val logoOffset by bob.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200), RepeatMode.Reverse),
        label = "logoOffset",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(extra.splash),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
        ) {
            PetMateLogo(
                size = 112.dp,
                modifier = Modifier.graphicsLayer { translationY = logoOffset * density },
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = onSplash,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                color = onSplash.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
            Spacer(Modifier.height(14.dp))
            SplashPhotos()
        }

        if (onStartClick != null) {
            Surface(
                onClick = onStartClick,
                shape = CircleShape,
                color = onSplash.copy(alpha = 0.18f),
                contentColor = onSplash,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(bottom = 24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 18.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pm_next),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.splash_tap_to_start),
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashPhotos(modifier: Modifier = Modifier) {
    val photos = listOf(R.drawable.splash_firulais, R.drawable.splash_michi, R.drawable.splash_nala)
    // Las fotos se enciman 14 dp, como en la maqueta.
    Row(horizontalArrangement = Arrangement.spacedBy((-14).dp), modifier = modifier) {
        photos.forEach { photo -> RoundPhoto(photo = photo) }
    }
}

@Composable
private fun RoundPhoto(@DrawableRes photo: Int, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.petMateColors.onSplash.copy(alpha = 0.9f)
    Image(
        painter = painterResource(photo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .shadow(10.dp, CircleShape)
            .size(76.dp)
            .clip(CircleShape)
            .border(4.dp, ring, CircleShape),
    )
}

@PetMatePreviews
@Composable
private fun SplashPreview() {
    PetMateEdelTheme {
        SplashContent(onStartClick = {})
    }
}

@PetMatePreviews
@Composable
private fun SplashCheckingSessionPreview() {
    PetMateEdelTheme {
        SplashContent(onStartClick = null)
    }
}
