package mx.petmate.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.data.repository.SettingsRepositoryImpl
import mx.petmate.app.domain.repository.SettingsRepository
import javax.inject.Singleton

/** Ajustes reversibles del usuario (spec 017, T-005). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
