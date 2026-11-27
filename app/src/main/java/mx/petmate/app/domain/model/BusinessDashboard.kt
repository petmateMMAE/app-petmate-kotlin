package mx.petmate.app.domain.model

/**
 * Panel del afiliado: los tres bloques de la maqueta (spec 012, T-001, R-01 a R-06).
 *
 * Portada del negocio, resumen con el conteo de alertas, y las listas de
 * alertas y de mascotas atendidas.
 *
 * El resumen se guarda contado para que la pantalla no recorra las listas, y
 * porque si el backend acaba devolviendo el resumen armado (Q-01) los conteos
 * llegan de ahi sin cambiar este modelo.
 */
data class BusinessDashboard(
    val businessId: String,
    val businessName: String,
    val kind: BusinessKind?,
    val photoUrl: String? = null,
    val summary: DashboardSummary = DashboardSummary(),
    val alerts: List<FollowUpAlert> = emptyList(),
    val attended: List<AttendedPet> = emptyList(),
) {
    val isEmpty: Boolean get() = alerts.isEmpty() && attended.isEmpty()
}

/**
 * Conteos de la portada (spec 012, A-02 y C-06): atrasadas, proximas y mensajes
 * sin leer.
 */
data class DashboardSummary(
    val overdue: Int = 0,
    val soon: Int = 0,
    val unreadMessages: Int = 0,
) {
    companion object {
        /** Cuenta el resumen a partir de las alertas ya clasificadas. */
        fun of(alerts: List<FollowUpAlert>) = DashboardSummary(
            overdue = alerts.count(FollowUpAlert::isOverdue),
            soon = alerts.count(FollowUpAlert::isSoon),
            unreadMessages = alerts.sumOf(FollowUpAlert::unreadMessages),
        )
    }
}
