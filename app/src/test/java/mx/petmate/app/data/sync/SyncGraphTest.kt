package mx.petmate.app.data.sync

import mx.petmate.app.domain.sync.SyncEntityType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Niveles del grafo y deteccion de ciclos (spec 005, T-003; plan RT-03). */
class SyncGraphTest {

    @Test
    fun `la mascota es nivel 0 y todo el carnet nivel 1`() {
        val graph = SyncGraph.fromPolicy()

        assertEquals(0, graph.level(SyncEntityType.Pet))
        listOf(
            SyncEntityType.Vaccine,
            SyncEntityType.Deworming,
            SyncEntityType.Weight,
            SyncEntityType.Allergy,
            SyncEntityType.Condition,
        ).forEach { assertEquals("$it", 1, graph.level(it)) }
    }

    @Test(expected = IllegalStateException::class)
    fun `un ciclo falla al construir el grafo`() {
        SyncGraph(
            mapOf(
                SyncEntityType.Pet to SyncEntityType.Vaccine,
                SyncEntityType.Vaccine to SyncEntityType.Pet,
            ),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `una entidad que depende de si misma tambien es un ciclo`() {
        SyncGraph(mapOf(SyncEntityType.Pet to SyncEntityType.Pet))
    }
}
