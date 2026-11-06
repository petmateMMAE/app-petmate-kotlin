package mx.petmate.app.data.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.RegisterData
import mx.petmate.app.domain.model.User
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
 * Contrato real del backend v0.1.0 contra un servidor simulado (spec 002, Q-01).
 * Las respuestas copian el formato de docs/api/endpoints.md del backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuthApiService
    private val sessionStore: SessionStore = mockk(relaxed = true)
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
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun repository() = AuthRepositoryImpl(api, sessionStore, dispatchers, logger)

    private fun authBody(code: Int, role: String = "Owner", access: String = "acc-1", refresh: String = "ref-1") = """
        {"statusCode":$code,"message":"ok","data":{
          "accessToken":"$access","tokenType":"Bearer","accessTokenExpiresAt":"2026-11-06T17:15:00Z",
          "refreshToken":"$refresh","refreshTokenExpiresAt":"2026-12-06T17:00:00Z",
          "user":{"id":"u-1","email":"ana@petmate.mx","role":"$role"}}}
    """.trimIndent()

    private fun profileBody(name: String?, role: String = "Owner") = """
        {"statusCode":200,"message":"ok","data":{"id":"u-1","email":"ana@petmate.mx","role":"$role",
          "displayName":${if (name == null) "null" else "\"$name\""},"createdAt":"2026-11-06T17:00:00Z"}}
    """.trimIndent()

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun `el registro envia solo correo y contrasena y luego guarda el nombre en el servidor`() = runTest(dispatcher) {
        server.enqueue(json(201, authBody(201)))
        server.enqueue(json(200, profileBody("Ana")))

        val result = repository().register(
            RegisterData("Ana", " ana@petmate.mx ", "contrasena-segura", "contrasena-segura", true),
        )

        val request = server.takeRequest()
        assertEquals("/api/v1/auth/register", request.path)
        val sent = request.body.readUtf8()
        assertEquals("""{"email":"ana@petmate.mx","password":"contrasena-segura"}""", sent)
        assertTrue(result is AppResult.Success)
        assertEquals("Ana", (result as AppResult.Success).data.name)
        verify { sessionStore.rememberName("ana@petmate.mx", "Ana") }
        verify { sessionStore.save(any(), "acc-1", "ref-1") }
        val nameRequest = server.takeRequest()
        assertEquals("PUT", nameRequest.method)
        assertEquals("/api/v1/users/me/update", nameRequest.path)
        assertEquals("Bearer acc-1", nameRequest.getHeader("Authorization"))
        assertEquals("""{"displayName":"Ana"}""", nameRequest.body.readUtf8())
    }

    @Test
    fun `si guardar el nombre falla el registro igual termina con el nombre local`() = runTest(dispatcher) {
        server.enqueue(json(201, authBody(201)))
        server.enqueue(json(503, """{"statusCode":503,"message":"No disponible.","data":null}"""))

        val result = repository().register(
            RegisterData("Ana", "ana@petmate.mx", "contrasena-segura", "contrasena-segura", true),
        )

        assertEquals("Ana", (result as AppResult.Success).data.name)
    }

    @Test
    fun `el login toma el nombre del servidor`() = runTest(dispatcher) {
        server.enqueue(json(200, authBody(200)))
        server.enqueue(json(200, profileBody("Ana Pérez")))

        val result = repository().login(LoginCredentials("ana@petmate.mx", "contrasena-segura"))

        assertEquals("Ana Pérez", (result as AppResult.Success).data.name)
        server.takeRequest()
        val profile = server.takeRequest()
        assertEquals("/api/v1/users/me/get", profile.path)
        assertEquals("Bearer acc-1", profile.getHeader("Authorization"))
    }

    @Test
    fun `el perfil renueva el token una vez si expiro`() = runTest(dispatcher) {
        every { sessionStore.readAccessToken() } returns "acc-viejo"
        every { sessionStore.readRefreshToken() } returns "ref-viejo"
        every { sessionStore.readUser() } returns null
        server.enqueue(json(401, """{"statusCode":401,"message":"No autenticado.","data":null}"""))
        server.enqueue(json(200, authBody(200, access = "acc-nuevo", refresh = "ref-nuevo")))
        server.enqueue(json(200, profileBody("Ana", role = "Business")))

        val result = repository().loadProfile()

        val profile = (result as AppResult.Success).data
        assertEquals("Ana", profile.name)
        assertTrue(profile.isAffiliate)
        assertEquals("2026-11-06T17:00:00Z", profile.memberSince)
        verify { sessionStore.updateTokens("acc-nuevo", "ref-nuevo") }
        server.takeRequest()
        server.takeRequest()
        assertEquals("Bearer acc-nuevo", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `si la renovacion tambien falla el perfil cierra la sesion`() = runTest(dispatcher) {
        every { sessionStore.readAccessToken() } returns "acc-viejo"
        every { sessionStore.readRefreshToken() } returns "ref-viejo"
        server.enqueue(json(401, """{"statusCode":401,"message":"No autenticado.","data":null}"""))
        server.enqueue(json(401, """{"statusCode":401,"message":"La sesión no es válida.","data":null}"""))

        val result = repository().loadProfile()

        assertEquals(AppError.Unauthorized, (result as AppResult.Failure).error)
        verify { sessionStore.clear() }
    }

    @Test
    fun `un usuario Business se marca como afiliado`() = runTest(dispatcher) {
        server.enqueue(json(200, authBody(200, role = "Business")))
        server.enqueue(json(200, profileBody(null, role = "Business")))
        every { sessionStore.knownNameFor(any()) } returns null

        val result = repository().login(LoginCredentials("ana@petmate.mx", "contrasena-segura"))

        val user = (result as AppResult.Success<User>).data
        assertTrue(user.isAffiliate)
        assertEquals("ana", user.name)
    }

    @Test
    fun `credenciales invalidas devuelven Unauthorized`() = runTest(dispatcher) {
        server.enqueue(json(401, """{"statusCode":401,"message":"Credenciales inválidas.","data":null}"""))

        val result = repository().login(LoginCredentials("ana@petmate.mx", "incorrecta-123"))

        assertEquals(AppError.Unauthorized, (result as AppResult.Failure).error)
    }

    @Test
    fun `correo repetido y limite de intentos conservan su codigo`() = runTest(dispatcher) {
        server.enqueue(json(409, """{"statusCode":409,"message":"No se pudo completar el registro.","data":null}"""))
        server.enqueue(json(429, """{"statusCode":429,"message":"Demasiadas solicitudes.","data":null}"""))
        val data = RegisterData("Ana", "ana@petmate.mx", "contrasena-segura", "contrasena-segura", true)

        val conflict = repository().register(data)
        val limited = repository().register(data)

        assertEquals(AppError.Server(409), (conflict as AppResult.Failure).error)
        assertEquals(AppError.Server(429), (limited as AppResult.Failure).error)
    }

    @Test
    fun `el logout manda el bearer y el refresh token y borra lo local`() = runTest(dispatcher) {
        every { sessionStore.readAccessToken() } returns "acc-1"
        every { sessionStore.readRefreshToken() } returns "ref-1"
        server.enqueue(json(200, """{"statusCode":200,"message":"Sesión cerrada.","data":null}"""))

        val result = repository().logout()

        val request = server.takeRequest()
        assertEquals("/api/v1/auth/logout", request.path)
        assertEquals("Bearer acc-1", request.getHeader("Authorization"))
        assertEquals("""{"refreshToken":"ref-1"}""", request.body.readUtf8())
        assertTrue(result is AppResult.Success)
        verify { sessionStore.clear() }
    }

    @Test
    fun `si el access token expiro el logout renueva y reintenta una vez`() = runTest(dispatcher) {
        every { sessionStore.readAccessToken() } returns "acc-viejo"
        every { sessionStore.readRefreshToken() } returns "ref-viejo"
        server.enqueue(json(401, """{"statusCode":401,"message":"No autenticado.","data":null}"""))
        server.enqueue(json(200, authBody(200, access = "acc-nuevo", refresh = "ref-nuevo")))
        server.enqueue(json(200, """{"statusCode":200,"message":"Sesión cerrada.","data":null}"""))

        repository().logout()

        assertEquals("/api/v1/auth/logout", server.takeRequest().path)
        val refresh = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", refresh.path)
        assertEquals("""{"refreshToken":"ref-viejo"}""", refresh.body.readUtf8())
        val retry = server.takeRequest()
        assertEquals("Bearer acc-nuevo", retry.getHeader("Authorization"))
        assertEquals("""{"refreshToken":"ref-nuevo"}""", retry.body.readUtf8())
        verify { sessionStore.updateTokens("acc-nuevo", "ref-nuevo") }
        verify { sessionStore.clear() }
    }

    @Test
    fun `sin red el logout igual borra la sesion local`() = runTest(dispatcher) {
        every { sessionStore.readAccessToken() } returns "acc-1"
        every { sessionStore.readRefreshToken() } returns "ref-1"
        server.shutdown()

        val result = repository().logout()

        assertTrue(result is AppResult.Success)
        verify { sessionStore.clear() }
        assertFalse(result is AppResult.Failure)
    }
}
