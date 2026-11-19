package mx.petmate.app.domain.usecase

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.data.account.DeviceDataWiper
import mx.petmate.app.data.local.CarnetRows
import mx.petmate.app.data.local.PetEntity
import mx.petmate.app.data.local.PetMateDatabase
import mx.petmate.app.data.local.VaccineEntity
import mx.petmate.app.data.push.NotificationPreferencesStore
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tras la limpieza no queda nada del usuario en el telefono (spec 017, T-009, C-10, R-11).
 *
 * Revisa **todas** las tablas que existan en SQLite, no una lista fija: si alguien agrega una tabla nueva, esta
 * prueba la cubre sola y falla si la limpieza no la vacia.
 */
@RunWith(AndroidJUnit4::class)
class WipeLocalDataUseCaseTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dispatchers = object : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
        override val main = Dispatchers.Main
    }
    private val silent = object : Logger {
        override fun debug(tag: String, message: String) = Unit
        override fun error(tag: String, message: String, throwable: Throwable?) = Unit
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: PetMateDatabase
    private lateinit var preferences: NotificationPreferencesStore
    private lateinit var session: SessionStore
    private lateinit var prefsFile: File

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PetMateDatabase::class.java).build()
        prefsFile = File(context.cacheDir, "wipe-test-${System.nanoTime()}.preferences_pb")
        preferences = NotificationPreferencesStore(PreferenceDataStoreFactory.create(scope = scope) { prefsFile })
        session = SessionStore(context, silent)
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
        prefsFile.delete()
        session.clear()
    }

    private fun userTables(): List<String> = database.openHelper.readableDatabase
        .query("SELECT name FROM sqlite_master WHERE type = 'table'")
        .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
        .filterNot { it.startsWith("sqlite_") || it == "android_metadata" || it == "room_master_table" }

    private fun rows(table: String): Int = database.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM `$table`")
        .use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }

    @Test
    fun la_limpieza_vacia_todas_las_tablas_las_preferencias_y_la_sesion() = runBlocking {
        database.petDao().upsert(PetEntity("ana", "p-1", "Luna", "Perro", null, null, "Female", syncedAtMillis = 1L))
        val vaccine = VaccineEntity("ana", "p-1", "v-1", "Rabia", "2026-10-01", null, null, null, false, null)
        val carnet = CarnetRows(listOf(vaccine), emptyList(), emptyList(), emptyList())
        database.healthDao().replaceCarnet("ana", "p-1", carnet, 1L)
        preferences.set(NotificationCategory.News, on = true)
        session.save(User("ana", "Ana", "ana@correo.com"), "access", "refresh")
        assertTrue(userTables().any { rows(it) > 0 })

        WipeLocalDataUseCase(DeviceDataWiper(database, preferences, session, dispatchers))()

        userTables().forEach { assertEquals("tabla $it", 0, rows(it)) }
        assertEquals(NotificationPreferences.DEFAULT, preferences.current())
        assertNull(session.readUser())
        assertNull(session.readAccessToken())
    }
}
