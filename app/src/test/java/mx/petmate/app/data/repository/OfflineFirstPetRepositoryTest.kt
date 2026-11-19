package mx.petmate.app.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.local.AllergyEntity
import mx.petmate.app.data.local.CarnetSyncEntity
import mx.petmate.app.data.local.DewormingEntity
import mx.petmate.app.data.local.HealthDao
import mx.petmate.app.data.local.PetDao
import mx.petmate.app.data.local.PetEntity
import mx.petmate.app.data.local.VaccineEntity
import mx.petmate.app.data.local.WeightEntity
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.repository.PetRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lecturas desde Room, aislamiento por sesion y red ausente (spec 007, T-004; spec 008, T-005). */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstPetRepositoryTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }
    private val remote: PetRepository = mockk()
    private val petDao = InMemoryPetDao()
    private val healthDao = InMemoryHealthDao()
    private var user: String? = "ana"
    private val repository =
        OfflineFirstPetRepository(remote, petDao, healthDao, { user }, dispatchers, now = { 1L })

    private val luna = Pet("p-1", "Luna", PetSpecies.Dog, "Mestiza", "2022-01-10", PetSex.Female)
    private val michi = Pet("p-2", "Michi", PetSpecies.Cat, null, null, PetSex.Unknown)
    private val rabia = Vaccine("v-1", "Rabia", "2026-10-01", null, null, "2027-10-01", RecordOrigin.Owner)
    private val alergia = Allergy("a-1", "Pollo", AllergyKind.Allergy, null, true, RecordOrigin(true, "Vet Tula"))

    private fun onlineWith(
        pets: List<Pet>,
        carnet: Carnet = Carnet(vaccines = listOf(rabia), allergies = listOf(alergia)),
    ) {
        coEvery { remote.getPets() } returns AppResult.Success(pets)
        coEvery { remote.getCarnet(any()) } returns AppResult.Success(carnet)
    }

    private fun offline() {
        coEvery { remote.getPets() } returns AppResult.Failure(AppError.Network)
        coEvery { remote.getCarnet(any()) } returns AppResult.Failure(AppError.Network)
    }

    @Test
    fun `refrescar guarda mascotas y carnets, y leer ya no toca la red`() = runTest(dispatcher) {
        onlineWith(listOf(luna, michi))
        // Cada registro del backend es de una sola mascota: Michi no puede traer la misma vacuna "v-1" que Luna.
        coEvery { remote.getCarnet("p-2") } returns AppResult.Success(Carnet())
        repository.refreshPets()

        val pets = repository.getPets()
        val carnet = repository.getCarnet("p-1")

        assertEquals(listOf(luna, michi), (pets as AppResult.Success).data)
        assertEquals(listOf(rabia), (carnet as AppResult.Success).data.vaccines)
        assertEquals(RecordOrigin(true, "Vet Tula"), carnet.data.allergies.single().origin)
        // Una llamada por el refresco; ninguna mas por las lecturas (008, C-02).
        coVerify(exactly = 1) { remote.getPets() }
        coVerify(exactly = 2) { remote.getCarnet(any()) }
    }

    @Test
    fun `sin red, lo guardado se sigue leyendo completo`() = runTest(dispatcher) {
        onlineWith(listOf(luna))
        repository.refreshPets()
        offline()

        val refreshed = repository.refreshPets()

        assertEquals(AppResult.Failure(AppError.Network), refreshed)
        assertEquals(listOf(luna), (repository.getPets() as AppResult.Success).data)
        assertEquals(listOf(rabia), (repository.getCarnet("p-1") as AppResult.Success).data.vaccines)
    }

    @Test
    fun `un carnet que nunca se bajo da error de red y uno vacio no`() = runTest(dispatcher) {
        assertEquals(AppResult.Failure(AppError.Network), repository.getCarnet("p-1"))

        onlineWith(listOf(luna), Carnet())
        repository.refreshCarnet("p-1")

        assertEquals(AppResult.Success(Carnet()), repository.getCarnet("p-1"))
    }

    @Test
    fun `dos sesiones en el mismo telefono no se mezclan`() = runTest(dispatcher) {
        onlineWith(listOf(luna))
        repository.refreshPets()

        user = "beto"
        onlineWith(listOf(michi))
        repository.refreshPets()

        assertEquals(listOf(michi), (repository.getPets() as AppResult.Success).data)
        assertEquals(AppResult.Failure(AppError.Network), repository.getCarnet("p-1"))
        user = "ana"
        assertEquals(listOf(luna), (repository.getPets() as AppResult.Success).data)
    }

    @Test
    fun `sin sesion no se lee nada local`() = runTest(dispatcher) {
        user = null
        assertEquals(AppResult.Failure(AppError.Unauthorized), repository.getPets())
    }

    @Test
    fun `lo que el servidor ya no manda desaparece al refrescar`() = runTest(dispatcher) {
        onlineWith(listOf(luna, michi))
        repository.refreshPets()
        onlineWith(listOf(michi))
        repository.refreshPets()

        assertEquals(listOf(michi), (repository.getPets() as AppResult.Success).data)
    }

    @Test
    fun `crear guarda en el telefono solo si el servidor confirma`() = runTest(dispatcher) {
        val draft = PetDraft("Luna", PetSpecies.Dog, "", null, PetSex.Female)
        coEvery { remote.createPet(draft) } returns AppResult.Failure(AppError.Network)
        repository.createPet(draft)
        assertTrue((repository.getPets() as AppResult.Success).data.isEmpty())

        coEvery { remote.createPet(draft) } returns AppResult.Success(luna)
        repository.createPet(draft)
        assertEquals(listOf(luna), (repository.getPets() as AppResult.Success).data)
    }

    @Test
    fun `si borrar falla en el servidor la mascota vuelve a verse`() = runTest(dispatcher) {
        onlineWith(listOf(luna))
        repository.refreshPets()
        coEvery { remote.deletePet("p-1") } returns AppResult.Failure(AppError.Network)

        repository.deletePet("p-1")

        assertEquals(listOf(luna), (repository.getPets() as AppResult.Success).data)
    }

    @Test
    fun `borrar en el servidor quita la mascota y su carnet`() = runTest(dispatcher) {
        onlineWith(listOf(luna))
        repository.refreshPets()
        coEvery { remote.deletePet("p-1") } returns AppResult.Success(Unit)

        repository.deletePet("p-1")

        assertTrue((repository.getPets() as AppResult.Success).data.isEmpty())
        assertEquals(AppResult.Failure(AppError.Network), repository.getCarnet("p-1"))
    }

    @Test
    fun `una vacuna confirmada aparece en el carnet guardado`() = runTest(dispatcher) {
        onlineWith(listOf(luna), Carnet())
        repository.refreshPets()
        val draft = VaccineDraft("Rabia", "2026-10-01", "", "", "2027-10-01")
        coEvery { remote.addVaccine("p-1", draft) } returns AppResult.Success(rabia)

        repository.addVaccine("p-1", draft)

        assertEquals(listOf(rabia), repository.observeCarnet("p-1").first().vaccines)
    }
}

