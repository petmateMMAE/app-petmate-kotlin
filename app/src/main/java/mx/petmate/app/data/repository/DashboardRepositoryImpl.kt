package mx.petmate.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.local.dao.DashboardDao
import mx.petmate.app.data.local.entity.AttendedPetEntity
import mx.petmate.app.data.local.entity.DashboardAlertEntity
import mx.petmate.app.data.local.entity.DashboardEntity
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessDashboard
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.DashboardSummary
import mx.petmate.app.domain.model.DateRange
import mx.petmate.app.domain.model.FollowUpAlert
import mx.petmate.app.domain.repository.BusinessRepository
import mx.petmate.app.domain.repository.DashboardRepository
import mx.petmate.app.domain.usecase.ReviewDateClass
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Panel del afiliado, offline-first (spec 012, T-007 y T-008).
 *
 * La pantalla observa Room y el refresco baja del servidor y escribe Room. Un
 * error de red **no** borra lo guardado: se devuelve el error y el panel sigue
 * mostrando lo ultimo que se vio (C-11).
 *
 * Todo se asocia al usuario de la sesion y se limpia al cambiar de cuenta
 * (T-008, C-13).
 *
 * **Lo que falta y por que:** las alertas de seguimiento no se descargan. El
 * backend no tiene endpoint de resumen del panel (Q-01) y sigue abierto si las
 * alertas las calcula el servidor o la app a partir de los seguimientos (Q-05);
 * por eso T-005 esta marcada como bloqueada. Hasta que se responda, la lista de
 * alertas queda vacia y el resumen en cero. Las mascotas atendidas si se bajan,
 * porque `scans/get-attended` ya existe.
 *
 * `get-attended` acepta un periodo grueso (`day`, `week`), no un rango de
 * fechas, asi que se baja la semana y el rango exacto se resuelve en SQLite.
 * Un dia del calendario mas viejo que lo descargado devuelve lo que haya en la
 * cache; eso tambien espera a Q-01.
 */
@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val businesses: BusinessRepository,
    private val dao: DashboardDao,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
) : DashboardRepository {

    override fun observeDashboard(): Flow<BusinessDashboard?> = flow {
        val ownerId = currentOwnerId()
        if (ownerId == null) {
            emit(null)
            return@flow
        }
        emitAll(
            combine(
                dao.observeDashboard(ownerId),
                dao.observeAlerts(ownerId),
                dao.observeAttended(ownerId),
            ) { dashboard, alerts, attended ->
                dashboard?.toDomain(alerts, attended)
            },
        )
    }

    override suspend fun refreshDashboard(): AppResult<Unit> = io {
        val ownerId = currentOwnerId() ?: return@io AppResult.Failure(AppError.Unauthorized)
        // La portada sale del negocio propio, que ya guarda BusinessRepository.
        val own = businesses.cachedOwnBusiness()
            ?: return@io AppResult.Failure(AppError.Server(NOT_AFFILIATED))

        // Si la red falla se sale antes de tocar Room: lo guardado no se pierde (C-11).
        val attended = when (val result = businesses.getAttended(ATTENDED_PERIOD)) {
            is AppResult.Failure -> return@io result
            is AppResult.Success -> result.data
        }

        runCatching {
            dao.clearOthers(ownerId)
            dao.replace(
                dashboard = DashboardEntity(
                    ownerId = ownerId,
                    businessId = own.id,
                    businessName = own.name,
                    kind = own.kind.apiValue,
                    photoUrl = null,
                    overdue = 0,
                    soon = 0,
                    unreadMessages = 0,
                    syncedAtMillis = System.currentTimeMillis(),
                ),
                // Vacio mientras Q-01 y Q-05 sigan abiertas; ver el comentario de la clase.
                alerts = emptyList(),
                attended = attended.map { it.toEntity(ownerId) },
            )
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Failure(AppError.Unknown(it)) },
        )
    }

    override suspend fun getAttendedPets(range: DateRange): AppResult<List<AttendedPet>> = io {
        val ownerId = currentOwnerId() ?: return@io AppResult.Failure(AppError.Unauthorized)
        runCatching { dao.attendedBetween(ownerId, range.from, range.to).map(AttendedPetEntity::toDomain) }
            .fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Failure(AppError.Unknown(it)) },
            )
    }

    override suspend fun clearDashboard(): AppResult<Unit> = io {
        runCatching { currentOwnerId()?.let { dao.clear(it) } ?: dao.clearAll() }
            .fold(
                onSuccess = { AppResult.Success(Unit) },
                onFailure = { AppResult.Failure(AppError.Unknown(it)) },
            )
    }

    /** Usuario de la sesion activa; null tras el logout. */
    private fun currentOwnerId(): String? = sessionStore.readUser()?.id

    private suspend fun <T> io(block: suspend () -> AppResult<T>): AppResult<T> =
        withContext(dispatchers.io) { block() }

    private companion object {
        /** El backend solo acepta periodos gruesos; se baja la semana y SQLite recorta el rango. */
        const val ATTENDED_PERIOD = "week"

        /** La cuenta no tiene negocio afiliado: no hay panel que mostrar. */
        const val NOT_AFFILIATED = 404
    }
}

// ---------- Mapeo ----------

internal fun DashboardEntity.toDomain(
    alerts: List<DashboardAlertEntity>,
    attended: List<AttendedPetEntity>,
): BusinessDashboard {
    val domainAlerts = alerts.map(DashboardAlertEntity::toDomain)
    return BusinessDashboard(
        businessId = businessId,
        businessName = businessName,
        kind = BusinessKind.fromApi(kind),
        photoUrl = photoUrl,
        // Se recuentan de las alertas guardadas; las columnas sirven cuando el
        // resumen llegue armado del backend (Q-01).
        summary = if (domainAlerts.isEmpty()) {
            DashboardSummary(overdue, soon, unreadMessages)
        } else {
            DashboardSummary.of(domainAlerts)
        },
        alerts = domainAlerts,
        attended = attended.map(AttendedPetEntity::toDomain),
    )
}

internal fun DashboardAlertEntity.toDomain() = FollowUpAlert(
    followupId = followupId,
    petId = petId,
    petName = petName,
    nextReviewOn = nextReviewOn,
    severity = ReviewDateClass.entries.firstOrNull { it.name == severity } ?: ReviewDateClass.None,
    unreadMessages = unreadMessages,
)

internal fun AttendedPetEntity.toDomain() = AttendedPet(name = name, scannedAt = scannedAt, petId = petId)

internal fun AttendedPet.toEntity(ownerId: String) = AttendedPetEntity(
    ownerId = ownerId,
    name = name,
    scannedAt = scannedAt,
    scannedOn = scannedOn,
    petId = petId,
)
