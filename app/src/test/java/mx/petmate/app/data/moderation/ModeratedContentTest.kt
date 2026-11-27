package mx.petmate.app.data.moderation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.moderation.ModerationGateway
import mx.petmate.app.domain.moderation.ModerationRejected
import mx.petmate.app.domain.moderation.ModerationRejection
import mx.petmate.app.domain.moderation.ModerationRequest
import mx.petmate.app.domain.moderation.ModerationVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * El camino unico de subida (001/T-038, C-25).
 *
 * Lo que de verdad hay que demostrar es que un rechazo **no deja rastro**. En vez de comprobar un
 * booleano, estas pruebas usan un [AlmacenFalso] que apunta todo lo que se guarda, igual que haria
 * Room o el servidor, y al final se exige que siga vacio. Asi la prueba falla si alguien algun dia
 * invierte el orden y guarda antes de moderar.
 */
class ModeratedContentTest {

    /** Hace de Room y del servidor: lo que entra aqui es rastro que no deberia existir. */
    private class AlmacenFalso {
        val guardado = mutableListOf<String>()

        fun guardar(texto: String): AppResult<String> {
            guardado += texto
            return AppResult.Success(texto)
        }
    }

    private class GatewayFijo(private val veredicto: ModerationVerdict) : ModerationGateway {
        var llamadas = 0
        override suspend fun moderateText(text: String) = veredicto.also { llamadas++ }
        override suspend fun moderateImage(bytes: ByteArray, mimeType: String) = veredicto.also { llamadas++ }
        override suspend fun moderateVideo(bytes: ByteArray, mimeType: String) = veredicto.also { llamadas++ }
    }

    private class GatewayQueLanza(private val error: Throwable) : ModerationGateway {
        override suspend fun moderateText(text: String): ModerationVerdict = throw error
        override suspend fun moderateImage(bytes: ByteArray, mimeType: String): ModerationVerdict = throw error
        override suspend fun moderateVideo(bytes: ByteArray, mimeType: String): ModerationVerdict = throw error
    }

    private val almacen = AlmacenFalso()
    private val texto = ModerationRequest.Text("mi perro se llama Firulais")

    private fun conVeredicto(veredicto: ModerationVerdict) =
        ModeratedContent(GatewayFijo(veredicto))

    private fun causaDe(resultado: AppResult<*>): Throwable? =
        ((resultado as AppResult.Failure).error as AppError.Unknown).cause

    // ---- Aprobado: se publica ----

    @Test
    fun `si aprueba se ejecuta la accion y se devuelve su resultado`() = runTest {
        val resultado = conVeredicto(ModerationVerdict.Approved).publish(texto) {
            almacen.guardar("firulais")
        }

        assertEquals(AppResult.Success("firulais"), resultado)
        assertEquals(listOf("firulais"), almacen.guardado)
    }

    @Test
    fun `si aprueba y la accion falla se devuelve el error de la accion`() = runTest {
        // Moderacion aprobo; que la escritura falle despues es otro problema y no se disfraza de
        // rechazo de moderacion.
        val resultado = conVeredicto(ModerationVerdict.Approved).publish<String>(texto) {
            AppResult.Failure(AppError.Timeout)
        }

        assertEquals(AppResult.Failure(AppError.Timeout), resultado)
        assertTrue(almacen.guardado.isEmpty())
    }

    @Test
    fun `la accion recibe un comprobante de aprobacion`() = runTest {
        // El comprobante es lo que hace que una escritura moderada no se pueda llamar sin pasar
        // por aqui: su constructor es internal y esta es la unica funcion que lo crea.
        var recibioComprobante = false

        conVeredicto(ModerationVerdict.Approved).publish(texto) { _ ->
            recibioComprobante = true
            AppResult.Success(Unit)
        }

        assertTrue(recibioComprobante)
    }

    // ---- Rechazado: no queda rastro ----

    @Test
    fun `si rechaza no se guarda nada`() = runTest {
        val veredicto = ModerationVerdict.Rejected(ModerationRejection.ContentNotAllowed)

        val resultado = conVeredicto(veredicto).publish(texto) {
            almacen.guardar("esto no deberia guardarse")
        }

        assertTrue("Un rechazo no puede dejar rastro en Room ni en el servidor", almacen.guardado.isEmpty())
        assertTrue(resultado is AppResult.Failure)
    }

