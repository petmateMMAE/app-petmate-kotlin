package mx.petmate.app.domain.sharing

import mx.petmate.app.domain.health.usecase.GetCriticalAllergiesUseCase
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.SharedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Secciones elegibles y alergias obligatorias (spec 009, T-002, C-03). */
class ShareSectionsTest {

    private val owner = RecordOrigin.Owner

    private fun allergy(id: String, kind: AllergyKind = AllergyKind.Allergy, active: Boolean = true) =
        Allergy(id = id, name = "Polen $id", kind = kind, notes = null, active = active, origin = owner)

    private val carnet = Carnet(
        allergies = listOf(
            allergy("a1"),
            allergy("a2"),
            allergy("c1", kind = AllergyKind.Condition),
            allergy("a3", active = false),
        ),
    )

    private val critical = GetCriticalAllergiesUseCase()(carnet)

    // ---- Las secciones elegibles son cinco, y las alergias no estan entre ellas ----

    @Test
    fun `las alergias no son una seccion que se pueda elegir`() {
        val elegibles = ShareSection.Selectable.map(ShareSection::data)

        assertTrue(SharedData.Vaccines in elegibles)
        assertTrue(SharedData.Dewormings in elegibles)
        assertTrue(SharedData.Weights in elegibles)
        assertTrue(SharedData.Consultations in elegibles)
        assertEquals("no debe haber mas opciones que esas cuatro", 4, elegibles.size)
    }

    @Test
    fun `toda seccion elegible se puede desmarcar`() {
        ShareSection.entries.forEach { assertTrue("$it", ShareSections.isSelectable(it)) }
    }

    // ---- C-03: una seleccion sin alergias las incluye igual ----

    @Test
    fun `una seleccion vacia comparte las alergias de todos modos`() {
        val content = ShareContent.of(ShareSelection.Empty, critical)

        assertTrue("no se eligio ninguna seccion", content.toSharedData().isEmpty())
        assertEquals(listOf("a1", "a2"), content.allergies.map(Allergy::id))
        assertTrue(content.isOnlyAllergies)
    }

    @Test
    fun `una seleccion parcial no quita las alergias`() {
        val selection = ShareSelection.Empty.toggle(ShareSection.Vaccines)
        val content = ShareContent.of(selection, critical)

        assertEquals(setOf(SharedData.Vaccines), content.toSharedData())
        assertEquals(listOf("a1", "a2"), content.allergies.map(Allergy::id))
        assertFalse(content.isOnlyAllergies)
    }

    @Test
    fun `no hay forma de desmarcar las alergias porque no estan en la seleccion`() {
        // Marcar y desmarcar todo lo elegible no toca las alergias.
        var selection = ShareSelection.All
        ShareSection.entries.forEach { selection = selection.toggle(it) }

        val content = ShareContent.of(selection, critical)

        assertTrue("quedo todo desmarcado", content.toSharedData().isEmpty())
        assertEquals("las alergias siguen", 2, content.allergies.size)
    }

    @Test
    fun `solo van las alergias activas y no las condiciones`() {
        val content = ShareContent.of(ShareSelection.All, critical)

        assertTrue("c1 es condicion", content.allergies.none { it.id == "c1" })
        assertTrue("a3 esta superada", content.allergies.none { it.id == "a3" })
    }

    @Test
    fun `un carnet sin alergias comparte la seleccion y una lista vacia`() {
        val content = ShareContent.of(ShareSelection.All, GetCriticalAllergiesUseCase()(Carnet()))

        assertEquals(4, content.toSharedData().size)
        assertTrue(content.allergies.isEmpty())
    }

    // ---- Alternar secciones ----

    @Test
    fun `alternar una seccion la marca y la desmarca`() {
        val once = ShareSelection.Empty.toggle(ShareSection.Weights)
        val twice = once.toggle(ShareSection.Weights)

        assertEquals(setOf(ShareSection.Weights), once.sections)
        assertTrue(twice.isEmpty)
    }

    @Test
    fun `todo marcado manda las cuatro secciones`() {
        assertEquals(
            setOf(SharedData.Vaccines, SharedData.Dewormings, SharedData.Weights, SharedData.Consultations),
            ShareSelection.All.toSharedData(),
        )
    }

    @Test
    fun `las alergias que se adjuntan son exactamente las criticas`() {
        assertEquals(critical, ShareSections.allergiesFor(critical))
    }
}
