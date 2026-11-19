package mx.petmate.app.domain.model

import mx.petmate.app.core.result.AppError

/** Resultado de pedir un texto a la IA (spec 019, T-001). Tres casos distintos. */
sealed interface GenerationResult {
    data class Generated(val text: GeneratedText) : GenerationResult

    /** La funcion no esta disponible (sin ruta del backend, sin cuota o apagada): se muestra solo el original. */
    data object Unavailable : GenerationResult

    data class Failed(val error: AppError) : GenerationResult
}
