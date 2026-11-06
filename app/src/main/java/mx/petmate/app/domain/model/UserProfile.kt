package mx.petmate.app.domain.model

/**
 * Perfil propio tal como lo devuelve el backend (`GET users/me/get`, pm-037).
 *
 * [memberSince] es la fecha de alta en formato ISO-8601 (UTC); la interfaz decide como mostrarla.
 */
data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val isAffiliate: Boolean,
    val memberSince: String?,
)

/**
 * Cuenta de demostracion para la revision y las pruebas (06/11/2026).
 * Existe en el backend real y en el simulado con las mismas credenciales.
 * No es un secreto: es una cuenta publica sin datos reales.
 */
object DemoAccount {
    const val EMAIL = "demo@petmate.mx"
    const val PASSWORD = "PetMateDemo2026"
    const val NAME = "Usuario Demo"
}
