package mx.petmate.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import mx.petmate.app.domain.usecase.ClassifyReviewDateUseCase
import mx.petmate.app.domain.usecase.PetDates
import mx.petmate.app.domain.usecase.ReviewDateClass
import mx.petmate.app.ui.theme.petMateColors

/** Fondo y texto para una fecha proxima. */
data class DueColors(val container: Color, val content: Color)

/**
 * Colores de una fecha proxima con los umbrales unicos de `DueThresholds` (spec 010, R-07; spec 015, R-12):
 * rojo si ya paso o faltan 3 dias o menos, amarillo si faltan 14 o menos, y [neutral] si falta mas o no hay fecha.
 */
@Composable
fun dueColors(date: String?, neutral: DueColors? = null, today: String = PetDates.today()): DueColors =
    dueColors(ClassifyReviewDateUseCase { today }(date), neutral)

/**
 * Lo mismo, cuando la fecha ya viene clasificada.
 *
 * Lo usa la lista de alertas del panel (spec 012, T-011), que recibe la
 * severidad ya calculada y no debe volver a clasificarla.
 */
@Composable
fun dueColors(level: ReviewDateClass, neutral: DueColors? = null): DueColors {
    val colors = MaterialTheme.petMateColors
    return when (level) {
        ReviewDateClass.Overdue, ReviewDateClass.Urgent -> DueColors(colors.alertSoft, colors.alert)
        ReviewDateClass.Soon -> DueColors(colors.sunSoft, colors.sunInk)
        else -> neutral ?: DueColors(colors.surface2, colors.ink2)
    }
}
