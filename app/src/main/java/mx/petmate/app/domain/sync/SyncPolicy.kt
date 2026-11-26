package mx.petmate.app.domain.sync

/** Como se resuelve un conflicto entre la version del servidor y la del dispositivo (spec 005, A-07). */
enum class ConflictRule {
    /** Gana el servidor y se avisa al usuario. Datos clinicos (R-07). */
    ServerWins,

    /** Gana la version con la marca mas reciente. Datos de la mascota (R-08). */
    LatestWins,
}

/** Lo que una entidad permite hacer sin conexion y como resuelve sus conflictos. */
data class EntitySyncRules(
    val canCreate: Boolean,
    val canUpdate: Boolean,
    val canDelete: Boolean,
    val conflict: ConflictRule,
    /** Tipo de la entidad padre que debe sincronizarse antes (R-03), o `null`. */
    val parentType: SyncEntityType?,
)

/**
 * Politica de sincronizacion por entidad: el unico lugar donde se decide (spec 005, R-13; plan 005, D-01).
 *
 * Crear, editar y borrar siguen las operaciones que hoy expone el backend para cada entidad
 * (`PetsApiService`): una vacuna se crea y se borra pero no se edita; un peso solo se registra; una alergia
 * o condicion se crea y se edita pero no se borra. Si el backend agrega una operacion, se amplia esta tabla
 * con aprobacion; la unidad que la consume no inventa su politica (spec 005, supuesto 2).
 */
object SyncPolicy {

    private val clinicalChild = { create: Boolean, update: Boolean, delete: Boolean ->
        EntitySyncRules(
            canCreate = create,
            canUpdate = update,
            canDelete = delete,
            conflict = ConflictRule.ServerWins,
            parentType = SyncEntityType.Pet,
        )
    }

    private val rules: Map<SyncEntityType, EntitySyncRules> = mapOf(
        SyncEntityType.Pet to EntitySyncRules(
            canCreate = true,
            canUpdate = true,
            canDelete = true,
            conflict = ConflictRule.LatestWins,
            parentType = null,
        ),
        SyncEntityType.Vaccine to clinicalChild(true, false, true),
        SyncEntityType.Deworming to clinicalChild(true, false, true),
        SyncEntityType.Weight to clinicalChild(true, false, false),
        SyncEntityType.Allergy to clinicalChild(true, true, false),
        SyncEntityType.Condition to clinicalChild(true, true, false),
    )

    /** Reglas de [type]. Toda entidad de [SyncEntityType] tiene reglas declaradas. */
    fun rulesFor(type: SyncEntityType): EntitySyncRules = rules.getValue(type)

    /** Indica si [action] sobre [type] se puede hacer sin conexion y encolar. */
    fun allows(type: SyncEntityType, action: SyncAction): Boolean {
        val entity = rulesFor(type)
        return when (action) {
            SyncAction.Create -> entity.canCreate
            SyncAction.Update -> entity.canUpdate
            SyncAction.Delete -> entity.canDelete
        }
    }
}
