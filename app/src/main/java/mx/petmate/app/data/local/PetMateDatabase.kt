package mx.petmate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import mx.petmate.app.data.local.converter.DateConverters
import mx.petmate.app.data.local.dao.AffiliationDraftDao
import mx.petmate.app.data.local.dao.ConsultationDraftDao
import mx.petmate.app.data.local.dao.DashboardDao
import mx.petmate.app.data.local.dao.GeneratedTextDao
import mx.petmate.app.data.local.dao.MessageDao
import mx.petmate.app.data.local.dao.ProbeDao
import mx.petmate.app.data.local.entity.AffiliationDraftEntity
import mx.petmate.app.data.local.entity.AttendedPetEntity
import mx.petmate.app.data.local.entity.ConsultationDraftEntity
import mx.petmate.app.data.local.entity.DashboardAlertEntity
import mx.petmate.app.data.local.entity.DashboardEntity
import mx.petmate.app.data.local.entity.GeneratedTextEntity
import mx.petmate.app.data.local.entity.MessageEntity
import mx.petmate.app.data.local.entity.ProbeEntity
import mx.petmate.app.data.sync.SyncOperationDao
import mx.petmate.app.data.sync.SyncOperationEntity
import mx.petmate.app.data.sync.TombstoneDao
import mx.petmate.app.data.sync.TombstoneEntity

/**
 * Base local de la app: la fuente de verdad de la UI (constitucion P4; spec 001, K-10).
 *
 * `exportSchema = false` mientras no haya una version publicada con la base: el primer cambio de esquema
 * despues de publicar exige migracion y exportar el esquema (constitucion §4). La version publicada en la prueba
 * cerrada (versionCode 3) no trae base, asi que la "version anterior" de 007/T-002 y 008/T-002 es "sin base":
 * Room crea todas las tablas al instalar o actualizar, y no hay migracion que probar todavia.
 */
