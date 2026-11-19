package mx.petmate.app.data.sync

import mx.petmate.app.domain.sync.SyncEntityRef

/**
 * Decide que operaciones de la cola se pueden enviar y en que orden (spec 005, T-003; plan SY-04, D-04).
 *
 * - Primero los niveles bajos del grafo y, dentro de cada nivel, FIFO: una vacuna nunca sale antes que su
 *   mascota aunque se haya encolado antes (R-03).
 * - Un elemento con una operacion enviandose o con error permanente queda bloqueado: sus operaciones
 *   siguientes y las de sus hijos esperan, para no mandar una edicion antes que su alta ni una vacuna de una
 *   mascota que el servidor no tiene.
 *
 * Es logica pura: no envia nada ni toca la base. El motor manda la lista en orden y, si algo falla, vuelve a
 * pedir el plan con la cola actualizada.
 */
class SyncScheduler(private val graph: SyncGraph = SyncGraph.fromPolicy()) {

    /** Operaciones pendientes listas para enviarse, en el orden en que deben salir. */
    fun plan(queue: List<QueuedOperation>): List<QueuedOperation> {
        val ordered = queue.sortedWith(
            compareBy<QueuedOperation>(
                { graph.level(it.operation.target.type) },
                { it.operation.createdAtMillis },
                { it.localId },
            ),
        )
        val blocked = mutableSetOf<SyncEntityRef>()
        val ready = mutableListOf<QueuedOperation>()
        for (item in ordered) {
            val target = item.operation.target
            val parentBlocked = item.operation.dependsOn?.let { it in blocked } ?: false
            when {
                item.state != SyncOperationState.Pending -> blocked += target
                target in blocked || parentBlocked -> blocked += target
                else -> ready += item
            }
        }
        return ready
    }
}
