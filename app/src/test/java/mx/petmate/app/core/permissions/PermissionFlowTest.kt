package mx.petmate.app.core.permissions

import mx.petmate.app.domain.permissions.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** Concedido, negado y negado permanente (spec 003, T-004, C-07, C-08). */
class PermissionFlowTest {

    private val idle = PermissionFlowState()

    @Test
    fun `si ya esta concedido no se explica nada`() {
        assertEquals(PermissionStep.Granted, PermissionFlow.start(idle, PermissionStatus.Granted).step)
    }

    @Test
    fun `siempre se explica antes del dialogo del sistema`() {
        val asked = PermissionFlow.start(idle, PermissionStatus.NotRequested)

        assertEquals(PermissionStep.ShowRationale, asked.step)
        assertEquals(PermissionStep.LaunchSystem, PermissionFlow.onRationaleAccepted(asked).step)
    }

    @Test
    fun `ahora no en la explicacion no cuenta como negativa`() {
        val dismissed = PermissionFlow.onRationaleDismissed(PermissionFlow.start(idle, PermissionStatus.NotRequested))

        assertEquals(PermissionStep.Denied, dismissed.step)
        assertEquals(0, dismissed.denials)
    }

    @Test
    fun `camino concedido`() {
        val result = PermissionFlow.onSystemResult(idle, granted = true, canAskAgain = true)

        assertEquals(PermissionStep.Granted, result.step)
    }

    @Test
    fun `camino negado una vez deja volver a pedir`() {
        val once = PermissionFlow.onSystemResult(idle, granted = false, canAskAgain = true)

        assertEquals(PermissionStep.Denied, once.step)
        assertEquals(PermissionStep.ShowRationale, PermissionFlow.start(once, PermissionStatus.Denied).step)
    }

    @Test
    fun `a la segunda negativa se ofrecen ajustes y no un tercer dialogo`() {
        val once = PermissionFlow.onSystemResult(idle, granted = false, canAskAgain = true)
        val twice = PermissionFlow.onSystemResult(once, granted = false, canAskAgain = true)

        assertEquals(PermissionStep.OfferSettings, twice.step)
        assertEquals(PermissionStep.OfferSettings, PermissionFlow.start(twice, PermissionStatus.Denied).step)
    }

    @Test
    fun `camino permanente ofrece ajustes desde la primera negativa`() {
        val permanent = PermissionFlow.onSystemResult(idle, granted = false, canAskAgain = false)

        assertEquals(PermissionStep.OfferSettings, permanent.step)
        val again = PermissionFlow.start(idle, PermissionStatus.PermanentlyDenied)
        assertEquals(PermissionStep.OfferSettings, again.step)
    }

    @Test
    fun `al volver de ajustes con permiso se activa y sin el queda negado`() {
        val waiting = PermissionFlow.onSettingsOpened(PermissionFlowState(PermissionStep.OfferSettings, 2))

        assertEquals(PermissionStep.WaitingSettings, waiting.step)
        assertEquals(PermissionStep.Granted, PermissionFlow.onReturnFromSettings(waiting, PermissionStatus.Granted).step)
        assertEquals(PermissionStep.Denied, PermissionFlow.onReturnFromSettings(waiting, PermissionStatus.Denied).step)
    }

    @Test
    fun `el estado se guarda y se restaura igual`() {
        val state = PermissionFlowState(PermissionStep.OfferSettings, 2)

        assertEquals(state, PermissionFlowState.fromSaved(state.toSaved()))
    }
}
