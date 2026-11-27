package mx.petmate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.petmate.app.data.local.entity.AffiliationDraftEntity

/**
 * Acceso al borrador de afiliacion (spec 011, T-006).
 *
 * Toda consulta filtra por `ownerId`: es la regla que no se negocia, dos
 * cuentas en el mismo telefono no se ven los datos.
 */
@Dao
interface AffiliationDraftDao {

    @Query("SELECT * FROM affiliation_drafts WHERE ownerId = :ownerId")
    suspend fun get(ownerId: String): AffiliationDraftEntity?

    @Query("SELECT * FROM affiliation_drafts WHERE ownerId = :ownerId")
    fun observe(ownerId: String): Flow<AffiliationDraftEntity?>

    @Upsert
    suspend fun upsert(draft: AffiliationDraftEntity)

    @Query("DELETE FROM affiliation_drafts WHERE ownerId = :ownerId")
    suspend fun delete(ownerId: String)

    /** Borra los borradores de cualquier otra cuenta: el cambio de sesion no los hereda (T-008). */
    @Query("DELETE FROM affiliation_drafts WHERE ownerId <> :ownerId")
    suspend fun deleteOthers(ownerId: String)

    @Query("DELETE FROM affiliation_drafts")
    suspend fun deleteAll()
}
