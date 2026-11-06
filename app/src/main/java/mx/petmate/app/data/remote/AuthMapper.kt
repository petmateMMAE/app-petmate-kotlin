package mx.petmate.app.data.remote

import mx.petmate.app.data.remote.dto.UserDto
import mx.petmate.app.domain.model.User

/**
 * Traduce el transporte al modelo de dominio.
 *
 * El backend v0.1.0 no guarda el nombre (spec 002, Q-09; el perfil llega con
 * pm-037/pm-038 del backend), asi que el nombre se conserva solo en el
 * dispositivo. Si no se conoce, se usa la parte local del correo.
 */
fun UserDto.toDomain(localName: String? = null): User = User(
    id = id,
    name = localName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@'),
    email = email,
    isAffiliate = role == UserDto.ROLE_BUSINESS,
)
