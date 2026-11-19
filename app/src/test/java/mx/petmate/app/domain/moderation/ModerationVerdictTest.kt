package mx.petmate.app.domain.moderation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Estados del veredicto y despacho por tipo (spec 004, T-001, R-08, RT-04). */
class ModerationVerdictTest {

    @Test
    fun `solo aprobado deja publicar`() {
        assertTrue(ModerationVerdict.Approved.isPublishable)
        ModerationRejection.entries.forEach { assertFalse("$it", ModerationVerdict.Rejected(it).isPublishable) }
    }

    @Test
    fun `no validado bloquea igual que un rechazo pero es otro estado`() {
        assertFalse(ModerationVerdict.NotValidated.isPublishable)
        assertTrue(ModerationVerdict.NotValidated != ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed))
    }

    @Test
    fun `cada solicitud lleva su tipo`() {
        assertEquals(ModerationContentType.Text, ModerationRequest.Text("hola").type)
        assertEquals(ModerationContentType.Image, ModerationRequest.Image(byteArrayOf(1), "image/jpeg").type)
        assertEquals(ModerationContentType.Video, ModerationRequest.Video(byteArrayOf(1), "video/mp4").type)
    }

    @Test
    fun `moderate despacha al metodo de cada tipo`() = runTest {
        val seen = mutableListOf<ModerationContentType>()
        val gateway = object : ModerationGateway {
            override suspend fun moderateText(text: String) =
                ModerationVerdict.Approved.also { seen += ModerationContentType.Text }

            override suspend fun moderateImage(bytes: ByteArray, mimeType: String) =
                ModerationVerdict.NotValidated.also { seen += ModerationContentType.Image }

            override suspend fun moderateVideo(bytes: ByteArray, mimeType: String) =
                ModerationVerdict.Rejected(ModerationRejection.FileTooLarge)
                    .also { seen += ModerationContentType.Video }
        }

        assertEquals(ModerationVerdict.Approved, gateway.moderate(ModerationRequest.Text("hola")))
        val image = ModerationRequest.Image(byteArrayOf(1), "image/png")
        assertEquals(ModerationVerdict.NotValidated, gateway.moderate(image))
        gateway.moderate(ModerationRequest.Video(byteArrayOf(1), "video/mp4"))
        assertEquals(ModerationContentType.entries, seen)
    }
}
