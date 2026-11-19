package mx.petmate.app.domain.model

/**
 * Resultado de clasificar una foto (spec 018, T-001, D-03). Son tres casos distintos a proposito: la pantalla
 * no debe confundir "no se reconocio la raza" con "el modelo no esta en este telefono".
 */
sealed interface ClassificationResult {
    /** Sugerencias de la mas probable a la menos; nunca vacia. */
    data class Suggestions(val items: List<BreedSuggestion>) : ClassificationResult {
        init {
            require(items.isNotEmpty()) { "Sin sugerencias se usa NotIdentified" }
        }
    }

    /** El modelo corrio pero ninguna raza paso el umbral: se deja escribir la raza a mano. */
    data object NotIdentified : ClassificationResult

    /** El modelo no se pudo cargar o no existe en este telefono: la funcion se oculta sin error. */
    data object ModelUnavailable : ClassificationResult
}
