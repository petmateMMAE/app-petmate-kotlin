package mx.petmate.app.data.moderation

import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.moderation.ModerationGateway
import mx.petmate.app.domain.moderation.ModerationRejection
import mx.petmate.app.domain.moderation.ModerationRequest
import mx.petmate.app.domain.moderation.ModerationVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Punto de moderacion: el no-op y los dos dobles que pide la tarea, uno que
 * aprueba y otro que rechaza (spec 001, T-037).
 */
class NoOpModerationGatewayTest {

    private class RecordingLogger : Logger {
        val messages = mutableListOf<String>()

        override fun debug(tag: String, message: String) {
            messages += message
        }

        override fun error(tag: String, message: String, throwable: Throwable?) {
            messages += message
        }
    }

    /** Doble que aprueba todo. */
    private class ApprovingGateway : ModerationGateway {
        override suspend fun moderateText(text: String) = ModerationVerdict.Approved
        override suspend fun moderateImage(bytes: ByteArray, mimeType: String) = ModerationVerdict.Approved
        override suspend fun moderateVideo(bytes: ByteArray, mimeType: String) = ModerationVerdict.Approved
    }

    /** Doble que rechaza todo. */
    private class RejectingGateway : ModerationGateway {
        private val verdict = ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed)
        override suspend fun moderateText(text: String) = verdict
        override suspend fun moderateImage(bytes: ByteArray, mimeType: String) = verdict
        override suspend fun moderateVideo(bytes: ByteArray, mimeType: String) = verdict
    }

    private val logger = RecordingLogger()
    private val noOp = NoOpModerationGateway(logger)

    private val bytes = byteArrayOf(1, 2, 3)

    // ---- El no-op no aprueba ----

    @Test
    fun `el no-op devuelve no validado para los tres tipos`() = runTest {
        assertEquals(ModerationVerdict.NotValidated, noOp.moderateText("hola"))
        assertEquals(ModerationVerdict.NotValidated, noOp.moderateImage(bytes, "image/jpeg"))
        assertEquals(ModerationVerdict.NotValidated, noOp.moderateVideo(bytes, "video/mp4"))
    }

    @Test
    fun `el no-op nunca deja publicar`() = runTest {
        assertFalse(noOp.moderateText("hola").isPublishable)
        assertFalse(noOp.moderateImage(bytes, "image/png").isPublishable)
        assertFalse(noOp.moderateVideo(bytes, "video/mp4").isPublishable)
    }

    @Test
    fun `el no-op queda registrado en cada llamada`() = runTest {
        noOp.moderateText("hola")
        noOp.moderateImage(bytes, "image/png")

        assertEquals(2, logger.messages.size)
        assertTrue("debe apuntar a la tarea que falta", logger.messages.all { it.contains("004") })
    }

    @Test
    fun `el no-op no registra el contenido`() = runTest {
        noOp.moderateText("mi direccion es Calle Falsa 123")

        assertTrue(logger.messages.none { it.contains("Calle Falsa") })
    }

    @Test
    fun `el despacho por tipo tambien devuelve no validado`() = runTest {
        val requests = listOf(
            ModerationRequest.Text("hola"),
            ModerationRequest.Image(bytes, "image/jpeg"),
            ModerationRequest.Video(bytes, "video/mp4"),
        )

        requests.forEach { assertEquals("$it", ModerationVerdict.NotValidated, noOp.moderate(it)) }
    }

    // ---- Los dos dobles ----

    @Test
    fun `el doble que aprueba deja publicar`() = runTest {
        val gateway: ModerationGateway = ApprovingGateway()

        assertEquals(ModerationVerdict.Approved, gateway.moderate(ModerationRequest.Text("hola")))
        assertTrue(gateway.moderate(ModerationRequest.Text("hola")).isPublishable)
    }

    @Test
    fun `el doble que rechaza no deja publicar y da un motivo generico`() = runTest {
        val gateway: ModerationGateway = RejectingGateway()

        val verdict = gateway.moderate(ModerationRequest.Image(bytes, "image/jpeg"))

        assertEquals(ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed), verdict)
        assertFalse(verdict.isPublishable)
    }

    @Test
    fun `el despacho manda cada peticion a su metodo`() = runTest {
        val seen = mutableListOf<String>()
        val gateway = object : ModerationGateway {
            override suspend fun moderateText(text: String): ModerationVerdict {
                seen += "texto"
                return ModerationVerdict.Approved
            }

            override suspend fun moderateImage(bytes: ByteArray, mimeType: String): ModerationVerdict {
                seen += "imagen"
                return ModerationVerdict.Approved
            }

            override suspend fun moderateVideo(bytes: ByteArray, mimeType: String): ModerationVerdict {
                seen += "video"
                return ModerationVerdict.Approved
            }
        }

        gateway.moderate(ModerationRequest.Text("hola"))
        gateway.moderate(ModerationRequest.Image(bytes, "image/jpeg"))
        gateway.moderate(ModerationRequest.Video(bytes, "video/mp4"))

        assertEquals(listOf("texto", "imagen", "video"), seen)
    }
}
