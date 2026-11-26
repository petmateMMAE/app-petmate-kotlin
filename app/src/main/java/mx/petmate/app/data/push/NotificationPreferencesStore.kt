package mx.petmate.app.data.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mx.petmate.app.domain.push.NotificationCategory
import mx.petmate.app.domain.push.NotificationPreferences
import java.io.IOException

/**
 * Preferencias de notificacion guardadas en el telefono (spec 006, T-007, R-03). Sobreviven al cierre de la
 * app; el interruptor cambia de inmediato porque [observe] emite en cuanto se escribe. Sincronizarlas con el
 * servidor (T-008) espera a que Miguel defina el endpoint (Q-02).
 */
class NotificationPreferencesStore(private val dataStore: DataStore<Preferences>) {

    fun observe(): Flow<NotificationPreferences> = dataStore.data
        // Un archivo danado no debe tumbar la app: se usan los valores por defecto.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { read(it) }

    suspend fun current(): NotificationPreferences = observe().first()

    /** Enciende o apaga [category] y devuelve como quedaron. Seguridad no se apaga (A-09). */
    suspend fun set(category: NotificationCategory, on: Boolean): NotificationPreferences {
        var saved = NotificationPreferences.DEFAULT
        dataStore.edit { prefs ->
            saved = read(prefs).with(category, on)
            prefs[ENABLED] = saved.enabledCategories.map { it.key }.toSet()
        }
        return saved
    }

    /** Vuelve a los valores por defecto; lo usa la limpieza local de 017 (T-009). */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun read(prefs: Preferences): NotificationPreferences =
        prefs[ENABLED]
            ?.mapNotNull(NotificationCategory::fromKey)
            ?.toSet()
            ?.let(NotificationPreferences::of)
            ?: NotificationPreferences.DEFAULT

    private companion object {
        val ENABLED = stringSetPreferencesKey("enabled_categories")
    }
}
