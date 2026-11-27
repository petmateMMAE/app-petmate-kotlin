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
import mx.petmate.app.core.log.Logger
import mx.petmate.app.data.push.NotificationActivation
import mx.petmate.app.data.push.NotificationPreferencesStore
import mx.petmate.app.data.push.PushMessageRouter
import mx.petmate.app.data.push.PushRegistrar
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.DeviceApiService
import mx.petmate.app.domain.push.DeviceRegistration
import mx.petmate.app.domain.push.NoPushTokenProvider
import mx.petmate.app.domain.push.PushTokenProvider
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

    /**
     * De donde sale el token de push.
     *
     * `NoPushTokenProvider` mientras Q-08 siga abierta: que proyecto de Firebase se usa y quien
     * administra `google-services.json`. Devuelve `null`, asi que `PushRegistrar` no llama al
     * backend y la app arranca y funciona igual sin Firebase, que es lo que exige D-19.
     *
     * **Para enchufar Firebase se cambia solo esta linea** por el proveedor real; ni
     * `PushRegistrar` ni `PushMessageRouter` se enteran.
     */
    @Provides
    @Singleton
    fun providePushTokenProvider(): PushTokenProvider = NoPushTokenProvider

    @Provides
    @Singleton
    fun providePushRegistrar(
        tokenProvider: PushTokenProvider,
        deviceApi: DeviceApiService,
        caller: AuthorizedCaller,
        dispatchers: DispatcherProvider,
        logger: Logger,
    ): PushRegistrar = PushRegistrar(tokenProvider, deviceApi, caller, dispatchers, logger)

    /** Lo que ven `LoginUseCase` y `LogoutUseCase`; `domain` no puede importar `data` (regla 3). */
    @Provides
    @Singleton
    fun provideDeviceRegistration(registrar: PushRegistrar): DeviceRegistration = registrar

    @Provides
    @Singleton
    fun providePushMessageRouter(
        store: NotificationPreferencesStore,
        logger: Logger,
    ): PushMessageRouter = PushMessageRouter(store, logger)

    @Provides
    @Singleton
    fun provideNotificationActivation(
        store: NotificationPreferencesStore,
        permissions: PermissionGateway,
    ): NotificationActivation = NotificationActivation(store, permissions)
}
