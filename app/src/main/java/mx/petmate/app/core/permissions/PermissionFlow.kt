package mx.petmate.app.core.permissions

import mx.petmate.app.domain.permissions.PermissionStatus

/**
 * Reglas del flujo de un permiso (spec 003, T-004). Logica pura: no lanza dialogos, solo dice el siguiente paso.
 *
 * - Siempre hay explicacion antes del dialogo del sistema (R-02).
 * - A la segunda negativa, o si el sistema ya no muestra el dialogo, se ofrecen los ajustes en vez de un tercer
 *   dialogo (R-04).
 */
object PermissionFlow {

    /** Dialogos del sistema que se muestran como maximo antes de ofrecer los ajustes. */
    const val MAX_SYSTEM_DIALOGS = 2

    /** La pantalla necesita el permiso. */
    fun start(state: PermissionFlowState, status: PermissionStatus): PermissionFlowState = when {
        status == PermissionStatus.Granted -> state.copy(step = PermissionStep.Granted)
        status == PermissionStatus.PermanentlyDenied -> state.copy(step = PermissionStep.OfferSettings)
        state.denials >= MAX_SYSTEM_DIALOGS -> state.copy(step = PermissionStep.OfferSettings)
        else -> state.copy(step = PermissionStep.ShowRationale)
    }

    fun onRationaleAccepted(state: PermissionFlowState) = state.copy(step = PermissionStep.LaunchSystem)

    /** "Ahora no" en la explicacion: no se lanza el dialogo y no cuenta como negativa del sistema. */
    fun onRationaleDismissed(state: PermissionFlowState) = state.copy(step = PermissionStep.Denied)

    fun onSystemResult(state: PermissionFlowState, granted: Boolean, canAskAgain: Boolean): PermissionFlowState {
        if (granted) return state.copy(step = PermissionStep.Granted)
        val denials = state.denials + 1
        val stop = !canAskAgain || denials >= MAX_SYSTEM_DIALOGS
        return PermissionFlowState(if (stop) PermissionStep.OfferSettings else PermissionStep.Denied, denials)
    }

    fun onSettingsOpened(state: PermissionFlowState) = state.copy(step = PermissionStep.WaitingSettings)

    fun onSettingsDismissed(state: PermissionFlowState) = state.copy(step = PermissionStep.Denied)

    /** Al volver de ajustes se vuelve a consultar; si concedio, la funcion se activa sin reiniciar (T-005). */
    fun onReturnFromSettings(state: PermissionFlowState, status: PermissionStatus): PermissionFlowState =
        if (status == PermissionStatus.Granted) {
            PermissionFlowState(PermissionStep.Granted, denials = 0)
        } else {
            state.copy(step = PermissionStep.Denied)
        }
}
