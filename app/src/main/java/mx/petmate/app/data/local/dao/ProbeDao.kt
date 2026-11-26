package mx.petmate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mx.petmate.app.data.local.entity.ProbeEntity

/** DAO de la entidad de prueba (spec 001, T-011). Expone `Flow` para que la UI observe la base (P4). */
@Dao
interface ProbeDao {

    @Insert
    suspend fun insert(probe: ProbeEntity): Long

    @Query("SELECT * FROM probe ORDER BY id")
    fun observeAll(): Flow<List<ProbeEntity>>

    @Query("SELECT * FROM probe WHERE id = :id")
    suspend fun getById(id: Long): ProbeEntity?
}
