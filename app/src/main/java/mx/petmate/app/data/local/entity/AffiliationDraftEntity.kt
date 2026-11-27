package mx.petmate.app.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

/**
 * Borrador de afiliacion guardado en el telefono (spec 011, T-006, R-06).
 *
 * [ownerId] es la llave: un borrador por cuenta, asi dos cuentas en el mismo
 * telefono no ven el borrador de la otra (C-08 y T-008, C-13). Sobrevive a
 * cerrar la app, que es justo lo que pide la tarea.
 *
 * El horario y los servicios van como JSON porque son listas y el borrador
 * siempre se lee y se escribe completo; no hay consulta que filtre por ellos.
 * Los demas campos son columnas para que se puedan inspeccionar en la base.
 */
@Entity(tableName = "affiliation_drafts", primaryKeys = ["ownerId"])
data class AffiliationDraftEntity(
    val ownerId: String,
    val kind: String?,
    val name: String,
    val phone: String,
    val whatsApp: String,
    val address: String,
    val description: String,
    val licenseNumber: String,
    val scheduleJson: String,
    val servicesJson: String,
    val latitude: Double?,
    val longitude: Double?,
    val step: String,
    val savedAtMillis: Long,
)

/** Franja horaria del borrador, para el JSON de [AffiliationDraftEntity.scheduleJson]. */
@Serializable
data class DraftTimeRangeJson(val open: String, val close: String)

/** Servicio del borrador, para el JSON de [AffiliationDraftEntity.servicesJson]. */
@Serializable
data class DraftServiceJson(val name: String, val price: Double? = null)
