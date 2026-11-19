package mx.petmate.app.domain.sync

/** Estado de sincronizacion de un elemento, tal como lo ve el usuario (spec 005, R-10; plan D-08). */
enum class SyncStatus {
    /** No tiene cambios en la cola: lo que se ve ya esta en el servidor. */
    Synced,

    /** Tiene cambios guardados en el telefono que esperan su turno. */
    Pending,

    /** Uno de sus cambios se esta enviando. */
    Syncing,

    /** El servidor rechazo uno de sus cambios; se puede reintentar o descartar. */
    Error,
}
