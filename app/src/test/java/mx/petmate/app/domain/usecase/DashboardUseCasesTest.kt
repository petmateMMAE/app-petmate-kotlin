package mx.petmate.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessDashboard
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.DateRange
import mx.petmate.app.domain.model.DayFilter
import mx.petmate.app.domain.repository.DashboardRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * Panel y filtro por dia (spec 012, T-004, C-08 y C-09).
 *
 * El reloj se inyecta: ninguna prueba depende del dia en que se corra.
 */
class DashboardUseCasesTest {

    /** Doble del repositorio: guarda el rango que le pidieron. */
    private class FakeDashboardRepository(
        private val dashboard: BusinessDashboard? = null,
        private val attended: List<AttendedPet> = emptyList(),
    ) : DashboardRepository {
        var askedFor: DateRange? = null
        var refreshes = 0

        override fun observeDashboard(): Flow<BusinessDashboard?> = flowOf(dashboard)

        override suspend fun refreshDashboard(): AppResult<Unit> {
            refreshes++
            return AppResult.Success(Unit)
        }

        override suspend fun getAttendedPets(range: DateRange): AppResult<List<AttendedPet>> {
            askedFor = range
            return AppResult.Success(attended)
        }

        override suspend fun clearDashboard(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private val originalZone: TimeZone = TimeZone.getDefault()

    @After
    fun restoreZone() {
        TimeZone.setDefault(originalZone)
    }

    private fun useCase(
        today: String = TODAY,
        repository: FakeDashboardRepository = FakeDashboardRepository(),
    ) = GetAttendedPetsUseCase(repository) { today } to repository

    // ---- C-08: Hoy ----

    @Test
    fun `Hoy cubre solo el dia de hoy`() {
        val (attended, _) = useCase()

        assertEquals(DateRange(TODAY, TODAY), attended.rangeOf(DayFilter.Today))
    }

    @Test
    fun `Hoy le pide al repositorio justo ese rango`() = runTest {
        val (attended, repository) = useCase()

        attended(DayFilter.Today)

        assertEquals(DateRange(TODAY, TODAY), repository.askedFor)
    }

    // ---- C-09: Ayer, Semana y fecha concreta ----

    @Test
    fun `Ayer cubre solo el dia anterior`() {
        val (attended, _) = useCase()

        assertEquals(DateRange("2026-11-25", "2026-11-25"), attended.rangeOf(DayFilter.Yesterday))
    }

    @Test
    fun `Semana cubre siete dias contando hoy`() {
        val (attended, _) = useCase()

        assertEquals(DateRange("2026-11-20", TODAY), attended.rangeOf(DayFilter.Week))
    }

    @Test
    fun `una fecha concreta cubre solo ese dia`() {
        val (attended, _) = useCase()

        assertEquals(
            DateRange("2026-03-01", "2026-03-01"),
            attended.rangeOf(DayFilter.On("2026-03-01")),
        )
    }

    @Test
    fun `Ayer y Semana cruzan bien el cambio de ano`() {
        val (attended, _) = useCase(today = "2026-01-01")

        assertEquals(DateRange("2025-12-31", "2025-12-31"), attended.rangeOf(DayFilter.Yesterday))
        assertEquals(DateRange("2025-12-26", "2026-01-01"), attended.rangeOf(DayFilter.Week))
    }

    @Test
    fun `Semana cruza bien el cambio de mes`() {
        val (attended, _) = useCase(today = "2026-03-03")

        assertEquals(DateRange("2026-02-25", "2026-03-03"), attended.rangeOf(DayFilter.Week))
    }

    @Test
    fun `un ano bisiesto no se salta el 29 de febrero`() {
        val (attended, _) = useCase(today = "2028-03-01")

        assertEquals(DateRange("2028-02-29", "2028-02-29"), attended.rangeOf(DayFilter.Yesterday))
    }

    // ---- C-09: zona horaria ----

    @Test
    fun `el rango sale de la fecha local, asi que respeta la zona horaria`() {
        // Dos zonas con 25 horas de diferencia: casi siempre estan en dias distintos.
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        val east = GetAttendedPetsUseCase(FakeDashboardRepository())
        val eastToday = east.rangeOf(DayFilter.Today)

        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Niue"))
        val west = GetAttendedPetsUseCase(FakeDashboardRepository())
        val westToday = west.rangeOf(DayFilter.Today)

        // Cada uno toma su propio "hoy" y el rango es de un solo dia.
        assertEquals(eastToday.from, eastToday.to)
        assertEquals(westToday.from, westToday.to)
        assertTrue("el este nunca va antes que el oeste", eastToday.from >= westToday.from)
    }

    @Test
    fun `la zona horaria no parte la semana`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"))
        val range = GetAttendedPetsUseCase(FakeDashboardRepository()).rangeOf(DayFilter.Week)

        assertEquals(
            "siempre siete dias, sin importar la zona",
            DayFilter.WEEK_DAYS - 1,
            PetDates.daysBetween(range.from, range.to),
        )
    }

    // ---- El rango incluye los dos extremos ----

    @Test
    fun `el rango incluye sus dos extremos y excluye lo de fuera`() {
        val range = useCase().first.rangeOf(DayFilter.Week)

        assertTrue("2026-11-20" in range)
        assertTrue(TODAY in range)
        assertTrue("2026-11-23" in range)
        assertTrue("2026-11-19" !in range)
        assertTrue("2026-11-27" !in range)
    }

    // ---- GetDashboardUseCase ----

    @Test
    fun `el panel se observa desde el repositorio`() = runTest {
        val dashboard = BusinessDashboard(
            businessId = "b1",
            businessName = "Veterinaria Centro",
            kind = BusinessKind.Veterinary,
        )
        val repository = FakeDashboardRepository(dashboard = dashboard)

        assertEquals(dashboard, GetDashboardUseCase(repository)().first())
    }

    @Test
    fun `sin panel guardado el flujo da nulo y no truena`() = runTest {
        assertEquals(null, GetDashboardUseCase(FakeDashboardRepository())().first())
    }

    @Test
    fun `refrescar pasa por el repositorio`() = runTest {
        val repository = FakeDashboardRepository()

        GetDashboardUseCase(repository).refresh()

        assertEquals(1, repository.refreshes)
    }

    private companion object {
        const val TODAY = "2026-11-26"
    }
}