    @Test
    fun `un rechazo dice el motivo generico`() = runTest {
        val veredicto = ModerationVerdict.Rejected(ModerationRejection.FileTooLarge)

        val resultado = conVeredicto(veredicto).publish(texto) { almacen.guardar("x") }

        val causa = causaDe(resultado)
        assertEquals(ModerationRejected.NotAllowed(ModerationRejection.FileTooLarge), causa)
    }

    @Test
    fun `los tres motivos de rechazo llegan intactos`() = runTest {
        ModerationRejection.entries.forEach { motivo ->
            val resultado = conVeredicto(ModerationVerdict.Rejected(motivo)).publish(texto) {
                almacen.guardar("x")
            }

            assertEquals(ModerationRejected.NotAllowed(motivo), causaDe(resultado))
        }
        assertTrue(almacen.guardado.isEmpty())
    }

    // ---- No validado: tampoco se publica (R-08) ----

    @Test
    fun `si no se pudo validar tampoco se guarda`() = runTest {
        // Sin red, timeout o servicio caido. Ante la duda no se publica: dejar pasar algo sin
        // revisar es peor que pedirle al usuario que lo intente de nuevo.
        val resultado = conVeredicto(ModerationVerdict.NotValidated).publish(texto) {
            almacen.guardar("esto no deberia guardarse")
        }

        assertTrue(almacen.guardado.isEmpty())
        assertEquals(ModerationRejected.CouldNotValidate, causaDe(resultado))
    }

    @Test
    fun `si el gateway lanza se trata como no validado y no se guarda`() = runTest {
        // El contrato dice que no lanza, pero el camino no se apoya en esa promesa.
        val moderado = ModeratedContent(GatewayQueLanza(IllegalStateException("el servicio truena")))

        val resultado = moderado.publish(texto) { almacen.guardar("esto no deberia guardarse") }

        assertTrue(almacen.guardado.isEmpty())
        assertEquals(ModerationRejected.CouldNotValidate, causaDe(resultado))
    }

    @Test
    fun `la cancelacion no se atrapa`() = runTest {
        // Si se tragara la cancelacion, la corrutina que la pidio se quedaria colgada.
        val moderado = ModeratedContent(GatewayQueLanza(CancellationException("cancelado")))

        try {
            moderado.publish(texto) { almacen.guardar("x") }
            fail("La cancelacion debe seguir subiendo, no volverse NotValidated")
        } catch (_: CancellationException) {
            // Es lo que se espera.
        }
        assertTrue(almacen.guardado.isEmpty())
    }

    // ---- Orden: primero moderar, luego escribir ----

    @Test
    fun `se modera antes de ejecutar la accion`() = runTest {
        val orden = mutableListOf<String>()
        val gateway = object : ModerationGateway {
            override suspend fun moderateText(text: String): ModerationVerdict {
                orden += "moderar"
                return ModerationVerdict.Approved
            }

            override suspend fun moderateImage(bytes: ByteArray, mimeType: String) = ModerationVerdict.Approved
            override suspend fun moderateVideo(bytes: ByteArray, mimeType: String) = ModerationVerdict.Approved
        }

        ModeratedContent(gateway).publish(texto) {
            orden += "guardar"
            AppResult.Success(Unit)
        }

        assertEquals(listOf("moderar", "guardar"), orden)
    }

    @Test
    fun `se modera una sola vez por publicacion`() = runTest {
        val gateway = GatewayFijo(ModerationVerdict.Approved)

        ModeratedContent(gateway).publish(texto) { AppResult.Success(Unit) }

        assertEquals(1, gateway.llamadas)
    }

    @Test
    fun `las imagenes y los videos pasan por el mismo camino`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val peticiones = listOf(
            ModerationRequest.Image(bytes, "image/jpeg"),
            ModerationRequest.Video(bytes, "video/mp4"),
        )

        peticiones.forEach { peticion ->
            val resultado = conVeredicto(ModerationVerdict.NotValidated).publish(peticion) {
                almacen.guardar("x")
            }
            assertFalse("${peticion.type} debe rechazarse igual que el texto", resultado is AppResult.Success)
        }

        assertTrue(almacen.guardado.isEmpty())
    }
}
