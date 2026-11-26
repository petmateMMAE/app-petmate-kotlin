package mx.petmate.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.account.DeviceDataWiper
import mx.petmate.app.data.local.PetMateDatabase
import mx.petmate.app.data.push.NotificationActivation
import mx.petmate.app.data.push.NotificationPreferencesStore
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.permissions.PermissionGateway
import mx.petmate.app.domain.usecase.LocalDataWiper
import mx.petmate.app.domain.usecase.WipeLocalDataUseCase
import javax.inject.Singleton

/**
 * Preferencias de notificacion en el telefono (spec 006, T-007 y T-009; aun sin Firebase: Q-01 a Q-03) y la
 * limpieza local de todo lo guardado (spec 017, T-009).
 */
@Module
@InstallIn(SingletonComponent::class)
object PushModule {

    @Provides
    @Singleton
    fun provideNotificationPreferencesStore(@ApplicationContext context: Context): NotificationPreferencesStore {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("notification_preferences") },
        )
        return NotificationPreferencesStore(dataStore)
    }

    @Provides
    @Singleton
    fun provideLocalDataWiper(
        database: PetMateDatabase,
        store: NotificationPreferencesStore,
        sessionStore: SessionStore,
        dispatchers: DispatcherProvider,
    ): LocalDataWiper = DeviceDataWiper(database, store, sessionStore, dispatchers)

    @Provides
    fun provideWipeLocalDataUseCase(wiper: LocalDataWiper): WipeLocalDataUseCase = WipeLocalDataUseCase(wiper)

    @Provides
    @Singleton
    fun provideNotificationActivation(
        store: NotificationPreferencesStore,
        permissions: PermissionGateway,
    ): NotificationActivation = NotificationActivation(store, permissions)
}
