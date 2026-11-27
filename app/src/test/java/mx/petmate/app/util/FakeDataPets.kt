package mx.petmate.app.util

import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetQr
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.WeightRecord

/**
 * Constructores de datos falsos de mascotas y carnet (001/T-023).
 *
 * En archivo aparte de [usuarioFalso] y compania para que no crezca uno solo sin medida: esto es
 * el dominio de las specs 007 y 008, aquello el de 002 y 017.
 */

fun mascotaFalsa(
    id: String = "p-1",
    name: String = "Firulais",
    species: PetSpecies = PetSpecies.Dog,
    breed: String? = "Criollo",
    birthDate: String? = "2022-05-10",
    sex: PetSex = PetSex.Male,
): Pet = Pet(id = id, name = name, species = species, breed = breed, birthDate = birthDate, sex = sex)

fun vacunaFalsa(
    id: String = "v-1",
    name: String = "Rabia",
    appliedOn: String = "2026-01-10",
    nextDoseOn: String? = null,
    porNegocio: Boolean = false,
): Vaccine = Vaccine(
    id = id,
    name = name,
    appliedOn = appliedOn,
    batch = null,
    appliedBy = null,
    nextDoseOn = nextDoseOn,
    origin = RecordOrigin(porNegocio, if (porNegocio) "Veterinaria San Jorge" else null),
)

fun desparasitacionFalsa(
    id: String = "d-1",
    product: String = "Drontal",
    appliedOn: String = "2026-01-10",
    nextOn: String? = null,
): Deworming = Deworming(
    id = id,
    product = product,
    appliedOn = appliedOn,
    type = null,
    appliedBy = null,
    nextOn = nextOn,
    origin = RecordOrigin.Owner,
)

fun pesoFalso(
    id: String = "w-1",
    measuredOn: String = "2026-01-10",
    kilograms: Double = 12.4,
): WeightRecord = WeightRecord(
    id = id,
    measuredOn = measuredOn,
    kilograms = kilograms,
    origin = RecordOrigin.Owner,
)

fun alergiaFalsa(
    id: String = "a-1",
    name: String = "Pollo",
    kind: AllergyKind = AllergyKind.Allergy,
    active: Boolean = true,
): Allergy = Allergy(
    id = id,
    name = name,
    kind = kind,
    notes = null,
    active = active,
    origin = RecordOrigin.Owner,
)

fun qrFalso(
    petId: String = "p-1",
    token: String = "tok-" + "a".repeat(39),
    shortCode: String = "ABCD1234",
): PetQr = PetQr(petId = petId, token = token, shortCode = shortCode)

fun carnetFalso(
    vaccines: List<Vaccine> = emptyList(),
    dewormings: List<Deworming> = emptyList(),
    weights: List<WeightRecord> = emptyList(),
    allergies: List<Allergy> = emptyList(),
): Carnet = Carnet(vaccines = vaccines, dewormings = dewormings, weights = weights, allergies = allergies)
