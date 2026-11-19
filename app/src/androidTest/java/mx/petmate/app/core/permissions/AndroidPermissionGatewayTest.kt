package mx.petmate.app.core.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mx.petmate.app.domain.permissions.PermissionStatus
import mx.petmate.app.domain.permissions.PermissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Gateway con las APIs reales del dispositivo (spec 003, T-002). */
@RunWith(AndroidJUnit4::class)
class AndroidPermissionGatewayTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val gateway = AndroidPermissionGateway(context)

    private fun cameraGranted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    @Test
    fun las_fotos_no_piden_permiso() {
        assertTrue(gateway.systemPermissions(PermissionType.Photos).isEmpty())
        assertEquals(PermissionStatus.Granted, gateway.status(PermissionType.Photos))
    }

    @Test
    fun la_ubicacion_pide_precisa_y_aproximada() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            gateway.systemPermissions(PermissionType.Location),
        )
    }

    @Test
    fun sin_respuesta_la_camara_no_se_ha_pedido() {
        assumeTrue(!cameraGranted())
        assertEquals(PermissionStatus.NotRequested, gateway.status(PermissionType.Camera))
    }

    @Test
    fun una_negativa_queda_registrada_y_se_observa() = runBlocking {
        assumeTrue(!cameraGranted())

        gateway.onSystemResult(PermissionType.Camera, granted = false, canAskAgain = true)
        assertEquals(PermissionStatus.Denied, gateway.observe(PermissionType.Camera).first())

        gateway.onSystemResult(PermissionType.Camera, granted = false, canAskAgain = false)
        assertEquals(PermissionStatus.PermanentlyDenied, gateway.status(PermissionType.Camera))
    }

    @Test
    fun refresh_relee_sin_tronar_en_el_hilo_principal() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            gateway.refresh()
            PermissionType.entries.forEach { gateway.status(it) }
        }
    }
}
