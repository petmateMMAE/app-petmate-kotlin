package mx.petmate.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.BusinessDashboard
import mx.petmate.app.domain.repository.DashboardRepository

/**
 * Panel del afiliado para la pantalla (spec 012, T-004, R-01).
 *
 * Observa lo guardado y no pide nada a la red: refrescar es una llamada aparte,
 * para que la pantalla pinte de inmediato lo que ya tiene y el error de red no
 * vacie el panel (T-007, C-11).
 */
class GetDashboardUseCase(private val repository: DashboardRepository) {

    /** Panel guardado; null mientras no se haya descargado nunca. */
    operator fun invoke(): Flow<BusinessDashboard?> = repository.observeDashboard()

    /** Trae lo nuevo del servidor. Lo llama la pantalla al abrirse y al tirar para refrescar. */
    suspend fun refresh(): AppResult<Unit> = repository.refreshDashboard()
}
