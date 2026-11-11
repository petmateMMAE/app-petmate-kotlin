package mx.petmate.app.data.repository

import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.VeterinaryApiService
import mx.petmate.app.data.remote.dto.AddFollowupNoteRequestDto
import mx.petmate.app.data.remote.dto.AddMedicationRequestDto
import mx.petmate.app.data.remote.dto.ConsultationResponseDto
import mx.petmate.app.data.remote.dto.ConsultationSummaryResponseDto
import mx.petmate.app.data.remote.dto.CreateConsultationRequestDto
import mx.petmate.app.data.remote.dto.CreateFollowupRequestDto
import mx.petmate.app.data.remote.dto.DewormingByBusinessRequestDto
import mx.petmate.app.data.remote.dto.DewormingDto
import mx.petmate.app.data.remote.dto.FollowupNoteResponseDto
import mx.petmate.app.data.remote.dto.FollowupResponseDto
import mx.petmate.app.data.remote.dto.HistoryItemResponseDto
import mx.petmate.app.data.remote.dto.MedicationRequestDto
import mx.petmate.app.data.remote.dto.MedicationResponseDto
import mx.petmate.app.data.remote.dto.OriginDto
import mx.petmate.app.data.remote.dto.UpdateFollowupRequestDto
import mx.petmate.app.data.remote.dto.VaccineByBusinessRequestDto
import mx.petmate.app.data.remote.dto.VaccineDto
import mx.petmate.app.domain.model.Consultation
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.ConsultationSummary
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.FollowupNote
import mx.petmate.app.domain.model.HistoryItem
import mx.petmate.app.domain.model.Medication
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.repository.VeterinaryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VeterinaryRepositoryImpl @Inject constructor(
    private val api: VeterinaryApiService,
    private val caller: AuthorizedCaller,
    private val dispatchers: DispatcherProvider,
) : VeterinaryRepository {

    private suspend fun <T> io(block: suspend () -> AppResult<T>): AppResult<T> =
        withContext(dispatchers.io) { block() }

    override suspend fun createConsultation(draft: ConsultationDraft): AppResult<Consultation> = io {
        val body = CreateConsultationRequestDto(
            petId = draft.petId,
            reason = draft.reason.trim(),
            diagnosis = draft.diagnosis.trim(),
            weightKg = draft.weightKg,
            medications = draft.medications.takeIf { it.isNotEmpty() }?.map { m ->
                MedicationRequestDto(
                    name = m.name.trim(),
                    dose = m.dose.trim(),
                    frequency = m.frequency.trim(),
                    duration = m.duration.trim(),
                    instructions = m.instructions?.trim()?.ifEmpty { null },
                )
            },
        )
        caller.call { api.createConsultation(it, body).data!!.toDomain() }
    }

    override suspend fun addMedication(consultationId: String, medication: MedicationDraft): AppResult<Consultation> = io {
        val body = AddMedicationRequestDto(
            consultationId = consultationId,
            name = medication.name.trim(),
            dose = medication.dose.trim(),
            frequency = medication.frequency.trim(),
            duration = medication.duration.trim(),
            instructions = medication.instructions?.trim()?.ifEmpty { null },
        )
        caller.call { api.addMedication(it, body).data!!.toDomain() }
    }

    override suspend fun getConsultations(petId: String): AppResult<List<ConsultationSummary>> = io {
        caller.call {
            api.getConsultations(it, petId).data?.items.orEmpty().map(ConsultationSummaryResponseDto::toDomain)
        }
    }

    override suspend fun getConsultationById(id: String): AppResult<Consultation> = io {
        caller.call { api.getConsultationById(it, id).data!!.toDomain() }
    }

    override suspend fun createVaccineByBusiness(
        petId: String,
        name: String,
        appliedOn: String,
        batch: String?,
        nextDoseOn: String?,
    ): AppResult<Vaccine> = io {
        val body = VaccineByBusinessRequestDto(
            petId = petId,
            appliedOn = appliedOn,
            name = name.trim(),
            batch = batch?.trim()?.ifEmpty { null },
            nextDoseOn = nextDoseOn?.ifEmpty { null },
        )
        caller.call { api.createVaccineByBusiness(it, body).data!!.toDomain() }
    }

    override suspend fun createDewormingByBusiness(
        petId: String,
        product: String,
        appliedOn: String,
        type: DewormingType?,
        nextOn: String?,
    ): AppResult<Deworming> = io {
        val body = DewormingByBusinessRequestDto(
            petId = petId,
            appliedOn = appliedOn,
            product = product.trim(),
            type = type?.apiValue,
            nextOn = nextOn?.ifEmpty { null },
        )
        caller.call { api.createDewormingByBusiness(it, body).data!!.toDomain() }
    }

    override suspend fun getHistory(petId: String, types: String?): AppResult<List<HistoryItem>> = io {
        caller.call {
            api.getHistory(it, petId, types).data?.items.orEmpty().map(HistoryItemResponseDto::toDomain)
        }
    }

    override suspend fun createFollowup(consultationId: String, nextReviewInDays: Int?): AppResult<Followup> = io {
        val body = CreateFollowupRequestDto(
            consultationId = consultationId,
            nextReviewInDays = nextReviewInDays,
        )
        caller.call { api.createFollowup(it, body).data!!.toDomain() }
    }

    override suspend fun updateFollowup(id: String, status: String, nextReviewInDays: Int?): AppResult<Followup> = io {
        val body = UpdateFollowupRequestDto(
            status = status,
            nextReviewInDays = nextReviewInDays,
        )
        caller.call { api.updateFollowup(it, id, body).data!!.toDomain() }
    }

    override suspend fun addFollowupNote(followupId: String, text: String): AppResult<Followup> = io {
        val body = AddFollowupNoteRequestDto(
            followupId = followupId,
            text = text.trim(),
        )
        caller.call { api.addFollowupNote(it, body).data!!.toDomain() }
    }

    override suspend fun getFollowupByConsultation(consultationId: String): AppResult<Followup?> = io {
        caller.call {
            api.getFollowupByConsultation(it, consultationId).data?.toDomain()
        }
    }

    override suspend fun getFollowups(onlyActive: Boolean): AppResult<List<Followup>> = io {
        caller.call {
            api.getFollowups(it, onlyActive = onlyActive).data?.items.orEmpty().map(FollowupResponseDto::toDomain)
        }
    }
}

