package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.DateRange
import mx.petmate.app.domain.model.DayFilter
import mx.petmate.app.domain.repository.DashboardRepository

/**
 * Mascotas atendidas, filtradas por dia (spec 012, T-004, R-06, C-08 y C-09).
 *
 * Traduce el [DayFilter] de la maqueta a un rango de fechas y se lo pasa al
 * repositorio. "Hoy" se inyecta: por omision es la fecha del telefono, que ya
 * viene en su zona horaria, asi que el filtro respeta la zona sin convertir
 * nada a mano. En las pruebas se pasa un dia fijo.
 */
class GetAttendedPetsUseCase(
    private val repository: DashboardRepository,
    private val today: () -> String = PetDates::today,
) {

    suspend operator fun invoke(filter: DayFilter): AppResult<List<AttendedPet>> =
        repository.getAttendedPets(rangeOf(filter))

    /** Rango que cubre [filter], con los dos extremos incluidos. */
    fun rangeOf(filter: DayFilter): DateRange {
        val today = today()
        return when (filter) {
            DayFilter.Today -> DateRange(today, today)
            DayFilter.Yesterday -> shift(today, -1).let { DateRange(it, it) }
            // Siete dias contando hoy: de hoy - 6 a hoy.
            DayFilter.Week -> DateRange(shift(today, -(DayFilter.WEEK_DAYS - 1)), today)
            is DayFilter.On -> DateRange(filter.date, filter.date)
        }
    }

    /**
     * Mueve [iso] [days] dias.
     *
     * Se calcula en UTC a proposito: [iso] ya es la fecha local, y sumar dias
     * sobre una fecha sin hora no puede cambiar de zona. Asi no hay saltos por
     * horario de verano.
     */
    private fun shift(iso: String, days: Int): String {
        val millis = PetDates.toMillisUtc(iso) ?: return iso
        return PetDates.fromMillisUtc(millis + days * DAY_MILLIS)
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
