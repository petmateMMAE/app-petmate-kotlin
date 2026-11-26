package mx.petmate.app.core.permissions

/** Paso del flujo de un permiso (spec 003, T-004). */
enum class PermissionStep {
    /** Nadie ha pedido nada. */
    Idle,

    /** Se muestra la explicacion propia antes del dialogo del sistema (R-02). */
    ShowRationale,

    /** Toca lanzar el dialogo del sistema. */
    LaunchSystem,

    /** Ya no se insiste: se ofrece abrir los ajustes de la app (R-04). */
    OfferSettings,

    /** Se fue a ajustes y se espera el regreso para volver a consultar (T-005). */
    WaitingSettings,

    Granted,

    /** Negado por ahora: la pantalla muestra su alternativa. */
    Denied,
}

/**
 * Estado del flujo. Son solo un paso y un contador, para guardarse con `rememberSaveable` y sobrevivir a la
 * rotacion y a la muerte del proceso (R-10).
 */
data class PermissionFlowState(
    val step: PermissionStep = PermissionStep.Idle,
    val denials: Int = 0,
) {
    fun toSaved(): List<Any> = listOf(step.name, denials)

    companion object {
        fun fromSaved(saved: List<Any>): PermissionFlowState =
            PermissionFlowState(PermissionStep.valueOf(saved[0] as String), saved[1] as Int)
    }
}
