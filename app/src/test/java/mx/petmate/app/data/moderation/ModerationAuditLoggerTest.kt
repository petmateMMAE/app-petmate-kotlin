package mx.petmate.app.data.moderation

import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.moderation.ModerationContentType
import mx.petmate.app.domain.moderation.ModerationRejection
import mx.petmate.app.domain.moderation.ModerationVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El registro de moderacion no puede contener lo moderado (spec 004, T-009, R-11). */
class ModerationAuditLoggerTest {

    /** Captura todo lo que se escribiria, para poder buscarlo. */
    private class CapturingLogger : Logger {
        val lines = mutableListOf<String>()
        override fun debug(tag: String, message: String) { lines += "$tag $message" }
        override fun error(tag: String, message: String, throwable: Throwable?) { lines += "$tag $message" }
    }

    private val logger = CapturingLogger()
    private val audit = ModerationAuditLogger(logger)

    @Test
    fun `el contenido rechazado no aparece en el registro`() {
        // El criterio central: se busca el texto rechazado en la salida y no debe estar. Un log con el
        // contenido prohibido es una copia de ese contenido en un sitio menos protegido, que ademas puede
        // salir del telefono en un reporte de errores.
        val texto = "insulto muy especifico que no debe aparecer en ningun log"

        audit.record(ModerationContentType.Text, ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed), 120, texto)

        val salida = logger.lines.joinToString("\n")
        assertFalse("El texto moderado se filtro al registro.", salida.contains(texto))
        texto.split(" ").filter { it.length > 4 }.forEach { palabra ->
            assertFalse("La palabra \"$palabra\" del contenido se filtro al registro.", salida.contains(palabra))
        }
    }

    @Test
    fun `se registra tipo, resultado y duracion`() {
        audit.record(ModerationContentType.Image, ModerationVerdict.Approved, 345, "bytes de una foto")

        val linea = logger.lines.single()
        assertTrue(linea.contains("type=Image"))
        assertTrue(linea.contains("result=approved"))
        assertTrue(linea.contains("ms=345"))
    }

    @Test
    fun `no se registra el puntaje ni la categoria`() {
        // Con el puntaje y la categoria alguien podria calibrar un texto hasta pasar el filtro.
        audit.record(ModerationContentType.Text, ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed), 90, "algo")

        val linea = logger.lines.single()
        assertFalse("El resultado no debe llevar la razon del rechazo.", linea.contains("ContentNotAllowed"))
        listOf("score", "puntaje", "0.", "category", "categoria").forEach {
            assertFalse("El registro no debe llevar \"$it\".", linea.lowercase().contains(it))
        }
    }

    @Test
    fun `el identificador es opaco y estable`() {
        val a = ModerationAuditLogger.opaqueId("mismo contenido")
        val b = ModerationAuditLogger.opaqueId("mismo contenido")
        val c = ModerationAuditLogger.opaqueId("otro contenido")

        // Estable: sirve para correlacionar dos intentos del mismo texto.
        assertEquals(a, b)
        assertTrue(a != c)
        // Corto e irreversible: no se puede reconstruir el contenido a partir de el.
        assertEquals(ModerationAuditLogger.ID_LENGTH, a.length)
        assertFalse(a.contains("mismo"))
    }

    @Test
    fun `los tres resultados se distinguen`() {
        audit.record(ModerationContentType.Text, ModerationVerdict.Approved, 1, "a")
        audit.record(ModerationContentType.Text, ModerationVerdict.Rejected(ModerationRejection.FileTooLarge), 1, "b")
        audit.record(ModerationContentType.Text, ModerationVerdict.NotValidated, 1, "c")

        val resultados = logger.lines.map { it.substringAfter("result=").substringBefore(" ") }
        assertEquals(listOf("approved", "rejected", "not_validated"), resultados)
    }
}
