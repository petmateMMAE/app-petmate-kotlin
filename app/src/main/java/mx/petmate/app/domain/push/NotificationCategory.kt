package mx.petmate.app.domain.push

/**
 * Categorias de notificacion: conjunto cerrado (spec 006, T-001, A-01). [key] es el nombre estable con el que
 * se guardan y, cuando Miguel defina el contrato (Q-01, Q-02), con el que viajan al servidor.
 */
enum class NotificationCategory(val key: String) {
    /** Vacunas y desparasitaciones proximas. */
    Health("health"),

    /** Cambios en el seguimiento de un tratamiento (015). */
    FollowUp("followup"),

    /** Mensajes del chat con la veterinaria (016). */
    Chat("chat"),

    /** Novedades del negocio o de la app. Es publicidad: se acepta a proposito (Q-09). */
    News("news"),

    /** Inicio de sesion nuevo, cambio de contrasena, eliminacion de cuenta. No se puede apagar (A-09). */
    Security("security"),
    ;

    companion object {
        fun fromKey(key: String): NotificationCategory? = entries.firstOrNull { it.key == key }
    }
}
