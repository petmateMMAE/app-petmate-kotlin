package mx.petmate.app.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.PetsApiService
import mx.petmate.app.data.remote.dto.AllergyCreateRequestDto
import mx.petmate.app.data.remote.dto.AllergyDto
import mx.petmate.app.data.remote.dto.AllergyUpdateRequestDto
import mx.petmate.app.data.remote.dto.DewormingDto
import mx.petmate.app.data.remote.dto.DewormingRequestDto
import mx.petmate.app.data.remote.dto.OriginDto
import mx.petmate.app.data.remote.dto.PetDto
import mx.petmate.app.data.remote.dto.PetQrDto
import mx.petmate.app.data.remote.dto.PetRequestDto
import mx.petmate.app.data.remote.dto.VaccineDto
import mx.petmate.app.data.remote.dto.VaccineRequestDto
import mx.petmate.app.data.remote.dto.WeightDto
import mx.petmate.app.data.remote.dto.WeightRequestDto
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetQr
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.model.WeightRecord
import mx.petmate.app.domain.repository.PetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mascotas, QR y carnet contra el backend real (docs/api/endpoints.md, pm-040 a pm-103).
 *
 * En linea, sin copia local: la spec 007 preveia Room y modo sin conexion, pero el alcance de hoy es el
 * del backend del primer hito. La copia local queda pendiente (spec 005).
 */
@Singleton
class PetRepositoryImpl @Inject constructor(
    private val api: PetsApiService,
    private val caller: AuthorizedCaller,
    private val dispatchers: DispatcherProvider,
) : PetRepository {

    override suspend fun getPets(): AppResult<List<Pet>> = io {
        caller.call { api.getPets(it).data?.items.orEmpty().map(PetDto::toDomain) }
    }

    override suspend fun getPet(id: String): AppResult<Pet> = io {
        caller.call { required(api.getPet(it, id).data).toDomain() }
    }

    override suspend fun createPet(draft: PetDraft): AppResult<Pet> = io {
        caller.call { required(api.createPet(it, draft.toRequest()).data).toDomain() }
    }

    override suspend fun updatePet(id: String, draft: PetDraft): AppResult<Pet> = io {
        caller.call { required(api.updatePet(it, id, draft.toRequest()).data).toDomain() }
    }

    override suspend fun deletePet(id: String): AppResult<Unit> = io {
        caller.call { api.deletePet(it, id); Unit }
    }

    override suspend fun getQr(petId: String): AppResult<PetQr> = io {
        caller.call { required(api.getQr(it, petId).data).toDomain() }
    }

    override suspend fun regenerateQr(petId: String): AppResult<PetQr> = io {
        caller.call { required(api.regenerateQr(it, petId).data).toDomain() }
    }

    override suspend fun getCarnet(petId: String): AppResult<Carnet> = io {
        caller.call { bearer ->
            coroutineScope {
                val vaccines = async { api.getVaccines(bearer, petId).data?.items.orEmpty() }
                val dewormings = async { api.getDewormings(bearer, petId).data?.items.orEmpty() }
                val weights = async { api.getWeights(bearer, petId).data?.items.orEmpty() }
                val allergies = async { api.getAllergies(bearer, petId).data?.items.orEmpty() }
                Carnet(
                    vaccines = vaccines.await().map(VaccineDto::toDomain),
                    dewormings = dewormings.await().map(DewormingDto::toDomain),
                    weights = weights.await().map(WeightDto::toDomain),
                    allergies = allergies.await().map(AllergyDto::toDomain),
                )
            }
        }
    }

    override suspend fun addVaccine(petId: String, draft: VaccineDraft): AppResult<Vaccine> = io {
        caller.call {
            val body = VaccineRequestDto(
                petId = petId,
                appliedOn = draft.appliedOn,
                name = draft.name.trim(),
                batch = draft.batch.trim().ifEmpty { null },
                appliedBy = draft.appliedBy.trim().ifEmpty { null },
                nextDoseOn = draft.nextDoseOn,
            )
            required(api.createVaccine(it, body).data).toDomain()
        }
    }

    override suspend fun deleteVaccine(id: String): AppResult<Unit> = io {
        caller.call { api.deleteVaccine(it, id); Unit }
    }

    override suspend fun addDeworming(petId: String, draft: DewormingDraft): AppResult<Deworming> = io {
        caller.call {
            val body = DewormingRequestDto(
                petId = petId,
                appliedOn = draft.appliedOn,
                product = draft.product.trim(),
                type = draft.type.apiValue,
                appliedBy = draft.appliedBy.trim().ifEmpty { null },
                nextOn = draft.nextOn,
            )
            required(api.createDeworming(it, body).data).toDomain()
        }
    }

    override suspend fun deleteDeworming(id: String): AppResult<Unit> = io {
        caller.call { api.deleteDeworming(it, id); Unit }
    }

    override suspend fun addWeight(petId: String, measuredOn: String, kilograms: Double): AppResult<WeightRecord> = io {
        caller.call { required(api.createWeight(it, WeightRequestDto(petId, measuredOn, kilograms)).data).toDomain() }
    }

    override suspend fun addAllergy(petId: String, draft: AllergyDraft): AppResult<Allergy> = io {
        caller.call {
            val body = AllergyCreateRequestDto(
                petId = petId,
                name = draft.name.trim(),
                kind = draft.kind.apiValue,
                notes = draft.notes.trim().ifEmpty { null },
            )
            val created = required(api.createAllergy(it, body).data)
            // El alta siempre nace activa; si el usuario la marco como superada se actualiza enseguida.
            if (draft.active) created.toDomain() else required(api.updateAllergy(it, created.id, draft.toUpdate()).data).toDomain()
        }
    }

    override suspend fun updateAllergy(id: String, draft: AllergyDraft): AppResult<Allergy> = io {
        caller.call { required(api.updateAllergy(it, id, draft.toUpdate()).data).toDomain() }
    }

    private suspend fun <T> io(block: suspend () -> AppResult<T>): AppResult<T> = withContext(dispatchers.io) { block() }

    private fun <T> required(data: T?): T = data ?: throw IllegalStateException("Respuesta sin data")
}

