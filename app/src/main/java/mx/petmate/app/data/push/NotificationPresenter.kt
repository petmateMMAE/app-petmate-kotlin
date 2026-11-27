package mx.petmate.app.data.push

import androidx.annotation.StringRes
import mx.petmate.app.R
import mx.petmate.app.domain.push.NotificationCategory

/**
 * Lo que se muestra de una notificacion (spec 006, T-004, R-04 y C-04).
 *
 * Son identificadores de recurso, no texto: el texto sale de `strings.xml` y lo
 * resuelve quien la pinta. Asi es imposible que un campo del payload acabe en
 * la notificacion, porque aqui no viaja ningun texto del servidor.
 *
 * [targetId] es lo unico que se guarda del payload: con eso la app carga el
 * detalle **al abrir** la notificacion, en lugar de meterlo en el aviso.
 */
data class PushNotification(
    val category: NotificationCategory,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val targetId: String?,
)

/**
 * Traduce el payload de FCM a lo que se muestra (spec 006, T-004).
 *
 * **El payload no se muestra crudo nunca.** Cada categoria tiene su texto fijo
 * en `strings.xml`; del payload solo se leen tres claves tecnicas
 * ([ALLOWED_KEYS]) y todo lo demas se ignora. Es a proposito: el backend manda
 * datos clinicos en el payload (diagnostico, receta, notas del seguimiento) y
 * esos no pueden aparecer en la pantalla de bloqueo, donde los ve cualquiera
 * que levante el telefono.
 *
 * Es logica pura, sin Android: se prueba en la JVM.
 */
object NotificationPresenter {

    /** Clave del payload que dice de que categoria es. */
    const val KEY_CATEGORY = "category"

    /** Clave del payload con el identificador para abrir el detalle. */
    const val KEY_TARGET_ID = "targetId"

    /** Clave del payload para no repetir una notificacion ya mostrada (T-005). */
    const val KEY_EVENT_KEY = "eventKey"

    /** Lo unico que se lee del payload. Cualquier otra clave se descarta. */
    val ALLOWED_KEYS = setOf(KEY_CATEGORY, KEY_TARGET_ID, KEY_EVENT_KEY)

    /**
     * Notificacion a mostrar, o null si el payload no trae una categoria que se
     * reconozca.
     *
     * Se devuelve null a proposito en lugar de inventar un texto generico: una
     * categoria desconocida puede ser de una version mas nueva de la app, y es
     * mejor no mostrar nada que mostrar un aviso que no lleva a ningun lado.
     */
    fun present(payload: Map<String, String>): PushNotification? {
        val category = NotificationCategory.fromKey(payload[KEY_CATEGORY]?.trim().orEmpty()) ?: return null
        return PushNotification(
            category = category,
            titleRes = titleOf(category),
            bodyRes = bodyOf(category),
            targetId = payload[KEY_TARGET_ID]?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    /** Clave de deduplicacion del payload, si viene (la usa T-005). */
    fun eventKeyOf(payload: Map<String, String>): String? =
        payload[KEY_EVENT_KEY]?.trim()?.takeIf { it.isNotEmpty() }

    @StringRes
    fun titleOf(category: NotificationCategory): Int = when (category) {
        NotificationCategory.Health -> R.string.notifications_health_title
        NotificationCategory.FollowUp -> R.string.notifications_followup_title
        NotificationCategory.Chat -> R.string.notifications_chat_title
        NotificationCategory.News -> R.string.notifications_news_title
        NotificationCategory.Security -> R.string.notifications_security_title
    }

    @StringRes
    fun bodyOf(category: NotificationCategory): Int = when (category) {
        NotificationCategory.Health -> R.string.notifications_health_body
        NotificationCategory.FollowUp -> R.string.notifications_followup_body
        NotificationCategory.Chat -> R.string.notifications_chat_body
        NotificationCategory.News -> R.string.notifications_news_body
        NotificationCategory.Security -> R.string.notifications_security_body
    }
}
