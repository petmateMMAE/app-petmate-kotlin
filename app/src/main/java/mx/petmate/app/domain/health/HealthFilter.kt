package mx.petmate.app.domain.health

/**
 * Tipos de registro del carnet (spec 008, R-01: las seis secciones).
 *
 * [apiKind] es el texto que usa el historial unificado del backend
 * (`HistoryItem.kind`, pm-142), para no tener dos vocabularios.
 */
enum class HealthEventType(val apiKind: String) {
    Vaccine("Vaccine"),
    Deworming("Deworming"),
    Weight("Weight"),
    Allergy("Allergy"),
    Condition("Condition"),
    Consultation("Consultation"),
    ;

    /** Las alergias nunca se ocultan ni se filtran (R-06). */
    val isAlwaysVisible: Boolean get() = this == Allergy

    companion object {
        fun fromApi(value: String?): HealthEventType? =
            entries.firstOrNull { it.apiKind.equals(value, ignoreCase = true) }
    }
}

/**
 * Filtro de la linea de tiempo por tipo y por rango de fechas (spec 008, T-006, R-09).
 *
 * Las fechas son texto `yyyy-MM-dd`, como en todo el carnet, asi que se comparan
 * en orden alfabetico: para ese formato equivale a comparar la fecha. [from] y
 * [to] son inclusivos; null significa "sin limite".
 *
 * Es logica pura: no da formato a nada. Mostrar la fecha en formato local es
 * trabajo de la interfaz (R-10), que es la unica capa que conoce el idioma del
 * telefono.
 */
data class HealthFilter(
    val types: Set<HealthEventType> = HealthEventType.entries.toSet(),
    val from: String? = null,
    val to: String? = null,
) {
    /**
     * Si el evento pasa el filtro.
     *
     * Una alergia pasa siempre, aunque el tipo este desmarcado o quede fuera del
     * rango: R-06 prohibe ocultarlas y el riesgo G-02 es justo que una se pierda
     * por un filtro. Un evento sin fecha (las alergias y condiciones no la
     * traen en el contrato del backend) no se descarta por el rango.
     */
    fun accepts(event: HealthEvent): Boolean {
        if (event.type.isAlwaysVisible) return true
        if (event.type !in types) return false
        val date = event.date ?: return true
        if (from != null && date < from) return false
        if (to != null && date > to) return false
        return true
    }

    companion object {
        /** Todo visible: el estado inicial de la pantalla. */
        val All = HealthFilter()
    }
}
