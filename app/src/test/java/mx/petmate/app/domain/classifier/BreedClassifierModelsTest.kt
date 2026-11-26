package mx.petmate.app.domain.classifier

import kotlinx.coroutines.test.runTest
import mx.petmate.app.domain.model.BreedSuggestion
import mx.petmate.app.domain.model.ClassificationResult
import mx.petmate.app.domain.model.PetSpecies
import org.junit.Assert.assertEquals
import org.junit.Test

/** Modelos y contrato del clasificador de razas (spec 018, T-001, D-01, D-03). */
class BreedClassifierModelsTest {

    @Test(expected = IllegalArgumentException::class)
    fun `la confianza no puede pasar de 1`() {
        BreedSuggestion("Labrador", 1.2f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sugerencias vacias no existen, para eso esta no identificada`() {
        ClassificationResult.Suggestions(emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se clasifica una imagen vacia`() {
        PetImage(ByteArray(0))
    }

    @Test
    fun `el contrato se puede doblar en la JVM con bytes y no con Bitmap`() = runTest {
        val fake = object : BreedClassifier {
            override suspend fun classify(image: PetImage, species: PetSpecies?): ClassificationResult =
                if (species == PetSpecies.Cat) {
                    ClassificationResult.NotIdentified
                } else {
                    ClassificationResult.ModelUnavailable
                }
        }

        assertEquals(ClassificationResult.NotIdentified, fake.classify(PetImage(byteArrayOf(1)), PetSpecies.Cat))
        assertEquals(ClassificationResult.ModelUnavailable, fake.classify(PetImage(byteArrayOf(1)), null))
    }
}
