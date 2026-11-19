package mx.petmate.app.domain.usecase

/**
 * Umbrales de color para fechas proximas (spec 015, R-12). Es la constante que deben compartir 008 (carnet),
 * 010 (inicio), 012 (panel) y 015 (seguimiento) para que "pronto" signifique lo mismo en toda la app.
 *
 * Son los de la decision aprobada en 010 (R-07, C-07): rojo con 3 dias o menos, amarillo con 14 o menos.
 */
object DueThresholds {
    /** A esta distancia o menos la fecha se pinta en rojo. */
    const val URGENT_DAYS = 3

    /** A esta distancia o menos (y mas que [URGENT_DAYS]) se pinta en amarillo. */
    const val SOON_DAYS = 14

    /** Nivel de una fecha que esta a [days] dias de hoy (negativo si ya paso). El unico calculo de la app. */
    fun level(days: Int): ReviewDateClass = when {
        days < 0 -> ReviewDateClass.Overdue
        days <= URGENT_DAYS -> ReviewDateClass.Urgent
        days <= SOON_DAYS -> ReviewDateClass.Soon
        else -> ReviewDateClass.Normal
    }
}
