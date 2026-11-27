package mx.petmate.app.data.moderation

import mx.petmate.app.core.text.ModerationMessages
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.moderation.ModerationRejection
import mx.petmate.app.domain.moderation.ModerationVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Veredicto, errores y mensaje unico (spec 004, T-003, R-07 y R-08). */
class ModerationPolicyTest {

    @Test
    fun `una respuesta aprobada deja publicar`() {
        val verdict = ModerationPolicy.fromResponse(approved = true)

        assertEquals(ModerationVerdict.Approved, verdict)
        assertTrue(verdict.isPublishable)
    }

    @Test
    fun `una respuesta rechazada no deja publicar`() {
        val verdict = ModerationPolicy.fromResponse(approved = false)

        assertEquals(ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed), verdict)
        assertFalse(verdict.isPublishable)
    }

    @Test
    fun `sin respuesta del servicio no se publica`() {
        // R-08: ante la duda no se publica. Si un moderador caido dejara pasar, tirarlo seria la forma mas
        // facil de saltarse la moderacion.
        val verdict = ModerationPolicy.fromResponse(approved = null)

        assertEquals(ModerationVerdict.NotValidated, verdict)
        assertFalse(verdict.isPublishable)
    }

    @Test
    fun `un fallo de red nunca se traduce a aprobado`() {
        val verdict = ModerationPolicy.fromFailure()

        assertEquals(ModerationVerdict.NotValidated, verdict)
        assertFalse(verdict.isPublishable)
    }

    @Test
    fun `un puntaje alto rechaza aunque el servicio diga que aprobo`() {
        // El umbral es nuestro, no del proveedor.
        val verdict = ModerationPolicy.fromResponse(approved = true, highestScore = 0.9)

        assertFalse(verdict.isPublishable)
    }

    @Test
    fun `un puntaje bajo con aprobacion si publica`() {
        assertTrue(ModerationPolicy.fromResponse(approved = true, highestScore = 0.1).isPublishable)
    }

    @Test
    fun `el umbral rechaza justo en el limite`() {
        assertFalse(ModerationPolicy.fromResponse(true, ModerationPolicy.REJECTION_THRESHOLD).isPublishable)
        assertTrue(ModerationPolicy.fromResponse(true, ModerationPolicy.REJECTION_THRESHOLD - 0.01).isPublishable)
    }

    @Test
    fun `dos categorias distintas dan el MISMO mensaje`() {
        // El criterio central de T-003. Se simulan dos rechazos de categorias internas distintas: los dos
        // tienen que llegar al usuario como el mismo texto, sin pista de cual fue.
        val violencia = ModerationPolicy.fromResponse(approved = false, highestScore = 0.97)
        val otraCategoria = ModerationPolicy.fromResponse(approved = false, highestScore = 0.62)

        assertEquals(violencia, otraCategoria)
        assertEquals(
            ModerationMessages.forVerdict(violencia),
            ModerationMessages.forVerdict(otraCategoria),
        )
    }

    @Test
    fun `el mensaje no revela el puntaje ni el proveedor`() {
        val mensaje = ModerationMessages.forVerdict(
            ModerationPolicy.fromResponse(approved = false, highestScore = 0.97),
        )

        // Es un id de recurso, no texto armado: no hay forma de colar un puntaje dentro.
        assertTrue("El mensaje debe salir de strings.xml, no construirse con datos del proveedor.", mensaje is UiText.Resource)
        assertTrue("El mensaje no debe llevar argumentos del moderador.", (mensaje as UiText.Resource).args.isEmpty())
    }

    @Test
    fun `aprobado no tiene mensaje`() {
        assertNull(ModerationMessages.forVerdict(ModerationVerdict.Approved))
    }

    @Test
    fun `tamano y formato si tienen mensaje propio`() {
        // Aqui la persona si puede actuar: elegir otro archivo o comprimirlo. Por eso se distingue.
        val grande = ModerationMessages.forRejection(ModerationRejection.FileTooLarge)
        val formato = ModerationMessages.forRejection(ModerationRejection.UnsupportedFormat)
        val noPermitido = ModerationMessages.forRejection(ModerationRejection.ContentNotAllowed)

        assertTrue(grande != formato && formato != noPermitido && grande != noPermitido)
    }

    @Test
    fun `un formato no admitido se rechaza sin consultar al moderador`() {
        val verdict = ModerationPolicy.forFile(
            sizeBytes = 1_000,
            mimeType = "image/bmp",
            maxBytes = 5_000_000,
            allowedTypes = setOf("image/jpeg", "image/png"),
        )

        assertEquals(ModerationVerdict.Rejected(ModerationRejection.UnsupportedFormat), verdict)
    }

    @Test
    fun `un archivo demasiado grande se rechaza sin consultar al moderador`() {
        val verdict = ModerationPolicy.forFile(
            sizeBytes = 9_000_000,
            mimeType = "image/jpeg",
            maxBytes = 5_000_000,
            allowedTypes = setOf("image/jpeg", "image/png"),
        )

        assertEquals(ModerationVerdict.Rejected(ModerationRejection.FileTooLarge), verdict)
    }

    @Test
    fun `el formato se revisa antes que el tamano`() {
        // Un archivo que falla en las dos cosas se reporta por formato: cambiar de formato es la accion util,
        // comprimir un BMP no sirve de nada.
        val verdict = ModerationPolicy.forFile(9_000_000, "image/bmp", 5_000_000, setOf("image/jpeg"))

        assertEquals(ModerationVerdict.Rejected(ModerationRejection.UnsupportedFormat), verdict)
    }

    @Test
    fun `el tipo de archivo no distingue mayusculas`() {
        assertTrue(ModerationPolicy.forFile(100, "IMAGE/JPEG", 5_000, setOf("image/jpeg")).isPublishable)
    }
}
