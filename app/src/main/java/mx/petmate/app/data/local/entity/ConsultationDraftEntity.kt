package mx.petmate.app.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

/**
 * Borrador de una consulta a medio capturar, guardado en el telefono (spec 014, T-004, D-05).
 *
 * **Por que la llave es (ownerId, petId)** y no solo `ownerId`, a diferencia del borrador de
 * afiliacion: una veterinaria atiende varias mascotas en la misma manana. Si la llave fuera solo la
 * cuenta, empezar la consulta de un segundo perro borraria la del primero. Con las dos columnas,
 * cada mascota conserva su avance.
 *
 * `ownerId` sigue siendo parte de la llave porque **toda consulta de Room filtra por ownerId**: dos
 * cuentas en el mismo telefono no se ven los datos, y eso no se negocia.
 *
 * Los medicamentos van como JSON porque son una lista y el borrador siempre se lee y se escribe
 * completo: no hay consulta que filtre por un medicamento. Es el mismo criterio que ya se uso en
 * `AffiliationDraftEntity` con el horario y los servicios.
 */
@Entity(tableName = "consultation_drafts", primaryKeys = ["ownerId", "petId"])
data class ConsultationDraftEntity(
    val ownerId: String,
    val petId: String,
    val reason: String,
    val diagnosis: String,
    val weightKg: Double?,
    val medicationsJson: String,
    val savedAtMillis: Long,
)

/** Medicamento del borrador, para el JSON de [ConsultationDraftEntity.medicationsJson]. */
@Serializable
data class DraftMedicationJson(
    val name: String = "",
    val dose: String = "",
    val frequency: String = "",
    val duration: String = "",
    val instructions: String? = null,
)
