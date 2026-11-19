package mx.petmate.app.data.repository

import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.core.result.onSuccess
import mx.petmate.app.data.local.FollowUpDao
import mx.petmate.app.data.local.FollowUpEntity
import mx.petmate.app.data.remote.dto.FollowupNoteResponseDto
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.FollowupNote
import mx.petmate.app.domain.repository.VeterinaryRepository

/**
 * Seguimiento con lectura sin conexion (spec 015, T-008, R-10). Todo lo demas de 014 pasa directo a [remote].
 *
 * Cada seguimiento que llega del servidor (al leerlo, abrirlo, cambiarlo o agregar una nota) se guarda en Room.
 * Al leer se intenta primero el servidor para no mostrar un estado viejo a la veterinaria; si no hay red, se
 * devuelve lo guardado. Asi el dueno ve el seguimiento de su mascota en modo avion.
 */
class OfflineFirstFollowUpRepository(
    private val remote: VeterinaryRepository,
    private val dao: FollowUpDao,
    private val json: Json,
    private val currentUserId: () -> String?,
    private val dispatchers: DispatcherProvider,
) : VeterinaryRepository by remote {

    override suspend fun getFollowupByConsultation(consultationId: String): AppResult<Followup?> {
        val result = remote.getFollowupByConsultation(consultationId)
        val owner = currentUserId() ?: return result
        return when (result) {
            is AppResult.Success -> {
                result.data?.let { save(owner, it) }
                result
            }
            is AppResult.Failure -> {
                val cached = withContext(dispatchers.io) { dao.get(owner, consultationId) }
                if (cached != null) AppResult.Success(cached.toDomain()) else result
            }
        }
    }

    override suspend fun createFollowup(consultationId: String, nextReviewInDays: Int?) =
        remote.createFollowup(consultationId, nextReviewInDays).onSuccess { saveForSession(it) }

    override suspend fun updateFollowup(id: String, status: String, nextReviewInDays: Int?) =
        remote.updateFollowup(id, status, nextReviewInDays).onSuccess { saveForSession(it) }

    override suspend fun addFollowupNote(followupId: String, text: String) =
        remote.addFollowupNote(followupId, text).onSuccess { saveForSession(it) }

    private suspend fun saveForSession(followup: Followup) {
        val owner = currentUserId() ?: return
        save(owner, followup)
    }

    private suspend fun save(owner: String, followup: Followup) = withContext(dispatchers.io) {
        dao.upsert(followup.toEntity(owner))
    }

    private fun Followup.toEntity(owner: String) = FollowUpEntity(
        ownerId = owner,
        consultationId = consultationId,
        id = id,
        petId = petId,
        petName = petName,
        status = status,
        nextReviewOn = nextReviewOn,
        isOverdue = isOverdue,
        notesJson = json.encodeToString(NOTES, notes.map { FollowupNoteResponseDto(it.at, it.text) }),
        conversationId = conversationId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun FollowUpEntity.toDomain() = Followup(
        id = id,
        consultationId = consultationId,
        petId = petId,
        petName = petName,
        status = status,
        nextReviewOn = nextReviewOn,
        isOverdue = isOverdue,
        notes = json.decodeFromString(NOTES, notesJson).map { FollowupNote(it.at, it.text) },
        conversationId = conversationId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private companion object {
        val NOTES = ListSerializer(FollowupNoteResponseDto.serializer())
    }
}
