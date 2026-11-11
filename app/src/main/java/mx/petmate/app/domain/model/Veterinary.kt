package mx.petmate.app.domain.model

/**
 * Modelos de dominio para consultas veterinarias, recetas, historial unificado
 * y seguimiento (pm-130 a pm-142).
 */

data class MedicationDraft(
    val name: String = "",
    val dose: String = "",
    val frequency: String = "",
    val duration: String = "",
    val instructions: String? = null,
)

data class ConsultationDraft(
    val petId: String,
    val reason: String,
    val diagnosis: String,
    val weightKg: Double? = null,
    val medications: List<MedicationDraft> = emptyList(),
)

data class Consultation(
    val id: String,
    val petId: String,
    val consultedOn: String,
    val origin: RecordOrigin,
    val reason: String,
    val diagnosis: String,
    val weightKg: Double?,
    val medications: List<Medication>,
    val createdAt: String?,
)

data class ConsultationSummary(
    val id: String,
    val consultedOn: String,
    val businessName: String?,
    val reason: String,
    val diagnosis: String,
)

data class HistoryItem(
    val kind: String, // "Consultation", "Vaccine", "Deworming", "Weight"
    val id: String,
    val date: String,
    val title: String,
    val origin: RecordOrigin,
)

data class FollowupNote(
    val at: String,
    val text: String,
)

data class Followup(
    val id: String,
    val consultationId: String,
    val petId: String,
    val petName: String,
    val status: String,
    val nextReviewOn: String?,
    val isOverdue: Boolean,
    val notes: List<FollowupNote>,
    val conversationId: String?,
    val createdAt: String?,
    val updatedAt: String?,
)
