package mx.petmate.app.domain.repository

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Consultation
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.ConsultationSummary
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.HistoryItem
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.model.Vaccine

interface VeterinaryRepository {

    suspend fun createConsultation(draft: ConsultationDraft): AppResult<Consultation>

    suspend fun addMedication(consultationId: String, medication: MedicationDraft): AppResult<Consultation>

    suspend fun getConsultations(petId: String): AppResult<List<ConsultationSummary>>

    suspend fun getConsultationById(id: String): AppResult<Consultation>

    suspend fun createVaccineByBusiness(
        petId: String,
        name: String,
        appliedOn: String,
        batch: String?,
        nextDoseOn: String?,
    ): AppResult<Vaccine>

    suspend fun createDewormingByBusiness(
        petId: String,
        product: String,
        appliedOn: String,
        type: DewormingType?,
        nextOn: String?,
    ): AppResult<Deworming>

    suspend fun getHistory(petId: String, types: String? = null): AppResult<List<HistoryItem>>

    suspend fun createFollowup(consultationId: String, nextReviewInDays: Int?): AppResult<Followup>

    suspend fun updateFollowup(id: String, status: String, nextReviewInDays: Int?): AppResult<Followup>

    suspend fun addFollowupNote(followupId: String, text: String): AppResult<Followup>

    suspend fun getFollowupByConsultation(consultationId: String): AppResult<Followup?>

    suspend fun getFollowups(onlyActive: Boolean = true): AppResult<List<Followup>>
}
