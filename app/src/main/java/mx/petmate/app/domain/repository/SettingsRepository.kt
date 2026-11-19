package mx.petmate.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences

/**
 * Ajustes reversibles del usuario (spec 017, T-002). Las preferencias son las de 006, no se redefinen.
 * Donde se guardan (solo telefono o tambien servidor) depende de Q-04.
 */
interface SettingsRepository {
    fun observeNotificationPreferences(): Flow<NotificationPreferences>

    suspend fun setNotification(category: NotificationCategory, on: Boolean): NotificationPreferences
}
