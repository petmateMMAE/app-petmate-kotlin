package mx.petmate.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.data.repository.DashboardRepositoryImpl
import mx.petmate.app.domain.repository.DashboardRepository
import javax.inject.Singleton

/** Panel del afiliado (spec 012, T-007). */
@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
}
