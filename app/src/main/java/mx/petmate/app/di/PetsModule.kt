package mx.petmate.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import mx.petmate.app.BuildConfig
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.local.HealthDao
import mx.petmate.app.data.local.PetDao
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.PetsApiService
import mx.petmate.app.data.repository.OfflineFirstPetRepository
import mx.petmate.app.data.repository.PetRepositoryImpl
import mx.petmate.app.data.repository.PetRepositoryRouter
import mx.petmate.app.data.repository.SamplePetRepository
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.repository.AuthRepository
import mx.petmate.app.domain.repository.PetRepository
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Mascotas, QR y carnet: Room con el backend detras (offline-first), o datos de ejemplo en el modo demo y con el
 * backend simulado.
 */
@Module
@InstallIn(SingletonComponent::class)
object PetsModule {

    @Provides
    @Singleton
    fun providePetsApiService(retrofit: Retrofit): PetsApiService = retrofit.create(PetsApiService::class.java)

    @Provides
    @Singleton
    fun providePetRepository(
        api: PetsApiService,
        caller: AuthorizedCaller,
        authRepository: AuthRepository,
        dispatchers: DispatcherProvider,
        petDao: PetDao,
        healthDao: HealthDao,
    ): PetRepository {
        val sample = SamplePetRepository()
        if (BuildConfig.USE_FAKE_BACKEND) return sample
        val session = authRepository.sessionState.stateIn(
            scope = CoroutineScope(SupervisorJob() + dispatchers.default),
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Checking,
        )
        // Sesion real: Room es la fuente de verdad y el backend la actualiza (specs 007/T-004 y 008/T-005, P4).
        val offlineFirst = OfflineFirstPetRepository(
            remote = PetRepositoryImpl(api, caller, dispatchers),
            petDao = petDao,
            healthDao = healthDao,
            currentUserId = { (session.value as? SessionState.SignedIn)?.user?.id },
            dispatchers = dispatchers,
        )
        return PetRepositoryRouter(
            remote = offlineFirst,
            demo = sample,
            isGuest = { session.value == SessionState.Guest },
        )
    }
}
