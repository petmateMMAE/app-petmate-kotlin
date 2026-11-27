package mx.petmate.app.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.BusinessApiService
import mx.petmate.app.data.remote.dto.AccessGrantRequestDto
import mx.petmate.app.data.remote.dto.BusinessDto
import mx.petmate.app.data.remote.dto.CreateBusinessRequestDto
import mx.petmate.app.data.remote.dto.NearbyBusinessDto
import mx.petmate.app.data.remote.dto.PublicBusinessDto
import mx.petmate.app.data.remote.dto.ResolveScanRequestDto
import mx.petmate.app.data.remote.dto.ScanFichaDto
import mx.petmate.app.data.remote.dto.ServiceDto
import mx.petmate.app.data.remote.dto.ServiceRequestDto
import mx.petmate.app.data.remote.dto.TimeRangeDto
import mx.petmate.app.data.remote.dto.UpdateBusinessRequestDto
import mx.petmate.app.data.remote.dto.UpdateLocationRequestDto
import mx.petmate.app.data.local.dao.AffiliationDraftDao
import mx.petmate.app.data.local.entity.AffiliationDraftEntity
import mx.petmate.app.data.local.entity.DraftServiceJson
import mx.petmate.app.data.local.entity.DraftTimeRangeJson
import mx.petmate.app.data.remote.toAppError
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.AccessGrant
import mx.petmate.app.domain.model.AffiliationDraft
import mx.petmate.app.domain.model.AffiliationStep
import mx.petmate.app.domain.model.DraftedService
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessDraft
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.BusinessService
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.FichaConsultation
import mx.petmate.app.domain.model.Medication
import mx.petmate.app.domain.model.NearbyBusiness
import mx.petmate.app.domain.model.OwnBusiness
import mx.petmate.app.domain.model.PublicBusiness
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.ScanFicha
import mx.petmate.app.domain.model.ShareDuration
import mx.petmate.app.domain.model.SharedData
import mx.petmate.app.domain.model.TimeRange
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.WeightRecord
import mx.petmate.app.domain.repository.BusinessRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Negocios, mapa, autorizaciones y escaneo contra el backend 0.4.0 (docs/api/endpoints.md). */
@Singleton
class BusinessRepositoryImpl @Inject constructor(
    private val api: BusinessApiService,
    private val caller: AuthorizedCaller,
    private val sessionStore: SessionStore,
    private val draftDao: AffiliationDraftDao,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : BusinessRepository {

    override suspend fun getNearby(latitude: Double, longitude: Double): AppResult<List<NearbyBusiness>> = io {
        anonymous {
            val nearby = api.getNearby(latitude, longitude).data?.items.orEmpty().mapNotNull { it.toDomain() }
            // get-nearby no trae coordenadas: el pin sale del perfil publico (cache del servidor de 5 min).
            coroutineScope {
                nearby.take(MAX_PINS).map { b ->
                    async {
                        runCatching { api.getPublicBusiness(b.id).data }.getOrNull()?.let { p ->
                            b.copy(latitude = p.latitude, longitude = p.longitude, phone = p.phone, whatsApp = p.whatsApp)
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }
    }

    override suspend fun getPublicBusiness(id: String): AppResult<PublicBusiness> = io {
        anonymous { required(required(api.getPublicBusiness(id).data).toDomain()) }
    }

    override fun cachedOwnBusiness(): OwnBusiness? =
        sessionStore.readBusinessJson()?.let { raw -> runCatching { json.decodeFromString(BusinessDto.serializer(), raw).toDomain() }.getOrNull() }

    override suspend fun createBusiness(draft: BusinessDraft, latitude: Double, longitude: Double): AppResult<OwnBusiness> = io {
        val kind = requireNotNull(draft.kind) { "El tipo de negocio es obligatorio" }
        val body = CreateBusinessRequestDto(
            type = kind.apiValue,
            name = draft.name.trim(),
            phone = draft.phone.trim(),
            whatsApp = draft.whatsApp.trim().ifEmpty { null },
            address = draft.address.trim(),
            description = draft.description.trim().ifEmpty { null },
            latitude = latitude,
            longitude = longitude,
            licenseNumber = draft.licenseNumber.trim().ifEmpty { null }.takeIf { kind == BusinessKind.Veterinary },
            schedule = draft.schedule.toDto(),
        )
        caller.call { remember(required(api.createBusiness(it, body).data)) }
    }

    override suspend fun updateBusiness(draft: BusinessDraft): AppResult<OwnBusiness> = io {
        // Reemplazo completo (pm-109): se manda todo lo que se quiere conservar.
        val body = UpdateBusinessRequestDto(
            name = draft.name.trim(),
            description = draft.description.trim().ifEmpty { null },
            phone = draft.phone.trim(),
            whatsApp = draft.whatsApp.trim().ifEmpty { null },
            schedule = draft.schedule.toDto(),
        )
        caller.call { remember(required(api.updateBusiness(it, body).data)) }
    }

    override suspend fun updateLocation(latitude: Double, longitude: Double): AppResult<OwnBusiness> = io {
        caller.call { remember(required(api.updateLocation(it, UpdateLocationRequestDto(latitude, longitude)).data)) }
    }

    override suspend fun deleteBusiness(): AppResult<Unit> = io {
        caller.call {
            api.deleteBusiness(it)
            sessionStore.saveBusinessJson(null)
        }
    }

    override suspend fun getServices(): AppResult<List<BusinessService>> = io {
        caller.call { api.getServices(it).data?.items.orEmpty().map(ServiceDto::toDomain) }
    }

    override suspend fun createService(name: String, description: String?, price: Double?): AppResult<BusinessService> = io {
        caller.call { required(api.createService(it, ServiceRequestDto(name.trim(), description?.trim()?.ifEmpty { null }, price)).data).toDomain() }
    }

    override suspend fun updateService(id: String, name: String, description: String?, price: Double?): AppResult<BusinessService> = io {
        caller.call { required(api.updateService(it, id, ServiceRequestDto(name.trim(), description?.trim()?.ifEmpty { null }, price)).data).toDomain() }
    }

    override suspend fun deleteService(id: String): AppResult<Unit> = io {
        caller.call { api.deleteService(it, id); Unit }
    }

    override suspend fun shareAccess(petId: String, businessId: String?, data: Set<SharedData>, duration: ShareDuration): AppResult<AccessGrant> = io {
        val body = AccessGrantRequestDto(petId, businessId, data.map { it.apiValue }, duration.minutes)
        caller.call {
            val dto = required(api.createAccessGrant(it, body).data)
            AccessGrant(dto.id, dto.businessId, dto.data.mapNotNull { v -> SharedData.entries.firstOrNull { e -> e.apiValue == v } }, dto.expiresAt)
        }
    }

    override suspend fun resolveScan(code: String): AppResult<ScanFicha> = io {
        caller.call { required(api.resolveScan(it, ResolveScanRequestDto(code.trim())).data).toDomain() }
    }

    override suspend fun getAttended(period: String): AppResult<List<AttendedPet>> = io {
        caller.call { api.getAttended(it, period).data?.items.orEmpty().map { a -> AttendedPet(a.petName, a.scannedAt) } }
    }

    // ---- Borrador de afiliacion (spec 011, T-002 y T-006; lo limpia T-008) ----
    //
    // Vive en Room, asi sobrevive a cerrar la app (T-006, C-08). El horario y
    // los servicios se guardan como JSON porque el borrador siempre se lee y se
    // escribe completo.

    private val draftLock = Mutex()

    override suspend fun saveAffiliationDraft(draft: AffiliationDraft): AppResult<Unit> = io {
        draftLock.withLock {
            val ownerId = currentOwnerId() ?: return@withLock AppResult.Failure(AppError.Unauthorized)
            runCatching {
                draftDao.deleteOthers(ownerId)
                draftDao.upsert(draft.toEntity(ownerId, System.currentTimeMillis()))
            }.fold(
                onSuccess = { AppResult.Success(Unit) },
                onFailure = { AppResult.Failure(AppError.Unknown(it)) },
            )
        }
    }

    override suspend fun readAffiliationDraft(): AppResult<AffiliationDraft?> = io {
        draftLock.withLock {
            val ownerId = currentOwnerId()
            runCatching {
                // El cambio de sesion no hereda borradores (T-008, C-13). Sin
                // sesion no queda ninguno: el logout los borra.
                if (ownerId == null) {
                    draftDao.deleteAll()
                    null
                } else {
                    draftDao.deleteOthers(ownerId)
                    draftDao.get(ownerId)?.toDomain(json)
                }
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Failure(AppError.Unknown(it)) },
            )
        }
    }

    override suspend fun clearAffiliationDraft(): AppResult<Unit> = io {
        draftLock.withLock {
            runCatching {
                currentOwnerId()?.let { draftDao.delete(it) } ?: draftDao.deleteAll()
            }.fold(
                onSuccess = { AppResult.Success(Unit) },
                onFailure = { AppResult.Failure(AppError.Unknown(it)) },
            )
        }
    }

    /** Usuario de la sesion activa; null tras el logout. */
    private fun currentOwnerId(): String? = sessionStore.readUser()?.id

    /** Guarda la copia local del negocio propio para el panel y el formulario de edicion. */
    private fun remember(dto: BusinessDto): OwnBusiness {
        sessionStore.saveBusinessJson(json.encodeToString(BusinessDto.serializer(), dto))
        return required(dto.toDomain())
    }

    /** Llamadas publicas: sin token, sin renovacion de sesion. */
    private suspend fun <T> anonymous(block: suspend () -> T): AppResult<T> =
        runCatching { block() }.fold(onSuccess = { AppResult.Success(it) }, onFailure = { AppResult.Failure(it.toAppError()) })

    private suspend fun <T> io(block: suspend () -> AppResult<T>): AppResult<T> = withContext(dispatchers.io) { block() }

    private fun <T> required(data: T?): T = data ?: throw IllegalStateException("Respuesta sin data")

    private companion object {
        const val MAX_PINS = 30
    }
}

// ---------- Mapeo ----------

private fun Map<String, List<TimeRangeDto>>.toDomainSchedule() = mapValues { (_, v) -> v.map { TimeRange(it.open, it.close) } }

private fun Map<String, List<TimeRange>>.toDto(): Map<String, List<TimeRangeDto>>? =
    filterValues { it.isNotEmpty() }.mapValues { (_, v) -> v.map { TimeRangeDto(it.open, it.close) } }.ifEmpty { null }

internal fun BusinessDto.toDomain(): OwnBusiness? = BusinessKind.fromApi(type)?.let { kind ->
    OwnBusiness(id, kind, name, phone, whatsApp, address, description, latitude, longitude, licenseNumber, schedule.toDomainSchedule())
}

internal fun NearbyBusinessDto.toDomain(): NearbyBusiness? = BusinessKind.fromApi(type)?.let { kind ->
    NearbyBusiness(id, name, kind, isOpenNow, todayHours.map { TimeRange(it.open, it.close) }, distanceMeters, photoUrl)
}

internal fun PublicBusinessDto.toDomain(): PublicBusiness? = BusinessKind.fromApi(type)?.let { kind ->
    PublicBusiness(
        id = id,
        name = name,
        kind = kind,
        description = description,
        isOpen = isOpenNow,
        todayHours = todayHours.map { TimeRange(it.open, it.close) },
        schedule = schedule.toDomainSchedule(),
        latitude = latitude,
        longitude = longitude,
        address = address,
        phone = phone,
        whatsApp = whatsApp,
        services = services.map(ServiceDto::toDomain),
    )
}

internal fun ServiceDto.toDomain() = BusinessService(id, name, description, price)

internal fun ScanFichaDto.toDomain(): ScanFicha {
    val origin = RecordOrigin.Owner
    return ScanFicha(
        petId = petId,
        name = name,
        species = species,
        breed = breed,
        ageYears = ageYears,
        ageMonths = ageMonths,
        allergies = allergies.mapIndexed { i, a -> Allergy("a$i", a.name, AllergyKind.fromApi(a.kind), a.notes, true, origin) },
        vaccines = vaccines?.mapIndexed { i, v -> Vaccine("v$i", v.name, v.appliedOn, v.batch, v.appliedBy, v.nextDoseOn, origin) },
        dewormings = dewormings?.mapIndexed { i, d -> Deworming("d$i", d.product, d.appliedOn, DewormingType.fromApi(d.type), d.appliedBy, d.nextOn, origin) },
        weights = weights?.mapIndexed { i, w -> WeightRecord("w$i", w.measuredOn, w.kilograms, origin) },
        consultations = consultations?.map { c ->
            FichaConsultation(c.consultedOn, c.businessName, c.reason, c.diagnosis, c.medications.map { m -> Medication(m.name, m.dose, m.frequency, m.duration, m.instructions) })
        },
    )
}

// ---------- Borrador de afiliacion (spec 011, T-006) ----------

private val SCHEDULE_JSON = MapSerializer(String.serializer(), ListSerializer(DraftTimeRangeJson.serializer()))
private val SERVICES_JSON = ListSerializer(DraftServiceJson.serializer())

internal fun AffiliationDraft.toEntity(ownerId: String, nowMillis: Long) = AffiliationDraftEntity(
    ownerId = ownerId,
    kind = business.kind?.apiValue,
    name = business.name,
    phone = business.phone,
    whatsApp = business.whatsApp,
    address = business.address,
    description = business.description,
    licenseNumber = business.licenseNumber,
    scheduleJson = Json.encodeToString(
        SCHEDULE_JSON,
        business.schedule.mapValues { (_, ranges) -> ranges.map { DraftTimeRangeJson(it.open, it.close) } },
    ),
    servicesJson = Json.encodeToString(SERVICES_JSON, services.map { DraftServiceJson(it.name, it.price) }),
    latitude = latitude,
    longitude = longitude,
    step = step.name,
    savedAtMillis = nowMillis,
)

internal fun AffiliationDraftEntity.toDomain(json: Json): AffiliationDraft {
    val schedule = runCatching { json.decodeFromString(SCHEDULE_JSON, scheduleJson) }.getOrDefault(emptyMap())
    val services = runCatching { json.decodeFromString(SERVICES_JSON, servicesJson) }.getOrDefault(emptyList())
    return AffiliationDraft(
        business = BusinessDraft(
            kind = BusinessKind.fromApi(kind),
            name = name,
            phone = phone,
            whatsApp = whatsApp,
            address = address,
            description = description,
            licenseNumber = licenseNumber,
            schedule = schedule.mapValues { (_, ranges) -> ranges.map { TimeRange(it.open, it.close) } },
        ),
        services = services.map { DraftedService(it.name, it.price) },
        latitude = latitude,
        longitude = longitude,
        // Un paso que no se reconozca no debe tirar el borrador: se vuelve al primero.
        step = AffiliationStep.entries.firstOrNull { it.name == step } ?: AffiliationStep.Data,
    )
}

/** Duraciones legibles para la interfaz. */
fun ShareDuration.label(): String = when (this) {
    ShareDuration.ThirtyMinutes -> "30 min"
    ShareDuration.OneDay -> "1 día"
    ShareDuration.SevenDays -> "7 días"
}
