package mx.petmate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import mx.petmate.app.data.local.entity.GeneratedTextEntity

/** Cache de textos generados (spec 019, T-004). Solo devuelve el texto si la huella coincide. */
@Dao
interface GeneratedTextDao {

    @Query(
        "SELECT * FROM generated_texts WHERE ownerId = :ownerId AND requestKey = :requestKey " +
            "AND sourceFingerprint = :fingerprint",
    )
    suspend fun get(ownerId: String, requestKey: String, fingerprint: String): GeneratedTextEntity?

    /** Un contenido guarda un solo texto: el nuevo reemplaza al de la huella anterior. */
    @Upsert
    suspend fun upsert(text: GeneratedTextEntity)
}
