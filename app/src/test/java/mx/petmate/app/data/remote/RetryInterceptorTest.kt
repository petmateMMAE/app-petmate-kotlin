package mx.petmate.app.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Reintentos contra un servidor de verdad (001/T-033, C-24, C-30).
 *
 * Usa [MockWebServer] en vez de un doble del interceptor: lo que hay que comprobar es cuantas
 * peticiones llegaron realmente al servidor, y eso solo se ve desde el otro lado del socket. Un
 * doble diria lo que nosotros programamos que dijera.
 */
class RetryInterceptorTest {

    private lateinit var server: MockWebServer

    /** Las esperas que pidio el interceptor, en vez de dormirlas de verdad. */
    private val esperas = mutableListOf<Long>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun cliente(
        maxAttempts: Int = 3,
        baseDelayMillis: Long = 10,
        maxDelayMillis: Long = 1_000,
    ): OkHttpClient = OkHttpClient.Builder()
        .readTimeout(TIMEOUT_CORTO_MS, TimeUnit.MILLISECONDS)
        .addInterceptor(
            RetryInterceptor(maxAttempts, baseDelayMillis, maxDelayMillis) { esperas += it },
        )
        .build()

    private fun pedir(cliente: OkHttpClient, metodo: String = "GET") = cliente
        .newCall(
            Request.Builder()
                .url(server.url("/pets"))
                .apply { if (metodo != "GET") method(metodo, "".toRequestBody()) }
                .build(),
        )
        .execute()

    private fun sinRespuesta() = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)

    private fun ok() = MockResponse().setResponseCode(200).setBody("{}")

    @Test
    fun `un 500 se reintenta y se devuelve la respuesta buena`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(ok())

        pedir(cliente()).use { respuesta ->
            assertEquals(200, respuesta.code)
        }
        assertEquals("Debio reintentar una vez", 2, server.requestCount)
        assertEquals("Debio esperar antes del reintento", listOf(10L), esperas)
    }

    @Test
    fun `un 400 no se reintenta`() {
        // El servidor ya dijo que la peticion esta mal: repetirla da el mismo error.
        server.enqueue(MockResponse().setResponseCode(400))

        pedir(cliente()).use { respuesta ->
            assertEquals(400, respuesta.code)
        }
        assertEquals("Un 4xx no se reintenta nunca", 1, server.requestCount)
        assertTrue("No debio esperar nada", esperas.isEmpty())
    }

    @Test
    fun `un 401 tampoco se reintenta`() {
        // Renovar la sesion es trabajo de AuthorizedCaller, no de los reintentos de red: si aqui
        // se reintentara, se gastarian los intentos antes de que nadie refresque el token.
        server.enqueue(MockResponse().setResponseCode(401))

        pedir(cliente()).use { respuesta ->
            assertEquals(401, respuesta.code)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `un 404 tampoco se reintenta`() {
        server.enqueue(MockResponse().setResponseCode(404))

        pedir(cliente()).use { respuesta ->
            assertEquals(404, respuesta.code)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `un timeout se reintenta`() {
        // Este es el caso del balanceador de Azure moviendo una replica.
        server.enqueue(sinRespuesta())
        server.enqueue(sinRespuesta())
        server.enqueue(ok())

        pedir(cliente()).use { respuesta ->
            assertEquals(200, respuesta.code)
        }
        assertEquals(3, server.requestCount)
        assertEquals(listOf(10L, 20L), esperas)
    }

    @Test
    fun `se rinde tras el maximo de intentos y devuelve el ultimo 503`() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(503)) }

        pedir(cliente(maxAttempts = 3)).use { respuesta ->
            assertEquals(
                "Tras agotar los intentos se devuelve la respuesta, no una excepcion",
                503,
                respuesta.code,
            )
        }
        assertEquals(3, server.requestCount)
        assertEquals("Dos esperas para tres intentos", listOf(10L, 20L), esperas)
    }

    @Test
    fun `si todos los intentos cortan la red se propaga la excepcion`() {
        repeat(3) { server.enqueue(sinRespuesta()) }

        try {
            pedir(cliente(maxAttempts = 3)).close()
            fail("Debio propagar la IOException del ultimo intento")
        } catch (_: IOException) {
            // Es lo que se espera: la app la traduce a AppError.Timeout o AppError.Network.
        }
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `una respuesta buena no gasta reintentos`() {
        server.enqueue(ok())

        pedir(cliente()).use { respuesta ->
            assertEquals(200, respuesta.code)
        }
        assertEquals(1, server.requestCount)
        assertTrue(esperas.isEmpty())
    }

    @Test
    fun `una escritura con 5xx tambien se reintenta`() {
        // Reintentar un POST solo es seguro porque va con clave de idempotencia (D-15), y de eso
        // se encarga IdempotencyKeyInterceptor, que corre por fuera de este.
        server.enqueue(MockResponse().setResponseCode(502))
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))

        pedir(cliente(), metodo = "POST").use { respuesta ->
            assertEquals(201, respuesta.code)
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `con un solo intento no se reintenta nada`() {
        server.enqueue(MockResponse().setResponseCode(500))

        pedir(cliente(maxAttempts = 1)).use { respuesta ->
            assertEquals(500, respuesta.code)
        }
        assertEquals(1, server.requestCount)
        assertTrue(esperas.isEmpty())
    }

    @Test
    fun `la espera crece exponencial y se topa en el maximo`() {
        val interceptor = RetryInterceptor(
            maxAttempts = 6,
            baseDelayMillis = 100,
            maxDelayMillis = 250,
        ) { }

        assertEquals(100L, interceptor.esperaPara(1))
        assertEquals(200L, interceptor.esperaPara(2))
        assertEquals("400 pasa del tope, se queda en 250", 250L, interceptor.esperaPara(3))
        assertEquals("Y sigue topado, no crece sin limite", 250L, interceptor.esperaPara(4))
    }

    @Test
    fun `maxAttempts invalido se rechaza al construir`() {
        try {
            RetryInterceptor(maxAttempts = 0)
            fail("maxAttempts = 0 dejaria la peticion sin enviarse nunca")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("maxAttempts"))
        }
    }

    private companion object {
        const val TIMEOUT_CORTO_MS = 200L
    }
}
