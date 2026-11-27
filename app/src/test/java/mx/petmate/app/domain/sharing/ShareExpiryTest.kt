package mx.petmate.app.domain.sharing

import mx.petmate.app.domain.model.AccessGrant
import mx.petmate.app.domain.model.ShareDuration
import mx.petmate.app.domain.model.SharedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cuenta regresiva de la comparticion, con reloj fijo (spec 009, T-005, C-06 y C-07).
 *
 * Todas las pruebas pasan el "ahora" a mano: la marca de expiracion la manda el
 * servidor y la app solo mide la distancia.
 */
class ShareExpiryTest {

    /** 2026-11-26 10:00:00 UTC. */
    private val now = instant("2026-11-26T10:00:00Z")

    private fun instant(iso: String): Long {
        // Se arma con la misma funcion publica para no duplicar el parseo:
        // una marca a 0 ms de distancia deja el estado justo en vencido.
        val state = ShareExpiry.remaining(iso, 0L)
        return (state as ShareExpiry.State.Live).millis
    }

    private fun grant(expiresAt: String) = AccessGrant(
        id = "g1",
        businessId = null,
        data = listOf(SharedData.Vaccines),
        expiresAt = expiresAt,
    )

    // ---- C-06: la cuenta regresiva usa la marca del servidor ----

    @Test
    fun `con media hora por delante quedan treinta minutos`() {
        val state = ShareExpiry.remaining("2026-11-26T10:30:00Z", now)

        assertTrue(state is ShareExpiry.State.Live)
        assertEquals(30, (state as ShareExpiry.State.Live).minutes)
        assertEquals(0, state.seconds)
    }

    @Test
    fun `los minutos y segundos se parten bien`() {
        val state = ShareExpiry.remaining("2026-11-26T10:02:05Z", now) as ShareExpiry.State.Live

        assertEquals(2, state.minutes)
        assertEquals(5, state.seconds)
        assertEquals(125, state.totalSeconds)
    }

    @Test
    fun `un dia por delante son veinticuatro horas`() {
        val state = ShareExpiry.remaining("2026-11-27T10:00:00Z", now) as ShareExpiry.State.Live

        assertEquals(24, state.hours)
    }

    @Test
    fun `el reloj del telefono no decide, mover el ahora cambia lo que queda`() {
        val expiry = "2026-11-26T10:30:00Z"

        val early = ShareExpiry.remaining(expiry, now) as ShareExpiry.State.Live
        val late = ShareExpiry.remaining(expiry, now + 29 * 60 * 1000L) as ShareExpiry.State.Live

        assertEquals(30, early.minutes)
        assertEquals(1, late.minutes)
    }

    // ---- C-07: al llegar a cero el QR deja de mostrarse ----

    @Test
    fun `al llegar exactamente a cero ya esta vencida`() {
        assertEquals(ShareExpiry.State.Expired, ShareExpiry.remaining("2026-11-26T10:00:00Z", now))
    }

    @Test
    fun `una marca ya pasada esta vencida`() {
        assertEquals(ShareExpiry.State.Expired, ShareExpiry.remaining("2026-11-26T09:59:59Z", now))
    }

    @Test
    fun `el QR se muestra mientras quede tiempo y no cuando vence`() {
        assertTrue(ShareExpiry.isVisible("2026-11-26T10:00:01Z", now))
        assertFalse(ShareExpiry.isVisible("2026-11-26T10:00:00Z", now))
        assertFalse(ShareExpiry.isVisible("2026-11-26T09:00:00Z", now))
    }

    // ---- Marcas que no se pueden leer: no se arriesga ----

    @Test
    fun `una marca ilegible se trata como vencida y no se muestra`() {
        listOf(null, "", "   ", "manana", "2026-13-40T99:99:99Z", "2026-11-26").forEach {
            assertEquals("marca: $it", ShareExpiry.State.Unknown, ShareExpiry.remaining(it, now))
            assertFalse("marca: $it", ShareExpiry.isVisible(it, now))
        }
    }

    // ---- Formatos que manda el backend ----

    @Test
    fun `acepta fracciones de segundo`() {
        val state = ShareExpiry.remaining("2026-11-26T10:30:00.123Z", now)

        assertTrue(state is ShareExpiry.State.Live)
    }

    @Test
    fun `acepta desplazamiento de zona y lo convierte bien`() {
        // 05:30 en UTC-5 es 10:30 UTC: media hora por delante.
        val state = ShareExpiry.remaining("2026-11-26T05:30:00-05:00", now) as ShareExpiry.State.Live

        assertEquals(30, state.minutes)
    }

    @Test
    fun `el mismo instante en dos zonas da lo mismo`() {
        val utc = ShareExpiry.remaining("2026-11-26T12:00:00Z", now) as ShareExpiry.State.Live
        val offset = ShareExpiry.remaining("2026-11-26T14:00:00+02:00", now) as ShareExpiry.State.Live

        assertEquals(utc.millis, offset.millis)
    }

    @Test
    fun `cruza bien el cambio de ano`() {
        val newYear = ShareExpiry.remaining("2027-01-01T00:00:00Z", instant("2026-12-31T23:59:00Z"))

        assertEquals(60, (newYear as ShareExpiry.State.Live).totalSeconds)
    }

    // ---- La autorizacion emitida ----

    @Test
    fun `tambien mide una autorizacion ya emitida`() {
        val state = ShareExpiry.remaining(grant("2026-11-26T10:30:00Z"), now)

        assertEquals(30, (state as ShareExpiry.State.Live).minutes)
    }

    // ---- Las tres duraciones de la maqueta ----

    @Test
    fun `ofrece las tres duraciones de menor a mayor`() {
        assertEquals(
            listOf(ShareDuration.ThirtyMinutes, ShareDuration.OneDay, ShareDuration.SevenDays),
            ShareExpiry.durations,
        )
    }
}
