package mx.petmate.app.domain.model

import mx.petmate.app.domain.usecase.ReviewDateClass

/**
 * Que tan urgente es una alerta del panel (spec 012, T-001).
 *
 * Es el mismo tipo que usan el carnet (008), el inicio (010) y el seguimiento
 * (015): un alias, no una copia. La spec 012 pide en T-003 que los umbrales
 * sean las constantes compartidas y no unas propias, asi que el vocabulario
 * tambien se comparte.
 */
typealias AlertSeverity = ReviewDateClass

/**
 * Seguimiento que necesita atencion, tal como se lista en el panel (spec 012, T-001, R-05 y R-07).
 *
 * [followupId] es con lo que se navega al seguimiento (R-07, C-07).
 * [severity] la calcula `ClassifyAlertUseCase` (T-003) contra el reloj: aqui se
 * guarda ya clasificada para que la pantalla no vuelva a calcularla.
 */
data class FollowUpAlert(
    val followupId: String,
    val petId: String,
    val petName: String,
    val nextReviewOn: String?,
    val severity: AlertSeverity = ReviewDateClass.None,
    /** Mensajes del dueno sin leer en el chat de este seguimiento (spec 016). */
    val unreadMessages: Int = 0,
) {
    val isOverdue: Boolean get() = severity == ReviewDateClass.Overdue

    /** Atrasada o a punto de vencer: lo que el resumen cuenta como "proxima". */
    val isSoon: Boolean get() = severity == ReviewDateClass.Urgent || severity == ReviewDateClass.Soon
}
