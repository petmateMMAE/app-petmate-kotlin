package mx.petmate.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Panel del afiliado guardado en el telefono (spec 012, T-006).
 *
 * **No guarda contenido clinico** (decision D-05 de la spec): aqui solo hay
 * identificadores, nombres, fechas y conteos. El diagnostico y la receta se le
 * piden al backend cuando el negocio abre el detalle, nunca salen de esta
 * cache. Por eso no existen columnas de diagnostico, receta ni notas.
 *
 * [ownerId] es el usuario de la sesion: dos cuentas en el mismo telefono no ven
 * el panel de la otra (T-008, C-13).
 */
@Entity(tableName = "dashboard", primaryKeys = ["ownerId"])
data class DashboardEntity(
    val ownerId: String,
    val businessId: String,
    val businessName: String,
    val kind: String?,
    val photoUrl: String?,
    val overdue: Int,
    val soon: Int,
    val unreadMessages: Int,
    val syncedAtMillis: Long,
)

/**
 * Alerta de seguimiento del panel (spec 012, T-006).
 *
 * Solo el identificador para navegar, el nombre de la mascota, la fecha de
 * revision y su severidad ya clasificada. Nada clinico.
 */
@Entity(
    tableName = "dashboard_alerts",
    primaryKeys = ["ownerId", "followupId"],
    indices = [Index(value = ["ownerId", "nextReviewOn"])],
)
data class DashboardAlertEntity(
    val ownerId: String,
    val followupId: String,
    val petId: String,
    val petName: String,
    val nextReviewOn: String?,
    val severity: String,
    val unreadMessages: Int,
)

/**
 * Mascota atendida por el negocio (spec 012, T-006).
 *
 * [scannedOn] es el dia en `yyyy-MM-dd`, sacado de [scannedAt], para que el
 * filtro por dia sea una comparacion de texto en SQLite y no haya que leer
 * todas las filas en memoria.
 */
@Entity(
    tableName = "dashboard_attended",
    primaryKeys = ["ownerId", "scannedAt", "name"],
    indices = [Index(value = ["ownerId", "scannedOn"])],
)
data class AttendedPetEntity(
    val ownerId: String,
    val name: String,
    val scannedAt: String,
    val scannedOn: String?,
    val petId: String?,
)
