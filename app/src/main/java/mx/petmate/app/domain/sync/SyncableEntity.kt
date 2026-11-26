package mx.petmate.app.domain.sync

/**
 * Contrato que cumple toda entidad que se sincroniza (plan 005, SY-02).
 *
 * El motor de sincronizacion solo conoce este contrato, nunca la mascota ni el carnet: 007 y 008 aportan
 * sus propias implementaciones (plan 005, principio 3).
 */
interface SyncableEntity {
    /** Tipo e id de cliente del elemento. El id lo genera el dispositivo y es estable (plan 005, D-03). */
    val ref: SyncEntityRef

    /** Elemento padre del que depende, o `null` si no depende de nadie. Una vacuna depende de su mascota. */
    val parent: SyncEntityRef?

    /** Marca de la ultima modificacion, para resolver conflictos de "gana el mas reciente" (R-08). */
    val updatedAtMillis: Long

    /** Cuerpo de la peticion al servidor para [action]. No incluye credenciales (R-12). */
    fun serialize(action: SyncAction): String
}

/** Arma la operacion encolable de [action] sobre esta entidad. */
fun SyncableEntity.toOperation(action: SyncAction, nowMillis: Long): SyncOperation =
    SyncOperation(
        target = ref,
        action = action,
        payload = serialize(action),
        dependsOn = parent,
        createdAtMillis = nowMillis,
    )
