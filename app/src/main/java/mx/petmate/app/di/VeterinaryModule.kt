package mx.petmate.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.data.remote.VeterinaryApiService
import mx.petmate.app.data.repository.VeterinaryRepositoryImpl
import mx.petmate.app.domain.repository.VeterinaryRepository
import retrofit2.Retrofit
import javax.inject.Singleton

/** Consultas veterinarias, historial clinico y seguimiento (backend 0.4.0). */
@Module
@InstallIn(SingletonComponent::class)
abstract class VeterinaryModule {

    @Binds
    @Singleton
    abstract fun bindVeterinaryRepository(impl: VeterinaryRepositoryImpl): VeterinaryRepository

    companion object {
        @Provides
        @Singleton
        fun provideVeterinaryApiService(retrofit: Retrofit): VeterinaryApiService =
            retrofit.create(VeterinaryApiService::class.java)
    }
}
