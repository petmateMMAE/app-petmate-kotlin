package mx.petmate.app.data.sync

import mx.petmate.app.domain.sync.SyncOperation

/**
 * Clave de idempotencia de una operación encolada (spec 005, T-004, R-04).
 *
 * **Se deriva de la operación, no se sortea.** Un `UUID.randomUUID()` también sirve mientras la
 * fila viva en Room, porque la clave se guarda una vez y cada reintento manda la guardada. El
 * problema es el caso que importa: si la fila se pierde —base reinstalada, backup restaurado, un
 * borrado de datos a medias— y la app vuelve a encolar la misma escritura, un UUID nuevo hace que
 * el servidor la vea como otra operación y **la aplique dos veces**. Derivarla de la operación hace
 * que la segunda vez traiga la misma clave y el servidor la reconozca como repetida.
 *
 * Qué entra en la clave y por qué cada parte: el **id de cliente** de la entidad (es lo que pide la
 * aceptación: dos mascotas distintas nunca comparten clave), su **tipo** (un id puede repetirse
 * entre tablas), la **acción** (crear y borrar la misma mascota son dos operaciones, no una) y el
 * **instante en que se encoló** (dos pesos de la misma mascota son dos registros legítimos, y sin
 * el instante se verían como uno repetido y el segundo se perdería).
 *
 * No lleva nada del usuario ni del contenido: `payload` puede traer datos de la mascota y la clave
 * viaja en una cabecera HTTP, donde no debe ir nada que no haga falta (R-12).
 */
object SyncIdempotencyKey {

    /**
     * La clave de [operation]. La misma operación da siempre la misma clave.
     *
     * Formato: `petmate-<tipo>-<accion>-<id>-<instante>`, con el id saneado. Se deja legible a
     * propósito en vez de un hash: cuando una operación se queda atorada, poder leer su clave en el
     * registro del servidor y saber de qué era vale más que ahorrar unos caracteres.
     */
    fun of(operation: SyncOperation): String = listOf(
        PREFIJO,
        operation.target.type.name.lowercase(),
        operation.action.name.lowercase(),
        sanea(operation.target.id),
        operation.createdAtMillis.toString(),
    ).joinToString("-")

    /**
     * Deja solo lo que viaja sin sorpresas en una cabecera HTTP.
     *
     * Un id de cliente es nuestro, pero el día que uno venga del servidor con un acento o un
     * espacio, la cabecera se rompe y la petición falla por algo que no tiene nada que ver.
     *
     * El guion **sí** pasa, aunque también sea el separador de la clave: los ids del proyecto son
     * `p-1`, `v-3`, y cambiárselos aquí los volvería irreconocibles justo cuando alguien busca una
     * operación atorada en el registro del servidor. La clave no se vuelve a partir en sus piezas
     * en ningún lado —para el servidor es un token opaco—, así que el guion repetido no estorba.
     */
    private fun sanea(id: String): String =
        id.map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }
            .joinToString("")
            .take(MAX_ID)
            .ifEmpty { "sinid" }

    private const val PREFIJO = "petmate"
    private const val MAX_ID = 64
}
