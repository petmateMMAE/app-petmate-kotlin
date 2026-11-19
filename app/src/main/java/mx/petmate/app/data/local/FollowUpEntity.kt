package mx.petmate.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

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

    @Upsert
    suspend fun upsert(followup: FollowUpEntity)
}
