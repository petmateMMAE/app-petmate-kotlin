package mx.petmate.app.data.repository

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.data.push.NotificationPreferencesStore
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences
import mx.petmate.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Ajustes del usuario sobre el almacen de preferencias de 006 (spec 017, T-005).
 *
 * No hay entidad ni DAO de Room: las preferencias son un conjunto chico de interruptores y 006 ya las guarda
 * en DataStore, que persiste igual y no obliga a migrar el esquema de la base. La spec pedia lectura sin
 * conexion, no una tabla: DataStore lee del disco, asi que los ajustes se ven con o sin red.
 *
 * Sincronizar con el servidor espera a que Miguel defina el endpoint (Q-04); mientras, lo que el usuario
 * cambia aqui vive solo en su telefono y no se pierde.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val store: NotificationPreferencesStore,
) : SettingsRepository {

    override fun observeNotificationPreferences(): Flow<NotificationPreferences> = store.observe()

    override suspend fun setNotification(category: NotificationCategory, on: Boolean): NotificationPreferences =
        store.set(category, on)
}