@Database(
    entities = [
        ProbeEntity::class,
        SyncOperationEntity::class,
        TombstoneEntity::class,
        PetEntity::class,
        VaccineEntity::class,
        DewormingEntity::class,
        WeightEntity::class,
        AllergyEntity::class,
        CarnetSyncEntity::class,
        FollowUpEntity::class,
        GeneratedTextEntity::class,
        AffiliationDraftEntity::class,
        DashboardEntity::class,
        DashboardAlertEntity::class,
        AttendedPetEntity::class,
        ConsultationDraftEntity::class,
        MessageEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(DateConverters::class)
abstract class PetMateDatabase : RoomDatabase() {

    abstract fun probeDao(): ProbeDao

    /** Cola de sincronizacion offline (spec 005, T-002). */
    abstract fun syncOperationDao(): SyncOperationDao

    /** Lapidas de borrado offline (spec 005, T-007). */
    abstract fun tombstoneDao(): TombstoneDao

    /** Mascotas del dueno (spec 007, T-002). */
    abstract fun petDao(): PetDao

    /** Carnet de salud (spec 008, T-002). */
    abstract fun healthDao(): HealthDao

    /** Seguimientos para leerse sin conexion (spec 015, T-007). */
    abstract fun followUpDao(): FollowUpDao

    /** Cache de textos generados por IA (spec 019, T-004). */
    abstract fun generatedTextDao(): GeneratedTextDao

    /** Borrador de afiliacion (spec 011, T-006). */
    abstract fun affiliationDraftDao(): AffiliationDraftDao

    /** Cache del panel del afiliado (spec 012, T-006). */
    abstract fun dashboardDao(): DashboardDao

    /** Borrador de consulta a medio capturar (spec 014, T-004). */
    abstract fun consultationDraftDao(): ConsultationDraftDao

    /** Historial del chat de seguimiento (spec 016, T-004). */
    abstract fun messageDao(): MessageDao

    companion object {
        const val NAME = "petmate.db"

        /**
         * De la version 1 a la 2: borrador de afiliacion (011/T-006) y cache del
         * panel (012/T-006).
         *
         * Solo crea tablas nuevas; ninguna tabla anterior cambia, asi que no hay
         * datos que mover y nada que se pueda perder. Se escribe la migracion en
         * lugar de dejar que Room recree la base, para no borrarle las mascotas
         * ni el carnet a quien ya tenga la version 1 instalada.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `affiliation_drafts` (" +
                        "`ownerId` TEXT NOT NULL, `kind` TEXT, `name` TEXT NOT NULL, " +
                        "`phone` TEXT NOT NULL, `whatsApp` TEXT NOT NULL, `address` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, `licenseNumber` TEXT NOT NULL, " +
                        "`scheduleJson` TEXT NOT NULL, `servicesJson` TEXT NOT NULL, " +
                        "`latitude` REAL, `longitude` REAL, `step` TEXT NOT NULL, " +
                        "`savedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`ownerId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dashboard` (" +
                        "`ownerId` TEXT NOT NULL, `businessId` TEXT NOT NULL, " +
                        "`businessName` TEXT NOT NULL, `kind` TEXT, `photoUrl` TEXT, " +
                        "`overdue` INTEGER NOT NULL, `soon` INTEGER NOT NULL, " +
                        "`unreadMessages` INTEGER NOT NULL, `syncedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`ownerId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dashboard_alerts` (" +
                        "`ownerId` TEXT NOT NULL, `followupId` TEXT NOT NULL, `petId` TEXT NOT NULL, " +
                        "`petName` TEXT NOT NULL, `nextReviewOn` TEXT, `severity` TEXT NOT NULL, " +
                        "`unreadMessages` INTEGER NOT NULL, PRIMARY KEY(`ownerId`, `followupId`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_dashboard_alerts_ownerId_nextReviewOn` " +
                        "ON `dashboard_alerts` (`ownerId`, `nextReviewOn`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dashboard_attended` (" +
                        "`ownerId` TEXT NOT NULL, `name` TEXT NOT NULL, `scannedAt` TEXT NOT NULL, " +
                        "`scannedOn` TEXT, `petId` TEXT, PRIMARY KEY(`ownerId`, `scannedAt`, `name`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_dashboard_attended_ownerId_scannedOn` " +
                        "ON `dashboard_attended` (`ownerId`, `scannedOn`)",
                )
            }
        }

        /**
         * De la version 2 a la 3: borrador de consulta (014/T-004).
         *
         * Igual que la anterior, solo crea una tabla nueva: ninguna columna existente cambia, asi
         * que no hay datos que mover y no se le borra el carnet a nadie. La llave es
         * (ownerId, petId) porque una veterinaria atiende varias mascotas la misma manana y cada
         * una conserva su avance.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `consultation_drafts` (" +
                        "`ownerId` TEXT NOT NULL, `petId` TEXT NOT NULL, `reason` TEXT NOT NULL, " +
                        "`diagnosis` TEXT NOT NULL, `weightKg` REAL, " +
                        "`medicationsJson` TEXT NOT NULL, `savedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`ownerId`, `petId`))",
                )
            }
        }

        /**
         * Lapidas de borrado offline (spec 005, T-007).
         *
         * Solo crea una tabla nueva: no toca ni mueve datos existentes, asi que no hay nada que
         * respaldar ni que convertir. `confirmed` entra con DEFAULT 0 porque las filas que
         * existieran en una base vieja no las hay (la tabla nace aqui), pero el DEFAULT es lo que
         * permite insertar sin nombrar la columna.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_tombstones` (" +
                        "`entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, " +
                        "`deletedAtMillis` INTEGER NOT NULL, " +
                        "`confirmed` INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`entityType`, `entityId`))",
                )
            }
        }

        /**
         * Historial del chat de seguimiento (spec 016, T-004). Autorizada por Adrian el 27/11/2026.
         *
         * Solo crea la tabla `messages` y su indice: **ninguna columna existente cambia**, asi que a
         * quien ya tenga la version 4 instalada no se le borra ni se le mueve nada.
         *
         * La llave lleva `ownerId` porque toda consulta de Room filtra por la cuenta (C-14), y el
         * indice por `(ownerId, conversationId, seq)` es el que sirve la conversacion ordenada. La
         * foto no tiene columna: solo `hasPhoto`, porque la URL viene firmada y expira (D-05).
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `messages` (" +
                        "`ownerId` TEXT NOT NULL, `id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, " +
                        "`seq` INTEGER NOT NULL, `kind` TEXT NOT NULL, `isMine` INTEGER NOT NULL, " +
                        "`text` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, " +
                        "`hasPhoto` INTEGER NOT NULL, `status` TEXT NOT NULL, `localId` TEXT, " +
                        "PRIMARY KEY(`ownerId`, `id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_messages_ownerId_conversationId_seq` " +
                        "ON `messages` (`ownerId`, `conversationId`, `seq`)",
                )
            }
        }
    }
}
