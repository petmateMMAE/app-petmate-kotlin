package mx.petmate.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET

/**
 * La infraestructura compartida de pruebas se prueba a si misma (001/T-023).
 *
 * Suena redundante y no lo es: si `jsonDePrueba` dejara de coincidir con el de `NetworkModule`, o
 * si `sobre()` armara un JSON mal formado, decenas de pruebas empezarian a fallar por motivos que
 * no tienen nada que ver con lo que intentan comprobar, y se perderia una tarde buscando donde.
 */
class SharedTestSupportTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ---- Dispatchers ----

    @Test
    fun `el proveedor de prueba manda las tres propiedades al mismo dispatcher`() {
        val dispatcher = mainDispatcher.dispatcher
        val dispatchers = testDispatcherProvider(dispatcher)

        assertSame(dispatcher, dispatchers.io)
        assertSame(dispatcher, dispatchers.default)
        assertSame(dispatcher, dispatchers.main)
    }

    @Test
    fun `la regla deja usable Dispatchers Main`() = runTest {
        // Sin la regla esto truena con IllegalStateException: fuera de Android no hay un Main.
        var corrio = false

        withContext(Dispatchers.Main) { corrio = true }

        assertTrue(corrio)
    }

    // ---- Logger ----

    @Test
    fun `el logger apunta lo que se registra`() {
        val logger = RecordingLogger()

        logger.debug("TAG", "cargando mascotas")
        logger.error("TAG", "fallo la carga", IllegalStateException("boom"))

        assertEquals(listOf("cargando mascotas", "fallo la carga"), logger.mensajes)
        assertEquals(1, logger.errores.size)
        assertEquals("boom", logger.errores.single().throwable?.message)
    }

    @Test
    fun `el logger sirve para comprobar que no se filtran secretos`() {
        // Esto es lo que hace cumplir P6: que la prueba pueda exigirlo en vez de confiar.
        val logger = RecordingLogger()

        logger.debug("AUTH", "sesion iniciada para el usuario u-1")

        assertTrue(logger.noRegistro("contrasena-secreta"))
        assertFalse(logger.noRegistro("u-1"))
    }

    // ---- JSON y el sobre del backend ----

    @Test
    fun `el sobre del backend sale bien formado`() {
        val texto = sobre(codigo = 201, datos = """{"id":"p-1"}""", mensaje = "creado")

        Json.parseToJsonElement(texto)

        assertTrue(texto.contains(""""statusCode":201"""))
        assertTrue(texto.contains(""""message":"creado""""))
        assertTrue(texto.contains(""""data":{"id":"p-1"}"""))
    }

    @Test
    fun `el sobre acepta data nula`() {
        val texto = sobre(codigo = 404, datos = "null", mensaje = "No encontrado")

        Json.parseToJsonElement(texto)

        assertTrue(texto.contains(""""data":null"""))
    }

    @Test
    fun `el json de prueba ignora claves desconocidas, igual que produccion`() {
        // Si las pruebas usaran una configuracion mas estricta o mas laxa que NetworkModule,
        // pasarian en verde con respuestas que la app no sabe leer, o al contrario.
        val conClaveDeMas = """{"id":"p-1","campoQueNoConocemos":42}"""

        val leido = jsonDePrueba.decodeFromString<CosaDePrueba>(conClaveDeMas)

        assertEquals("p-1", leido.id)
    }

    @Test
    fun `los cuerpos de sesion y perfil son JSON valido`() {
        // Se arman por concatenacion, asi que vale la pena comprobar que no quedan comas sueltas.
        Json.parseToJsonElement(cuerpoDeSesion())
        Json.parseToJsonElement(cuerpoDeSesion(nombre = null, tipoDeNegocio = "Veterinary"))
        Json.parseToJsonElement(cuerpoDePerfil())
        Json.parseToJsonElement(cuerpoDePerfil(nombre = null, esAfiliado = true))
    }

    // ---- Datos falsos ----

    @Test
    fun `los constructores falsos dejan nombrar solo lo que importa`() {
        val afiliado = usuarioFalso(isAffiliate = true)

        assertTrue(afiliado.isAffiliate)
        assertEquals(CORREO, afiliado.email)
        assertEquals(NOMBRE, afiliado.name)
    }

    @Test
    fun `el perfil falso trae fecha de alta por omision`() {
        assertEquals(FECHA_ALTA, perfilFalso().memberSince)
        assertEquals(null, perfilFalso(memberSince = null).memberSince)
    }

    // ---- MockWebServer ----

    @Test
    fun `el servicio apunta al servidor simulado y lee el sobre`() = runTest {
        server.encolarSobre(200, """{"id":"p-1"}""")
        val api: ApiDePrueba = server.servicio()

        val respuesta = api.cosa()

        assertEquals(200, respuesta.statusCode)
        assertEquals("p-1", respuesta.data?.id)
        assertEquals("${RUTA_BASE_API}cosa", server.takeRequest().path)
    }

    @Test
    fun `encolarError arma un sobre con data nula`() = runTest {
        server.encolarError(404, "No encontrado")
        val api: ApiDePrueba = server.servicio()

        val respuesta = api.cosaCruda()

        assertEquals(404, respuesta.code())
        val sobreDeError = jsonDePrueba.decodeFromString<SobreDePrueba>(
            respuesta.errorBody()!!.string(),
        )
        assertEquals(404, sobreDeError.statusCode)
        assertEquals("No encontrado", sobreDeError.message)
        assertNull(sobreDeError.data)
    }

    // ---- Tipos solo para esta prueba ----

    @Serializable
    data class CosaDePrueba(val id: String)

    @Serializable
    data class SobreDePrueba(val statusCode: Int, val message: String, val data: CosaDePrueba? = null)

    // Internal y no private: Retrofit le arma un proxy dinamico y necesita poder verla.
    internal interface ApiDePrueba {
        @GET("cosa")
        suspend fun cosa(): SobreDePrueba

        /**
         * Con un codigo que no es 2xx, Retrofit lanza `HttpException` si el tipo de retorno es el
         * cuerpo pelado. Para poder leer el sobre de error hay que pedir `Response`, que es lo que
         * hacen los repositorios de verdad.
         */
        @GET("cosa")
        suspend fun cosaCruda(): Response<SobreDePrueba>
    }
}