// ---------- Mappers ----------

internal fun MedicationResponseDto.toDomain() = Medication(
    name = name,
    dose = dose,
    frequency = frequency,
    duration = duration,
    instructions = instructions,
)

internal fun ConsultationResponseDto.toDomain() = Consultation(
    id = id,
    petId = petId,
    consultedOn = consultedOn,
    origin = origin.toDomain(),
    reason = reason,
    diagnosis = diagnosis,
    weightKg = weightKg,
    medications = medications.map(MedicationResponseDto::toDomain),
    createdAt = createdAt,
)

internal fun ConsultationSummaryResponseDto.toDomain() = ConsultationSummary(
    id = id,
    consultedOn = consultedOn,
    businessName = businessName,
    reason = reason,
    diagnosis = diagnosis,
)

internal fun HistoryItemResponseDto.toDomain() = HistoryItem(
    kind = kind,
    id = id,
    date = date,
    title = title,
    origin = origin.toDomain(),
)

internal fun FollowupNoteResponseDto.toDomain() = FollowupNote(
    at = at,
    text = text,
)

internal fun FollowupResponseDto.toDomain() = Followup(
    id = id,
    consultationId = consultationId,
    petId = petId,
    petName = petName,
    status = status,
    nextReviewOn = nextReviewOn,
    isOverdue = isOverdue,
    notes = notes.map(FollowupNoteResponseDto::toDomain),
    conversationId = conversationId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
