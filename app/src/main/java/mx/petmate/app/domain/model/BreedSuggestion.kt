package mx.petmate.app.domain.model

/** Una raza sugerida por el clasificador y que tan seguro esta, de 0 a 1 (spec 018, T-001). */
data class BreedSuggestion(
    val breed: String,
    val confidence: Float,
) {
    init {
        require(confidence in 0f..1f) { "La confianza va de 0 a 1" }
    }
}
