package mx.petmate.app.data.cache

import mx.petmate.app.core.time.Clock

// Las duraciones van a nivel de archivo y no en el companion de `CachedData` a proposito: las
// entradas del enum las usan en su constructor, y referenciar el companion desde ahi es un orden de
// inicializacion que conviene no tentar. Son `const`, asi que se compilan en linea.
private const val MINUTO = 60 * 1_000L
private const val CINCO_MINUTOS = 5 * MINUTO
private const val DIEZ_MINUTOS = 10 * MINUTO
private const val QUINCE_MINUTOS = 15 * MINUTO
private const val UNA_HORA = 60 * MINUTO

/**
 * Tipos de dato que se guardan y cuánto vale cada uno (spec 001, T-018).
 *
 * **La duración la decide quién puede cambiar el dato sin que la app se entere, no su tamaño.** Un
 * dato que solo cambia el dueño desde este teléfono puede vivir horas; uno que una veterinaria
 * puede cambiar en cualquier momento tiene que revisarse seguido, porque si no el dueño ve un
 * carnet viejo y cree que no le registraron la vacuna.
 *
 * Son los valores por omisión que Q-06 pide proponer, con su motivo. **Si el equipo quiere otros,
 * se cambian aquí y en ningún otro lado.**
 */
enum class CachedData(val vigenciaMillis: Long) {

    /**
     * Lista de mascotas y cada mascota. Solo las cambia su dueño, desde la app.
     * Una hora: si el dueño edita desde otro teléfono, tarda a lo más eso en verse.
     */
    Pets(UNA_HORA),

    /**
     * Carnet de salud: vacunas, desparasitaciones, pesos, alergias.
     * **Cinco minutos.** Es el más corto de todos y es el que importa: una veterinaria puede
     * registrar una vacuna mientras el dueño está en el mostrador mirando la pantalla.
     */
    Carnet(CINCO_MINUTOS),

    /**
     * Seguimiento de tratamiento (spec 015). Lo cambia la veterinaria.
     * Cinco minutos, por lo mismo que el carnet.
     */
    FollowUps(CINCO_MINUTOS),

    /**
     * Negocios cercanos del mapa (spec 010). Cambia al moverse, no con el tiempo.
     * Diez minutos: el disparador de verdad es que el usuario se mueva, y de eso se encarga
     * `MapViewModel` volviendo a consultar cuando cambia el centro.
     */
    NearbyBusinesses(DIEZ_MINUTOS),

    /**
     * Perfil público de un negocio: horario, teléfono, servicios.
     * Una hora: un negocio no cambia su horario varias veces al día.
     */
    BusinessProfile(UNA_HORA),

    /** Perfil del usuario. Lo cambia él mismo y la app ya refresca al guardar. */
    Profile(UNA_HORA),

    /**
     * Historial unificado de la mascota (spec 014).
     * Quince minutos: lo escriben negocios, pero no es lo que el dueño mira en el mostrador.
     */
    History(QUINCE_MINUTOS),
}

/** Qué hacer con un dato guardado. */
enum class CacheDecision {
    /** Está vigente: se sirve lo guardado y no se toca la red. */
    ServeCache,

    /** Venció o no hay nada: hay que pedirlo. */
    Refresh,
}

/**
 * Decide si un dato guardado todavía sirve (spec 001, T-018, C-11).
 *
 * Lo que **no** hace, y conviene tenerlo claro: no decide si se muestra algo. La app es
 * offline-first (P4), así que la pantalla siempre pinta lo que hay en Room; esto solo decide **si
 * además se va a la red**. Un dato vencido se sigue mostrando mientras llega el nuevo, y si la red
 * falla, se queda el viejo. Si esto decidiera qué mostrar, un dato vencido dejaría la pantalla en
 * blanco sin conexión, que es justo lo que la arquitectura quiere evitar.
 */
class CachePolicy(private val clock: Clock) {

    /**
     * Si [storedAtMillis] (cuándo se guardó) sigue dentro de la vigencia de [data].
     *
     * `null` significa "nunca se guardó": no está vigente. Es distinto de "se guardó hace mucho",
     * aunque la decisión sea la misma, y por eso se acepta nulo en vez de obligar a quien llama a
     * inventar un 0.
     */
    fun isFresh(storedAtMillis: Long?, data: CachedData): Boolean {
        if (storedAtMillis == null) return false
        val edad = clock.nowMillis() - storedAtMillis
        // Una edad negativa significa que el dato dice haberse guardado en el futuro: pasa cuando
        // el usuario mueve el reloj del teléfono. Se trata como vencido, que es lo conservador:
        // volver a pedirlo cuesta una petición, y creerle dejaría el dato guardado para siempre.
        if (edad < 0) return false
        return edad < data.vigenciaMillis
    }

    /** La decisión de [data] según cuándo se guardó. */
    fun decide(storedAtMillis: Long?, data: CachedData): CacheDecision =
        if (isFresh(storedAtMillis, data)) CacheDecision.ServeCache else CacheDecision.Refresh

    /**
     * Cuántos milisegundos le quedan de vigencia, o 0 si ya venció.
     *
     * Sirve para no programar un refresco antes de que haga falta.
     */
    fun vigenciaRestante(storedAtMillis: Long?, data: CachedData): Long {
        if (storedAtMillis == null) return 0
        val restante = data.vigenciaMillis - (clock.nowMillis() - storedAtMillis)
        return restante.coerceAtLeast(0)
    }
}
