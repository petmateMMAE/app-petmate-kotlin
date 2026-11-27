package mx.petmate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.petmate.app.data.local.entity.ConsultationDraftEntity

/**
 * Acceso al borrador de consulta (spec 014, T-004).
 *
 * Toda consulta filtra por `ownerId`: es la regla que no se negocia, dos cuentas en el mismo
 * telefono no se ven los datos.
 */
@Dao
interface ConsultationDraftDao {

    @Query("SELECT * FROM consultation_drafts WHERE ownerId = :ownerId AND petId = :petId")
    suspend fun get(ownerId: String, petId: String): ConsultationDraftEntity?

    @Query("SELECT * FROM consultation_drafts WHERE ownerId = :ownerId AND petId = :petId")
    fun observe(ownerId: String, petId: String): Flow<ConsultationDraftEntity?>

    /** Todos los borradores de la cuenta, para una pantalla de "consultas a medias". */
    @Query("SELECT * FROM consultation_drafts WHERE ownerId = :ownerId ORDER BY savedAtMillis DESC")
    suspend fun getAll(ownerId: String): List<ConsultationDraftEntity>

    @Upsert
    suspend fun upsert(draft: ConsultationDraftEntity)

    @Query("DELETE FROM consultation_drafts WHERE ownerId = :ownerId AND petId = :petId")
    suspend fun delete(ownerId: String, petId: String)

    /** Borra los borradores de cualquier otra cuenta: el cambio de sesion no los hereda. */
    @Query("DELETE FROM consultation_drafts WHERE ownerId <> :ownerId")
    suspend fun deleteOthers(ownerId: String)

    @Query("DELETE FROM consultation_drafts")
    suspend fun deleteAll()
}
