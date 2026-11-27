package mx.petmate.app.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mx.petmate.app.core.coroutines.DispatcherProvider
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Infraestructura compartida de pruebas (001/T-023).
 *
 * Antes de esto, trece archivos de prueba repetian el mismo bloque:
 *
 * ```
 * private val dispatcher = UnconfinedTestDispatcher()
 * private val dispatchers = object : DispatcherProvider {
 *     override val io = dispatcher
 *     override val default = dispatcher
 *     override val main = dispatcher
 * }
 * ```
 *
 * Copiado trece veces, la decimocuarta se copia tambien, y el dia que haya que cambiar algo hay
 * que cambiarlo en catorce lugares.
 */

/**
 * Sustituye `Dispatchers.Main` por un dispatcher de prueba mientras corre la prueba, y lo deja
 * como estaba al terminar.
 *
 * Hace falta para cualquier cosa que use `viewModelScope`: fuera de Android no existe un
 * `Dispatchers.Main`, y sin esto la prueba truena con `IllegalStateException`. Que se restaure al
 * final importa igual: si no, una prueba le deja el dispatcher puesto a la siguiente y los fallos
 * empiezan a depender del orden.
 *
 * ```
 * @get:Rule val mainDispatcher = MainDispatcherRule()
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/**
 * Un [DispatcherProvider] que manda todo al mismo dispatcher de prueba, para que la prueba
 * controle el tiempo en vez de depender de hilos reales.
 */
fun testDispatcherProvider(dispatcher: CoroutineDispatcher): DispatcherProvider =
    object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

/** Atajo para el caso normal: un [UnconfinedTestDispatcher] en las tres propiedades. */
@OptIn(ExperimentalCoroutinesApi::class)
fun testDispatcherProvider(): DispatcherProvider = testDispatcherProvider(UnconfinedTestDispatcher())
