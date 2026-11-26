package mx.petmate.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.core.permissions.AndroidPermissionGateway
import mx.petmate.app.domain.permissions.PermissionGateway
import javax.inject.Singleton

/** Gateway de permisos unico para toda la app (spec 003, T-002). */
@Module
@InstallIn(SingletonComponent::class)
object PermissionsModule {

    @Provides
    @Singleton
    fun providePermissionGateway(@ApplicationContext context: Context): PermissionGateway =
        AndroidPermissionGateway(context)
}

/** Acceso al gateway desde un Composable que no tiene ViewModel propio (`rememberPermissionRequest`). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PermissionsEntryPoint {
    fun permissionGateway(): PermissionGateway
}
