package mx.petmate.app.domain.health

import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.ConsultationSummary
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.HistoryItem
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Linea de tiempo unica, orden estable y filtros (spec 008, T-006, C-03, C-09 y C-10). */
class HealthTimelineTest {

    private val owner = RecordOrigin.Owner

    private val carnet = Carnet(
        vaccines = listOf(
            Vaccine("v1", "Rabia", "2026-03-10", null, null, "2027-03-10", owner),
            Vaccine("v2", "Moquillo", "2026-01-05", null, null, null, owner),
        ),
        dewormings = listOf(
            Deworming("d1", "Pipeta", "2026-02-20", DewormingType.External, null, null, owner),
        ),
        weights = listOf(
            WeightRecord("w1", "2026-03-10", 12.5, owner),
        ),
        allergies = listOf(
            Allergy("a1", "Polen", AllergyKind.Allergy, null, true, owner),
            Allergy("c1", "Displasia", AllergyKind.Condition, null, true, owner),
        ),
    )

    private val consultations = listOf(
        ConsultationSummary("k1", "2026-02-28", "Veterinaria Centro", "Tos", "Traqueobronquitis"),
    )

    // ---- C-03: una sola linea con las seis secciones ----

    @Test
    fun `une las seis secciones en una sola lista`() {
        val events = HealthTimeline.of(carnet, consultations)

        assertEquals(7, events.size)
        assertEquals(HealthEventType.entries.toSet(), events.map(HealthEvent::type).toSet())
    }

    @Test
    fun `ordena lo mas reciente primero y deja sin fecha al principio`() {
        val events = HealthTimeline.of(carnet, consultations)

        assertEquals(
            listOf("a1", "c1", "v1", "w1", "k1", "d1", "v2"),
            events.map(HealthEvent::id),
        )
    }

    @Test
    fun `a igual fecha ordena por id y el orden no cambia entre dos lecturas`() {
        val first = HealthTimeline.of(carnet, consultations).map(HealthEvent::id)
        val second = HealthTimeline.of(carnet, consultations).map(HealthEvent::id)

        assertEquals(first, second)
        // v1 y w1 son del mismo dia: gana el id menor.
        assertTrue(first.indexOf("v1") < first.indexOf("w1"))
    }

    @Test
    fun `un carnet vacio da una lista vacia`() {
        assertTrue(HealthTimeline.of(Carnet()).isEmpty())
    }

    // ---- C-09 y C-10: filtros por tipo y por rango ----

    @Test
    fun `el filtro por tipo deja solo ese tipo`() {
        val events = HealthTimeline.of(carnet, consultations, HealthFilter(types = setOf(HealthEventType.Vaccine)))

        assertEquals(listOf("a1", "v1", "v2"), events.map(HealthEvent::id))
    }

    @Test
    fun `el filtro por rango es inclusivo en los dos extremos`() {
        val events = HealthTimeline.of(
            carnet,
            consultations,
            HealthFilter(from = "2026-02-20", to = "2026-02-28"),
        )

        // a1 y c1 no tienen fecha: el rango no los descarta.
        assertEquals(listOf("a1", "c1", "k1", "d1"), events.map(HealthEvent::id))
    }

    @Test
    fun `el rango sin limite superior toma todo lo posterior`() {
        val events = HealthTimeline.of(carnet, consultations, HealthFilter(from = "2026-03-01"))

        assertEquals(listOf("a1", "c1", "v1", "w1"), events.map(HealthEvent::id))
    }

    @Test
    fun `la alergia nunca se oculta aunque el filtro la excluya`() {
        val events = HealthTimeline.of(
            carnet,
            consultations,
            HealthFilter(types = setOf(HealthEventType.Weight), from = "2026-03-01", to = "2026-03-31"),
        )

        assertTrue("la alergia debe seguir ahi (R-06)", events.any { it.id == "a1" })
        assertTrue("la condicion si se puede filtrar", events.none { it.id == "c1" })
    }

    // ---- Historial unificado del backend ----

    @Test
    fun `tambien ordena el historial unificado y descarta los tipos desconocidos`() {
        val history = listOf(
            HistoryItem("Vaccine", "v1", "2026-01-05", "Moquillo", owner),
            HistoryItem("Consultation", "k1", "2026-04-01", "Tos", owner),
            HistoryItem("Otro", "x1", "2026-05-01", "Desconocido", owner),
        )

        assertEquals(listOf("k1", "v1"), HealthTimeline.of(history).map(HealthEvent::id))
    }
}
