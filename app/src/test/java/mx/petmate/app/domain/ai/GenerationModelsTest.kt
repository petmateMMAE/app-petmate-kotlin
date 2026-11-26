package mx.petmate.app.domain.ai

import kotlinx.coroutines.test.runTest
import mx.petmate.app.domain.model.ContentFingerprint
import mx.petmate.app.domain.model.GeneratedText
import mx.petmate.app.domain.model.GenerationRequest
import mx.petmate.app.domain.model.GenerationResult
import mx.petmate.app.domain.repository.AiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Modelos, huella y contrato de la IA generativa (spec 019, T-001 y T-002, D-04). */
class GenerationModelsTest {

    @Test
    fun `la misma receta da la misma huella y un cambio da otra`() {
        val original = "Amoxicilina 250 mg cada 12 horas por 7 dias"

        assertEquals(ContentFingerprint.of(original), ContentFingerprint.of(original))
        assertNotEquals(ContentFingerprint.of(original), ContentFingerprint.of("$original."))
        assertEquals(64, ContentFingerprint.of(original).length)
    }

    @Test
    fun `cada pedido tiene su llave de cache`() {
        assertEquals("prescription:c-1", GenerationRequest.ExplainPrescription("c-1", "x").key)
        assertEquals("history:p-1", GenerationRequest.SummarizeHistory("p-1", "x").key)
    }

    @Test
    fun `todo texto generado queda marcado como IA`() {
        assertTrue(GeneratedText("hola", 1L, ContentFingerprint.of("x")).isAiGenerated)
    }

    @Test
    fun `el contrato devuelve los tres casos y por defecto no fuerza regenerar`() = runTest {
        var forced: Boolean? = null
        val fake = object : AiRepository {
            override suspend fun generate(request: GenerationRequest, forceRegenerate: Boolean): GenerationResult {
                forced = forceRegenerate
                return GenerationResult.Unavailable
            }
        }

        assertEquals(GenerationResult.Unavailable, fake.generate(GenerationRequest.SummarizeHistory("p-1", "x")))
        assertEquals(false, forced)
    }
}
