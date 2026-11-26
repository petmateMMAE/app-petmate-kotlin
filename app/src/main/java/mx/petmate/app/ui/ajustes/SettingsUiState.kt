package mx.petmate.app.ui.ajustes

import androidx.annotation.StringRes
import mx.petmate.app.domain.model.LegalLinks
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences

/**
 * Una categoria tal como la ve la pantalla (spec 017, T-006).
 *
 * [canBeDisabled] viaja en el estado y no se recalcula en la vista: la pantalla dibuja lo que el estado dice,
 * y la regla de que Seguridad no se apaga (A-09) vive en el dominio.
 */
data class NotificationToggle(
    val category: NotificationCategory,
    val isOn: Boolean,
    val canBeDisabled: Boolean,
)

/** Estado de la pantalla de ajustes (spec 017, T-006). */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val toggles: List<NotificationToggle> = emptyList(),
    /** Si no esta configurado (Q-02), la pantalla no muestra un enlace roto. */
    val legalLinks: LegalLinks = LegalLinks.PENDING,
    val isLoggingOut: Boolean = false,
    /** Cerrada la sesion, la pantalla pide volver al grafo de identidad. */
    val loggedOut: Boolean = false,
    @param:StringRes val error: Int? = null,
) {
    companion object {
        /** Orden estable: el mismo de [NotificationCategory], para que los interruptores no se muevan. */
        fun togglesOf(preferences: NotificationPreferences): List<NotificationToggle> =
            NotificationCategory.entries.map { category ->
                NotificationToggle(
                    category = category,
                    isOn = preferences.isEnabled(category),
                    canBeDisabled = NotificationPreferences.canBeDisabled(category),
                )
            }
    }
}
