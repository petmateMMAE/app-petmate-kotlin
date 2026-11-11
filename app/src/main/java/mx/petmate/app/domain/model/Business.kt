package mx.petmate.app.domain.model

/*
 * Negocio afiliado, mapa y acceso por QR (specs 010 a 013 de la app; backend 0.4.0, pm-105 a pm-129).
 */

/** Tipo de negocio del backend: uno solo por negocio. */
enum class BusinessKind(val apiValue: String) {
    Veterinary("Veterinary"),
    Store("Store"),
    Grooming("Grooming"),
    ;

    companion object {
        fun fromApi(value: String?): BusinessKind? = entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

/** Franja `HH:mm` (cierre exclusivo). */
data class TimeRange(val open: String, val close: String)

/** Dias del horario en el orden del backend. */
val SCHEDULE_DAYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

/** Negocio propio (lo ve su dueno; incluye la cedula). */
data class OwnBusiness(
    val id: String,
    val kind: BusinessKind,
    val name: String,
    val phone: String,
    val whatsApp: String?,
    val address: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val licenseNumber: String?,
    val schedule: Map<String, List<TimeRange>>,
)

/** Lo que llena el formulario de "Afilia tu negocio" o "Mi negocio". */
data class BusinessDraft(
    val kind: BusinessKind?,
    val name: String,
    val phone: String,
    val whatsApp: String,
    val address: String,
    val description: String,
    val licenseNumber: String,
    val schedule: Map<String, List<TimeRange>>,
)

data class BusinessService(val id: String, val name: String, val description: String?, val price: Double?)

/** Negocio del mapa publico. */
data class NearbyBusiness(
    val id: String,
    val name: String,
    val kind: BusinessKind,
    val isOpen: Boolean,
    val todayHours: List<TimeRange>,
    val distanceMeters: Double,
    val photoUrl: String?,
    /** get-nearby no trae coordenadas ni contacto: se completan con el perfil publico de cada negocio. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    val whatsApp: String? = null,
)

/** Perfil publico del negocio (nunca trae la cedula). */
data class PublicBusiness(
    val id: String,
    val name: String,
    val kind: BusinessKind,
    val description: String?,
    val isOpen: Boolean,
    val todayHours: List<TimeRange>,
    val schedule: Map<String, List<TimeRange>>,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phone: String?,
    val whatsApp: String?,
    val services: List<BusinessService>,
)

/** Lo que el dueno comparte ademas de las alergias, que siempre van (pm-123). */
enum class SharedData(val apiValue: String) { Vaccines("Vaccines"), Dewormings("Dewormings"), Weights("Weights"), Consultations("Consultations") }

/** Duraciones que acepta el backend. */
enum class ShareDuration(val minutes: Int) { ThirtyMinutes(30), OneDay(1440), SevenDays(10080) }

data class AccessGrant(val id: String, val businessId: String?, val data: List<SharedData>, val expiresAt: String)

data class Medication(val name: String, val dose: String, val frequency: String, val duration: String, val instructions: String?)

data class FichaConsultation(val date: String, val businessName: String?, val reason: String, val diagnosis: String, val medications: List<Medication>)

/** Ficha que ve el negocio al escanear. Las listas son null cuando el negocio no las puede ver. */
data class ScanFicha(
    val petId: String?,
    val name: String,
    val species: String,
    val breed: String?,
    val ageYears: Int?,
    val ageMonths: Int?,
    val allergies: List<Allergy>,
    val vaccines: List<Vaccine>?,
    val dewormings: List<Deworming>?,
    val weights: List<WeightRecord>?,
    val consultations: List<FichaConsultation>?,
)

data class AttendedPet(val name: String, val scannedAt: String)
