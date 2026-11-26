package mx.petmate.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.local.FollowUpDao
import mx.petmate.app.data.local.HealthDao
import mx.petmate.app.data.local.PetDao
import mx.petmate.app.data.local.PetMateDatabase
import mx.petmate.app.data.local.dao.ProbeDao
import mx.petmate.app.data.sync.SyncOperationDao
import mx.petmate.app.data.sync.SyncOutbox
import mx.petmate.app.data.sync.SyncStatusRepositoryImpl
import mx.petmate.app.domain.sync.SyncStatusRepository
import javax.inject.Singleton

/** Base Room unica de la app y sus DAOs (spec 001, T-011). */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PetMateDatabase =
        Room.databaseBuilder(context, PetMateDatabase::class.java, PetMateDatabase.NAME).build()

    @Provides
    fun provideProbeDao(database: PetMateDatabase): ProbeDao = database.probeDao()

    @Provides
    fun providePetDao(database: PetMateDatabase): PetDao = database.petDao()

    @Provides
    fun provideHealthDao(database: PetMateDatabase): HealthDao = database.healthDao()

    @Provides
    fun provideFollowUpDao(database: PetMateDatabase): FollowUpDao = database.followUpDao()

    @Provides
    fun provideSyncOperationDao(database: PetMateDatabase): SyncOperationDao = database.syncOperationDao()

    /** Una sola cola para toda la app (spec 005, plan D-02). */
    @Provides
    @Singleton
    fun provideSyncOutbox(dao: SyncOperationDao, dispatchers: DispatcherProvider): SyncOutbox =
        SyncOutbox(dao, dispatchers)

    /** Estado de sincronizacion para la UI, sin exponer la cola (spec 005, T-008). */
    @Provides
    @Singleton
    fun provideSyncStatusRepository(outbox: SyncOutbox): SyncStatusRepository = SyncStatusRepositoryImpl(outbox)
}
