package mx.petmate.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.local.FollowUpDao
import mx.petmate.app.data.remote.VeterinaryApiService
import mx.petmate.app.data.repository.OfflineFirstFollowUpRepository
import mx.petmate.app.data.repository.VeterinaryRepositoryImpl
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.repository.AuthRepository
import mx.petmate.app.domain.repository.VeterinaryRepository
import retrofit2.Retrofit
import javax.inject.Singleton

/** Consultas veterinarias, historial clinico y seguimiento (backend 0.4.0). */
@Module
@InstallIn(SingletonComponent::class)
object VeterinaryModule {

    @Provides
    @Singleton
    fun provideVeterinaryApiService(retrofit: Retrofit): VeterinaryApiService =
        retrofit.create(VeterinaryApiService::class.java)

    /** El backend de 014 con el seguimiento guardado en Room para leerse sin conexion (spec 015, T-008). */
    @Provides
    @Singleton
    fun provideVeterinaryRepository(
        impl: VeterinaryRepositoryImpl,
        followUpDao: FollowUpDao,
        json: Json,
        authRepository: AuthRepository,
        dispatchers: DispatcherProvider,
    ): VeterinaryRepository {
        val session = authRepository.sessionState.stateIn(
            scope = CoroutineScope(SupervisorJob() + dispatchers.default),
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Checking,
        )
        return OfflineFirstFollowUpRepository(
            remote = impl,
            dao = followUpDao,
            json = json,
            currentUserId = { (session.value as? SessionState.SignedIn)?.user?.id },
            dispatchers = dispatchers,
        )
    }
}
