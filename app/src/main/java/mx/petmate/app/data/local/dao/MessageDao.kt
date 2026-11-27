package mx.petmate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.petmate.app.data.local.entity.MessageEntity

/**
 * Historial del chat guardado en el telefono (spec 016, T-004).
 *
 * Toda consulta filtra por `ownerId`: dos cuentas en el mismo telefono no se ven los mensajes
 * (C-14). No hay ninguna consulta sin ese filtro, a proposito.
 */
@Dao
interface MessageDao {

    /**
     * La conversacion completa, en el orden en que se lee.
     *
     * **Los pendientes van al final.** Un mensaje que aun no sale del telefono tiene
     * `seq = 0` ([MessageEntity.PENDING_SEQ]), que es menor que cualquier secuencia real: ordenar
     * por `seq` a secas los pondria **arriba de todo**, como si fueran los mas viejos de la
     * conversacion. `seq = 0` primero da 0 a los confirmados y 1 a los pendientes, asi que los
     * confirmados se ordenan entre si por `seq` y los pendientes quedan abajo por fecha, que es
     * donde el usuario acaba de escribirlos.
     */
    @Query(
        "SELECT * FROM messages WHERE ownerId = :ownerId AND conversationId = :conversationId " +
            "ORDER BY seq = 0, seq ASC, createdAtMillis ASC",
    )
    fun observeConversation(ownerId: String, conversationId: String): Flow<List<MessageEntity>>

    /** La misma consulta, de una sola vez. */
    @Query(
        "SELECT * FROM messages WHERE ownerId = :ownerId AND conversationId = :conversationId " +
            "ORDER BY seq = 0, seq ASC, createdAtMillis ASC",
    )
    suspend fun getConversation(ownerId: String, conversationId: String): List<MessageEntity>

    /**
     * El mensaje confirmado mas avanzado de la conversacion: es el **cursor** para pedirle al
     * servidor lo que falta (`after` recibe su `id`).
     *
     * Excluye los pendientes: su `seq` es 0 y su `id` es local, asi que el servidor no lo conoce y
     * responderia 404.
     */
    @Query(
        "SELECT * FROM messages WHERE ownerId = :ownerId AND conversationId = :conversationId " +
            "AND seq > 0 ORDER BY seq DESC LIMIT 1",
    )
    suspend fun lastConfirmed(ownerId: String, conversationId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE ownerId = :ownerId AND id = :id")
    suspend fun get(ownerId: String, id: String): MessageEntity?

    /** Los que aun no salieron del telefono, del mas viejo al mas nuevo: el orden en que se reintentan. */
    @Query(
        "SELECT * FROM messages WHERE ownerId = :ownerId AND status = :status " +
            "ORDER BY createdAtMillis ASC",
    )
    suspend fun byStatus(ownerId: String, status: String): List<MessageEntity>

    /** Cuantos mensajes ajenos sin leer hay en una conversacion. */
    @Query(
        "SELECT COUNT(*) FROM messages WHERE ownerId = :ownerId AND conversationId = :conversationId " +
            "AND isMine = 0 AND seq > :upToSeq",
    )
    fun observeUnread(ownerId: String, conversationId: String, upToSeq: Long): Flow<Int>

    @Upsert
    suspend fun upsert(messages: List<MessageEntity>)

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query("DELETE FROM messages WHERE ownerId = :ownerId AND id = :id")
    suspend fun delete(ownerId: String, id: String)

    /** Borra los mensajes de cualquier otra cuenta: el cambio de sesion no los hereda (C-14). */
    @Query("DELETE FROM messages WHERE ownerId <> :ownerId")
    suspend fun deleteOthers(ownerId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}
