package mx.petmate.app.data.sync

import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.ValidationReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reintentos y errores permanentes de la cola (spec 005, T-005, C-05).
 *
 * Lo que fija esta prueba es la frontera: **lo que el servidor rechazó por su contenido no se
 * reintenta**. Un 400 reenviado cinco veces sigue siendo un 400, y mientras tanto tapa la cola.
 */
class SyncRetryPolicyTest {

    private val policy = SyncRetryPolicy()

    @Test
    fun `5xx, timeout y corte de red se reintentan`() {
        listOf(AppError.Server(500), AppError.Server(503), AppError.Timeout, AppError.Network)
            .forEach { error ->
                assertFalse("$error falló por el camino, no por el dato", policy.esPermanente(error))
                assertTrue(
                    "$error debería reintentarse en el primer intento",
                    policy.decide(error, attempts = 1) is SyncRetryDecision.Retry,
                )
            }
    }

    @Test
    fun `un 4xx se marca como error permanente y no se reintenta`() {
        // C-05 con los tres que de verdad llegan: dato mal formado, sin permiso, y conflicto.
        listOf(AppError.Server(400), AppError.Server(403), AppError.Server(409))
            .forEach { error ->
                assertTrue("$error es del dato: reintentar da lo mismo", policy.esPermanente(error))
                val decision = policy.decide(error, attempts = 1)
                assertTrue("$error no debería reintentarse", decision is SyncRetryDecision.Permanent)
                assertTrue(
                    "El motivo tiene que quedar visible para que el usuario corrija o descarte",
                    (decision as SyncRetryDecision.Permanent).reason.isNotBlank(),
                )
            }
    }

    @Test
    fun `un dato que el servidor no valida tambien es permanente`() {
        val error = AppError.Validation("email", ValidationReason.INVALID_EMAIL)

        val decision = policy.decide(error, attempts = 1)

        assertTrue(decision is SyncRetryDecision.Permanent)
        assertTrue(
            "El motivo debe nombrar el campo para que la pantalla sepa dónde mandar al usuario",
            (decision as SyncRetryDecision.Permanent).reason.contains("email"),
        )
    }

    @Test
    fun `al agotar los intentos de un error transitorio se rinde, pero aparte de un permanente`() {
        // La diferencia importa: aquí el dato probablemente está bien y falló la red, así que el
        // reintento manual tiene sentido. En un permanente no lo tiene hasta que alguien corrija.
        val decision = policy.decide(AppError.Network, attempts = SyncRetryPolicy.DEFAULT_MAX_ATTEMPTS)

        assertTrue(decision is SyncRetryDecision.GaveUp)
    }

    @Test
    fun `la espera crece al doble y se topa`() {
        assertEquals(1_000L, policy.esperaPara(1))
        assertEquals(2_000L, policy.esperaPara(2))
        assertEquals(4_000L, policy.esperaPara(3))
        assertEquals(
            "Sin tope, un intento alto dejaría al usuario esperando horas",
            SyncRetryPolicy.DEFAULT_MAX_DELAY_MILLIS,
            policy.esperaPara(99),
        )
    }
}
