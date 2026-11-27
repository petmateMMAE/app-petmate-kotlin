package mx.petmate.app.data.moderation

import java.io.File
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.moderation.ModerationGateway
import mx.petmate.app.domain.moderation.ModerationRejection
import mx.petmate.app.domain.moderation.ModerationVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Moderacion de imagen antes de subir (spec 004, T-006, C-06 a C-08). */
class ImageModeratorTest {

    @get:Rule val folder = TemporaryFolder()

    private class FakeGateway(private val verdict: ModerationVerdict?) : ModerationGateway {
        override suspend fun moderateText(text: String) = ModerationVerdict.NotValidated
        override suspend fun moderateImage(bytes: ByteArray, mimeType: String): ModerationVerdict =
            verdict ?: throw IllegalStateException("sin red")
        override suspend fun moderateVideo(bytes: ByteArray, mimeType: String) = ModerationVerdict.NotValidated
    }

    private object SilentLogger : Logger {
        override fun debug(tag: String, message: String) = Unit
        override fun error(tag: String, message: String, throwable: Throwable?) = Unit
    }

    private fun moderator(verdict: ModerationVerdict?): Pair<TemporaryFileStore, ImageModerator> {
        val store = FileSystemTemporaryStore(folder.root)
        return store to ImageModerator(FakeGateway(verdict), store, ModerationAuditLogger(SilentLogger))
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)

    @Test
    fun `la imagen aprobada se sube una sola vez`() = runTest {
        val (_, moderator) = moderator(ModerationVerdict.Approved)
        val subidas = mutableListOf<String>()

        val verdict = moderator.moderateThenUpload(jpeg, "image/jpeg") { subidas += it.name }

        assertTrue(verdict.isPublishable)
        assertEquals(1, subidas.size)
    }

    @Test
    fun `la imagen rechazada no se sube y el temporal se borra`() = runTest {
        // C-07: el usuario cree que no se guardo nada. Tiene que ser cierto tambien en la cache.
        val (store, moderator) = moderator(ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed))
        val subidas = mutableListOf<File>()

        val verdict = moderator.moderateThenUpload(jpeg, "image/jpeg") { subidas += it }

        assertFalse(verdict.isPublishable)
        assertTrue("Lo rechazado no puede subirse.", subidas.isEmpty())
        assertEquals("No puede quedar ningun temporal en la cache.", 0, store.count())
    }

    @Test
    fun `sin red no se sube y el temporal tampoco se queda`() = runTest {
        // C-08: no se encola el archivo. Encolarlo sin moderar seria subirlo mas tarde sin revisar.
        val (store, moderator) = moderator(null)
        val subidas = mutableListOf<File>()

        val verdict = moderator.moderateThenUpload(jpeg, "image/jpeg") { subidas += it }

        assertEquals(ModerationVerdict.NotValidated, verdict)
        assertTrue(subidas.isEmpty())
        assertEquals(0, store.count())
    }

    @Test
    fun `el temporal se borra incluso si la subida lanza`() = runTest {
        val (store, moderator) = moderator(ModerationVerdict.Approved)

        runCatching {
            moderator.moderateThenUpload(jpeg, "image/jpeg") { error("fallo la subida") }
        }

        // Va en un finally justo por esto: un fallo a medio camino no puede dejar basura en la cache.
        assertEquals(0, store.count())
    }

    @Test
    fun `el archivo existe mientras se sube`() = runTest {
        // La subida recibe un archivo real, no una ruta a algo ya borrado.
        val (_, moderator) = moderator(ModerationVerdict.Approved)
        var existiaAlSubir = false

        moderator.moderateThenUpload(jpeg, "image/jpeg") { existiaAlSubir = it.exists() && it.length() > 0 }

        assertTrue("El archivo debe existir y tener contenido cuando se sube.", existiaAlSubir)
    }
}
