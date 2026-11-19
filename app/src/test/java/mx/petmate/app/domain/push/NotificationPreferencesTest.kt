package mx.petmate.app.domain.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Categorias, valores por defecto y seguridad no desactivable (spec 006, T-001, A-09, R-02). */
class NotificationPreferencesTest {

    @Test
    fun `son exactamente las cinco categorias de la spec`() {
        assertEquals(
            setOf("health", "followup", "chat", "news", "security"),
            NotificationCategory.entries.map { it.key }.toSet(),
        )
    }

    @Test
    fun `por defecto todo encendido menos novedades`() {
        val defaults = NotificationPreferences.DEFAULT

        assertFalse(defaults.isEnabled(NotificationCategory.News))
        (NotificationCategory.entries - NotificationCategory.News).forEach {
            assertTrue("$it", defaults.isEnabled(it))
        }
    }

    @Test
    fun `seguridad no se puede apagar`() {
        val prefs = NotificationPreferences.DEFAULT.with(NotificationCategory.Security, on = false)

        assertTrue(prefs.isEnabled(NotificationCategory.Security))
        assertFalse(NotificationPreferences.canBeDisabled(NotificationCategory.Security))
        assertTrue(NotificationPreferences.of(emptySet()).isEnabled(NotificationCategory.Security))
    }

    @Test
    fun `las demas se encienden y se apagan`() {
        val off = NotificationPreferences.DEFAULT.with(NotificationCategory.Chat, on = false)
        val on = off.with(NotificationCategory.News, on = true)

        assertFalse(off.isEnabled(NotificationCategory.Chat))
        assertTrue(on.isEnabled(NotificationCategory.News))
        assertTrue(NotificationPreferences.canBeDisabled(NotificationCategory.Chat))
    }

    @Test
    fun `la clave guardada se vuelve a leer`() {
        NotificationCategory.entries.forEach { assertEquals(it, NotificationCategory.fromKey(it.key)) }
        assertEquals(null, NotificationCategory.fromKey("otra"))
    }
}
