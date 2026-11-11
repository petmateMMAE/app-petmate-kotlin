package mx.petmate.app.domain.model

/**
 * Usuario autenticado.
 *
 * Desde pm-086 hay un solo rol (`User`): una cuenta es tambien negocio cuando tiene uno afiliado, y la sesion lo
 * informa en [businessType] (`Veterinary`, `Store` o `Grooming`; pm-087).
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val isAffiliate: Boolean = false,
    val businessType: String? = null,
)
