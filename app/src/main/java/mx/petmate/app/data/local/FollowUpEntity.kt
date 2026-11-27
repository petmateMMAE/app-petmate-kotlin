package mx.petmate.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Seguimiento guardado para leerse sin conexion (spec 015, T-007, R-10). Guarda estado, fechas y notas, y se
 * filtra por el usuario de la sesion. Las notas van como JSON porque el backend las manda siempre completas.
 */
@Entity(tableName = "followups", primaryKeys = ["ownerId", "consultationId"])
data class FollowUpEntity(
    val ownerId: String,
    val consultationId: String,
    val id: String,
    val petId: String,
    val petName: String,
    val status: String,
    val nextReviewOn: String?,
    val isOverdue: Boolean,
    val notesJson: String,
    val conversationId: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

/** Acceso a los seguimientos guardados (spec 015, T-007). */
@Dao
interface FollowUpDao {

    @Query("SELECT * FROM followups WHERE ownerId = :ownerId AND consultationId = :consultationId")
    suspend fun get(ownerId: String, consultationId: String): FollowUpEntity?

    /**
     * El mismo seguimiento, pero observable (spec 015, T-007: "el DAO expone `Flow`").
     *
     * Es lo que hace que la pantalla se actualice sola cuando el servidor confirma un cambio de
     * estado: el repositorio escribe en Room y Room reemite. Con `get` suspend habria que acordarse
     * de volver a leer despues de cada escritura, y el dia que alguien se olvide la veterinaria
     * vera el estado viejo y creera que su cambio no se guardo.
     *
     * Emite null cuando no hay nada guardado para esa cuenta, que no es lo mismo que una lista
     * vacia: significa "este telefono nunca vio ese seguimiento".
     */
    @Query("SELECT * FROM followups WHERE ownerId = :ownerId AND consultationId = :consultationId")
    fun observe(ownerId: String, consultationId: String): Flow<FollowUpEntity?>

    /**
     * Todos los seguimientos guardados de la cuenta, para la bandeja del afiliado.
     *
     * Orden: primero los que tienen fecha de revision, de la mas proxima a la mas lejana, y al
     * final los que no tienen fecha. Es el orden en que hay que atenderlos; dejarlo al cliente
     * seria repetir la decision en cada pantalla.
     */
    @Query(
        """
        SELECT * FROM followups
        WHERE ownerId = :ownerId
        ORDER BY nextReviewOn IS NULL, nextReviewOn ASC
        """,
    )
    fun observeAll(ownerId: String): Flow<List<FollowUpEntity>>

    @Upsert
    suspend fun upsert(followup: FollowUpEntity)
}
