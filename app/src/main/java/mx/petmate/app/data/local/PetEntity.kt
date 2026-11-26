package mx.petmate.app.data.local

import androidx.room.Entity
import androidx.room.Index
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies

/**
 * Mascota guardada en el telefono (spec 007, T-002). Es la fuente de verdad de la lista (constitucion P4).
 *
 * [ownerId] es el usuario de la sesion que la descargo: toda consulta filtra por el, asi dos cuentas en el mismo
 * telefono nunca ven las mascotas de la otra (spec 007, C-09). Enums por nombre del backend, no por orden.
 */
@Entity(
    tableName = "pets",
    primaryKeys = ["ownerId", "id"],
    indices = [Index(value = ["ownerId", "active"])],
)
data class PetEntity(
    val ownerId: String,
    val id: String,
    val name: String,
    val species: String,
    val breed: String?,
    val birthDate: String?,
    val sex: String,
    /** Falso mientras se borra: deja de verse en la lista aunque el servidor aun no confirme. */
    val active: Boolean = true,
    val syncedAtMillis: Long,
)

fun PetEntity.toDomain() = Pet(
    id = id,
    name = name,
    species = PetSpecies.fromApi(species),
    breed = breed,
    birthDate = birthDate,
    sex = PetSex.fromApi(sex),
)

fun Pet.toEntity(ownerId: String, nowMillis: Long) = PetEntity(
    ownerId = ownerId,
    id = id,
    name = name,
    species = species.apiValue,
    breed = breed,
    birthDate = birthDate,
    sex = sex.apiValue,
    syncedAtMillis = nowMillis,
)
