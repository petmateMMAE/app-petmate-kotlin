package mx.petmate.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.local.FollowUpDao
import mx.petmate.app.data.local.HealthDao
import mx.petmate.app.data.local.PetDao
import mx.petmate.app.data.local.PetMateDatabase
import mx.petmate.app.data.local.dao.AffiliationDraftDao
import mx.petmate.app.data.local.dao.ConsultationDraftDao
import mx.petmate.app.data.local.dao.DashboardDao
import mx.petmate.app.data.local.dao.MessageDao
import mx.petmate.app.data.local.dao.ProbeDao
import mx.petmate.app.data.sync.SyncOperationDao
import mx.petmate.app.data.sync.SyncOutbox
import mx.petmate.app.data.sync.SyncStatusRepositoryImpl
import mx.petmate.app.data.sync.TombstoneDao
import mx.petmate.app.data.sync.TombstoneStore
import mx.petmate.app.domain.sync.SyncStatusRepository

/** Base Room unica de la app y sus DAOs (spec 001, T-011). */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PetMateDatabase =
        Room.databaseBuilder(context, PetMateDatabase::class.java, PetMateDatabase.NAME)
            .addMigrations(
                PetMateDatabase.MIGRATION_1_2,
                PetMateDatabase.MIGRATION_2_3,
                PetMateDatabase.MIGRATION_3_4,
                PetMateDatabase.MIGRATION_4_5,
            )
            .build()

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

    /** Borrador de afiliacion (spec 011, T-006). */
    @Provides
    fun provideAffiliationDraftDao(database: PetMateDatabase): AffiliationDraftDao = database.affiliationDraftDao()

    /** Cache del panel del afiliado (spec 012, T-006). */
    @Provides
    fun provideDashboardDao(database: PetMateDatabase): DashboardDao = database.dashboardDao()

    /** Borrador de consulta a medio capturar (spec 014, T-004). */
    @Provides
    fun provideConsultationDraftDao(database: PetMateDatabase): ConsultationDraftDao =
        database.consultationDraftDao()

    /** Historial del chat de seguimiento (spec 016, T-004). */
    @Provides
    fun provideMessageDao(database: PetMateDatabase): MessageDao = database.messageDao()

    /** Una sola cola para toda la app (spec 005, plan D-02). */
    @Provides
    @Singleton
    fun provideSyncOutbox(dao: SyncOperationDao, dispatchers: DispatcherProvider): SyncOutbox =
        SyncOutbox(dao, dispatchers)

    /** Lapidas de borrado offline (spec 005, T-007). */
    @Provides
    @Singleton
    fun provideTombstoneDao(database: PetMateDatabase): TombstoneDao = database.tombstoneDao()

    @Provides
    @Singleton
    fun provideTombstoneStore(dao: TombstoneDao, dispatchers: DispatcherProvider): TombstoneStore =
        TombstoneStore(dao, dispatchers)

    /** Estado de sincronizacion para la UI, sin exponer la cola (spec 005, T-008). */
    @Provides
    @Singleton
    fun provideSyncStatusRepository(outbox: SyncOutbox): SyncStatusRepository = SyncStatusRepositoryImpl(outbox)
}
