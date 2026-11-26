package mx.petmate.app.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tabla de politica por entidad (spec 005, A-01, R-07, R-08, R-13). */
class SyncPolicyTest {

    private val clinical = listOf(
        SyncEntityType.Vaccine,
        SyncEntityType.Deworming,
        SyncEntityType.Weight,
        SyncEntityType.Allergy,
        SyncEntityType.Condition,
    )

    @Test
    fun `toda entidad tiene reglas declaradas`() {
        SyncEntityType.entries.forEach { SyncPolicy.rulesFor(it) }
    }

    @Test
    fun `la mascota se crea, edita y borra sin conexion y gana el cambio mas reciente`() {
        val pet = SyncPolicy.rulesFor(SyncEntityType.Pet)
        assertTrue(pet.canCreate && pet.canUpdate && pet.canDelete)
        assertEquals(ConflictRule.LatestWins, pet.conflict)
        assertNull(pet.parentType)
    }

    @Test
    fun `en datos clinicos gana el servidor y dependen de la mascota`() {
        clinical.forEach {
            val rules = SyncPolicy.rulesFor(it)
            assertEquals("$it", ConflictRule.ServerWins, rules.conflict)
            assertEquals("$it", SyncEntityType.Pet, rules.parentType)
            assertTrue("$it", rules.canCreate)
        }
    }

    @Test
    fun `vacuna y desparasitacion se borran pero no se editan`() {
        listOf(SyncEntityType.Vaccine, SyncEntityType.Deworming).forEach {
            assertTrue("$it", SyncPolicy.allows(it, SyncAction.Delete))
            assertFalse("$it", SyncPolicy.allows(it, SyncAction.Update))
        }
    }

    @Test
    fun `el peso solo se registra`() {
        assertTrue(SyncPolicy.allows(SyncEntityType.Weight, SyncAction.Create))
        assertFalse(SyncPolicy.allows(SyncEntityType.Weight, SyncAction.Update))
        assertFalse(SyncPolicy.allows(SyncEntityType.Weight, SyncAction.Delete))
    }

    @Test
    fun `alergia y condicion se editan pero no se borran`() {
        listOf(SyncEntityType.Allergy, SyncEntityType.Condition).forEach {
            assertTrue("$it", SyncPolicy.allows(it, SyncAction.Update))
            assertFalse("$it", SyncPolicy.allows(it, SyncAction.Delete))
        }
    }

    @Test
    fun `toOperation copia el padre y serializa segun la accion`() {
        val vaccine = object : SyncableEntity {
            override val ref = SyncEntityRef(SyncEntityType.Vaccine, "v-1")
            override val parent = SyncEntityRef(SyncEntityType.Pet, "p-1")
            override val updatedAtMillis = 10L
            override fun serialize(action: SyncAction) = "{\"action\":\"$action\"}"
        }

        val operation = vaccine.toOperation(SyncAction.Create, nowMillis = 42L)

        assertEquals(vaccine.ref, operation.target)
        assertEquals(vaccine.parent, operation.dependsOn)
        assertEquals("{\"action\":\"Create\"}", operation.payload)
        assertEquals(42L, operation.createdAtMillis)
    }
}
