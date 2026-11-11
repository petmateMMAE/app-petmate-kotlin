package mx.petmate.app.data.remote.dto

import kotlinx.serialization.Serializable

/*
 * Transporte de negocios, mapa publico, autorizaciones de acceso y escaneo (backend 0.4.0, pm-105 a pm-129).
 * Contrato: docs/api/endpoints.md del backend. Los cuerpos llevan SOLO los campos del contrato (miembro desconocido = 400).
 */

/** Franja `HH:mm` (cierre exclusivo; medianoche se escribe 23:59). */
@Serializable
data class TimeRangeDto(val open: String, val close: String)

/** `type`: Veterinary, Store o Grooming. `schedule`: dias monday..sunday. */
@Serializable
data class CreateBusinessRequestDto(
    val type: String,
    val name: String,
    val phone: String,
    val whatsApp: String? = null,
    val address: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val licenseNumber: String? = null,
    val schedule: Map<String, List<TimeRangeDto>>? = null,
)

/** Reemplaza el perfil completo: lo que no se envie se borra (pm-109). El tipo no se cambia. */
@Serializable
data class UpdateBusinessRequestDto(
    val name: String,
    val description: String? = null,
    val phone: String,
    val whatsApp: String? = null,
    val schedule: Map<String, List<TimeRangeDto>>? = null,
)

@Serializable
data class UpdateLocationRequestDto(val latitude: Double, val longitude: Double)

/** Negocio visto por su dueno (incluye la cedula, que nunca se publica). */
@Serializable
data class BusinessDto(
    val id: String,
    val type: String,
    val name: String,
    val phone: String,
    val whatsApp: String? = null,
    val address: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val licenseNumber: String? = null,
    val schedule: Map<String, List<TimeRangeDto>> = emptyMap(),
)

@Serializable
data class ServiceRequestDto(
    val name: String,
    val description: String? = null,
    val price: Double? = null,
)

@Serializable
data class ServiceDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double? = null,
)

/** Elemento de `businesses/get-nearby` (publico). */
@Serializable
data class NearbyBusinessDto(
    val id: String,
    val name: String,
    val type: String,
    val photoUrl: String? = null,
    val isOpenNow: Boolean = false,
    val todayHours: List<TimeRangeDto> = emptyList(),
    val distanceMeters: Double = 0.0,
)

/** `businesses/get-by-id/{id}` (publico; nunca trae la cedula). */
@Serializable
data class PublicBusinessDto(
    val id: String,
    val name: String,
    val type: String,
    val description: String? = null,
    val photoUrl: String? = null,
    val isOpenNow: Boolean = false,
    val todayHours: List<TimeRangeDto> = emptyList(),
    val schedule: Map<String, List<TimeRangeDto>> = emptyMap(),
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val phone: String? = null,
    val whatsApp: String? = null,
    val services: List<ServiceDto> = emptyList(),
)

/** `data`: Vaccines, Dewormings, Weights, Consultations (las alergias siempre van). `durationMinutes`: 30, 1440 o 10080. */
@Serializable
data class AccessGrantRequestDto(
    val petId: String,
    val businessId: String? = null,
    val data: List<String>,
    val durationMinutes: Int,
)

@Serializable
data class AccessGrantDto(
    val id: String,
    val petId: String,
    val businessId: String? = null,
    val data: List<String> = emptyList(),
    val expiresAt: String = "",
)

@Serializable
data class ResolveScanRequestDto(val code: String)

@Serializable
data class ScanAllergyDto(val name: String, val kind: String = "Allergy", val notes: String? = null)

@Serializable
data class ScanVaccineDto(val name: String, val appliedOn: String, val nextDoseOn: String? = null, val batch: String? = null, val appliedBy: String? = null)

@Serializable
data class ScanDewormingDto(val product: String, val type: String? = null, val appliedOn: String, val nextOn: String? = null, val appliedBy: String? = null)

@Serializable
data class ScanWeightDto(val measuredOn: String, val kilograms: Double)

@Serializable
data class MedicationDto(val name: String, val dose: String, val frequency: String, val duration: String, val instructions: String? = null)

@Serializable
data class ScanConsultationDto(
    val id: String,
    val consultedOn: String,
    val businessName: String? = null,
    val reason: String,
    val diagnosis: String,
    val medications: List<MedicationDto> = emptyList(),
)

/** Ficha que recibe el negocio al escanear (pm-127, pm-128). Una tienda o estetica solo recibe lo basico. */
@Serializable
data class ScanFichaDto(
    val petId: String? = null,
    val photoUrl: String? = null,
    val name: String,
    val species: String,
    val breed: String? = null,
    val ageYears: Int? = null,
    val ageMonths: Int? = null,
    val allergies: List<ScanAllergyDto> = emptyList(),
    val vaccines: List<ScanVaccineDto>? = null,
    val dewormings: List<ScanDewormingDto>? = null,
    val weights: List<ScanWeightDto>? = null,
    val consultations: List<ScanConsultationDto>? = null,
)

@Serializable
data class AttendedPetDto(val petName: String, val photoUrl: String? = null, val scannedAt: String)
