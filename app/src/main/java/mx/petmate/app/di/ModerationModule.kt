package mx.petmate.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.data.moderation.NoOpModerationGateway
import mx.petmate.app.domain.moderation.ModerationGateway
import javax.inject.Singleton

/**
 * Punto unico de moderacion (spec 001, T-037; constitucion P13).
 *
 * Hoy apunta al no-op. Cuando 004/T-002 tenga la ruta del backend (Q-01), se
 * cambia este `@Binds` por la implementacion real y no hay que tocar a nadie
 * mas: ese es el sentido de que el contrato viva en `domain`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModerationModule {

    @Binds
    @Singleton
    abstract fun bindModerationGateway(impl: NoOpModerationGateway): ModerationGateway
}
