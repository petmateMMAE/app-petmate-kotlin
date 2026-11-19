package mx.petmate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad desechable que solo prueba que la base funciona (spec 001, T-011).
 *
 * Se borra cuando la primera entidad real de producto llegue a la base y tenga su propia prueba.
 */
@Entity(tableName = "probe")
data class ProbeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val createdAt: Date,
)
