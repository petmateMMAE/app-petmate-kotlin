package mx.petmate.app.data.sync

import mx.petmate.app.domain.sync.ConflictRule
import mx.petmate.app.domain.sync.SyncEntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Resolución de conflictos (spec 005, T-006, C-06 y C-07).
 *
 * La reconciliación va por `updatedAt` porque el backend no devuelve versión (respuesta de Adrián
 * del 27/11/2026 a Q-02, Q-05 y Q-07).
 */
class ConflictResolverTest {

    private val resolver = ConflictResolver()

    @Test
    fun `en datos clinicos gana el servidor y se avisa, aunque el telefono sea mas reciente`() {
        // R-07: lo clínico lo registra una veterinaria. Dejar ganar al teléfono del dueño borraría
        // lo que escribió un profesional, y por eso la marca local ni se mira.
        listOf(
            SyncEntityType.Vaccine,
            SyncEntityType.Deworming,
            SyncEntityType.Weight,
            SyncEntityType.Allergy,
        ).forEach { tipo ->
            val resultado = resolver.resolve(
                type = tipo,
                local = "del telefono",
                server = "del servidor",
                localUpdatedAt = "2026-11-27T12:00:00Z",
                serverUpdatedAt = "2026-11-20T08:00:00Z",
                entityId = "x-1",
            )

            assertEquals("En $tipo debe ganar el servidor", "del servidor", resultado.winner)
            assertTrue(resultado.serverWon)
            assertNotNull("El usuario tiene que enterarse de que su cambio no quedó", resultado.notice)
            assertEquals(ConflictRule.ServerWins, resultado.notice?.rule)
        }
    }

    @Test
    fun `en la mascota gana la marca mas reciente y no se avisa si gana el usuario`() {
        val resultado = resolver.resolve(
            type = SyncEntityType.Pet,
            local = "Firulais",
            server = "Firulai",
            localUpdatedAt = "2026-11-27T12:00:00Z",
            serverUpdatedAt = "2026-11-20T08:00:00Z",
            entityId = "p-1",
        )

        assertEquals("Firulais", resultado.winner)
        assertFalse(resultado.serverWon)
        assertNull("Avisar cuando el usuario gana es ruido", resultado.notice)
    }

    @Test
    fun `el resultado es el mismo con las marcas invertidas`() {
        // C-07: la regla no depende de qué lado se pase, sino de cuál marca es mayor.
        val ganaServidor = resolver.resolve(
            type = SyncEntityType.Pet,
            local = "viejo",
            server = "nuevo",
            localUpdatedAt = "2026-11-20T08:00:00Z",
            serverUpdatedAt = "2026-11-27T12:00:00Z",
            entityId = "p-1",
        )

        assertEquals("nuevo", ganaServidor.winner)
        assertTrue(ganaServidor.serverWon)
        assertNotNull("Si el usuario pierde su cambio, se le avisa", ganaServidor.notice)
    }

    @Test
    fun `con marcas iguales gana el servidor`() {
        // `updatedAt` llega en segundos, así que dos cambios del mismo segundo empatan. Ante la
        // duda no se pierde lo que el servidor ya tenía: un reloj de teléfono puede estar mal.
        val misma = "2026-11-27T12:00:00Z"

        val resultado = resolver.resolve(
            type = SyncEntityType.Pet,
            local = "del telefono",
            server = "del servidor",
            localUpdatedAt = misma,
            serverUpdatedAt = misma,
            entityId = "p-1",
        )

        assertEquals("del servidor", resultado.winner)
        assertTrue(resultado.serverWon)
    }

    @Test
    fun `sin marca local gana el servidor, y sin marca del servidor gana lo local`() {
        assertFalse(
            "Si el teléfono no sabe cuándo se cambió, no puede alegar que es más nuevo",
            resolver.esMasReciente(localUpdatedAt = null, serverUpdatedAt = "2026-11-20T08:00:00Z"),
        )
        assertTrue(
            "Un dato que el servidor nunca marcó es nuevo para él",
            resolver.esMasReciente(localUpdatedAt = "2026-11-20T08:00:00Z", serverUpdatedAt = null),
        )
    }
}
