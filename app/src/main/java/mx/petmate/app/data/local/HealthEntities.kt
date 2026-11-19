package mx.petmate.app.data.local

import androidx.room.Entity
import androidx.room.Index
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.WeightRecord

/*
 * Carnet guardado en el telefono (spec 008, T-002). Cada seccion tiene su tabla; las condiciones viven en
 * `allergies` con `kind = Condition`, igual que en el backend. Todas llevan [ownerId] y [petId] para filtrar
 * por sesion y por mascota. El origen (`byBusiness`) viaja con cada registro: lo de un negocio sigue siendo de
 * solo lectura aunque se lea sin conexion (spec 008, C-07).
 */

@Entity(tableName = "vaccines", primaryKeys = ["ownerId", "id"], indices = [Index(value = ["ownerId", "petId"])])
data class VaccineEntity(
    val ownerId: String,
    val petId: String,
    val id: String,
    val name: String,
    val appliedOn: String,
    val batch: String?,
    val appliedBy: String?,
    val nextDoseOn: String?,
    val byBusiness: Boolean,
    val businessName: String?,
)

@Entity(tableName = "dewormings", primaryKeys = ["ownerId", "id"], indices = [Index(value = ["ownerId", "petId"])])
data class DewormingEntity(
    val ownerId: String,
    val petId: String,
    val id: String,
    val product: String,
    val appliedOn: String,
    val type: String?,
    val appliedBy: String?,
    val nextOn: String?,
    val byBusiness: Boolean,
    val businessName: String?,
)

@Entity(tableName = "weights", primaryKeys = ["ownerId", "id"], indices = [Index(value = ["ownerId", "petId"])])
data class WeightEntity(
    val ownerId: String,
    val petId: String,
    val id: String,
    val measuredOn: String,
    val kilograms: Double,
    val byBusiness: Boolean,
    val businessName: String?,
)

@Entity(tableName = "allergies", primaryKeys = ["ownerId", "id"], indices = [Index(value = ["ownerId", "petId"])])
data class AllergyEntity(
    val ownerId: String,
    val petId: String,
    val id: String,
    val name: String,
    val kind: String,
    val notes: String?,
    val active: Boolean,
    val byBusiness: Boolean,
    val businessName: String?,
)

/**
 * Marca de que el carnet de una mascota ya se descargo al menos una vez. Distingue "carnet vacio" de "nunca se
 * bajo": sin conexion, lo segundo es un error y lo primero no.
 */
@Entity(tableName = "carnet_sync", primaryKeys = ["ownerId", "petId"])
data class CarnetSyncEntity(
    val ownerId: String,
    val petId: String,
    val syncedAtMillis: Long,
)

private fun origin(byBusiness: Boolean, businessName: String?) = RecordOrigin(byBusiness, businessName)

fun VaccineEntity.toDomain() = Vaccine(
    id, name, appliedOn, batch, appliedBy, nextDoseOn, origin(byBusiness, businessName),
)

fun Vaccine.toEntity(ownerId: String, petId: String) = VaccineEntity(
    ownerId, petId, id, name, appliedOn, batch, appliedBy, nextDoseOn, origin.byBusiness, origin.businessName,
)

fun DewormingEntity.toDomain() = Deworming(
    id, product, appliedOn, DewormingType.fromApi(type), appliedBy, nextOn, origin(byBusiness, businessName),
)

fun Deworming.toEntity(ownerId: String, petId: String) = DewormingEntity(
    ownerId, petId, id, product, appliedOn, type?.apiValue, appliedBy, nextOn, origin.byBusiness, origin.businessName,
)

fun WeightEntity.toDomain() = WeightRecord(id, measuredOn, kilograms, origin(byBusiness, businessName))

fun WeightRecord.toEntity(ownerId: String, petId: String) = WeightEntity(
    ownerId, petId, id, measuredOn, kilograms, origin.byBusiness, origin.businessName,
)

fun AllergyEntity.toDomain() = Allergy(
    id, name, AllergyKind.fromApi(kind), notes, active, origin(byBusiness, businessName),
)

fun Allergy.toEntity(ownerId: String, petId: String) = AllergyEntity(
    ownerId, petId, id, name, kind.apiValue, notes, active, origin.byBusiness, origin.businessName,
)
