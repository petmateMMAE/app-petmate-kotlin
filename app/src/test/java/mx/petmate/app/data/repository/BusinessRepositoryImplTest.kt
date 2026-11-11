package mx.petmate.app.data.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.BusinessApiService
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.BusinessDraft
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.ShareDuration
import mx.petmate.app.domain.model.SharedData
import mx.petmate.app.domain.model.TimeRange
import mx.petmate.app.domain.repository.AuthRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/** Negocio, mapa publico, autorizaciones y escaneo contra el formato real del backend 0.4.0 (pm-105 a pm-130). */
@OptIn(ExperimentalCoroutinesApi::class)
class BusinessRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: BusinessRepositoryImpl
    private val sessionStore: SessionStore = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        every { sessionStore.readAccessToken() } returns "acc-1"
        every { sessionStore.readRefreshToken() } returns "ref-1"
        val caller = AuthorizedCaller(retrofit.create(AuthApiService::class.java), sessionStore, authRepository)
        repository = BusinessRepositoryImpl(retrofit.create(BusinessApiService::class.java), caller, sessionStore, json, dispatchers)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun response(code: Int, data: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody("""{"statusCode":$code,"message":"ok","data":$data}""")

    private val businessJson = """{"id":"b-1","type":"Veterinary","name":"Vet Tula","phone":"7711234567","whatsApp":null,
        "address":"Centro","description":null,"latitude":20.05,"longitude":-99.34,"licenseNumber":"1234567",
        "schedule":{"monday":[{"open":"09:00","close":"18:00"}]}}"""

    private val draft = BusinessDraft(
        kind = BusinessKind.Veterinary,
        name = " Vet Tula ",
        phone = "7711234567",
        whatsApp = "",
        address = "Centro",
        description = "",
        licenseNumber = "1234567",
        schedule = mapOf("monday" to listOf(TimeRange("09:00", "18:00")), "sunday" to emptyList()),
    )

    @Test
    fun `afiliar manda solo los campos del contrato y guarda la copia local`() = runTest(dispatcher) {
        server.enqueue(response(201, businessJson))
        val saved = slot<String>()
        every { sessionStore.saveBusinessJson(capture(saved)) } returns Unit

        val result = repository.createBusiness(draft, 20.05, -99.34)

        val request = server.takeRequest()
        assertEquals("/api/v1/businesses/create", request.path)
        assertEquals("Bearer acc-1", request.getHeader("Authorization"))
        assertEquals(
            """{"type":"Veterinary","name":"Vet Tula","phone":"7711234567","address":"Centro","latitude":20.05,"longitude":-99.34,""" +
                """"licenseNumber":"1234567","schedule":{"monday":[{"open":"09:00","close":"18:00"}]}}""",
            request.body.readUtf8(),
        )
        assertEquals("Vet Tula", (result as AppResult.Success).data.name)
        assertTrue(saved.captured.contains("\"id\":\"b-1\""))
    }

    @Test
    fun `una tienda no manda cedula aunque se haya escrito`() = runTest(dispatcher) {
        server.enqueue(response(201, businessJson.replace("Veterinary", "Store")))

        repository.createBusiness(draft.copy(kind = BusinessKind.Store), 1.0, 2.0)

        assertTrue(!server.takeRequest().body.readUtf8().contains("licenseNumber"))
    }

    @Test
    fun `editar reemplaza el perfil completo sin tipo ni direccion`() = runTest(dispatcher) {
        server.enqueue(response(200, businessJson))

        repository.updateBusiness(draft.copy(whatsApp = "7719998888"))

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals(
            """{"name":"Vet Tula","phone":"7711234567","whatsApp":"7719998888","schedule":{"monday":[{"open":"09:00","close":"18:00"}]}}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun `dar de baja borra la copia local`() = runTest(dispatcher) {
        server.enqueue(response(200, "null"))

        val result = repository.deleteBusiness()

        assertTrue(result is AppResult.Success)
        verify { sessionStore.saveBusinessJson(null) }
    }

    @Test
    fun `el mapa completa coordenadas con el perfil publico y sin token`() = runTest(dispatcher) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path!!.startsWith("/api/v1/businesses/get-nearby") -> response(
                    200,
                    """{"items":[{"id":"b-1","name":"Vet Tula","type":"Veterinary","photoUrl":null,"isOpenNow":true,
                       "todayHours":[{"open":"09:00","close":"18:00"}],"distanceMeters":420.5}],"page":1,"pageSize":50,"totalCount":1,"totalPages":1}""",
                )
                request.path == "/api/v1/businesses/get-by-id/b-1" -> response(
                    200,
                    """{"id":"b-1","name":"Vet Tula","type":"Veterinary","isOpenNow":true,"todayHours":[],"schedule":{},
                       "latitude":20.05,"longitude":-99.34,"address":"Centro","phone":"7711234567","whatsApp":"7719998888","services":[]}""",
                )
                else -> MockResponse().setResponseCode(404)
            }
        }

        val list = (repository.getNearby(20.0, -99.0) as AppResult.Success).data

        assertEquals(1, list.size)
        assertEquals(20.05, list.first().latitude!!, 0.0001)
        assertEquals("7719998888", list.first().whatsApp)
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `compartir manda petId, grupos y minutos`() = runTest(dispatcher) {
        server.enqueue(
            response(
                201,
                """{"id":"g-1","petId":"p-1","businessId":null,"data":["Vaccines"],"includesAllergies":true,
                   "createdAt":"2026-11-11T10:00:00Z","expiresAt":"2026-11-12T10:00:00Z"}""",
            ),
        )

        val grant = (repository.shareAccess("p-1", null, setOf(SharedData.Vaccines), ShareDuration.OneDay) as AppResult.Success).data

        assertEquals("""{"petId":"p-1","data":["Vaccines"],"durationMinutes":1440}""", server.takeRequest().body.readUtf8())
        assertEquals(listOf(SharedData.Vaccines), grant.data)
    }

    @Test
    fun `la ficha de una tienda no trae historial`() = runTest(dispatcher) {
        server.enqueue(
            response(
                200,
                """{"photoUrl":null,"name":"Firulais","species":"Perro","breed":"Beagle","ageYears":5,"ageMonths":2,
                   "allergies":[{"name":"Pollo","kind":"Allergy","notes":null}]}""",
            ),
        )

        val ficha = (repository.resolveScan(" K7Q2M9XA ") as AppResult.Success).data

        assertEquals("""{"code":"K7Q2M9XA"}""", server.takeRequest().body.readUtf8())
        assertEquals("Pollo", ficha.allergies.single().name)
        assertNull(ficha.vaccines)
        assertNull(ficha.consultations)
    }

    @Test
    fun `sin autorizacion el escaneo responde 403`() = runTest(dispatcher) {
        server.enqueue(response(403, "null"))

        val result = repository.resolveScan("K7Q2M9XA")

        assertEquals(AppError.Server(403), (result as AppResult.Failure).error)
    }
}
