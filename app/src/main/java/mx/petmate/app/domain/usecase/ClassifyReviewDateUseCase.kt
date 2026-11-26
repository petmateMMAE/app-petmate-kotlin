package mx.petmate.app.domain.usecase

/** Que tan cerca esta una fecha proxima: revision (015), vacuna o desparasitacion (008, 010). */
enum class ReviewDateClass {
    /** Ya paso la fecha. */
    Overdue,

    /** Faltan [DueThresholds.URGENT_DAYS] dias o menos: rojo. */
    Urgent,

    /** Faltan [DueThresholds.SOON_DAYS] dias o menos: amarillo. */
    Soon,

    Normal,

    /** Sin fecha de revision o con una fecha que no se puede leer. */
    None,
}

/**
 * Clasifica la fecha de proxima revision (`yyyy-MM-dd`) contra "hoy" (spec 015, T-003, R-11, R-12).
 *
 * "Hoy" se inyecta para que las pruebas no dependan del reloj; por defecto es la fecha del telefono. Los
 * umbrales salen de [DueThresholds]: este caso de uso no define los suyos.
 */
class ClassifyReviewDateUseCase(private val today: () -> String = PetDates::today) {

    operator fun invoke(reviewOn: String?): ReviewDateClass {
        if (reviewOn.isNullOrBlank()) return ReviewDateClass.None
        val days = PetDates.daysBetween(today(), reviewOn) ?: return ReviewDateClass.None
        return DueThresholds.level(days)
    }
}
