package mx.petmate.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Un mensaje del chat de seguimiento guardado en el telefono (spec 016, T-004).
 *
 * ## La llave es (ownerId, id)
 *
 * `ownerId` entra en la llave porque **toda consulta de Room filtra por ownerId**: dos cuentas en
 * el mismo telefono no se ven los datos, y eso no se negocia (C-14). El `id` del mensaje lo asigna
 * el servidor y ya es unico, pero sin `ownerId` en la llave dos sesiones compartirian fila.
 *
 * ## La imagen NO se guarda aqui (D-05)
 *
 * [hasPhoto] dice que el mensaje trae foto; la URL **no** se guarda. Se pide a
 * `messages/get-photo-url` cuando se va a mostrar, porque viene **firmada y con expiracion**:
 * guardarla seria guardar una URL muerta, y guardar la imagen misma llenaria la base.
 *
 * ## El orden es [seq], no la fecha
 *
 * `seq` es monotono por conversacion y lo asigna el servidor. [createdAtMillis] llega en segundos
 * y dos mensajes del mismo segundo empatarian; ademas el reloj de un telefono puede estar mal. El
 * indice por `(ownerId, conversationId, seq)` es el que sirve las consultas de la conversacion.
 *
 * ## Los pendientes tienen seq = 0
 *
 * Un mensaje que todavia no sale del telefono no tiene secuencia del servidor. Se guarda con
 * [seq] en [PENDING_SEQ] y [localId] con la clave de idempotencia, para poder reemplazarlo por el
 * mensaje de verdad cuando el servidor lo confirme sin que aparezca dos veces. Como 0 es menor que
 * cualquier secuencia real, un pendiente ordenaria **arriba** de todo: por eso las consultas
 * ordenan por `seq = 0 primero? no` — ver `MessageDao.observeConversation`, que los manda al final.
 */
@Entity(
    tableName = "messages",
    primaryKeys = ["ownerId", "id"],
    indices = [Index(value = ["ownerId", "conversationId", "seq"])],
)
data class MessageEntity(
    val ownerId: String,
    val id: String,
    val conversationId: String,
    /** Secuencia del servidor; [PENDING_SEQ] mientras el mensaje no se haya confirmado. */
    val seq: Long,
    /** `Text`, `System` o `Photo`, como los nombra el backend (`MessageKind`). */
    val kind: String,
    val isMine: Boolean,
    val text: String,
    val createdAtMillis: Long,
    val hasPhoto: Boolean,
    /** `Pending`, `Sent`, `Delivered` o `Failed`; los dos primeros y el ultimo son estados locales. */
    val status: String,
    /** Clave de idempotencia de un mensaje aun no confirmado; `null` en los que vinieron del servidor. */
    val localId: String?,
) {
    companion object {
        /** Secuencia de un mensaje que todavia no confirma el servidor. */
        const val PENDING_SEQ = 0L
    }
}
