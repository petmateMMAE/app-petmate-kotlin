package mx.petmate.app.data.repository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.local.dao.DashboardDao
import mx.petmate.app.data.local.entity.AttendedPetEntity
import mx.petmate.app.data.local.entity.DashboardAlertEntity
import mx.petmate.app.data.local.entity.DashboardEntity
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.DateRange
import mx.petmate.app.domain.model.OwnBusiness
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.repository.BusinessRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Panel offline-first y aislamiento por cuenta (spec 012, T-007 y T-008, C-11 y C-13).
 *
 * El DAO se reemplaza por uno en memoria con las mismas reglas de `ownerId`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardRepositoryImplTest {

    /** DAO en memoria. Respeta el filtro por `ownerId` igual que el de Room. */
    internal class FakeDashboardDao : DashboardDao {
        val dashboards = mutableMapOf<String, DashboardEntity>()
        val alerts = mutableListOf<DashboardAlertEntity>()
        val attended = mutableListOf<AttendedPetEntity>()

        private val changes = kotlinx.coroutines.flow.MutableStateFlow(0)

        private fun touch() {
            changes.value += 1
        }

        override fun observeDashboard(ownerId: String): Flow<DashboardEntity?> =
            changes.map { dashboards[ownerId] }

        override fun observeAlerts(ownerId: String): Flow<List<DashboardAlertEntity>> =
            changes.map { alerts.filter { it.ownerId == ownerId } }

        override fun observeAttended(ownerId: String): Flow<List<AttendedPetEntity>> =
            changes.map { attended.filter { it.ownerId == ownerId }.sortedByDescending(AttendedPetEntity::scannedAt) }

        override suspend fun attendedBetween(ownerId: String, from: String, to: String): List<AttendedPetEntity> =
            attended
                .filter { it.ownerId == ownerId && it.scannedOn != null && it.scannedOn >= from && it.scannedOn <= to }
                .sortedByDescending(AttendedPetEntity::scannedAt)

        override suspend fun upsertDashboard(dashboard: DashboardEntity) {
            dashboards[dashboard.ownerId] = dashboard
            touch()
        }

        override suspend fun upsertAlerts(alerts: List<DashboardAlertEntity>) {
            this.alerts += alerts
            touch()
        }

        override suspend fun upsertAttended(attended: List<AttendedPetEntity>) {
            this.attended += attended
            touch()
        }

        override suspend fun deleteAlerts(ownerId: String) {
            alerts.removeAll { it.ownerId == ownerId }
            touch()
        }

        override suspend fun deleteAttended(ownerId: String) {
            attended.removeAll { it.ownerId == ownerId }
            touch()
        }

        override suspend fun deleteDashboard(ownerId: String) {
            dashboards.remove(ownerId)
            touch()
        }

        override suspend fun deleteDashboardOfOthers(ownerId: String) {
            dashboards.keys.retainAll(setOf(ownerId))
            touch()
        }

        override suspend fun deleteAlertsOfOthers(ownerId: String) {
            alerts.removeAll { it.ownerId != ownerId }
            touch()
        }

        override suspend fun deleteAttendedOfOthers(ownerId: String) {
            attended.removeAll { it.ownerId != ownerId }
            touch()
        }

        override suspend fun deleteAllDashboards() {
            dashboards.clear()
            touch()
        }

        override suspend fun deleteAllAlerts() {
            alerts.clear()
            touch()
        }

        override suspend fun deleteAllAttended() {
            attended.clear()
            touch()
        }
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }

    private val businesses: BusinessRepository = mockk()
    private val sessionStore: SessionStore = mockk()
    private val dao = FakeDashboardDao()

    private val repository = DashboardRepositoryImpl(businesses, dao, sessionStore, dispatchers)

    private val ana = User(id = "u1", name = "Ana", email = "ana@petmate.mx")
    private val beto = User(id = "u2", name = "Beto", email = "beto@petmate.mx")

    private val own = OwnBusiness(
        id = "b1",
        kind = BusinessKind.Veterinary,
        name = "Veterinaria Centro",
        phone = "5512345678",
        whatsApp = null,
        address = "Calle 1",
        description = null,
        latitude = 19.43,
        longitude = -99.13,
        licenseNumber = "CED-100",
        schedule = emptyMap(),
    )

    private val attendedFromServer = listOf(
        AttendedPet("Nube", "2026-11-26T10:00:00Z"),
        AttendedPet("Rocco", "2026-11-24T18:30:00Z"),
    )

    private fun signedInAs(user: User?) {
        every { sessionStore.readUser() } returns user
    }

    private fun hasBusiness(business: OwnBusiness? = own) {
        every { businesses.cachedOwnBusiness() } returns business
    }

    // ---- T-007: red refresca Room, la UI observa Room ----

    @Test
    fun `sin datos locales y con red exitosa el panel queda guardado`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)

        assertEquals(AppResult.Success(Unit), repository.refreshDashboard())

        val dashboard = repository.observeDashboard().first()
        assertNotNull(dashboard)
        assertEquals("Veterinaria Centro", dashboard?.businessName)
        assertEquals(listOf("Nube", "Rocco"), dashboard?.attended?.map(AttendedPet::name))
    }

    @Test
    fun `sin panel guardado el flujo da nulo`() = runTest {
        signedInAs(ana)

        assertNull(repository.observeDashboard().first())
    }

    @Test
    fun `un error de red no borra lo guardado y se reporta`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)
        repository.refreshDashboard()

        coEvery { businesses.getAttended(any()) } returns AppResult.Failure(AppError.Network)
        val result = repository.refreshDashboard()

        assertEquals(AppError.Network, (result as AppResult.Failure).error)
        val dashboard = repository.observeDashboard().first()
        assertEquals(
            "lo guardado debe seguir ahi",
            listOf("Nube", "Rocco"),
            dashboard?.attended?.map(AttendedPet::name),
        )
    }

    @Test
    fun `refrescar reemplaza lo atendido y no lo acumula`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)
        repository.refreshDashboard()

        coEvery { businesses.getAttended(any()) } returns
            AppResult.Success(listOf(AttendedPet("Nube", "2026-11-26T10:00:00Z")))
        repository.refreshDashboard()

        assertEquals(listOf("Nube"), repository.observeDashboard().first()?.attended?.map(AttendedPet::name))
    }

    @Test
    fun `sin negocio afiliado no hay panel que refrescar`() = runTest {
        signedInAs(ana)
        hasBusiness(null)

        val result = repository.refreshDashboard()

        assertEquals(AppError.Server(404), (result as AppResult.Failure).error)
    }

    @Test
    fun `sin sesion no se refresca`() = runTest {
        signedInAs(null)

        val result = repository.refreshDashboard()

        assertEquals(AppError.Unauthorized, (result as AppResult.Failure).error)
    }

    // ---- Lectura por rango: sale de Room, no de la red ----

    @Test
    fun `las atendidas de un rango salen de la base y no piden red`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)
        repository.refreshDashboard()

        val result = repository.getAttendedPets(DateRange("2026-11-26", "2026-11-26"))

        assertEquals(listOf("Nube"), (result as AppResult.Success).data.map(AttendedPet::name))
    }

    @Test
    fun `un rango sin nada devuelve lista vacia y no error`() = runTest {
        signedInAs(ana)

        val result = repository.getAttendedPets(DateRange("2020-01-01", "2020-01-02"))

        assertTrue((result as AppResult.Success).data.isEmpty())
    }

    // ---- T-008: aislamiento por cuenta (C-13) ----

    @Test
    fun `al cambiar de cuenta no se ve el panel del negocio anterior`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)
        repository.refreshDashboard()

        signedInAs(beto)

        assertNull(repository.observeDashboard().first())
        assertTrue(repository.getAttendedPets(DateRange("2026-11-01", "2026-11-30")).let {
            (it as AppResult.Success).data.isEmpty()
        })
    }

    @Test
    fun `refrescar con otra cuenta tira el panel de la anterior`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)
        repository.refreshDashboard()

        signedInAs(beto)
        repository.refreshDashboard()

        assertEquals(setOf("u2"), dao.dashboards.keys)
        assertTrue("no debe quedar nada de la cuenta anterior", dao.attended.none { it.ownerId == "u1" })
    }

    @Test
    fun `el logout borra el panel de todas las cuentas`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)
        repository.refreshDashboard()

        signedInAs(null)
        assertEquals(AppResult.Success(Unit), repository.clearDashboard())

        assertTrue(dao.dashboards.isEmpty())
        assertTrue(dao.attended.isEmpty())
        assertTrue(dao.alerts.isEmpty())
    }

    @Test
    fun `sin sesion el panel no se observa`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)
        repository.refreshDashboard()

        signedInAs(null)

        assertNull(repository.observeDashboard().first())
    }

    // ---- D-05: la cache no guarda contenido clinico ----

    @Test
    fun `lo guardado no tiene diagnostico ni receta`() = runTest {
        signedInAs(ana)
        hasBusiness()
        coEvery { businesses.getAttended(any()) } returns AppResult.Success(attendedFromServer)
        repository.refreshDashboard()

        // Las columnas de la cache son solo identificadores, nombres y fechas.
        val row = dao.attended.first()
        assertEquals("Nube", row.name)
        assertEquals("2026-11-26", row.scannedOn)
        assertNull("la cache no guarda el id clinico si el servidor no lo manda", row.petId)
    }
}
