package mx.petmate.app.domain.model

/**
 * Filtro por dia de las mascotas atendidas (spec 012, T-001, R-06 y A-06).
 *
 * Son las cuatro opciones de la maqueta. El rango de fechas lo calcula
 * `GetAttendedPetsUseCase` con el reloj inyectado (T-004): aqui solo se nombra
 * la opcion, sin tocar el reloj, para que el modelo siga siendo Kotlin puro.
 */
sealed interface DayFilter {

    /** Lo atendido hoy. */
    data object Today : DayFilter

    /** Lo atendido ayer. */
    data object Yesterday : DayFilter

    /** Los ultimos siete dias, hoy incluido. */
    data object Week : DayFilter

    /** Un dia concreto elegido en el calendario, en formato `yyyy-MM-dd`. */
    data class On(val date: String) : DayFilter

    companion object {
        /** Lo que muestra el panel al abrirse. */
        val Default: DayFilter = Today

        /** Dias que cubre [Week], hoy incluido. */
        const val WEEK_DAYS = 7
    }
}

/** Rango de fechas `yyyy-MM-dd`, con los dos extremos incluidos. */
data class DateRange(val from: String, val to: String) {
    /** Si [date] cae dentro del rango. Con el formato ISO, comparar texto equivale a comparar fecha. */
    operator fun contains(date: String): Boolean = date >= from && date <= to
}
