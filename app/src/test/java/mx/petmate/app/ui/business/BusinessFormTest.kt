package mx.petmate.app.ui.business

import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas del formulario de afiliacion y textos del modo negocio (pm-107, pm-109). */
class BusinessFormTest {

    private val valid = BusinessForm(kind = BusinessKind.Store, name = "Tienda Tula", phone = "7711234567", address = "Centro, Tula")

    @Test
    fun `un formulario completo no tiene errores`() = assertTrue(valid.validate().isEmpty())

    @Test
    fun `tipo, nombre, telefono y direccion son obligatorios`() {
        val errors = BusinessForm().validate()
        assertEquals(setOf("kind", "name", "phone", "address"), errors.keys)
    }

    @Test
    fun `la cedula solo se exige a veterinarias`() {
        assertTrue("license" in valid.copy(kind = BusinessKind.Veterinary).validate())
        assertFalse("license" in valid.copy(kind = BusinessKind.Grooming).validate())
    }

    @Test
    fun `telefonos de 10 digitos con o sin +52`() {
        assertTrue(isValidPhone("7711234567"))
        assertTrue(isValidPhone("+52 771 123 4567"))
        assertFalse(isValidPhone("12345"))
        assertTrue("whatsApp" in valid.copy(whatsApp = "123").validate())
        assertFalse("whatsApp" in valid.copy(whatsApp = "").validate())
    }

    @Test
    fun `el cierre debe ser despues de la apertura y en formato 24 h`() {
        assertTrue("hours" in valid.copy(open = "18:00", close = "09:00").validate())
        assertTrue("hours" in valid.copy(open = "9", close = "18:00").validate())
        assertTrue("hours" in valid.copy(close = "24:00").validate())
        assertFalse("hours" in valid.copy(days = emptySet(), open = "x").validate())
    }

    @Test
    fun `al editar no se piden tipo ni direccion`() {
        assertTrue(valid.copy(kind = null, address = "").validate(editing = true).isEmpty())
    }

    @Test
    fun `el horario se arma con los dias marcados`() {
        val schedule = valid.copy(days = setOf("monday", "saturday"), open = "10:00", close = "14:00").schedule()
        assertEquals(mapOf("monday" to listOf(TimeRange("10:00", "14:00")), "saturday" to listOf(TimeRange("10:00", "14:00"))), schedule)
    }

    @Test
    fun `el resumen agrupa dias seguidos con el mismo horario`() {
        val week = listOf("monday", "tuesday", "wednesday", "thursday", "friday").associateWith { listOf(TimeRange("09:00", "18:00")) }
        assertEquals("Lun a Vie 09:00–18:00 · Sáb 10:00–14:00", scheduleSummary(week + ("saturday" to listOf(TimeRange("10:00", "14:00")))))
        assertEquals("Sin horario", scheduleSummary(emptyMap()))
    }

    @Test
    fun `precios con y sin valor`() {
        assertEquals(300.0, parsePrice("$300")!!, 0.0)
        assertEquals(1250.5, parsePrice("1,250.50")!!, 0.0)
        assertNull(parsePrice("gratis"))
        assertEquals("Pregunta el precio", priceText(null))
        assertEquals("$1,250.50", priceText(1250.5))
    }

    @Test
    fun `cada tipo sugiere servicios y el primero va marcado`() {
        BusinessKind.entries.forEach { kind ->
            val services = suggestedServices(kind)
            assertTrue(services.isNotEmpty())
            assertTrue(services.first().selected)
        }
    }
}
