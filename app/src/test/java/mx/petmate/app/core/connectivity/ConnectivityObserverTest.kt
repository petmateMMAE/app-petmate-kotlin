package mx.petmate.app.core.connectivity

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Estado de la red con un emisor falso (spec 001, T-035). */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityObserverTest {

    @Test
    fun `empieza con red por omision`() = runTest {
        assertTrue(FakeConnectivityObserver().isOnline.first())
    }

    @Test
    fun `puede empezar sin red`() = runTest {
        assertFalse(FakeConnectivityObserver(initiallyOnline = false).isOnline.first())
    }

    @Test
    fun `avisa cuando se cae la red y cuando vuelve`() = runTest {
        val observer = FakeConnectivityObserver()

        observer.isOnline.test {
            assertTrue(awaitItem())

            observer.set(false)
            assertFalse(awaitItem())

            observer.set(true)
            assertTrue(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quien se suscribe tarde recibe el estado actual`() = runTest {
        val observer = FakeConnectivityObserver()
        observer.set(false)

        assertFalse("debe ver el ultimo estado, no el inicial", observer.isOnline.first())
    }

    @Test
    fun `dos suscriptores ven lo mismo`() = runTest {
        val observer = FakeConnectivityObserver()
        observer.set(false)

        assertEquals(observer.isOnline.first(), observer.isOnline.first())
    }
}