/** PetDao en memoria con los mismos filtros que las consultas de Room. */
private class InMemoryPetDao : PetDao() {
    private val rows = MutableStateFlow<List<PetEntity>>(emptyList())
    private fun active(owner: String) = rows.value.filter { it.ownerId == owner && it.active }

    override fun observeActive(ownerId: String): Flow<List<PetEntity>> =
        rows.map { list -> list.filter { it.ownerId == ownerId && it.active } }

    override suspend fun getActive(ownerId: String) = active(ownerId)
    override suspend fun get(ownerId: String, id: String) = active(ownerId).firstOrNull { it.id == id }
    override suspend fun count(ownerId: String) = rows.value.count { it.ownerId == ownerId }
    override suspend fun upsert(pet: PetEntity) = upsertAll(listOf(pet))
    override suspend fun upsertAll(pets: List<PetEntity>) {
        val keys = pets.map { it.ownerId to it.id }.toSet()
        rows.value = rows.value.filterNot { (it.ownerId to it.id) in keys } + pets
    }
    override suspend fun setActive(ownerId: String, id: String, active: Boolean) {
        rows.value = rows.value.map { if (it.ownerId == ownerId && it.id == id) it.copy(active = active) else it }
    }
    override suspend fun delete(ownerId: String, id: String) {
        rows.value = rows.value.filterNot { it.ownerId == ownerId && it.id == id }
    }
    override suspend fun deleteAll(ownerId: String) {
        rows.value = rows.value.filterNot { it.ownerId == ownerId }
    }
}

