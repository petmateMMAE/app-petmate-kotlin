package mx.petmate.app.domain.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Catalogo de permisos (spec 003, T-001, A-01). */
class PermissionCatalogTest {

    @Test
    fun `cada permiso tiene exactamente una regla`() {
        PermissionType.entries.forEach { type ->
            assertEquals("$type", 1, PermissionCatalog.rules.count { it.type == type })
        }
    }

    @Test
    fun `cada permiso tiene una alternativa distinta`() {
        assertEquals(PermissionType.entries.size, PermissionCatalog.rules.map { it.fallback }.toSet().size)
    }

    @Test
    fun `las alternativas son las de la spec`() {
        fun fallback(type: PermissionType) = PermissionCatalog.ruleFor(type).fallback
        assertEquals(PermissionFallback.DefaultCenterAndList, fallback(PermissionType.Location))
        assertEquals(PermissionFallback.ManualCode, fallback(PermissionType.Camera))
        assertEquals(PermissionFallback.Initials, fallback(PermissionType.Photos))
        assertEquals(PermissionFallback.InAppNotice, fallback(PermissionType.Notifications))
    }

    @Test
    fun `las fotos no pasan por el dialogo del sistema y lo demas si`() {
        assertFalse(PermissionCatalog.ruleFor(PermissionType.Photos).needsSystemDialog)
        listOf(PermissionType.Location, PermissionType.Camera, PermissionType.Notifications).forEach {
            assertTrue("$it", PermissionCatalog.ruleFor(it).needsSystemDialog)
        }
    }

    @Test
    fun `las notificaciones se piden solo al activarlas`() {
        val rule = PermissionCatalog.ruleFor(PermissionType.Notifications)
        assertEquals(PermissionMoment.EnableNotifications, rule.moment)
    }
}
