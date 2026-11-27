package mx.petmate.app.ui.business

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.util.BusinessRepositoryFalso
import mx.petmate.app.util.MainDispatcherRule
import mx.petmate.app.util.mascotaAtendidaFalsa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Mascotas atendidas del panel del afiliado (spec 012). */
@OptIn(ExperimentalCoroutinesApi::class)
class AttendedViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = BusinessRepositoryFalso()

    @Test
    fun `al abrir la pantalla pide el dia, no otro periodo`() = runTest(mainDispatcher.dispatcher) {
        // El panel abre en "hoy": si pidiera la semana, el afiliado veria numeros que no
        // corresponden a lo que tiene enfrente.
        val vm = AttendedViewModel(repository)
        advanceUntilIdle()

        assertEquals("day", repository.ultimoPeriodo)
        assertEquals("day", vm.state.value.period)
    }

    @Test
    fun `mientras carga lo dice y al terminar lo apaga`() = runTest(mainDispatcher.dispatcher) {
        val vm = AttendedViewModel(repository)

        assertTrue(vm.state.value.loading)
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `las mascotas atendidas llegan a la pantalla`() = runTest(mainDispatcher.dispatcher) {
        repository.atendidas = AppResult.Success(
            listOf(mascotaAtendidaFalsa(name = "Firulais"), mascotaAtendidaFalsa(name = "Pelusa")),
        )

        val vm = AttendedViewModel(repository)
        advanceUntilIdle()

        assertEquals(listOf("Firulais", "Pelusa"), vm.state.value.items.map { it.name })
        assertNull(vm.state.value.error)
    }

    @Test
    fun `cambiar de periodo vuelve a preguntar y lo recuerda`() = runTest(mainDispatcher.dispatcher) {
        val vm = AttendedViewModel(repository)
        advanceUntilIdle()

        vm.load("week")
        advanceUntilIdle()

        assertEquals("week", repository.ultimoPeriodo)
        assertEquals("week", vm.state.value.period)
        assertEquals(2, repository.vecesQueSeLlamo("getAttended"))
    }

    @Test
    fun `un fallo deja mensaje y no deja la pantalla girando`() = runTest(mainDispatcher.dispatcher) {
        repository.atendidas = AppResult.Failure(AppError.Network)

        val vm = AttendedViewModel(repository)
        advanceUntilIdle()

        assertFalse(vm.state.value.loading)
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.items.isEmpty())
    }

    @Test
    fun `reintentar despues de un fallo limpia el error`() = runTest(mainDispatcher.dispatcher) {
        repository.atendidas = AppResult.Failure(AppError.Timeout)
        val vm = AttendedViewModel(repository)
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        repository.atendidas = AppResult.Success(listOf(mascotaAtendidaFalsa()))
        vm.load("day")
        advanceUntilIdle()

        assertNull("Un error viejo no puede quedarse encima de datos buenos", vm.state.value.error)
        assertEquals(1, vm.state.value.items.size)
    }

    @Test
    fun `el periodo se cambia en el estado antes de que llegue la respuesta`() =
        runTest(mainDispatcher.dispatcher) {
            // Para que el chip seleccionado se vea de inmediato y no despues de la red.
            val vm = AttendedViewModel(repository)
            advanceUntilIdle()

            vm.load("month")

            assertEquals("month", vm.state.value.period)
            assertTrue(vm.state.value.loading)
        }
}
