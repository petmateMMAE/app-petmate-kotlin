package mx.petmate.app.ui.components.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

/** Iniciales de la mascota cuando no hay foto (spec 003, T-007, R-07). */
class PhotoFallbackInitialsTest {

    @Test
    fun `un nombre da una inicial en mayuscula`() {
        assertEquals("L", initialsOf("luna"))
    }

    @Test
    fun `dos o mas palabras dan las dos primeras iniciales`() {
        assertEquals("DG", initialsOf("Don Gato"))
        assertEquals("SM", initialsOf("señor  misifú  tercero"))
    }

    @Test
    fun `espacios de sobra no rompen las iniciales`() {
        assertEquals("R", initialsOf("   Rocky "))
    }

    @Test
    fun `un nombre vacio no truena`() {
        assertEquals("", initialsOf("  "))
    }
}
