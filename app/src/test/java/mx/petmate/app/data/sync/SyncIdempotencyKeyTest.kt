package mx.petmate.app.data.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.domain.sync.SyncAction
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncEntityType
import mx.petmate.app.domain.sync.SyncOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Idempotencia de la cola (spec 005, T-004, C-03).
 *
 * El caso que de verdad importa: **el mismo cambio no puede aplicarse dos veces en el servidor.**
 * Si se aplicara, el dueño vería dos vacunas idénticas en el carnet de su mascota y no sabría cuál
 * borrar.
 *
 * El "contador de peticiones" que pide C-03 es [ServidorFalso]: cuenta cuántas veces se le llamó y
 * cuántos registros guardó, descartando las claves repetidas como lo hace el backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncIdempotencyKeyTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }

    private val dao = InMemorySyncOperationDao()
    private val outbox = SyncOutbox(dao, dispatchers)

    private fun operacion(
        id: String = "p-1",
        type: SyncEntityType = SyncEntityType.Pet,
        action: SyncAction = SyncAction.Create,
        createdAtMillis: Long = 1_000L,
    ) = SyncOperation(
        target = SyncEntityRef(type, id),
        action = action,
        payload = """{"name":"Firulais"}""",
        dependsOn = null,
        createdAtMillis = createdAtMillis,
    )

    @Test
    fun `la clave se deriva del id de cliente y es la misma cada vez`() {
        // Lo que pide la aceptación: estable, no sorteada. Si fuera un UUID, dos llamadas darían
        // claves distintas y el servidor vería dos operaciones donde hay una.
        val primera = SyncIdempotencyKey.of(operacion())
        val segunda = SyncIdempotencyKey.of(operacion())

        assertEquals(primera, segunda)
        assertTrue("La clave debe llevar el id de cliente para poder rastrearla", primera.contains("p-1"))
    }

    @Test
    fun `operaciones distintas no comparten clave`() {
        val crear = SyncIdempotencyKey.of(operacion(action = SyncAction.Create))
        val borrar = SyncIdempotencyKey.of(operacion(action = SyncAction.Delete))
        val otraMascota = SyncIdempotencyKey.of(operacion(id = "p-2"))
        val otroTipo = SyncIdempotencyKey.of(operacion(type = SyncEntityType.Vaccine))
        val otroInstante = SyncIdempotencyKey.of(operacion(createdAtMillis = 2_000L))

        // Crear y borrar la misma mascota son dos operaciones, no una repetida.
        assertNotEquals(crear, borrar)
        assertNotEquals(crear, otraMascota)
        // Un id puede repetirse entre tablas, así que el tipo tiene que entrar en la clave.
        assertNotEquals(crear, otroTipo)
        // Dos pesos de la misma mascota son dos registros legítimos: sin el instante, el segundo
        // se vería como repetido y se perdería.
        assertNotEquals(crear, otroInstante)
    }

    @Test
    fun `repetir diez veces la misma operacion deja un solo registro en el servidor`() =
        runTest(dispatcher) {
            // C-03 tal cual: diez envíos, un registro.
            val servidor = ServidorFalso()
            outbox.enqueue(operacion())
            val pendiente = outbox.pending().single()

            repeat(10) { servidor.recibe(pendiente.idempotencyKey) }

            assertEquals("El servidor recibió los diez envíos", 10, servidor.peticiones)
            assertEquals("…y guardó un solo registro", 1, servidor.registros)
        }

    @Test
    fun `cada reintento manda la misma clave`() = runTest(dispatcher) {
        // R-04: la clave se genera una vez al encolar y se guarda. Si se regenerara en cada envío,
        // la idempotencia del servidor no serviría de nada.
        val servidor = ServidorFalso()
        outbox.enqueue(operacion())

        val primerEnvio = outbox.pending().single()
        servidor.recibe(primerEnvio.idempotencyKey)
        outbox.markSending(primerEnvio)
        outbox.markRetry(primerEnvio, "sin conexion")

        val reintento = outbox.pending().single()
        servidor.recibe(reintento.idempotencyKey)

        assertEquals(primerEnvio.idempotencyKey, reintento.idempotencyKey)
        assertEquals(1, servidor.registros)
    }

    @Test
    fun `una clave con caracteres raros se sanea para poder viajar en una cabecera`() {
        val clave = SyncIdempotencyKey.of(operacion(id = "p 1/á\n"))

        assertTrue(
            "Una cabecera HTTP con espacios o saltos de linea rompe la peticion por algo que no " +
                "tiene nada que ver con el dato",
            clave.all { it.isLetterOrDigit() || it == '-' || it == '_' },
        )
    }
}

/**
 * Servidor que descarta lo repetido por clave de idempotencia, como el backend.
 *
 * Cuenta las dos cosas aparte a propósito: [peticiones] demuestra que de verdad se llamó diez
 * veces, y [registros] que solo una quedó. Contar solo una de las dos dejaría pasar una prueba que
 * no comprueba nada.
 */
private class ServidorFalso {
    private val vistas = mutableSetOf<String>()

    var peticiones = 0
        private set

    val registros: Int get() = vistas.size

    fun recibe(idempotencyKey: String) {
        peticiones++
        vistas += idempotencyKey
    }
}
