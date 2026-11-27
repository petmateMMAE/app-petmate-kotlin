package mx.petmate.app.data.sync

import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncEntityType

/**
 * Lo que este dispositivo borro y el servidor todavia podria devolver (spec 005, T-007; plan D-06).
 *
 * El problema que resuelve: sin red, el usuario borra una mascota. El borrado se encola, pero
 * mientras no se envie, cualquier lectura del servidor sigue trayendo esa mascota. Si no hubiera
 * forma de saber que esta borrada, se volveria a guardar en Room y "reapareceria" (R-09, C-08).
 *
 * Son dos preguntas distintas y las dos pasan por aqui:
 * - al **leer** del servidor, [isDeleted] y [filterDeleted] dicen que descartar;
 * - al **sincronizar**, [confirm] cierra la lapida cuando el servidor acepta el borrado.
 *
 * No encola nada: el borrado como operacion es de `SyncOutbox`, y la lapida solo recuerda que paso.
 * Separado a proposito, porque la cola se vacia al confirmarse y la lapida tiene que durar mas.
 */
class TombstoneStore(
    private val dao: TombstoneDao,
    private val dispatchers: DispatcherProvider,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Deja constancia de que [ref] se borro aqui. Idempotente: repetirlo solo refresca la marca. */
    suspend fun remember(ref: SyncEntityRef) = withContext(dispatchers.io) {
        dao.upsert(
            TombstoneEntity(
                entityType = ref.type.name,
                entityId = ref.id,
                deletedAtMillis = now(),
            ),
        )
    }

    /**
     * `true` si [ref] esta borrado en este dispositivo.
     *
     * Devuelve `true` tambien cuando la lapida ya esta confirmada, no solo cuando esta pendiente:
     * una respuesta del servidor que salio antes de que procesara el borrado puede llegar despues,
     * y hasta que la lapida se limpie por antiguedad el elemento sigue sin deber reaparecer.
     */
    suspend fun isDeleted(ref: SyncEntityRef): Boolean = withContext(dispatchers.io) {
        dao.find(ref.type.name, ref.id) != null
    }

    /**
     * Quita de [items] lo que este borrado aqui.
     *
     * Se usa al guardar una lectura del servidor. Hace **una** consulta por tipo en lugar de una
     * por elemento: una lista de mascotas con sus vacunas puede traer cientos de filas, y preguntar
     * una por una por cada una es lo que vuelve lenta una sincronizacion.
     */
    suspend fun <T> filterDeleted(
        type: SyncEntityType,
        items: List<T>,
        idOf: (T) -> String,
    ): List<T> = withContext(dispatchers.io) {
        if (items.isEmpty()) return@withContext items
        val deleted = dao.idsOfType(type.name).toSet()
        if (deleted.isEmpty()) items else items.filterNot { idOf(it) in deleted }
    }

    /**
     * El servidor acepto el borrado de [ref].
     *
     * La lapida se marca, **no se borra**: ver el porque en [TombstoneEntity.confirmed].
     */
    suspend fun confirm(ref: SyncEntityRef) = withContext(dispatchers.io) {
        dao.markConfirmed(ref.type.name, ref.id)
    }

    /** Lapidas que el servidor todavia no confirma, para reenviar su borrado al recuperar la red. */
    suspend fun pending(): List<SyncEntityRef> = withContext(dispatchers.io) {
        dao.unconfirmed().mapNotNull { row ->
            // Un tipo que ya no existe en el enum (version vieja de la app) se ignora en lugar de
            // tumbar la sincronizacion entera por una fila.
            SyncEntityType.entries.firstOrNull { it.name == row.entityType }
                ?.let { SyncEntityRef(type = it, id = row.entityId) }
        }
    }

    /**
     * Borra las lapidas confirmadas con mas de [retentionMillis] de antiguedad.
     *
     * Existe porque si no, la tabla solo crece. El valor por omision es generoso a proposito: el
     * costo de una fila de sobra es nada, y el de limpiarla antes de tiempo es que el elemento
     * reaparezca.
     */
    suspend fun pruneConfirmed(retentionMillis: Long = RETENCION_POR_OMISION): Int =
        withContext(dispatchers.io) {
            dao.pruneConfirmed(now() - retentionMillis)
        }

    companion object {
        /** 30 dias: mas que suficiente para que cualquier lectura en vuelo haya terminado. */
        const val RETENCION_POR_OMISION: Long = 30L * 24 * 60 * 60 * 1000
    }
}
