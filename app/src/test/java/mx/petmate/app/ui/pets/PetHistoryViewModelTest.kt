package mx.petmate.app.ui.pets

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.VeterinaryRepositoryFalso
import mx.petmate.app.util.consultaFalsa
import mx.petmate.app.util.elementoDeHistorialFalso
import mx.petmate.app.util.medicamentoFalso
import mx.petmate.app.util.seguimientoFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Historial de la mascota y detalle de una consulta (specs 014 y 015).
 *
 * Esta pantalla no escribe nada, asi que lo que hay que cuidar es lo contrario de lo habitual: que
 * un fallo **no borre** lo que ya se estaba viendo, y que el detalle siga abriendo aunque la
 * consulta no tenga seguimiento, que es el caso normal.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetHistoryViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = VeterinaryRepositoryFalso()

    private fun viewModel() = PetHistoryViewModel(repository)

    @Test
    fun `el historial llega ordenado como lo manda el servidor`() = runTest(mainDispatcher.dispatcher) {
        // El orden lo decide el backend (pm-137); la pantalla no lo reacomoda.
        repository.historial = AppResult.Success(
            listOf(
                elementoDeHistorialFalso(kind = "Consultation", id = "c-2", date = "2026-11-20"),
                elementoDeHistorialFalso(kind = "Vaccine", id = "v-1", date = "2026-10-01", title = "Rabia"),
            ),
        )
        val vm = viewModel()

        vm.loadHistory("p-1")
        advanceUntilIdle()

        assertEquals(listOf("c-2", "v-1"), vm.historyState.value.items.map { it.id })
        assertEquals("p-1", repository.ultimoIdDeMascota)
        assertNull(vm.historyState.value.error)
    }

    @Test
    fun `pide el historial completo, sin filtrar tipos`() = runTest(mainDispatcher.dispatcher) {
        // La pantalla muestra todo junto: si mandara un filtro, se perderian vacunas o pesos.
        repository.historial = AppResult.Success(emptyList())
        val vm = viewModel()

        vm.loadHistory("p-1")
        advanceUntilIdle()

        assertNull(repository.ultimosTipos)
    }

    @Test
    fun `mientras carga lo dice y al terminar lo apaga`() = runTest(mainDispatcher.dispatcher) {
        repository.historial = AppResult.Success(emptyList())
        val vm = viewModel()

        vm.loadHistory("p-1")
        assertTrue(vm.historyState.value.isLoading)

        advanceUntilIdle()
        assertFalse(vm.historyState.value.isLoading)
    }

    @Test
    fun `un historial vacio no es un error`() = runTest(mainDispatcher.dispatcher) {
        // Una mascota recien registrada no tiene historial, y eso no es una falla que avisar.
        repository.historial = AppResult.Success(emptyList())
        val vm = viewModel()

        vm.loadHistory("p-1")
        advanceUntilIdle()

        assertTrue(vm.historyState.value.items.isEmpty())
        assertNull(vm.historyState.value.error)
    }

    @Test
    fun `sin conexion lo dice con palabras, no con un codigo`() = runTest(mainDispatcher.dispatcher) {
        repository.historial = AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.loadHistory("p-1")
        advanceUntilIdle()

        assertEquals("Sin conexión a internet.", vm.historyState.value.error)
        assertFalse(vm.historyState.value.isLoading)
    }

    @Test
    fun `cada error del servidor tiene su mensaje`() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        repository.historial = AppResult.Failure(AppError.Timeout)
        vm.loadHistory("p-1")
        advanceUntilIdle()
        assertEquals("El servidor tardó en responder.", vm.historyState.value.error)

        repository.historial = AppResult.Failure(AppError.Unauthorized)
        vm.loadHistory("p-1")
        advanceUntilIdle()
        assertEquals("Sesión expirada.", vm.historyState.value.error)

        repository.historial = AppResult.Failure(AppError.Server(503))
        vm.loadHistory("p-1")
        advanceUntilIdle()
        assertEquals("Error del servidor (503).", vm.historyState.value.error)

        repository.historial = AppResult.Failure(AppError.Unknown())
        vm.loadHistory("p-1")
        advanceUntilIdle()
        assertEquals("Ocurrió un error al cargar los datos.", vm.historyState.value.error)
    }

    @Test
    fun `reintentar limpia el error de antes`() = runTest(mainDispatcher.dispatcher) {
        repository.historial = AppResult.Failure(AppError.Network)
        val vm = viewModel()
        vm.loadHistory("p-1")
        advanceUntilIdle()
        assertNotNull(vm.historyState.value.error)

        repository.historial = AppResult.Success(listOf(elementoDeHistorialFalso()))
        vm.loadHistory("p-1")
        advanceUntilIdle()

        assertNull("Un error viejo encima de datos buenos confunde mas que no mostrar nada", vm.historyState.value.error)
        assertEquals(1, vm.historyState.value.items.size)
    }

    @Test
    fun `el detalle trae la consulta con su receta`() = runTest(mainDispatcher.dispatcher) {
        repository.consulta = AppResult.Success(
            consultaFalsa(medications = listOf(medicamentoFalso(name = "Doxiciclina"))),
        )
        val vm = viewModel()

        vm.loadConsultation("c-1")
        advanceUntilIdle()

        assertEquals("c-1", repository.ultimoIdDeConsulta)
        assertEquals("Traqueobronquitis", vm.detailState.value.consultation?.diagnosis)
        assertEquals(listOf("Doxiciclina"), vm.detailState.value.consultation?.medications?.map { it.name })
        assertFalse(vm.detailState.value.isLoading)
    }

    @Test
    fun `una consulta sin seguimiento abre igual`() = runTest(mainDispatcher.dispatcher) {
        // El caso normal: la mayoria de las consultas no deja seguimiento abierto.
        repository.consulta = AppResult.Success(consultaFalsa())
        repository.seguimientoDeConsulta = AppResult.Success(null)
        val vm = viewModel()

        vm.loadConsultation("c-1")
        advanceUntilIdle()

        assertNotNull(vm.detailState.value.consultation)
        assertNull(vm.detailState.value.followup)
        assertNull(vm.detailState.value.error)
    }

    @Test
    fun `si la consulta tiene seguimiento, viene con ella`() = runTest(mainDispatcher.dispatcher) {
        repository.consulta = AppResult.Success(consultaFalsa())
        repository.seguimientoDeConsulta = AppResult.Success(seguimientoFalso(nextReviewOn = "2026-12-01"))
        val vm = viewModel()

        vm.loadConsultation("c-1")
        advanceUntilIdle()

        assertEquals("2026-12-01", vm.detailState.value.followup?.nextReviewOn)
    }

    @Test
    fun `si falla el seguimiento, la consulta se muestra de todos modos`() = runTest(mainDispatcher.dispatcher) {
        // El seguimiento es un extra: perderlo no vale esconder el diagnostico y la receta, que es
        // lo que el dueno abrio a ver.
        repository.consulta = AppResult.Success(consultaFalsa())
        repository.seguimientoDeConsulta = AppResult.Failure(AppError.Server(500))
        val vm = viewModel()

        vm.loadConsultation("c-1")
        advanceUntilIdle()

        assertNotNull(vm.detailState.value.consultation)
        assertNull(vm.detailState.value.followup)
        assertNull("Un extra que falla no debe pintarse como si fallara la consulta", vm.detailState.value.error)
    }

    @Test
    fun `si falla la consulta no pregunta por el seguimiento`() = runTest(mainDispatcher.dispatcher) {
        repository.consulta = AppResult.Failure(AppError.Server(404))
        val vm = viewModel()

        vm.loadConsultation("c-1")
        advanceUntilIdle()

        assertFalse("Sin consulta no hay seguimiento que pedir", repository.seLlamo("getFollowupByConsultation"))
        assertEquals("Error del servidor (404).", vm.detailState.value.error)
        assertNull(vm.detailState.value.consultation)
    }

    @Test
    fun `el historial y el detalle no se pisan`() = runTest(mainDispatcher.dispatcher) {
        // Son dos estados separados a proposito: abrir un detalle no puede vaciar la lista de
        // atras, porque al cerrarlo el dueno vuelve a ella.
        repository.historial = AppResult.Success(listOf(elementoDeHistorialFalso()))
        repository.consulta = AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.loadHistory("p-1")
        advanceUntilIdle()
        vm.loadConsultation("c-1")
        advanceUntilIdle()

        assertEquals(1, vm.historyState.value.items.size)
        assertNull(vm.historyState.value.error)
        assertNotNull(vm.detailState.value.error)
    }
}
