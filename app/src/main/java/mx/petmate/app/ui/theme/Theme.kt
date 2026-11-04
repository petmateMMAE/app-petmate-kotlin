package mx.petmate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = LightBrand,
    onPrimary = LightOnBrand,
    primaryContainer = LightBrandSoft,
    onPrimaryContainer = LightBrand2,
    secondary = LightSun,
    onSecondary = OnSun,
    secondaryContainer = LightSunSoft,
    onSecondaryContainer = LightSunInk,
    tertiary = LightOk,
    onTertiary = White,
    tertiaryContainer = LightOkSoft,
    onTertiaryContainer = LightOk,
    background = LightBg,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightInk2,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface2,
    surfaceContainerHighest = LightSurface2,
    outline = LightLine,
    outlineVariant = LightLine,
    error = LightAlert,
    onError = White,
    errorContainer = LightAlertSoft,
    onErrorContainer = LightAlert,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrand,
    onPrimary = DarkOnBrand,
    primaryContainer = DarkBrandSoft,
    onPrimaryContainer = DarkBrand2,
    secondary = DarkSun,
    onSecondary = OnSun,
    secondaryContainer = DarkSunSoft,
    onSecondaryContainer = DarkSunInk,
    tertiary = DarkOk,
    onTertiary = DarkBg,
    tertiaryContainer = DarkOkSoft,
    onTertiaryContainer = DarkOk,
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkInk2,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface2,
    surfaceContainerHighest = DarkSurface2,
    outline = DarkLine,
    outlineVariant = DarkLine,
    error = DarkAlert,
    onError = DarkBg,
    errorContainer = DarkAlertSoft,
    onErrorContainer = DarkAlert,
)

/** Esquinas de la maqueta: chips 12, campos 14, botones 16, tarjetas 24. */
private val PetMateShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Tema de PetMate.
 *
 * Sin color dinamico (Android 12+): la app siempre usa la paleta de la marca
 * para verse igual que la maqueta aprobada.
 */
@Composable
fun PetMateEdelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val petMateColors = if (darkTheme) DarkPetMateColors else LightPetMateColors

    CompositionLocalProvider(LocalPetMateColors provides petMateColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = PetMateShapes,
            content = content,
        )
    }
}
