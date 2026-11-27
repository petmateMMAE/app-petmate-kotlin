package mx.petmate.app.ui.home

import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.usecase.ReviewDateClass
import mx.petmate.app.ui.pets.NextEvent
import mx.petmate.app.ui.pets.PetSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Elección del evento más próximo y su color (spec 010, T-010, C-06).
 *
 * «Hoy» se inyecta: ninguna prueba depende del día en que se corra. La parte
 * visual va en `androidTest` (`NextHealthCardUiTest`), porque Compose necesita
 * dispositivo.
 */
class NextHealthCardTest {

    private val today = "2026-11-27"

    private fun pet(id: String, name: String) =
        Pet(id, name, PetSpecies.Dog, "Criollo", "2022-03-01", PetSex.Female)

    private fun summary(id: String, name: String, date: String?, isVaccine: Boolean = true) =
        PetSummary(
            pet = pet(id, name),
            next = date?.let { NextEvent(isVaccine, if (isVaccine) "Rabia" else "Pipeta", it) },
            activeAllergies = 0,
        )

    // ---- Elección entre mascotas ----

    @Test
    fun `elige el evento con la fecha mas cercana`() {
        val pets = listOf(
            summary("p1", "Nube", "2026-12-20"),
            summary("p2", "Rocco", "2026-11-29"),
            summary("p3", "Luna", "2027-01-10"),
        )

        val nearest = nearestHealthEvent(pets, today)

        assertEquals("Rocco", nearest?.pet?.name)
        assertEquals(2, nearest?.daysAway)
    }

    @Test
    fun `una mascota sin eventos no compite`() {
        val pets = listOf(summary("p1", "Nube", null), summary("p2", "Rocco", "2026-12-01"))

        assertEquals("Rocco", nearestHealthEvent(pets, today)?.pet?.name)
    }

    @Test
    fun `sin mascotas no hay evento`() {
        assertNull(nearestHealthEvent(emptyList(), today))
    }

    @Test
    fun `con mascotas pero sin eventos no hay evento`() {
        val pets = listOf(summary("p1", "Nube", null), summary("p2", "Rocco", null))

        assertNull(nearestHealthEvent(pets, today))
    }

    @Test
    fun `a igual fecha gana el nombre menor y el orden no cambia`() {
        val pets = listOf(
            summary("p1", "Rocco", "2026-11-29"),
            summary("p2", "Luna", "2026-11-29"),
        )

        val first = nearestHealthEvent(pets, today)?.pet?.name
        val second = nearestHealthEvent(pets.reversed(), today)?.pet?.name

        assertEquals("Luna", first)
        assertEquals("el orden de la lista no debe cambiar el resultado", first, second)
    }

    @Test
    fun `una fecha ilegible se ignora en lugar de tronar`() {
        val pets = listOf(summary("p1", "Nube", "manana"), summary("p2", "Rocco", "2026-12-01"))

        assertEquals("Rocco", nearestHealthEvent(pets, today)?.pet?.name)
    }

    @Test
    fun `tambien considera las desparasitaciones`() {
        val pets = listOf(
            summary("p1", "Nube", "2026-12-20", isVaccine = true),
            summary("p2", "Rocco", "2026-11-28", isVaccine = false),
        )

        val nearest = nearestHealthEvent(pets, today)

        assertEquals("Rocco", nearest?.pet?.name)
        assertEquals(false, nearest?.event?.isVaccine)
    }

    // ---- Días y color, con los umbrales compartidos ----

    @Test
    fun `hoy son cero dias`() {
        assertEquals(0, nearestHealthEvent(listOf(summary("p1", "Nube", today)), today)?.daysAway)
    }

    @Test
    fun `una fecha pasada da dias negativos y nivel atrasado`() {
        val nearest = nearestHealthEvent(listOf(summary("p1", "Nube", "2026-11-20")), today)

        assertEquals(-7, nearest?.daysAway)
        assertEquals(ReviewDateClass.Overdue, nearest?.level)
    }

    @Test
    fun `los umbrales son los compartidos, rojo hasta tres y amarillo hasta catorce`() {
        fun levelAt(date: String) = nearestHealthEvent(listOf(summary("p1", "Nube", date)), today)?.level

        assertEquals(ReviewDateClass.Urgent, levelAt("2026-11-30")) // 3 dias
        assertEquals(ReviewDateClass.Soon, levelAt("2026-12-01")) // 4 dias
        assertEquals(ReviewDateClass.Soon, levelAt("2026-12-11")) // 14 dias
        assertEquals(ReviewDateClass.Normal, levelAt("2026-12-12")) // 15 dias
    }

    @Test
    fun `una fecha atrasada gana sobre una proxima`() {
        val pets = listOf(
            summary("p1", "Nube", "2026-11-20"),
            summary("p2", "Rocco", "2026-11-28"),
        )

        assertEquals("lo atrasado es lo mas urgente", "Nube", nearestHealthEvent(pets, today)?.pet?.name)
    }

    @Test
    fun `cruza bien el cambio de ano`() {
        val nearest = nearestHealthEvent(listOf(summary("p1", "Nube", "2027-01-02")), "2026-12-31")

        assertEquals(2, nearest?.daysAway)
    }
}
