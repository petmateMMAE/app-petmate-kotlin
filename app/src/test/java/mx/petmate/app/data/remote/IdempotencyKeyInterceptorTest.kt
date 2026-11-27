package mx.petmate.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Clave de idempotencia en las escrituras (001/T-033, D-15).
 *
 * Se comprueba leyendo la cabecera que **llego al servidor**, no la que creemos haber puesto.
 */
class IdempotencyKeyInterceptorTest {

    private lateinit var server: MockWebServer
    private var contador = 0

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** Claves predecibles: si el orden de los interceptores estuviera mal, se notaria al instante. */
    private fun claveDePrueba(): String = "clave-${contador++}"

    private fun cliente(conReintentos: Boolean = false): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(IdempotencyKeyInterceptor(::claveDePrueba))
        if (conReintentos) {
            // Mismo orden que NetworkModule: la clave por fuera, los reintentos por dentro.
            builder.addInterceptor(RetryInterceptor(maxAttempts = 3, baseDelayMillis = 1) { })
        }
        return builder.build()
    }

    private fun pedir(metodo: String, cliente: OkHttpClient = cliente()) = cliente
        .newCall(
            Request.Builder()
                .url(server.url("/pets"))
                .apply { if (metodo != "GET") method(metodo, "{}".toRequestBody()) }
                .build(),
        )
        .execute()

    private fun ok() = MockResponse().setResponseCode(200).setBody("{}")

    @Test
    fun `un POST lleva clave`() {
        server.enqueue(ok())

        pedir("POST").close()

        assertEquals("clave-0", server.takeRequest().getHeader(IDEMPOTENCY_HEADER))
    }

    @Test
    fun `PUT PATCH y DELETE tambien llevan clave`() {
        listOf("PUT", "PATCH", "DELETE").forEach { metodo ->
            server.enqueue(ok())
            pedir(metodo).close()
            val recibida = server.takeRequest()
            assertEquals(
                "$metodo cambia estado en el servidor, necesita clave",
                "clave-${contador - 1}",
                recibida.getHeader(IDEMPOTENCY_HEADER),
            )
        }
    }

    @Test
    fun `un GET no lleva clave`() {
        // Leer dos veces no causa dano, y mandar la cabecera de mas solo ensucia la peticion.
        server.enqueue(ok())

        pedir("GET").close()

        assertNull(server.takeRequest().getHeader(IDEMPOTENCY_HEADER))
    }

    @Test
    fun `si quien llama ya puso una clave se respeta`() {
        // La cola de envios sin conexion necesita que la clave sobreviva entre reintentos de dias
        // distintos, no solo entre reintentos de red, y esa clave la elige ella.
        server.enqueue(ok())

        cliente().newCall(
            Request.Builder()
                .url(server.url("/pets"))
                .header(IDEMPOTENCY_HEADER, "clave-de-la-cola-offline")
                .post("{}".toRequestBody())
                .build(),
        ).execute().close()

        assertEquals("clave-de-la-cola-offline", server.takeRequest().getHeader(IDEMPOTENCY_HEADER))
        assertEquals("No debio generar ninguna clave propia", 0, contador)
    }

    @Test
    fun `todos los reintentos de una escritura mandan la MISMA clave`() {
        // Esta es la prueba que justifica el orden de los interceptores en NetworkModule. Si la
        // clave se generara en cada intento, un reintento tras un 500 crearia dos mascotas: el
        // servidor veria dos peticiones distintas y no podria descartar el duplicado.
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(ok())

        pedir("POST", cliente(conReintentos = true)).use { respuesta ->
            assertEquals(200, respuesta.code)
        }

        assertEquals("Debieron llegar dos peticiones", 2, server.requestCount)
        val primera = server.takeRequest().getHeader(IDEMPOTENCY_HEADER)
        val segunda = server.takeRequest().getHeader(IDEMPOTENCY_HEADER)
        assertEquals("El reintento debe mandar la misma clave que el primer intento", primera, segunda)
        assertEquals("La clave se genera una sola vez por peticion logica", 1, contador)
    }

    @Test
    fun `dos escrituras distintas llevan claves distintas`() {
        // Si compartieran clave, el servidor descartaria la segunda mascota creyendola duplicado.
        server.enqueue(ok())
        server.enqueue(ok())

        pedir("POST").close()
        pedir("POST").close()

        val primera = server.takeRequest().getHeader(IDEMPOTENCY_HEADER)
        val segunda = server.takeRequest().getHeader(IDEMPOTENCY_HEADER)
        assertNotEquals(primera, segunda)
    }

    @Test
    fun `la clave por omision es un UUID distinto cada vez`() {
        val interceptor = IdempotencyKeyInterceptor()
        server.enqueue(ok())
        server.enqueue(ok())
        val conUuid = OkHttpClient.Builder().addInterceptor(interceptor).build()

        pedir("POST", conUuid).close()
        pedir("POST", conUuid).close()

        val primera = server.takeRequest().getHeader(IDEMPOTENCY_HEADER)!!
        val segunda = server.takeRequest().getHeader(IDEMPOTENCY_HEADER)!!
        assertNotEquals(primera, segunda)
        assertEquals("Un UUID son 36 caracteres", 36, primera.length)
    }
}
