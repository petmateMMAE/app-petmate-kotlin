package mx.petmate.app.domain.usecase

import mx.petmate.app.domain.model.FollowUpStatus

/**
 * Transiciones validas del seguimiento (spec 015, T-002, R-06).
 *
 * Desde "Dado de alta" no sale ninguna: el alta cierra el seguimiento. Q-05 (reabrir) sigue abierta; mientras
 * no se decida, un alta no se reabre y para volver a tratar a la mascota se abre un seguimiento nuevo.
 */
object FollowUpTransition {

    /** Todo seguimiento nuevo arranca aqui. */
    val INITIAL = FollowUpStatus.Treating

    private val allowed: Map<FollowUpStatus, Set<FollowUpStatus>> = mapOf(
        FollowUpStatus.Treating to setOf(FollowUpStatus.Improving, FollowUpStatus.Discharged),
        FollowUpStatus.Improving to setOf(FollowUpStatus.Treating, FollowUpStatus.Discharged),
        FollowUpStatus.Discharged to emptySet(),
    )

    fun isAllowed(from: FollowUpStatus, to: FollowUpStatus): Boolean = to in nextOf(from)

    /** Estados a los que se puede pasar desde [from]; la pantalla solo ofrece estos. */
    fun nextOf(from: FollowUpStatus): Set<FollowUpStatus> = allowed.getValue(from)

    fun isClosed(status: FollowUpStatus): Boolean = nextOf(status).isEmpty()
}
