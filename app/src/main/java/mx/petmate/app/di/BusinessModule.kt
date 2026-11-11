package mx.petmate.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.data.remote.BusinessApiService
import mx.petmate.app.data.repository.BusinessRepositoryImpl
import mx.petmate.app.domain.repository.BusinessRepository
import retrofit2.Retrofit
import javax.inject.Singleton

/** Negocio afiliado, mapa publico, autorizaciones y escaneo (backend 0.4.0). */
@Module
@InstallIn(SingletonComponent::class)
abstract class BusinessModule {

    @Binds
    @Singleton
    abstract fun bindBusinessRepository(impl: BusinessRepositoryImpl): BusinessRepository

    companion object {
        @Provides
        @Singleton
        fun provideBusinessApiService(retrofit: Retrofit): BusinessApiService = retrofit.create(BusinessApiService::class.java)
    }
}
