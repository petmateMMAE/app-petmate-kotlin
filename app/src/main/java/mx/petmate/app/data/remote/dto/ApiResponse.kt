package mx.petmate.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Envoltura comun de todas las respuestas del backend PetMate
 * (`statusCode`, `message`, `data`; `data` puede ser null).
 *
 * Contrato: docs/api/endpoints.md del repositorio backend-petmate-sdd-aspnetcore.
 */
@Serializable
data class ApiResponse<T>(
    val statusCode: Int,
    val message: String = "",
    val data: T? = null,
)

/** `data` de un 400 de validacion: `{ "errors": [{ "field", "message" }] }`. */
@Serializable
data class ValidationErrorsDto(
    val errors: List<FieldErrorDto> = emptyList(),
)

@Serializable
data class FieldErrorDto(
    val field: String = "",
    val message: String = "",
)
