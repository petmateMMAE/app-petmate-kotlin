package mx.petmate.app.domain.model

/**
 * Lo que se le pide a la IA generativa (spec 019, T-001). [key] identifica el contenido para la cache; [source]
 * es el texto original que se explica o resume, y de el sale la huella.
 */
sealed interface GenerationRequest {
    val key: String
    val source: String

    /** Explicar en lenguaje sencillo la receta de una consulta. */
    data class ExplainPrescription(val consultationId: String, override val source: String) : GenerationRequest {
        override val key: String get() = "prescription:$consultationId"
    }

    /** Resumir el historial clinico de una mascota. */
    data class SummarizeHistory(val petId: String, override val source: String) : GenerationRequest {
        override val key: String get() = "history:$petId"
    }
}
