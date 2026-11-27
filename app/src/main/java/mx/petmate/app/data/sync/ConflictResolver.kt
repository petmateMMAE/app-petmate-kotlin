package mx.petmate.app.data.sync

import mx.petmate.app.domain.sync.ConflictRule
import mx.petmate.app.domain.sync.SyncEntityType
import mx.petmate.app.domain.sync.SyncPolicy

/**
 * Lo que se avisa al usuario cuando su cambio no fue el que quedó (spec 005, T-006, R-07).
 *
 * Es un dato, no un texto: el mensaje lo elige la interfaz desde `strings.xml`. Si aquí viniera la
 * frase en español, la pantalla no podría traducirla ni darle formato.
 */
data class ConflictNotice(
    val type: SyncEntityType,
    val entityId: String,
    val rule: ConflictRule,
)

/** Qué versión queda y si hay que avisar. */
data class ConflictOutcome<T>(
    val winner: T,
    /** Cierto si lo que quedó es la versión del servidor y el usuario pierde su cambio. */
    val serverWon: Boolean,
    val notice: ConflictNotice?,
)

/**
 * Decide qué versión gana cuando el servidor y el teléfono cambiaron lo mismo (spec 005, T-006).
 *
 * Dos reglas, y la diferencia no es técnica sino de **quién puede equivocarse menos**:
 *
 * - **Datos clínicos** (vacunas, desparasitaciones, pesos, alergias): gana el servidor, siempre, y
 *   se avisa (R-07). Los registra una veterinaria, y el teléfono del dueño puede llevar días sin
 *   conexión. Dejar ganar al teléfono borraría lo que escribió un profesional.
 * - **La mascota** (nombre, raza, foto): gana la marca más reciente (R-08). Son datos del dueño y
 *   el último que la tocó sabe mejor cómo se llama su perro.
 *
 * Respuesta de Adrián del 27/11/2026 a Q-02, Q-05 y Q-07: **el servidor devuelve `updatedAt` pero
 * no una versión**, así que la reconciliación va por `updatedAt`.
 *
 * Por qué eso obliga a decidir los empates a propósito: `updatedAt` llega del servidor en segundos,
 * así que dos cambios del mismo segundo traen la misma marca. Con marcas iguales **gana el
 * servidor**, no el teléfono. Es la elección conservadora: ante la duda, no se pierde lo que ya
 * estaba guardado en el servidor. Si se decidiera al revés, un empate dejaría ganar a un teléfono
 * cuyo reloj puede estar mal puesto.
 */
class ConflictResolver {

    /**
     * Resuelve el conflicto de [type] entre [local] y [server].
     *
     * [localUpdatedAt] y [serverUpdatedAt] son marcas ISO-8601 en UTC, como las manda el backend;
     * se comparan como texto, que con ese formato equivale a comparar la fecha. Una marca local
     * nula significa "el teléfono no sabe cuándo se cambió": gana el servidor.
     */
    fun <T> resolve(
        type: SyncEntityType,
        local: T,
        server: T,
        localUpdatedAt: String?,
        serverUpdatedAt: String?,
        entityId: String,
    ): ConflictOutcome<T> = when (SyncPolicy.rulesFor(type).conflict) {
        ConflictRule.ServerWins -> ConflictOutcome(
            winner = server,
            serverWon = true,
            notice = ConflictNotice(type, entityId, ConflictRule.ServerWins),
        )

        ConflictRule.LatestWins -> {
            val ganaLocal = esMasReciente(localUpdatedAt, serverUpdatedAt)
            ConflictOutcome(
                winner = if (ganaLocal) local else server,
                serverWon = !ganaLocal,
                // Solo se avisa si el usuario pierde su cambio. Avisar cuando gana sería ruido.
                notice = if (ganaLocal) null else ConflictNotice(type, entityId, ConflictRule.LatestWins),
            )
        }
    }

    /**
     * Si la marca local es **estrictamente** más reciente que la del servidor.
     *
     * Empate y marca local nula devuelven false a propósito: ante la duda gana el servidor.
     */
    fun esMasReciente(localUpdatedAt: String?, serverUpdatedAt: String?): Boolean {
        if (localUpdatedAt == null) return false
        if (serverUpdatedAt == null) return true
        return localUpdatedAt > serverUpdatedAt
    }
}
