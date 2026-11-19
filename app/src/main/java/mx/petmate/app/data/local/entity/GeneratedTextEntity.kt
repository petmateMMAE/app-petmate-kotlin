package mx.petmate.app.data.local.entity

import androidx.room.Entity

/**
 * Texto generado guardado por usuario y contenido, con la huella del original (spec 019, T-004, D-04). La
 * consulta filtra por la huella: si el original cambia, ya no encuentra el texto viejo.
 */
@Entity(tableName = "generated_texts", primaryKeys = ["ownerId", "requestKey"])
data class GeneratedTextEntity(
    val ownerId: String,
    val requestKey: String,
    val sourceFingerprint: String,
    val text: String,
    val generatedAtMillis: Long,
)
