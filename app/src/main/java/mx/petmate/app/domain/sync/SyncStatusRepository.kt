package mx.petmate.app.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Lo que la UI necesita saber de la sincronizacion, sin conocer la cola (spec 005, T-008).
 *
 * Vive en `domain` y no en `data/sync` como dice la tarea: la UI no puede importar de `data`
 * (constitucion P3, que gana sobre la tarea).
 */
interface SyncStatusRepository {

    /** Estado de [ref], que cambia solo cuando la cola cambia. */
    fun observeStatus(ref: SyncEntityRef): Flow<SyncStatus>

    /** Vuelve a poner en espera los cambios de [ref] que el servidor rechazo (R-10). */
    suspend fun retry(ref: SyncEntityRef)
}
