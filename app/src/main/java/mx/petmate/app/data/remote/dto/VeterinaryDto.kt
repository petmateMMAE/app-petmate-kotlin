package mx.petmate.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTOs para consultas veterinarias, recetas, vacunas/desparasitaciones por negocio,
 * historial clinico y seguimiento (pm-130 a pm-142).
 * El backend rechaza miembros desconocidos: mandar SOLO los campos del contrato.
 */

@Serializable
data class MedicationRequestDto(
    val name: String,
    val dose: String,
    val frequency: String,
    val duration: String,
    val instructions: String? = null,
)

@Serializable
data class MedicationResponseDto(
    val name: String,
    val dose: String,
    val frequency: String,
    val duration: String,
    val instructions: String? = null,
)

@Serializable
data class CreateConsultationRequestDto(
    val petId: String,
    val reason: String,
    val diagnosis: String,
    val weightKg: Double? = null,
    val medications: List<MedicationRequestDto>? = null,
)

@Serializable
data class AddMedicationRequestDto(
    val consultationId: String,
    val name: String,
    val dose: String,
    val frequency: String,
    val duration: String,
    val instructions: String? = null,
)

@Serializable
data class ConsultationResponseDto(
    val id: String,
    val petId: String,
    val consultedOn: String,
    val origin: OriginDto = OriginDto(),
    val reason: String,
    val diagnosis: String,
    val weightKg: Double? = null,
    val medications: List<MedicationResponseDto> = emptyList(),
    val createdAt: String? = null,
)

@Serializable
data class ConsultationSummaryResponseDto(
    val id: String,
    val consultedOn: String,
    val businessName: String? = null,
    val reason: String,
    val diagnosis: String,
)

@Serializable
data class HistoryItemResponseDto(
    val kind: String,
    val id: String,
    val date: String,
    val title: String,
    val origin: OriginDto = OriginDto(),
)

@Serializable
data class VaccineByBusinessRequestDto(
    val petId: String,
    val appliedOn: String,
    val name: String,
    val batch: String? = null,
    val nextDoseOn: String? = null,
)

@Serializable
data class DewormingByBusinessRequestDto(
    val petId: String,
    val appliedOn: String,
    val product: String,
    val type: String? = null,
    val nextOn: String? = null,
)

@Serializable
data class CreateFollowupRequestDto(
    val consultationId: String,
    val nextReviewInDays: Int? = null,
)

@Serializable
data class UpdateFollowupRequestDto(
    val status: String,
    val nextReviewInDays: Int? = null,
)

@Serializable
data class AddFollowupNoteRequestDto(
    val followupId: String,
    val text: String,
)

@Serializable
data class FollowupNoteResponseDto(
    val at: String,
    val text: String,
)

@Serializable
data class FollowupResponseDto(
    val id: String,
    val consultationId: String,
    val petId: String,
    val petName: String,
    val status: String,
    val nextReviewOn: String? = null,
    val isOverdue: Boolean = false,
    val notes: List<FollowupNoteResponseDto> = emptyList(),
    val conversationId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
