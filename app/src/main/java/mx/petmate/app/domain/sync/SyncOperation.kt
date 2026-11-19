package mx.petmate.app.domain.sync

/** Entidades que se sincronizan con el backend (spec 005, supuesto 2). */
enum class SyncEntityType {
    Pet,
    Vaccine,
    Deworming,
    Weight,
    Allergy,
    Condition,
}

/** Lo que se le pide al servidor con una operacion encolada. */
enum class SyncAction {
    Create,
    Update,
    Delete,
}

/** Identidad de un elemento concreto: su tipo y su id de cliente. */
data class SyncEntityRef(
    val type: SyncEntityType,
    val id: String,
)

/**
 * Escritura pendiente de enviar al servidor (spec 005, R-01).
 *
 * [payload] es el cuerpo ya serializado; nunca lleva credenciales (R-12).
 * [dependsOn] es el elemento padre que debe existir en el servidor antes de enviar esta operacion (R-03).
 * [createdAtMillis] fija el orden FIFO dentro de cada nivel del grafo (plan 005, D-04).
 */
data class SyncOperation(
    val target: SyncEntityRef,
    val action: SyncAction,
    val payload: String,
    val dependsOn: SyncEntityRef?,
    val createdAtMillis: Long,
)
