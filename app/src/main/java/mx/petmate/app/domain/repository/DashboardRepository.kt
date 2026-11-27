package mx.petmate.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessDashboard
import mx.petmate.app.domain.model.DateRange

/**
 * Panel del afiliado (spec 012, T-002).
 *
 * La UI observa [observeDashboard], que sale de la base local, y pide
 * [refreshDashboard] para traer lo nuevo: un error de red no borra lo guardado
 * (T-007, C-11). Todo se asocia al negocio de la sesion activa; aqui nunca se
 * pasa un identificador de negocio (T-008, C-13).
 */
interface DashboardRepository {

    /** Panel guardado, que cambia solo cuando cambia la base local. Null mientras no haya nada. */
    fun observeDashboard(): Flow<BusinessDashboard?>

    /** Trae el panel del servidor y lo guarda. */
    suspend fun refreshDashboard(): AppResult<Unit>

    /**
     * Mascotas atendidas dentro de [range], con los dos extremos incluidos.
     *
     * El rango lo arma `GetAttendedPetsUseCase` a partir del `DayFilter` (T-004).
     */
    suspend fun getAttendedPets(range: DateRange): AppResult<List<AttendedPet>>

    /** Tira el panel guardado. Se llama en el logout y al cambiar de cuenta (T-008). */
    suspend fun clearDashboard(): AppResult<Unit>
}
