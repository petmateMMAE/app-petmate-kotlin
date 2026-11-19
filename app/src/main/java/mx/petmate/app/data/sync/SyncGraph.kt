package mx.petmate.app.data.sync

import mx.petmate.app.domain.sync.SyncEntityType
import mx.petmate.app.domain.sync.SyncPolicy

/**
 * Grafo de dependencias entre entidades (spec 005, T-003; plan D-04). El nivel de una entidad es la cantidad
 * de padres que tiene encima: la mascota es nivel 0 y sus registros del carnet nivel 1.
 *
 * Un ciclo es un error de programacion en la politica, no un dato del usuario: se detecta al construir el
 * grafo y la app falla al arrancar en lugar de quedarse sin sincronizar en silencio (plan RT-03).
 */
class SyncGraph(private val parents: Map<SyncEntityType, SyncEntityType?>) {

    private val levels: Map<SyncEntityType, Int> = parents.keys.associateWith { computeLevel(it) }

    /** Nivel de [type] en el grafo. Una entidad sin padre declarado es nivel 0. */
    fun level(type: SyncEntityType): Int = levels[type] ?: 0

    private fun computeLevel(type: SyncEntityType): Int {
        val seen = mutableSetOf(type)
        var level = 0
        var current = parents[type]
        while (current != null) {
            check(seen.add(current)) { "Ciclo en el grafo de sincronizacion: ${seen.joinToString(" -> ")} -> $current" }
            level++
            current = parents[current]
        }
        return level
    }

    companion object {
        /** Grafo tomado de la tabla unica de `SyncPolicy` (R-13). */
        fun fromPolicy(): SyncGraph =
            SyncGraph(SyncEntityType.entries.associateWith { SyncPolicy.rulesFor(it).parentType })
    }
}