// ---------- Mapeo transporte <-> dominio ----------

internal fun PetDto.toDomain() = Pet(
    id = id,
    name = name,
    species = PetSpecies.fromApi(species),
    breed = breed?.takeIf { it.isNotBlank() },
    birthDate = birthDate,
    sex = PetSex.fromApi(sex),
)

internal fun PetDraft.toRequest() = PetRequestDto(
    name = name.trim(),
    species = requireNotNull(species) { "La especie es obligatoria" }.apiValue,
    breed = breed.trim().ifEmpty { null },
    birthDate = birthDate,
    sex = sex.apiValue,
)

internal fun PetQrDto.toDomain() = PetQr(petId = petId, token = qrToken, shortCode = qrCode)

internal fun OriginDto.toDomain() = RecordOrigin(
    byBusiness = type.equals(OriginDto.ORIGIN_BUSINESS, ignoreCase = true),
    businessName = businessName,
)

internal fun VaccineDto.toDomain() = Vaccine(id, name, appliedOn, batch, appliedBy, nextDoseOn, origin.toDomain())

internal fun DewormingDto.toDomain() =
    Deworming(id, product, appliedOn, DewormingType.fromApi(type), appliedBy, nextOn, origin.toDomain())

internal fun WeightDto.toDomain() = WeightRecord(id, measuredOn, kilograms, origin.toDomain())

internal fun AllergyDto.toDomain() = Allergy(
    id = id,
    name = name,
    kind = AllergyKind.fromApi(kind),
    notes = notes,
    active = !status.equals(STATUS_RESOLVED, ignoreCase = true),
    origin = origin.toDomain(),
)

internal fun AllergyDraft.toUpdate() = AllergyUpdateRequestDto(
    name = name.trim(),
    kind = kind.apiValue,
    notes = notes.trim().ifEmpty { null },
    status = if (active) STATUS_ACTIVE else STATUS_RESOLVED,
)

private const val STATUS_ACTIVE = "Active"
private const val STATUS_RESOLVED = "Resolved"
