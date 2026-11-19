package mx.petmate.app.domain.push

/**
 * Que categorias quiere recibir el usuario (spec 006, T-001, A-02, R-02).
 *
 * Valores por defecto (decision registrada en `memoria.md`, la spec no los fija): todo encendido menos
 * [NotificationCategory.News], que es publicidad. [NotificationCategory.Security] siempre queda encendida: no
 * hay forma de construir unas preferencias con seguridad apagada.
 */
class NotificationPreferences private constructor(private val enabled: Set<NotificationCategory>) {

    fun isEnabled(category: NotificationCategory): Boolean = category in enabled

    /** Categorias encendidas, siempre con seguridad. */
    val enabledCategories: Set<NotificationCategory> get() = enabled

    /** Devuelve otras preferencias con [category] encendida o apagada. Apagar seguridad no tiene efecto. */
    fun with(category: NotificationCategory, on: Boolean): NotificationPreferences =
        of(if (on) enabled + category else enabled - category)

    override fun equals(other: Any?): Boolean = other is NotificationPreferences && other.enabled == enabled

    override fun hashCode(): Int = enabled.hashCode()

    override fun toString(): String = "NotificationPreferences(${enabled.joinToString { it.key }})"

    companion object {
        val DEFAULT: NotificationPreferences = of(NotificationCategory.entries.toSet() - NotificationCategory.News)

        /** Unicas que el usuario puede apagar. */
        fun canBeDisabled(category: NotificationCategory): Boolean = category != NotificationCategory.Security

        /** Preferencias con [categories] encendidas; seguridad se agrega siempre. */
        fun of(categories: Set<NotificationCategory>): NotificationPreferences =
            NotificationPreferences(categories + NotificationCategory.Security)
    }
}
