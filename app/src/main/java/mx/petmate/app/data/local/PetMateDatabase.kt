package mx.petmate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mx.petmate.app.data.local.converter.DateConverters
import mx.petmate.app.data.local.dao.GeneratedTextDao
import mx.petmate.app.data.local.dao.ProbeDao
import mx.petmate.app.data.local.entity.GeneratedTextEntity
import mx.petmate.app.data.local.entity.ProbeEntity
import mx.petmate.app.data.sync.SyncOperationDao
import mx.petmate.app.data.sync.SyncOperationEntity

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
        PetEntity::class,
        VaccineEntity::class,
        DewormingEntity::class,
        WeightEntity::class,
        AllergyEntity::class,
        CarnetSyncEntity::class,
        FollowUpEntity::class,
        GeneratedTextEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DateConverters::class)
abstract class PetMateDatabase : RoomDatabase() {

    abstract fun probeDao(): ProbeDao

    /** Cola de sincronizacion offline (spec 005, T-002). */
    abstract fun syncOperationDao(): SyncOperationDao

    /** Mascotas del dueno (spec 007, T-002). */
    abstract fun petDao(): PetDao

    /** Carnet de salud (spec 008, T-002). */
    abstract fun healthDao(): HealthDao

    /** Seguimientos para leerse sin conexion (spec 015, T-007). */
    abstract fun followUpDao(): FollowUpDao

    /** Cache de textos generados por IA (spec 019, T-004). */
    abstract fun generatedTextDao(): GeneratedTextDao

    companion object {
        const val NAME = "petmate.db"
    }
}
