package mx.petmate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import mx.petmate.app.ui.theme.petMateColors

private const val GRID = 27
private const val FINDER = 7

/**
 * Dibujo de un QR para ilustraciones (bienvenida). NO es un QR real ni se
 * puede escanear: reproduce el patron pseudoaleatorio de la maqueta a partir
 * de [seed]. El QR de verdad llega con la spec 009.
 */
@Composable
fun DecorativeQr(seed: String, modifier: Modifier = Modifier) {
    val cells = remember(seed) { qrCells(seed) }
    val ink = MaterialTheme.petMateColors.qrInk
    val paper = MaterialTheme.petMateColors.qrBackground

    Canvas(modifier = modifier) {
        val unit = size.minDimension / (GRID + 2)
        fun at(x: Float, y: Float) = Offset((x + 1) * unit, (y + 1) * unit)

        drawRect(paper)
        cells.forEach { (x, y) ->
            drawRect(ink, topLeft = at(x.toFloat(), y.toFloat()), size = Size(unit * 1.02f, unit * 1.02f))
        }
        listOf(0 to 0, GRID - FINDER to 0, 0 to GRID - FINDER).forEach { (fx, fy) ->
            val x = fx.toFloat()
            val y = fy.toFloat()
            drawRoundRect(ink, at(x, y), Size(7 * unit, 7 * unit), CornerRadius(1.6f * unit))
            drawRoundRect(paper, at(x + 1, y + 1), Size(5 * unit, 5 * unit), CornerRadius(1.1f * unit))
            drawRoundRect(ink, at(x + 2, y + 2), Size(3 * unit, 3 * unit), CornerRadius(0.8f * unit))
        }
    }
}

/** Mismo generador xorshift que la maqueta, para que el dibujo salga igual. */
private fun qrCells(seed: String): List<Pair<Int, Int>> {
    var h = 0
    seed.forEach { h = h * 31 + it.code }
    fun next(): Double {
        h = h xor (h shl 13)
        h = h xor (h ushr 17)
        h = h xor (h shl 5)
        return h.toUInt().toDouble() / 4_294_967_296.0
    }
    fun isFinder(x: Int, y: Int) =
        (x < FINDER && y < FINDER) || (x >= GRID - FINDER && y < FINDER) || (x < FINDER && y >= GRID - FINDER)

    val cells = mutableListOf<Pair<Int, Int>>()
    for (y in 0 until GRID) {
        for (x in 0 until GRID) {
            if (isFinder(x, y)) continue
            if (x in 10..16 && y in 10..16) continue
            if (next() < 0.48) cells += x to y
        }
    }
    return cells
}
