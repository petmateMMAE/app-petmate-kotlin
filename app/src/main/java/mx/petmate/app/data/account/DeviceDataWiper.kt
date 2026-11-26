package mx.petmate.app.data.account

import kotlinx.coroutines.withContext
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.local.PetMateDatabase
import mx.petmate.app.data.push.NotificationPreferencesStore
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.usecase.LocalDataWiper

/**
 * Borrado real de todo lo local (spec 017, T-009). `clearAllTables` vacia todas las tablas, incluidas las que se
 * agreguen despues, y corre fuera del hilo principal (R-13).
 */
class DeviceDataWiper(
    private val database: PetMateDatabase,
    private val notificationPreferences: NotificationPreferencesStore,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
) : LocalDataWiper {

    override suspend fun wipeAll() = withContext(dispatchers.io) {
        database.clearAllTables()
        notificationPreferences.clear()
        sessionStore.clear()
    }
}
