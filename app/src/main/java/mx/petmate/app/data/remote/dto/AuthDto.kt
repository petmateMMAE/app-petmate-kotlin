package mx.petmate.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Objetos de transporte de identidad.
 *
 * Contrato real del backend v0.1.0 (spec 002, Q-01 resuelta el 06/11/2026):
 * docs/api/endpoints.md del repositorio backend-petmate-sdd-aspnetcore.
 * El backend rechaza miembros desconocidos con 400: no agregar campos que no
 * esten en el contrato (por ejemplo, el nombre en el registro).
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

@Serializable
data class LogoutRequestDto(
    val refreshToken: String,
)

/** `data` de register, login y refresh. */
@Serializable
data class AuthDataDto(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val accessTokenExpiresAt: String = "",
    val refreshToken: String,
    val refreshTokenExpiresAt: String = "",
    val user: UserDto,
)

/** `data` de `GET users/me/get` y `PUT users/me/update` (pm-037, pm-038). */
@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    val role: String = UserDto.ROLE_OWNER,
    val displayName: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class UpdateProfileRequestDto(
    val displayName: String,
)

/** Rol: `Owner` (dueno) o `Business` (afiliado). */
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val role: String = ROLE_OWNER,
) {
    companion object {
        const val ROLE_OWNER = "Owner"
        const val ROLE_BUSINESS = "Business"
    }
}
