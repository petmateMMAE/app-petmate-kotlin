package mx.petmate.app.util

import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.Consultation
import mx.petmate.app.domain.model.ConsultationSummary
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.FichaConsultation
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.FollowupNote
import mx.petmate.app.domain.model.HistoryItem
import mx.petmate.app.domain.model.Medication
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.ScanFicha
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.WeightRecord

/**
 * Datos falsos de consulta, historial y seguimiento (specs 014 a 016).
 *
 * Igual que [FakeData]: todo con valor por omision para que cada prueba nombre solo el campo que
 * esta comprobando. Van aparte porque son otro grupo de modelos y asi ninguno de los dos archivos
 * se vuelve una lista interminable.
 */

fun consultaFalsa(
    id: String = "c-1",
    petId: String = "p-1",
    consultedOn: String = "2026-11-20",
    reason: String = "Tos seca",
    diagnosis: String = "Traqueobronquitis",
    weightKg: Double? = 12.4,
    medications: List<Medication> = emptyList(),
    origin: RecordOrigin = RecordOrigin(byBusiness = true, businessName = "Veterinaria San Jorge"),
): Consultation = Consultation(
    id = id,
    petId = petId,
    consultedOn = consultedOn,
    origin = origin,
    reason = reason,
    diagnosis = diagnosis,
    weightKg = weightKg,
    medications = medications,
    createdAt = "2026-11-20T10:00:00Z",
)

fun medicamentoFalso(
    name: String = "Doxiciclina",
    dose: String = "100 mg",
    frequency: String = "cada 12 h",
    duration: String = "7 dias",
    instructions: String? = null,
): Medication = Medication(
    name = name,
    dose = dose,
    frequency = frequency,
    duration = duration,
    instructions = instructions,
)

fun resumenDeConsultaFalso(
    id: String = "c-1",
    consultedOn: String = "2026-11-20",
    businessName: String? = "Veterinaria San Jorge",
    reason: String = "Tos seca",
    diagnosis: String = "Traqueobronquitis",
): ConsultationSummary = ConsultationSummary(
    id = id,
    consultedOn = consultedOn,
    businessName = businessName,
    reason = reason,
    diagnosis = diagnosis,
)

fun elementoDeHistorialFalso(
    kind: String = "Consultation",
    id: String = "c-1",
    date: String = "2026-11-20",
    title: String = "Consulta: tos seca",
    origin: RecordOrigin = RecordOrigin(byBusiness = true, businessName = "Veterinaria San Jorge"),
): HistoryItem = HistoryItem(kind = kind, id = id, date = date, title = title, origin = origin)

/**
 * Ficha que ve el negocio al escanear el QR.
 *
 * Las listas son `null` cuando el dueno no las compartio, y eso **no** es lo mismo que una lista
 * vacia: por omision aqui van en null para que una prueba que las espere tenga que pedirlas, que es
 * justo la regla de pm-123.
 */
fun fichaFalsa(
    petId: String? = "p-1",
    name: String = "Firulais",
    species: String = "Dog",
    breed: String? = "Criollo",
    ageYears: Int? = 3,
    allergies: List<Allergy> = emptyList(),
    vaccines: List<Vaccine>? = null,
    dewormings: List<Deworming>? = null,
    weights: List<WeightRecord>? = null,
    consultations: List<FichaConsultation>? = null,
): ScanFicha = ScanFicha(
    petId = petId,
    name = name,
    species = species,
    breed = breed,
    ageYears = ageYears,
    ageMonths = 4,
    allergies = allergies,
    vaccines = vaccines,
    dewormings = dewormings,
    weights = weights,
    consultations = consultations,
)

fun seguimientoFalso(
    id: String = "f-1",
    consultationId: String = "c-1",
    petId: String = "p-1",
    petName: String = "Firulais",
    status: String = "Active",
    nextReviewOn: String? = "2026-12-01",
    isOverdue: Boolean = false,
    notes: List<FollowupNote> = emptyList(),
    conversationId: String? = null,
): Followup = Followup(
    id = id,
    consultationId = consultationId,
    petId = petId,
    petName = petName,
    status = status,
    nextReviewOn = nextReviewOn,
    isOverdue = isOverdue,
    notes = notes,
    conversationId = conversationId,
    createdAt = "2026-11-20T10:05:00Z",
    updatedAt = null,
)
