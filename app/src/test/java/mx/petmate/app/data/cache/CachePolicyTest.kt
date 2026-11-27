package mx.petmate.app.data.cache

import mx.petmate.app.core.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vigencia de la caché con el reloj adelantado a mano (spec 001, T-018, C-11).
 *
 * El punto de inyectar el reloj es este: la prueba adelanta una hora sin esperarla. Con
 * `System.currentTimeMillis()` escrito a mano dentro de la política, comprobar que un dato vence
 * exigiría dormir de verdad, y nadie escribe esa prueba.
 */
class CachePolicyTest {

    /** Reloj que la prueba mueve a voluntad. */
    private var ahora = 1_000_000L
    private val policy = CachePolicy(Clock { ahora })

    @Test
    fun `un dato recien guardado se sirve de la cache y no toca la red`() {
        val guardado = ahora

        assertTrue(policy.isFresh(guardado, CachedData.Carnet))
        assertEquals(CacheDecision.ServeCache, policy.decide(guardado, CachedData.Carnet))
    }

    @Test
    fun `adelantar el reloj cambia la decision`() {
        // C-11 tal cual: lo mismo guardado, el mismo tipo de dato, y la decisión se invierte solo
        // porque pasó el tiempo.
        val guardado = ahora
        assertEquals(CacheDecision.ServeCache, policy.decide(guardado, CachedData.Carnet))

        ahora += CachedData.Carnet.vigenciaMillis + 1

        assertEquals(CacheDecision.Refresh, policy.decide(guardado, CachedData.Carnet))
        assertEquals(
            "Ya vencido, no le queda vigencia",
            0L,
            policy.vigenciaRestante(guardado, CachedData.Carnet),
        )
    }

    @Test
    fun `el carnet vence mucho antes que la lista de mascotas`() {
        // La diferencia que importa de toda la tabla: una veterinaria puede registrar una vacuna
        // mientras el dueño mira la pantalla en el mostrador, así que el carnet se revisa seguido.
        // Las mascotas solo las cambia su dueño.
        val guardado = ahora
        ahora += CachedData.Carnet.vigenciaMillis + 1

        assertEquals(CacheDecision.Refresh, policy.decide(guardado, CachedData.Carnet))
        assertEquals(
            "La lista de mascotas sigue vigente con la misma edad",
            CacheDecision.ServeCache,
            policy.decide(guardado, CachedData.Pets),
        )
    }

    @Test
    fun `lo que nunca se guardo hay que pedirlo`() {
        assertFalse(policy.isFresh(null, CachedData.Pets))
        assertEquals(CacheDecision.Refresh, policy.decide(null, CachedData.Pets))
        assertEquals(0L, policy.vigenciaRestante(null, CachedData.Pets))
    }

    @Test
    fun `un dato guardado en el futuro se trata como vencido`() {
        // Pasa cuando el usuario mueve el reloj del teléfono. Creerle dejaría ese dato guardado
        // para siempre sin volver a pedirlo nunca; volver a pedirlo cuesta una petición.
        val guardadoEnElFuturo = ahora + CachedData.Pets.vigenciaMillis

        assertFalse(policy.isFresh(guardadoEnElFuturo, CachedData.Pets))
        assertEquals(CacheDecision.Refresh, policy.decide(guardadoEnElFuturo, CachedData.Pets))
    }

    @Test
    fun `justo en el limite ya no esta vigente`() {
        // La vigencia es exclusiva: a los exactos cinco minutos ya se pide. Da igual cuál de las
        // dos se elija, pero tiene que estar fijada, porque si no cada quien asume una.
        val guardado = ahora
        ahora += CachedData.Carnet.vigenciaMillis

        assertFalse(policy.isFresh(guardado, CachedData.Carnet))
    }

    @Test
    fun `cada tipo de dato declara su vigencia y ninguna es cero`() {
        // Si alguien agrega un tipo nuevo y olvida la duración, un 0 lo volvería "siempre vencido"
        // y pediría a la red en cada lectura, sin que nadie lo note hasta ver la factura de Azure.
        CachedData.entries.forEach { dato ->
            assertTrue(
                "$dato tiene que declarar una vigencia mayor que cero",
                dato.vigenciaMillis > 0,
            )
        }
    }
}
