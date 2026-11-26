package mx.petmate.app.ui.components.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import dagger.hilt.android.EntryPointAccessors
import mx.petmate.app.core.permissions.PermissionFlow
import mx.petmate.app.core.permissions.PermissionFlowState
import mx.petmate.app.core.permissions.PermissionStep
import mx.petmate.app.core.permissions.openAppSettings
import mx.petmate.app.di.PermissionsEntryPoint
import mx.petmate.app.domain.permissions.PermissionGateway
import mx.petmate.app.domain.permissions.PermissionStatus
import mx.petmate.app.domain.permissions.PermissionType

private val FlowSaver = Saver<PermissionFlowState, List<Any>>(
    save = { it.toSaved() },
    restore = { PermissionFlowState.fromSaved(it) },
)

/**
 * Pide [type] como manda la spec 003: explicacion propia, dialogo del sistema, a la segunda negativa los
 * ajustes, y al volver de ajustes se vuelve a consultar. Devuelve la funcion que la pantalla llama en el
 * momento en que necesita el permiso (R-01); nunca se llama sola al componer.
 *
 * [onGranted] corre cuando el permiso queda concedido, incluso al volver de ajustes sin reiniciar (C-08).
 * [onDenied] corre cuando queda negado, para que la pantalla muestre su alternativa (R-05 a R-09).
 */
@Composable
fun rememberPermissionRequest(
    type: PermissionType,
    onGranted: () -> Unit,
    onDenied: () -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val gateway = remember(context) { context.permissionGateway() }
    val granted by rememberUpdatedState(onGranted)
    val denied by rememberUpdatedState(onDenied)
    var state by rememberSaveable(type, stateSaver = FlowSaver) { mutableStateOf(PermissionFlowState()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val ok = result.values.any { it }
        val activity = context.findActivity()
        val canAskAgain = activity != null &&
            gateway.systemPermissions(type).any { activity.shouldShowRequestPermissionRationale(it) }
        gateway.onSystemResult(type, ok, canAskAgain)
        state = PermissionFlow.onSystemResult(state, ok, canAskAgain)
    }

    LaunchedEffect(state.step) {
        when (state.step) {
            PermissionStep.LaunchSystem -> {
                val permissions = gateway.systemPermissions(type)
                if (permissions.isEmpty()) {
                    state = state.copy(step = PermissionStep.Granted)
                } else {
                    launcher.launch(permissions.toTypedArray())
                }
            }
            PermissionStep.Granted -> {
                state = state.copy(step = PermissionStep.Idle)
                granted()
            }
            PermissionStep.Denied -> {
                state = state.copy(step = PermissionStep.Idle)
                denied()
            }
            else -> Unit
        }
    }

    // Llave fija: solo reacciona a un ON_RESUME real, o sea al volver de los ajustes, y no al cambiar de paso.
    LifecycleResumeEffect(type) {
        if (state.step == PermissionStep.WaitingSettings) {
            gateway.refresh()
            state = PermissionFlow.onReturnFromSettings(state, gateway.status(type))
        }
        onPauseOrDispose { }
    }

    when (state.step) {
        PermissionStep.ShowRationale -> PermissionRationale(
            type = type,
            onContinue = { state = PermissionFlow.onRationaleAccepted(state) },
            onNotNow = { state = PermissionFlow.onRationaleDismissed(state) },
        )
        PermissionStep.OfferSettings -> PermissionSettingsPrompt(
            onOpenSettings = {
                state = if (context.openAppSettings()) {
                    PermissionFlow.onSettingsOpened(state)
                } else {
                    PermissionFlow.onSettingsDismissed(state)
                }
            },
            onNotNow = { state = PermissionFlow.onSettingsDismissed(state) },
        )
        else -> Unit
    }

    return remember(type, gateway) { { state = PermissionFlow.start(state, gateway.status(type)) } }
}

/** Estado actual de [type], sin pedir nada. Sirve para no pedir el permiso al entrar a una pantalla. */
@Composable
fun rememberPermissionStatus(type: PermissionType): PermissionStatus {
    val context = LocalContext.current
    return remember(context, type) { context.permissionGateway().status(type) }
}

/** Estado de [type] leido en el momento, para quien no es Composable (por ejemplo, un callback). */
fun Context.permissionStatus(type: PermissionType): PermissionStatus = permissionGateway().status(type)

private fun Context.permissionGateway(): PermissionGateway =
    EntryPointAccessors.fromApplication(applicationContext, PermissionsEntryPoint::class.java).permissionGateway()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
