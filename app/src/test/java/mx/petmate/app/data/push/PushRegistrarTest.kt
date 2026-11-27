package mx.petmate.app.data.push

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.DeviceApiService
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.push.NoPushTokenProvider
import mx.petmate.app.domain.push.PushTokenProvider
import mx.petmate.app.domain.repository.AuthRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Registro y baja del dispositivo para push (spec 001, T-039; D-19).
 *
 * Se prueba contra un servidor simulado con el sobre real del backend
 * (`{ statusCode, message, data }`) y se cuenta **cuantas peticiones llegaron de verdad**, que es
 * la unica forma de demostrar que sin token no se llama al backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PushRegistrarTest {

    private lateinit var server: MockWebServer
    private lateinit var deviceApi: DeviceApiService
    private lateinit var caller: AuthorizedCaller
    private val sessionStore: SessionStore = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val logger: Logger = mockk(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        every { sessionStore.readAccessToken() } returns "acc-1"
        every { sessionStore.readRefreshToken() } returns "ref-1"
        deviceApi = retrofit.create(DeviceApiService::class.java)
        caller = AuthorizedCaller(retrofit.create(AuthApiService::class.java), sessionStore, authRepository)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun ok() = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"statusCode":200,"message":"ok","data":null}""")

    private fun registrarWith(provider: PushTokenProvider) =
        PushRegistrar(provider, deviceApi, caller, dispatchers, logger)

    /** Proveedor de prueba: devuelve [token] y anota si le pidieron borrar. */
    private class FakeProvider(
        private val token: String?,
        private val throwOnToken: Boolean = false,
    ) : PushTokenProvider {
        var deleteCalled = false
            private set

        override suspend fun currentToken(): String? {
            if (throwOnToken) error("Firebase trono")
            return token
        }

        override suspend fun deleteToken() {
            deleteCalled = true
        }
    }

    @Test
    fun `register manda el token con el bearer de la sesion`() = runTest(dispatcher) {
        server.enqueue(ok())

        registrarWith(FakeProvider("tok-abc")).register()

        val request = server.takeRequest()
        assertEquals("/api/v1/devices/register", request.path)
        assertEquals("Bearer acc-1", request.getHeader("Authorization"))
        // El cuerpo lleva el token. NO lleva `platform`: el backend declara
        // additionalProperties:false y kotlinx no serializa valores por defecto, asi que el DTO
        // viaja con una sola clave. Si alguien pone encodeDefaults = true, esta prueba lo caza.
        val body = request.body.readUtf8()
        assertEquals("""{"token":"tok-abc"}""", body)
    }

    @Test
    fun `unregister da de baja y luego borra el token`() = runTest(dispatcher) {
        server.enqueue(ok())
        val provider = FakeProvider("tok-abc")

        registrarWith(provider).unregister()

        val request = server.takeRequest()
        assertEquals("/api/v1/devices/unregister", request.path)
        // El token va en el cuerpo porque una persona puede tener la app en varios telefonos:
        // la baja es de ESTE, no de la cuenta.
        assertEquals("""{"token":"tok-abc"}""", request.body.readUtf8())
        assertTrue("el token local debe borrarse al cerrar sesion", provider.deleteCalled)
    }

    @Test
    fun `sin token no se llama al backend y la app no truena`() = runTest(dispatcher) {
        // Es el caso real mientras Q-08 siga abierta: el proveedor inyectado es este.
        registrarWith(NoPushTokenProvider).register()

        assertEquals("sin token no debe salir ninguna peticion", 0, server.requestCount)
    }

    @Test
    fun `si el backend falla el logout igual borra el token local`() = runTest(dispatcher) {
        // Sin internet la baja remota no se logra, pero el telefono tiene que dejar de recibir.
        server.enqueue(MockResponse().setResponseCode(500).setBody(""))
        val provider = FakeProvider("tok-abc")

        registrarWith(provider).unregister()

        assertTrue("el borrado local no depende del servidor", provider.deleteCalled)
    }

    @Test
    fun `un proveedor que lanza no rompe el registro`() = runTest(dispatcher) {
        // El contrato dice que no lanza, pero si la implementacion de Firebase lo hace, el login
        // no se puede caer por eso (D-19).
        val provider = FakeProvider(token = null, throwOnToken = true)

        registrarWith(provider).register()

        assertEquals(0, server.requestCount)
        assertFalse(provider.deleteCalled)
    }
}
