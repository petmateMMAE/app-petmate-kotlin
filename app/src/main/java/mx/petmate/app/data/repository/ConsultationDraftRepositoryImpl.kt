package mx.petmate.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.local.dao.ConsultationDraftDao
import mx.petmate.app.data.local.entity.ConsultationDraftEntity
import mx.petmate.app.data.local.entity.DraftMedicationJson
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.repository.ConsultationDraftRepository

/**
 * Borrador de consulta guardado solo en el telefono (spec 014, T-004, D-05).
 *
 * El `ownerId` sale de la sesion y nunca de quien llama, asi que una pantalla no puede leer ni
 * escribir el borrador de otra cuenta aunque se equivoque.
 *
 * Si no hay sesion se devuelve [AppError.Unauthorized] en vez de guardar con un dueno vacio: un
 * borrador sin dueno lo veria la siguiente cuenta que entrara en el telefono.
 */
class ConsultationDraftRepositoryImpl(
    private val dao: ConsultationDraftDao,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
    private val logger: Logger,
    private val now: () -> Long = System::currentTimeMillis,
) : ConsultationDraftRepository {

    override suspend fun save(draft: ConsultationDraft): AppResult<Unit> = conDueno { owner ->
        dao.upsert(draft.toEntity(owner, now()))
        AppResult.Success(Unit)
    }

    override suspend fun get(petId: String): AppResult<ConsultationDraft?> = conDueno { owner ->
        AppResult.Success(dao.get(owner, petId)?.toDomain())
    }

    override fun observe(petId: String): Flow<ConsultationDraft?> {
        val owner = ownerId() ?: return emptyFlow()
        return dao.observe(owner, petId).map { it?.toDomain() }
    }

    override suspend fun getAll(): AppResult<List<ConsultationDraft>> = conDueno { owner ->
        AppResult.Success(dao.getAll(owner).map { it.toDomain() })
    }

    override suspend fun delete(petId: String): AppResult<Unit> = conDueno { owner ->
        dao.delete(owner, petId)
        AppResult.Success(Unit)
    }

    private fun ownerId(): String? = sessionStore.readUser()?.id

    /** Resuelve el dueno y corre [bloque] en E/S, traduciendo cualquier falla a [AppError]. */
    private suspend fun <T> conDueno(bloque: suspend (String) -> AppResult<T>): AppResult<T> {
        val owner = ownerId() ?: return AppResult.Failure(AppError.Unauthorized)
        return withContext(dispatchers.io) {
            runCatching { bloque(owner) }.getOrElse { error ->
                // No se registra el contenido del borrador: lleva diagnostico y peso de la mascota.
                logger.error(TAG, "Fallo la base local del borrador de consulta", error)
                AppResult.Failure(AppError.Unknown(error))
            }
        }
    }

    private fun ConsultationDraft.toEntity(owner: String, savedAt: Long) = ConsultationDraftEntity(
        ownerId = owner,
        petId = petId,
        reason = reason,
        diagnosis = diagnosis,
        weightKg = weightKg,
        medicationsJson = json.encodeToString(
            medications.map {
                DraftMedicationJson(it.name, it.dose, it.frequency, it.duration, it.instructions)
            },
        ),
        savedAtMillis = savedAt,
    )

    private fun ConsultationDraftEntity.toDomain() = ConsultationDraft(
        petId = petId,
        reason = reason,
        diagnosis = diagnosis,
        weightKg = weightKg,
        medications = medicamentos(medicationsJson),
    )

    /**
     * Un JSON ilegible devuelve lista vacia en lugar de tronar.
     *
     * Pasa si una version anterior guardo otro formato. Perder los medicamentos del borrador es
     * molesto; que la pantalla no abra es peor, y el veterinario puede volver a escribirlos.
     */
    private fun medicamentos(raw: String): List<MedicationDraft> =
        runCatching { json.decodeFromString<List<DraftMedicationJson>>(raw) }
            .onFailure { logger.error(TAG, "El borrador traia medicamentos ilegibles", it) }
            .getOrDefault(emptyList())
            .map { MedicationDraft(it.name, it.dose, it.frequency, it.duration, it.instructions) }

    private companion object {
        const val TAG = "ConsultationDraft"
    }
}
