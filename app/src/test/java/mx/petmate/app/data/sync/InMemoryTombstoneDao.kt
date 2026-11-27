package mx.petmate.app.data.sync

/**
 * `TombstoneDao` en memoria para las pruebas de JVM (spec 005, T-007).
 *
 * Imita lo que importa del comportamiento de Room: la llave primaria compuesta tipo + id, y que
 * `upsert` reemplace en lugar de duplicar. La persistencia de verdad se prueba en `androidTest`,
 * igual que con la cola (`InMemorySyncOperationDao`).
 */
class InMemoryTombstoneDao : TombstoneDao {

    private val rows = linkedMapOf<Pair<String, String>, TombstoneEntity>()

    /** Cuantas filas hay, para comprobar que borrar dos veces no deja dos lapidas. */
    val size: Int get() = rows.size

    override suspend fun upsert(tombstone: TombstoneEntity) {
        rows[tombstone.entityType to tombstone.entityId] = tombstone
    }

    override suspend fun find(type: String, id: String): TombstoneEntity? = rows[type to id]

    override suspend fun idsOfType(type: String): List<String> =
        rows.values.filter { it.entityType == type }.map { it.entityId }

    override suspend fun markConfirmed(type: String, id: String) {
        rows[type to id]?.let { rows[type to id] = it.copy(confirmed = true) }
    }

    override suspend fun unconfirmed(): List<TombstoneEntity> = rows.values.filterNot { it.confirmed }

    override suspend fun pruneConfirmed(beforeMillis: Long): Int {
        val victims = rows.filterValues { it.confirmed && it.deletedAtMillis < beforeMillis }.keys.toList()
        victims.forEach(rows::remove)
        return victims.size
    }
}
