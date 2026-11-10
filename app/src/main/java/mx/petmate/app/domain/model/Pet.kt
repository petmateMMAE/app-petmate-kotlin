package mx.petmate.app.domain.model

/**
 * Mascota del dueno (spec 007; contrato del backend pm-040 a pm-044).
 *
 * Fechas como texto `yyyy-MM-dd`, igual que el backend: la interfaz las formatea.
 */
data class Pet(
    val id: String,
    val name: String,
    val species: PetSpecies,
    val breed: String?,
    val birthDate: String?,
    val sex: PetSex,
)

/** Lista cerrada del backend: solo Perro o Gato (T110 del backend). */
enum class PetSpecies(val apiValue: String) {
    Dog("Perro"),
    Cat("Gato"),
    ;

    companion object {
        /** Una mascota guardada antes con otra especie se sigue leyendo; se muestra como perro hasta que se edite. */
        fun fromApi(value: String?): PetSpecies = entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: Dog
    }
}

/** Sexo opcional (T110 del backend): sin dato se guarda `Unknown`. */
enum class PetSex(val apiValue: String) {
    Male("Male"),
    Female("Female"),
    Unknown("Unknown"),
    ;

    companion object {
        fun fromApi(value: String?): PetSex = entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: Unknown
    }
}

/** Lo que el usuario llena en el formulario de alta o edicion. */
data class PetDraft(
    val name: String,
    val species: PetSpecies?,
    val breed: String,
    val birthDate: String?,
    val sex: PetSex,
)

/** QR permanente (pm-093 a pm-095): token para el codigo y codigo corto para dictarlo. */
data class PetQr(
    val petId: String,
    val token: String,
    val shortCode: String,
)
