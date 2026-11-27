package mx.petmate.app.data.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mx.petmate.app.util.RUTA_BASE_API
import mx.petmate.app.util.RecordingLogger
import mx.petmate.app.util.servicio
import mx.petmate.app.util.testDispatcherProvider
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Salud del backend al arrancar (spec 001, T-033; Q-02 resuelta el 27/11/2026).
 *
 * Lo que fija esta prueba no es tanto que lea bien el código, sino que **nunca tire la app**. Es una
 * comprobación que corre al abrir: si lanzara, el fallo sería una pantalla en blanco al arrancar,
 * que es el peor fallo posible y justo lo contrario de para lo que existe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackendHealthTest {

    private lateinit var server: MockWebServer
    private lateinit var health: BackendHealth
    private val logger = RecordingLogger()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        health = BackendHealth(server.servicio(), testDispatcherProvider(), logger)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `un 200 en health ready es backend listo, y pega a la ruta de Q-02`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val estado = health.check()

        assertEquals(BackendState.Ready, estado)
        assertEquals(
            "La ruta la resolvio Q-02 el 27/11/2026: health/ready sobre la base /api/v1/",
            RUTA_BASE_API + "health/ready",
            server.takeRequest().path,
        )
    }

    @Test
    fun `un 503 es que esta arrancando, no que este caido`() = runTest {
        // Es lo que contesta un contenedor de Azure que acaba de despertar. Tratarlo como caido
        // haria que la app avisara de una falla que se resuelve sola en segundos.
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(BackendState.Starting, health.check())
    }

    @Test
    fun `sin red devuelve inalcanzable en vez de lanzar`() = runTest {
        // El caso comun de verdad: el dueño abre la app en el veterinario sin señal.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertEquals(BackendState.Unreachable, health.check())
    }

    @Test
    fun `un cuerpo que no es JSON no rompe la comprobacion`() = runTest {
        // Un balanceador en problemas contesta un HTML de error. Si esto esperara el sobre JSON del
        // backend, fallaria al deserializar justo cuando mas falta hace que funcione.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html")
                .setBody("<html>502 Bad Gateway</html>"),
        )

        assertEquals(BackendState.Ready, health.check())
    }

    @Test
    fun `la bitacora no guarda la respuesta del servidor, solo el estado`() = runTest {
        // P6: en la bitacora no va nada que venga del servidor, que podria traer datos.
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"secreto":"no-deberia-salir"}"""))

        health.check()

        assertTrue(
            "En la bitacora no va nada que venga del servidor: lo registrado fue " +
                logger.mensajes.joinToString(" | "),
            logger.noRegistro("no-deberia-salir"),
        )
    }
}
