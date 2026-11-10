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
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.PetsApiService
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
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
 * Mascotas, QR y carnet contra un servidor simulado con el formato real del backend
 * (docs/api/endpoints.md, pm-040 a pm-103).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: PetRepositoryImpl
    private val sessionStore: SessionStore = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
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
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        every { sessionStore.readAccessToken() } returns "acc-1"
        every { sessionStore.readRefreshToken() } returns "ref-1"
        val caller = AuthorizedCaller(retrofit.create(AuthApiService::class.java), sessionStore, authRepository)
        repository = PetRepositoryImpl(retrofit.create(PetsApiService::class.java), caller, dispatchers)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun wrapper(code: Int, data: String) = """{"statusCode":$code,"message":"ok","data":$data}"""

    private val petJson = """{"id":"p-1","name":"Firulais","species":"Perro","breed":"Beagle","birthDate":"2020-05-01",
        "sex":"Male","photoUrl":null,"createdAt":"2026-11-09T10:00:00Z","updatedAt":"2026-11-09T10:00:00Z"}"""

    @Test
    fun `crear mascota manda solo los campos del contrato con el bearer`() = runTest(dispatcher) {
        server.enqueue(json(201, wrapper(201, petJson)))

        val result = repository.createPet(PetDraft(" Firulais ", PetSpecies.Dog, "Beagle", "2020-05-01", PetSex.Male))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/pets/create", request.path)
        assertEquals("Bearer acc-1", request.getHeader("Authorization"))
        assertEquals(
            """{"name":"Firulais","species":"Perro","breed":"Beagle","birthDate":"2020-05-01","sex":"Male"}""",
            request.body.readUtf8(),
        )
        val pet = (result as AppResult.Success).data
        assertEquals(PetSpecies.Dog, pet.species)
        assertEquals(PetSex.Male, pet.sex)
    }

    @Test
    fun `sin raza ni fecha no se mandan esos campos`() = runTest(dispatcher) {
        server.enqueue(json(201, wrapper(201, petJson)))

        repository.createPet(PetDraft("Michi", PetSpecies.Cat, "  ", null, PetSex.Unknown))

        assertEquals("""{"name":"Michi","species":"Gato","sex":"Unknown"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `el listado lee la pagina del backend`() = runTest(dispatcher) {
        server.enqueue(json(200, wrapper(200, """{"items":[$petJson],"page":1,"pageSize":50,"totalCount":1,"totalPages":1}""")))

        val pets = (repository.getPets() as AppResult.Success).data

        assertEquals("/api/v1/pets/get-all?page=1&pageSize=50", server.takeRequest().path)
        assertEquals(listOf("Firulais"), pets.map { it.name })
    }

    @Test
    fun `el QR trae token y codigo corto y regenerarlo usa POST`() = runTest(dispatcher) {
        server.enqueue(json(200, wrapper(200, """{"petId":"p-1","qrToken":"tok-43","qrCode":"PM7K3QXA"}""")))
        server.enqueue(json(200, wrapper(200, """{"petId":"p-1","qrToken":"tok-nuevo","qrCode":"PM4HD9TW"}""")))

        val qr = (repository.getQr("p-1") as AppResult.Success).data
        val renewed = (repository.regenerateQr("p-1") as AppResult.Success).data

        assertEquals("PM7K3QXA", qr.shortCode)
        assertEquals("/api/v1/pets/get-qr/p-1", server.takeRequest().path)
        val regen = server.takeRequest()
        assertEquals("POST", regen.method)
        assertEquals("/api/v1/pets/regenerate-qr/p-1", regen.path)
        assertEquals("tok-nuevo", renewed.token)
    }

    @Test
    fun `si el access token expiro se renueva una vez y se reintenta`() = runTest(dispatcher) {
        server.enqueue(json(401, wrapper(401, "null")))
        server.enqueue(
            json(
                200,
                wrapper(
                    200,
                    """{"accessToken":"acc-2","refreshToken":"ref-2","user":{"id":"u-1","email":"ana@petmate.mx","role":"User"}}""",
                ),
            ),
        )
        server.enqueue(json(200, wrapper(200, petJson)))

        val result = repository.getPet("p-1")

        assertTrue(result is AppResult.Success)
        verify { sessionStore.updateTokens("acc-2", "ref-2") }
        server.takeRequest()
        assertEquals("/api/v1/auth/refresh", server.takeRequest().path)
        assertEquals("Bearer acc-2", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `si la renovacion falla se cierra la sesion`() = runTest(dispatcher) {
        server.enqueue(json(401, wrapper(401, "null")))
        server.enqueue(json(401, wrapper(401, "null")))

        val result = repository.getPets()

        assertEquals(AppError.Unauthorized, (result as AppResult.Failure).error)
        verify { authRepository.expireSession() }
    }

    @Test
    fun `un 403 del carnet no cierra la sesion (lo registro el negocio)`() = runTest(dispatcher) {
        server.enqueue(json(403, wrapper(403, "null")))

        val result = repository.deleteVaccine("v-1")

        assertEquals(AppError.Server(403), (result as AppResult.Failure).error)
        verify(exactly = 0) { authRepository.expireSession() }
    }

    @Test
    fun `mascota ajena o inexistente es 404`() = runTest(dispatcher) {
        server.enqueue(json(404, wrapper(404, "null")))

        assertEquals(AppError.Server(404), (repository.getPet("otra") as AppResult.Failure).error)
    }

    @Test
    fun `el carnet junta vacunas, desparasitaciones, pesos y alergias con su origen`() = runTest(dispatcher) {
        val page = { items: String -> wrapper(200, """{"items":[$items],"page":1,"pageSize":50,"totalCount":1,"totalPages":1}""") }
        val dispatcherMap = mapOf(
            "vaccines" to page("""{"id":"v-1","petId":"p-1","appliedOn":"2026-05-12","name":"Rabia","nextDoseOn":"2027-05-12",
                "origin":{"type":"Business","businessId":"b-1","businessName":"Veterinaria San Marcos"}}"""),
            "dewormings" to page("""{"id":"d-1","petId":"p-1","appliedOn":"2026-08-20","product":"Drontal","type":"Internal","origin":{"type":"Owner"}}"""),
            "weights" to page("""{"id":"w-1","petId":"p-1","measuredOn":"2026-10-13","kilograms":12.4,"origin":{"type":"Owner"}}"""),
            "allergies" to page("""{"id":"a-1","petId":"p-1","name":"Pollo","kind":"Allergy","status":"Resolved","origin":{"type":"Owner"}}"""),
        )
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                val key = dispatcherMap.keys.first { request.path!!.contains("/$it/get-all?petId=p-1") }
                return json(200, dispatcherMap.getValue(key))
            }
        }

        val carnet = (repository.getCarnet("p-1") as AppResult.Success).data

        assertTrue(carnet.vaccines.single().origin.byBusiness)
        assertEquals("Veterinaria San Marcos", carnet.vaccines.single().origin.businessName)
        assertFalse(carnet.dewormings.single().origin.byBusiness)
        assertEquals(12.4, carnet.weights.single().kilograms, 0.0)
        assertFalse(carnet.allergies.single().active)
    }

    @Test
    fun `una alergia superada se crea y luego se actualiza su estado`() = runTest(dispatcher) {
        val allergy = """{"id":"a-1","petId":"p-1","name":"Pollo","kind":"Allergy","status":"Active","origin":{"type":"Owner"}}"""
        server.enqueue(json(201, wrapper(201, allergy)))
        server.enqueue(json(200, wrapper(200, allergy.replace("Active", "Resolved"))))

        val result = repository.addAllergy("p-1", AllergyDraft("Pollo", AllergyKind.Allergy, "", active = false))

        assertEquals("""{"petId":"p-1","name":"Pollo","kind":"Allergy"}""", server.takeRequest().body.readUtf8())
        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        assertEquals("/api/v1/allergies/update/a-1", update.path)
        assertEquals("""{"name":"Pollo","kind":"Allergy","status":"Resolved"}""", update.body.readUtf8())
        assertFalse((result as AppResult.Success).data.active)
    }
}
