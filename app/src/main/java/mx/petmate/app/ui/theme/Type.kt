package mx.petmate.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mx.petmate.app.R

/** Baloo 2: titulos (la "display" de la maqueta). */
val Baloo2 = FontFamily(
    Font(R.font.baloo2_medium, FontWeight.Medium),
    Font(R.font.baloo2_semibold, FontWeight.SemiBold),
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),
)

/** Figtree: todo el texto corrido, etiquetas y botones. */
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

/*
 * Correspondencia con la maqueta:
 *  displayMedium  → splash "PetMate"            (52 / 800)
 *  headlineLarge  → .big-title                  (30 / 800)
 *  headlineMedium → .slide-txt h2               (29 / 800)
 *  headlineSmall  → .topbar h1                  (22 / 700)
 *  titleLarge     → .biz-sheet h3               (18 / 700)
 *  titleMedium    → botones .btn                (16 / 700)
 *  titleSmall     → etiquetas de campo .fl      (14 / 700)
 *  bodyLarge      → texto de entrada y .lead    (16 / 400)
 *  bodyMedium     → texto base                  (15 / 400)
 *  bodySmall      → ayudas y errores            (13 / 400)
 *  labelLarge     → chips y filtros             (13 / 700)
 *  labelMedium    → pestañas y acciones         (12 / 700)
 *  labelSmall     → etiquetas del mapa          (11 / 700)
 */
val Typography = Typography(
    displayMedium = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 52.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.015).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 29.sp,
        lineHeight = 31.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).em,
    ),
    titleLarge = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)
