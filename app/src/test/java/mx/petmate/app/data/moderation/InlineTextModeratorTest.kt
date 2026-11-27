package mx.petmate.app.data.moderation

import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.moderation.ModerationGateway
import mx.petmate.app.domain.moderation.ModerationRejection
import mx.petmate.app.domain.moderation.ModerationVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Moderacion de texto en linea (spec 004, T-004, C-02 y C-03). */
class InlineTextModeratorTest {

    private class FakeGateway(
        private val verdict: ModerationVerdict? = ModerationVerdict.Approved,
    ) : ModerationGateway {
        var calls = 0
        override suspend fun moderateText(text: String): ModerationVerdict {
            calls++
            return verdict ?: throw IllegalStateException("sin red")
        }
        override suspend fun moderateImage(bytes: ByteArray, mimeType: String) = ModerationVerdict.NotValidated
        override suspend fun moderateVideo(bytes: ByteArray, mimeType: String) = ModerationVerdict.NotValidated
    }

    private object SilentLogger : Logger {
        override fun debug(tag: String, message: String) = Unit
        override fun error(tag: String, message: String, throwable: Throwable?) = Unit
    }

    private fun moderator(verdict: ModerationVerdict?) =
        FakeGateway(verdict).let { it to InlineTextModerator(it, ModerationAuditLogger(SilentLogger)) }

    @Test
    fun `el texto aprobado se guarda una sola vez`() = runTest {
        val (_, moderator) = moderator(ModerationVerdict.Approved)
        val guardados = mutableListOf<String>()

        val verdict = moderator.moderateThenSave("hola") { guardados += it }

        assertTrue(verdict.isPublishable)
        assertEquals(listOf("hola"), guardados)
    }

    @Test
    fun `el texto rechazado no se guarda`() = runTest {
        val (_, moderator) = moderator(ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed))
        val guardados = mutableListOf<String>()

        val verdict = moderator.moderateThenSave("algo no permitido") { guardados += it }

        assertFalse(verdict.isPublishable)
        assertTrue("Lo rechazado no puede llegar a la base.", guardados.isEmpty())
    }

    @Test
    fun `si el servicio falla no se guarda`() = runTest {
        // R-08: una excepcion no es permiso para pasar.
        val (_, moderator) = moderator(null)
        val guardados = mutableListOf<String>()

        val verdict = moderator.moderateThenSave("hola") { guardados += it }

        assertEquals(ModerationVerdict.NotValidated, verdict)
        assertTrue(guardados.isEmpty())
    }

    @Test
    fun `se modera ANTES de guardar, no despues`() = runTest {
        // Si fuera al reves, lo rechazado ya habria estado en la base un instante.
        val gateway = FakeGateway(ModerationVerdict.Approved)
        val moderator = InlineTextModerator(gateway, ModerationAuditLogger(SilentLogger))
        var llamadasAlModerarCuandoSeGuardo = -1

        moderator.moderateThenSave("hola") { llamadasAlModerarCuandoSeGuardo = gateway.calls }

        assertEquals("El guardado debe ocurrir con la moderacion ya hecha.", 1, llamadasAlModerarCuandoSeGuardo)
    }
}
