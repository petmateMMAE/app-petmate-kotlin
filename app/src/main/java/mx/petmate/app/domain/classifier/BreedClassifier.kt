package mx.petmate.app.domain.classifier

import mx.petmate.app.domain.model.ClassificationResult
import mx.petmate.app.domain.model.PetSpecies

/**
 * Foto que se va a clasificar, como bytes codificados (JPEG o PNG) y no como `Bitmap` de Android (spec 018,
 * D-01): asi `domain` no conoce Android y se puede probar en la JVM. La foto nunca sale del telefono (T-008).
 */
class PetImage(val encoded: ByteArray) {
    init {
        require(encoded.isNotEmpty()) { "Imagen vacia" }
    }
}

/**
 * Sugiere la raza a partir de una foto, todo dentro del telefono (spec 018, T-001). La tecnologia (TFLite, ML
 * Kit u otra) se decide en T-003 y vive en `data`; [species] se pasa si ya se sabe, por si el modelo distingue
 * perro y gato por separado (Q-04).
 */
interface BreedClassifier {
    suspend fun classify(image: PetImage, species: PetSpecies?): ClassificationResult
}
