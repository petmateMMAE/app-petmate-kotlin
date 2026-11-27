package mx.petmate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.petmate.app.data.local.entity.AttendedPetEntity
import mx.petmate.app.data.local.entity.DashboardAlertEntity
import mx.petmate.app.data.local.entity.DashboardEntity

/**
 * Acceso al panel guardado (spec 012, T-006).
 *
 * Las lecturas devuelven `Flow` para que la pantalla se actualice sola cuando
 * el refresco escribe. Toda consulta filtra por `ownerId`.
 */
@Dao
interface DashboardDao {

    @Query("SELECT * FROM dashboard WHERE ownerId = :ownerId")
    fun observeDashboard(ownerId: String): Flow<DashboardEntity?>

    @Query("SELECT * FROM dashboard_alerts WHERE ownerId = :ownerId ORDER BY nextReviewOn IS NULL, nextReviewOn ASC")
    fun observeAlerts(ownerId: String): Flow<List<DashboardAlertEntity>>

    @Query("SELECT * FROM dashboard_attended WHERE ownerId = :ownerId ORDER BY scannedAt DESC")
    fun observeAttended(ownerId: String): Flow<List<AttendedPetEntity>>

    /** Atendidas de un rango de dias, con los dos extremos incluidos. */
    @Query(
        "SELECT * FROM dashboard_attended WHERE ownerId = :ownerId " +
            "AND scannedOn IS NOT NULL AND scannedOn >= :from AND scannedOn <= :to " +
            "ORDER BY scannedAt DESC",
    )
    suspend fun attendedBetween(ownerId: String, from: String, to: String): List<AttendedPetEntity>

    @Upsert
    suspend fun upsertDashboard(dashboard: DashboardEntity)

    @Upsert
    suspend fun upsertAlerts(alerts: List<DashboardAlertEntity>)

    @Upsert
    suspend fun upsertAttended(attended: List<AttendedPetEntity>)

    @Query("DELETE FROM dashboard_alerts WHERE ownerId = :ownerId")
    suspend fun deleteAlerts(ownerId: String)

    @Query("DELETE FROM dashboard_attended WHERE ownerId = :ownerId")
    suspend fun deleteAttended(ownerId: String)

    @Query("DELETE FROM dashboard WHERE ownerId = :ownerId")
    suspend fun deleteDashboard(ownerId: String)

    /**
     * Reemplaza el panel completo de una cuenta en una sola transaccion.
     *
     * Se borra y se vuelve a escribir para que una alerta que el servidor ya
     * cerro desaparezca; si fuera solo un upsert se quedaria pegada.
     */
    @Transaction
    suspend fun replace(
        dashboard: DashboardEntity,
        alerts: List<DashboardAlertEntity>,
        attended: List<AttendedPetEntity>,
    ) {
        deleteAlerts(dashboard.ownerId)
        deleteAttended(dashboard.ownerId)
        upsertDashboard(dashboard)
        upsertAlerts(alerts)
        upsertAttended(attended)
    }

    /** Tira el panel de una cuenta (logout o cambio de cuenta; T-008). */
    @Transaction
    suspend fun clear(ownerId: String) {
        deleteAlerts(ownerId)
        deleteAttended(ownerId)
        deleteDashboard(ownerId)
    }

    /** Tira el panel de cualquier otra cuenta: el cambio de sesion no lo hereda (T-008). */
    @Query("DELETE FROM dashboard WHERE ownerId <> :ownerId")
    suspend fun deleteDashboardOfOthers(ownerId: String)

    @Query("DELETE FROM dashboard_alerts WHERE ownerId <> :ownerId")
    suspend fun deleteAlertsOfOthers(ownerId: String)

    @Query("DELETE FROM dashboard_attended WHERE ownerId <> :ownerId")
    suspend fun deleteAttendedOfOthers(ownerId: String)

    @Transaction
    suspend fun clearOthers(ownerId: String) {
        deleteAlertsOfOthers(ownerId)
        deleteAttendedOfOthers(ownerId)
        deleteDashboardOfOthers(ownerId)
    }

    @Query("DELETE FROM dashboard")
    suspend fun deleteAllDashboards()

    @Query("DELETE FROM dashboard_alerts")
    suspend fun deleteAllAlerts()

    @Query("DELETE FROM dashboard_attended")
    suspend fun deleteAllAttended()

    /** Tira el panel de todas las cuentas. Es lo que corre en el logout, cuando ya no hay sesion. */
    @Transaction
    suspend fun clearAll() {
        deleteAllAlerts()
        deleteAllAttended()
        deleteAllDashboards()
    }
}
