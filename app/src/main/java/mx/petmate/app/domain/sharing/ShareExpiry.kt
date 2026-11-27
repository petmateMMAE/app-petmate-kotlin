package mx.petmate.app.domain.sharing

import mx.petmate.app.domain.model.AccessGrant
import mx.petmate.app.domain.model.ShareDuration

/**
 * Cuanto le queda de vida a una comparticion (spec 009, T-005, R-06 y R-07).
 *
 * **La marca de expiracion la manda el servidor** (`AccessGrant.expiresAt`): la
 * app no la calcula sumando la duracion al reloj del telefono. Si el telefono
 * tiene la hora mal, el QR se veria vivo cuando el servidor ya lo rechaza, o al
 * contrario. Por eso [remaining] recibe la marca del servidor y solo mide la
 * distancia hasta ella.
 *
 * El "ahora" se inyecta para poder probar con un reloj fijo (C-06, C-07).
 */
object ShareExpiry {

    /** Estado de una comparticion frente al tiempo. */
    sealed interface State {
        /** Sigue viva: faltan [millis] milisegundos. */
        data class Live(val millis: Long) : State {
            val totalSeconds: Long get() = millis / MILLIS_PER_SECOND
            val minutes: Long get() = totalSeconds / SECONDS_PER_MINUTE
            val seconds: Long get() = totalSeconds % SECONDS_PER_MINUTE
            val hours: Long get() = minutes / MINUTES_PER_HOUR
        }

        /** Llego a cero: el QR deja de mostrarse (R-07). */
        data object Expired : State

        /** La marca del servidor no se pudo leer: se trata como vencida, no se arriesga. */
        data object Unknown : State
    }

    /**
     * Estado de [expiresAt] (ISO-8601, como lo manda el backend) frente a [nowMillis].
     *
     * Una marca ilegible devuelve [State.Unknown] y la pantalla no muestra el
     * QR: es mas seguro no ensenarlo que ensenar uno que el servidor ya no
     * acepta.
     */
    fun remaining(expiresAt: String?, nowMillis: Long): State {
        val expiry = parseIso(expiresAt) ?: return State.Unknown
        val left = expiry - nowMillis
        return if (left > 0) State.Live(left) else State.Expired
    }

    /** Estado de una autorizacion ya emitida. */
    fun remaining(grant: AccessGrant, nowMillis: Long): State = remaining(grant.expiresAt, nowMillis)

    /** Si el QR se debe seguir mostrando. */
    fun isVisible(expiresAt: String?, nowMillis: Long): Boolean = remaining(expiresAt, nowMillis) is State.Live

    /** Las tres duraciones que acepta el backend, en el orden de la maqueta. */
    val durations: List<ShareDuration> = ShareDuration.entries.sortedBy(ShareDuration::minutes)

    /**
     * Lee una marca ISO-8601 a milisegundos.
     *
     * Se hace a mano y en UTC para no depender de `java.time` (minSdk 24) ni de
     * la zona del telefono. Acepta `2026-11-26T10:00:00Z`, con o sin
     * fracciones de segundo, y con desplazamiento `+HH:mm`.
     */
    private fun parseIso(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = ISO.matchEntire(text) ?: return null
        val parts = (1..6).map { match.groupValues[it].toIntOrNull() ?: return null }
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]
        val hour = parts[3]
        val minute = parts[4]
        val second = parts[5]
        if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59 || second !in 0..60) return null

        val offsetMinutes = offsetOf(match.groupValues[OFFSET_GROUP]) ?: return null
        val secondsUtc = daysFromEpoch(year, month, day) * SECONDS_PER_DAY +
            hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second
        return (secondsUtc - offsetMinutes * SECONDS_PER_MINUTE) * MILLIS_PER_SECOND
    }

    /** Minutos de desplazamiento: `Z` y vacio son 0. */
    private fun offsetOf(raw: String?): Long? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text.equals("Z", ignoreCase = true)) return 0
        val sign = if (text.startsWith("-")) -1 else 1
        val body = text.removePrefix("+").removePrefix("-").replace(":", "")
        if (body.length != 4) return null
        val hours = body.substring(0, 2).toIntOrNull() ?: return null
        val minutes = body.substring(2, 4).toIntOrNull() ?: return null
        return sign * (hours * MINUTES_PER_HOUR + minutes).toLong()
    }

    /** Dias desde 1970-01-01 (algoritmo de dias civiles, sin librerias). */
    private fun daysFromEpoch(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097L + doe - 719_468L
    }

    private val ISO = Regex(
        """^(\d{4})-(\d{2})-(\d{2})[Tt ](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?(Z|z|[+-]\d{2}:?\d{2})?$""",
    )

    private const val OFFSET_GROUP = 7
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60
    private const val SECONDS_PER_HOUR = 3600L
    private const val SECONDS_PER_DAY = 86_400L
}
