package mx.petmate.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.core.connectivity.AndroidConnectivityObserver
import mx.petmate.app.core.connectivity.ConnectivityObserver
import mx.petmate.app.core.coroutines.DefaultDispatcherProvider
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.AndroidLogger
import mx.petmate.app.core.log.Logger
import mx.petmate.app.core.time.Clock
import mx.petmate.app.data.cache.CachePolicy
import mx.petmate.app.data.session.SessionStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideLogger(): Logger = AndroidLogger()

    @Provides
    @Singleton
    fun provideDispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    /** Estado de la red, sin sondeo (spec 001, T-035). */
    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver =
        AndroidConnectivityObserver(context)

    @Provides
    @Singleton
    fun provideSessionStore(
        @ApplicationContext context: Context,
        logger: Logger,
    ): SessionStore = SessionStore(context, logger)

    /**
     * El reloj, inyectable (spec 001, T-018).
     *
     * Existe para que una prueba pueda adelantar el tiempo en vez de esperarlo. En la app es
     * siempre el del sistema.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.SYSTEM

    /** Vigencia de la cache por tipo de dato (spec 001, T-018; valores por omision de Q-06). */
    @Provides
    @Singleton
    fun provideCachePolicy(clock: Clock): CachePolicy = CachePolicy(clock)
}