/** HealthDao en memoria con los mismos filtros que las consultas de Room. */
private class InMemoryHealthDao : HealthDao() {
    private val vaccines = mutableListOf<VaccineEntity>()
    private val dewormings = mutableListOf<DewormingEntity>()
    private val weights = mutableListOf<WeightEntity>()
    private val allergies = mutableListOf<AllergyEntity>()
    private val sync = MutableStateFlow<Map<Pair<String, String>, Long>>(emptyMap())

    override suspend fun vaccines(ownerId: String, petId: String) =
        vaccines.filter { it.ownerId == ownerId && it.petId == petId }
    override suspend fun dewormings(ownerId: String, petId: String) =
        dewormings.filter { it.ownerId == ownerId && it.petId == petId }
    override suspend fun weights(ownerId: String, petId: String) =
        weights.filter { it.ownerId == ownerId && it.petId == petId }
    override suspend fun allergies(ownerId: String, petId: String) =
        allergies.filter { it.ownerId == ownerId && it.petId == petId }
    override fun observeSync(ownerId: String, petId: String): Flow<Long?> = sync.map { it[ownerId to petId] }
    override suspend fun syncedAt(ownerId: String, petId: String) = sync.value[ownerId to petId]
    override suspend fun upsertVaccines(rows: List<VaccineEntity>) = upsert(vaccines, rows) { it.ownerId to it.id }
    override suspend fun upsertDewormings(rows: List<DewormingEntity>) =
        upsert(dewormings, rows) { it.ownerId to it.id }
    override suspend fun upsertWeights(rows: List<WeightEntity>) = upsert(weights, rows) { it.ownerId to it.id }
    override suspend fun upsertAllergies(rows: List<AllergyEntity>) = upsert(allergies, rows) { it.ownerId to it.id }
    override suspend fun markSynced(sync: CarnetSyncEntity) {
        this.sync.value = this.sync.value + ((sync.ownerId to sync.petId) to sync.syncedAtMillis)
    }
    override suspend fun deleteVaccine(ownerId: String, id: String) {
        vaccines.removeAll { it.ownerId == ownerId && it.id == id }
    }
    override suspend fun deleteDeworming(ownerId: String, id: String) {
        dewormings.removeAll { it.ownerId == ownerId && it.id == id }
    }
    override suspend fun petOfAllergy(ownerId: String, id: String) =
        allergies.firstOrNull { it.ownerId == ownerId && it.id == id }?.petId
    override suspend fun clearVaccines(ownerId: String, petId: String) {
        vaccines.removeAll { it.ownerId == ownerId && it.petId == petId }
    }
    override suspend fun clearDewormings(ownerId: String, petId: String) {
        dewormings.removeAll { it.ownerId == ownerId && it.petId == petId }
    }
    override suspend fun clearWeights(ownerId: String, petId: String) {
        weights.removeAll { it.ownerId == ownerId && it.petId == petId }
    }
    override suspend fun clearAllergies(ownerId: String, petId: String) {
        allergies.removeAll { it.ownerId == ownerId && it.petId == petId }
    }
    override suspend fun clearSync(ownerId: String, petId: String) {
        sync.value = sync.value - (ownerId to petId)
    }

    private fun <T, K> upsert(table: MutableList<T>, rows: List<T>, key: (T) -> K) {
        val keys = rows.map(key).toSet()
        table.removeAll { key(it) in keys }
        table.addAll(rows)
    }
}
