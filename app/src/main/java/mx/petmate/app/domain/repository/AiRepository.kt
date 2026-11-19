package mx.petmate.app.domain.repository

import mx.petmate.app.domain.model.GenerationRequest
import mx.petmate.app.domain.model.GenerationResult

/**
 * IA generativa vista desde la app (spec 019, T-002). Solo habla con el backend de PetMate: la llave del
 * proveedor nunca vive en el telefono, por eso aqui no hay ninguna llamada a un proveedor externo.
 */
interface AiRepository {
    /**
     * Explica o resume [request]. Si ya hay un texto guardado con la misma huella se devuelve sin red;
     * [forceRegenerate] lo pide de nuevo al backend.
     */
    suspend fun generate(request: GenerationRequest, forceRegenerate: Boolean = false): GenerationResult
}
