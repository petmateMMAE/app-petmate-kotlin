package mx.petmate.app.data.remote.dto

import kotlinx.serialization.Serializable

/*
 * Transporte de mascotas, QR y carnet de salud.
 *
 * Contrato: docs/api/endpoints.md del backend (secciones "Mascotas" y "Carnet de salud", pm-040 a pm-103).
 * El backend rechaza miembros desconocidos con 400: los cuerpos llevan SOLO los campos del contrato.
 * Fechas en `yyyy-MM-dd`.
 */

/** Listado paginado (convencion pm-047). */
@Serializable
data class PagedDto<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
)

@Serializable
data class PetDto(
    val id: String,
    val name: String,
    val species: String,
    val breed: String? = null,
    val birthDate: String? = null,
    val sex: String? = null,
    val photoUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** Cuerpo de `pets/create` y `pets/update/{id}` (el update es reemplazo completo). */
@Serializable
data class PetRequestDto(
    val name: String,
    val species: String,
    val breed: String? = null,
    val birthDate: String? = null,
    val sex: String? = null,
)

/** `data` de `pets/get-qr/{id}` y `pets/regenerate-qr/{id}` (pm-094, pm-095). */
@Serializable
data class PetQrDto(
    val petId: String,
    val qrToken: String,
    val qrCode: String,
)

/** Quien registro el dato: el dueno o un negocio (solo lectura para el dueno). */
@Serializable
data class OriginDto(
    val type: String = ORIGIN_OWNER,
    val businessId: String? = null,
    val businessName: String? = null,
) {
    companion object {
        const val ORIGIN_OWNER = "Owner"
        const val ORIGIN_BUSINESS = "Business"
    }
}

@Serializable
data class VaccineDto(
    val id: String,
    val petId: String = "",
    val appliedOn: String,
    val name: String,
    val batch: String? = null,
    val appliedBy: String? = null,
    val nextDoseOn: String? = null,
    val origin: OriginDto = OriginDto(),
)

@Serializable
data class VaccineRequestDto(
    val petId: String,
    val appliedOn: String,
    val name: String,
    val batch: String? = null,
    val appliedBy: String? = null,
    val nextDoseOn: String? = null,
)

@Serializable
data class DewormingDto(
    val id: String,
    val petId: String = "",
    val appliedOn: String,
    val product: String,
    val type: String? = null,
    val appliedBy: String? = null,
    val nextOn: String? = null,
    val origin: OriginDto = OriginDto(),
)

@Serializable
data class DewormingRequestDto(
    val petId: String,
    val appliedOn: String,
    val product: String,
    val type: String? = null,
    val appliedBy: String? = null,
    val nextOn: String? = null,
)

@Serializable
data class WeightDto(
    val id: String,
    val petId: String = "",
    val measuredOn: String,
    val kilograms: Double,
    val origin: OriginDto = OriginDto(),
)

@Serializable
data class WeightRequestDto(
    val petId: String,
    val measuredOn: String,
    val kilograms: Double,
)

@Serializable
data class AllergyDto(
    val id: String,
    val petId: String = "",
    val name: String,
    val kind: String,
    val notes: String? = null,
    val status: String = "Active",
    val origin: OriginDto = OriginDto(),
)

@Serializable
data class AllergyCreateRequestDto(
    val petId: String,
    val name: String,
    val kind: String,
    val notes: String? = null,
)

@Serializable
data class AllergyUpdateRequestDto(
    val name: String,
    val kind: String,
    val notes: String? = null,
    val status: String,
)
