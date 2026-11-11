package mx.petmate.app.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda las credenciales de sesion cifradas en el dispositivo.
 *
 * Usa EncryptedSharedPreferences, respaldada por el Keystore del sistema
 * (constitution.md P6, spec 002 C-12).
 *
 * Si el cifrado falla al inicializarse, se degrada de forma controlada: se
 * registra y la app pide iniciar sesion de nuevo, en lugar de caer o de
 * guardar en texto claro (spec 002, G-05).
 */
@Singleton
class SessionStore @Inject constructor(
    private val context: Context,
    private val logger: Logger,
) {

    private val prefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ) as SharedPreferences
        }.onFailure {
            logger.error(TAG, "No se pudo abrir el almacenamiento cifrado", it)
        }.getOrNull()
    }

    fun save(user: User, accessToken: String, refreshToken: String) {
        prefs?.edit()
            ?.putString(KEY_USER_ID, user.id)
            ?.putString(KEY_NAME, user.name)
            ?.putString(KEY_EMAIL, user.email)
            ?.putBoolean(KEY_AFFILIATE, user.isAffiliate)
            ?.putString(KEY_BUSINESS_TYPE, user.businessType)
            ?.putString(KEY_ACCESS_TOKEN, accessToken)
            ?.putString(KEY_REFRESH_TOKEN, refreshToken)
            ?.apply()
    }

    fun readUser(): User? {
        val store = prefs ?: return null
        val id = store.getString(KEY_USER_ID, null) ?: return null
        val token = store.getString(KEY_ACCESS_TOKEN, null) ?: return null
        if (token.isBlank()) return null
        return User(
            id = id,
            name = store.getString(KEY_NAME, "").orEmpty(),
            email = store.getString(KEY_EMAIL, "").orEmpty(),
            isAffiliate = store.getBoolean(KEY_AFFILIATE, false),
            businessType = store.getString(KEY_BUSINESS_TYPE, null),
        )
    }

    /** Actualiza los datos del usuario de la sesion sin tocar los tokens (p. ej. el nombre). */
    fun saveUser(user: User) {
        prefs?.edit()
            ?.putString(KEY_USER_ID, user.id)
            ?.putString(KEY_NAME, user.name)
            ?.putString(KEY_EMAIL, user.email)
            ?.putBoolean(KEY_AFFILIATE, user.isAffiliate)
            ?.putString(KEY_BUSINESS_TYPE, user.businessType)
            ?.apply()
    }

    fun readAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)

    fun readRefreshToken(): String? = prefs?.getString(KEY_REFRESH_TOKEN, null)

    /** Sustituye los tokens tras una renovacion (el refresh token rota en cada uso). */
    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs?.edit()
            ?.putString(KEY_ACCESS_TOKEN, accessToken)
            ?.putString(KEY_REFRESH_TOKEN, refreshToken)
            ?.apply()
    }

    /**
     * Nombre que el usuario escribio al registrarse con ese correo.
     *
     * El backend v0.1.0 no guarda el nombre (spec 002, Q-09): se conserva solo
     * aqui, cifrado, y sobrevive al cierre de sesion para mostrarlo al volver.
     */
    fun rememberName(email: String, name: String) {
        prefs?.edit()
            ?.putString(KEY_KNOWN_EMAIL, email.trim().lowercase())
            ?.putString(KEY_KNOWN_NAME, name.trim())
            ?.apply()
    }

    fun knownNameFor(email: String): String? {
        val store = prefs ?: return null
        val known = store.getString(KEY_KNOWN_EMAIL, null) ?: return null
        return if (known == email.trim().lowercase()) store.getString(KEY_KNOWN_NAME, null) else null
    }

    /**
     * Copia local del negocio propio (JSON). El backend no tiene "ver mi negocio": se guarda al afiliarlo o editarlo
     * para llenar el panel y el formulario de edicion.
     */
    fun saveBusinessJson(json: String?) {
        prefs?.edit()?.putString(KEY_BUSINESS_JSON, json)?.apply()
    }

    fun readBusinessJson(): String? = prefs?.getString(KEY_BUSINESS_JSON, null)

    /** Modo de la app para una cuenta con negocio: "owner" (mis mascotas) o "business" (mi negocio). */
    fun saveMode(mode: String) {
        prefs?.edit()?.putString(KEY_MODE, mode)?.apply()
    }

    fun readMode(): String? = prefs?.getString(KEY_MODE, null)

    /** Borra todo rastro de la sesion (tokens y usuario). Se llama siempre al cerrar sesion. */
    fun clear() {
        prefs?.edit()
            ?.remove(KEY_USER_ID)
            ?.remove(KEY_NAME)
            ?.remove(KEY_EMAIL)
            ?.remove(KEY_AFFILIATE)
            ?.remove(KEY_BUSINESS_TYPE)
            ?.remove(KEY_BUSINESS_JSON)
            ?.remove(KEY_MODE)
            ?.remove(KEY_ACCESS_TOKEN)
            ?.remove(KEY_REFRESH_TOKEN)
            ?.apply()
    }

    private companion object {
        const val TAG = "SessionStore"
        const val FILE_NAME = "petmate_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_AFFILIATE = "is_affiliate"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_KNOWN_EMAIL = "known_email"
        const val KEY_KNOWN_NAME = "known_name"
        const val KEY_BUSINESS_TYPE = "business_type"
        const val KEY_BUSINESS_JSON = "business_json"
        const val KEY_MODE = "mode"
    }
}
