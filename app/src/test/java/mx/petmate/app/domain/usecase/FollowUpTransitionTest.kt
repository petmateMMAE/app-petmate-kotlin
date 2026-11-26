package mx.petmate.app.domain.usecase

import mx.petmate.app.domain.model.FollowUpStatus
import mx.petmate.app.domain.model.FollowUpStatus.Discharged
import mx.petmate.app.domain.model.FollowUpStatus.Improving
import mx.petmate.app.domain.model.FollowUpStatus.Treating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Matriz completa de transiciones, validas e invalidas (spec 015, T-002, C-02 a C-05). */
class FollowUpTransitionTest {

    private val valid = setOf(
        Treating to Improving,
        Treating to Discharged,
        Improving to Treating,
        Improving to Discharged,
    )

    @Test
    fun `las nueve combinaciones dan lo que dice la spec`() {
        FollowUpStatus.entries.forEach { from ->
            FollowUpStatus.entries.forEach { to ->
                assertEquals("$from -> $to", (from to to) in valid, FollowUpTransition.isAllowed(from, to))
            }
        }
    }

    @Test
    fun `ningun estado pasa a si mismo`() {
        FollowUpStatus.entries.forEach { assertFalse("$it", FollowUpTransition.isAllowed(it, it)) }
    }

    @Test
    fun `dado de alta cierra el seguimiento`() {
        assertTrue(FollowUpTransition.isClosed(Discharged))
        assertTrue(FollowUpTransition.nextOf(Discharged).isEmpty())
        assertFalse(FollowUpTransition.isClosed(Treating))
        assertFalse(FollowUpTransition.isClosed(Improving))
    }

    @Test
    fun `un seguimiento nuevo arranca en tratamiento`() {
        assertEquals(Treating, FollowUpTransition.INITIAL)
    }

    @Test
    fun `el estado se lee del texto del backend`() {
        assertEquals(Treating, FollowUpStatus.fromApi("Treating"))
        assertEquals(Improving, FollowUpStatus.fromApi("improving"))
        assertEquals(Discharged, FollowUpStatus.fromApi("Discharged"))
        assertNull(FollowUpStatus.fromApi("Cancelled"))
    }
}
