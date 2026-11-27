package mx.petmate.app.domain.model

/**
 * Mascota que el negocio atendio (spec 012, T-001, R-06).
 *
 * [scannedAt] es cuando se escaneo su QR, como lo devuelve el backend
 * (`attended`, pm-129). Se mueve aqui desde `Business.kt` para que el modelo
 * viva en el archivo que nombra la tarea; es el mismo tipo, mismo paquete, asi
 * que nada de lo que ya lo usaba cambia.
 */
data class AttendedPet(
    val name: String,
    val scannedAt: String,
    /** Identificador de la mascota, cuando el backend lo manda; sirve para navegar a su ficha. */
    val petId: String? = null,
) {
    /** Dia del escaneo en `yyyy-MM-dd`, para el filtro por dia. Null si la fecha no se puede leer. */
    val scannedOn: String? get() = scannedAt.take(ISO_DATE_LENGTH).takeIf { it.length == ISO_DATE_LENGTH }

    private companion object {
        const val ISO_DATE_LENGTH = 10
    }
}
