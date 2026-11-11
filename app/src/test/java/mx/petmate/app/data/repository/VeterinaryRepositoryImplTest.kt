package mx.petmate.app.data.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.VeterinaryApiService
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.MedicationDraft
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
 * Consultas, recetas, vacunas y desparasitaciones por veterinaria, e historial del dueno,
 * contra el formato real del backend 0.4.0 (pm-130 a pm-142).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VeterinaryRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: VeterinaryRepositoryImpl
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
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        every { sessionStore.readAccessToken() } returns "acc-1"
        every { sessionStore.readRefreshToken() } returns "ref-1"
        val caller = AuthorizedCaller(retrofit.create(AuthApiService::class.java), sessionStore, authRepository)
        repository = VeterinaryRepositoryImpl(retrofit.create(VeterinaryApiService::class.java), caller, dispatchers)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun response(code: Int, data: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json")
            .setBody("""{"statusCode":$code,"message":"ok","data":$data}""")

    private val origin = """{"type":"Business","businessId":"b-1","businessName":"Vet Tula"}"""

    private val consultationJson = """{"id":"c-1","petId":"p-1","consultedOn":"2026-11-11","origin":$origin,
        "reason":"Vomito","diagnosis":"Gastritis","weightKg":12.4,
        "medications":[{"name":"Amoxicilina","dose":"1 tableta","frequency":"cada 12 h","duration":"7 dias","instructions":null}],
        "createdAt":"2026-11-11T10:00:00Z"}"""

    @Test
    fun `crear consulta manda solo los campos del contrato con el bearer`() = runTest(dispatcher) {
        server.enqueue(response(201, consultationJson))

        val result = repository.createConsultation(
            ConsultationDraft(
                petId = "p-1",
                reason = "  Vomito  ",
                diagnosis = "Gastritis",
                weightKg = 12.4,
                medications = listOf(MedicationDraft("Amoxicilina", "1 tableta", "cada 12 h", "7 dias")),
            ),
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/consultations/create", request.path)
        assertEquals("Bearer acc-1", request.getHeader("Authorization"))
        assertEquals(
            """{"petId":"p-1","reason":"Vomito","diagnosis":"Gastritis","weightKg":12.4,""" +
                """"medications":[{"name":"Amoxicilina","dose":"1 tableta","frequency":"cada 12 h","duration":"7 dias"}]}""",
            request.body.readUtf8(),
        )
        val consultation = (result as AppResult.Success).data
        assertEquals("c-1", consultation.id)
        assertTrue(consultation.origin.byBusiness)
        assertEquals("Vet Tula", consultation.origin.businessName)
        assertEquals("Amoxicilina", consultation.medications.single().name)
    }

    @Test
    fun `una consulta sin peso ni receta no manda esos campos`() = runTest(dispatcher) {
        server.enqueue(response(201, consultationJson))

        repository.createConsultation(ConsultationDraft(petId = "p-1", reason = "Revision", diagnosis = "Sano"))

        assertEquals(
            """{"petId":"p-1","reason":"Revision","diagnosis":"Sano"}""",
            server.takeRequest().body.readUtf8(),
        )
    }

    @Test
    fun `agregar medicamento usa el id de la consulta`() = runTest(dispatcher) {
        server.enqueue(response(200, consultationJson))

        repository.addMedication("c-1", MedicationDraft("Meloxicam", "0.5 ml", "cada 24 h", "3 dias", "con comida"))

        val request = server.takeRequest()
        assertEquals("/api/v1/consultations/add-medication", request.path)
        assertEquals(
            """{"consultationId":"c-1","name":"Meloxicam","dose":"0.5 ml","frequency":"cada 24 h","duration":"3 dias","instructions":"con comida"}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun `vacunar por negocio manda petId, fecha y nombre`() = runTest(dispatcher) {
        server.enqueue(
            response(
                201,
                """{"id":"v-1","name":"Rabia","appliedOn":"2026-11-11","batch":"L-22","appliedBy":"Vet Tula",
                   "nextDoseOn":"2027-11-11","origin":$origin}""",
            ),
        )

        val vaccine = (repository.createVaccineByBusiness("p-1", " Rabia ", "2026-11-11", "L-22", "2027-11-11") as AppResult.Success).data

        val request = server.takeRequest()
        assertEquals("/api/v1/vaccines/create-by-business", request.path)
        assertEquals(
            """{"petId":"p-1","appliedOn":"2026-11-11","name":"Rabia","batch":"L-22","nextDoseOn":"2027-11-11"}""",
            request.body.readUtf8(),
        )
        assertTrue(vaccine.origin.byBusiness)
        assertEquals("Vet Tula", vaccine.appliedBy)
    }

    @Test
    fun `desparasitar por negocio manda el tipo del contrato`() = runTest(dispatcher) {
        server.enqueue(
            response(
                201,
                """{"id":"d-1","product":"Drontal","appliedOn":"2026-11-11","type":"Internal","appliedBy":"Vet Tula",
                   "nextOn":"2027-02-11","origin":$origin}""",
            ),
        )

        val deworming = (
            repository.createDewormingByBusiness("p-1", "Drontal", "2026-11-11", DewormingType.Internal, "2027-02-11") as AppResult.Success
            ).data

        val request = server.takeRequest()
        assertEquals("/api/v1/dewormings/create-by-business", request.path)
        assertEquals(
            """{"petId":"p-1","appliedOn":"2026-11-11","product":"Drontal","type":"Internal","nextOn":"2027-02-11"}""",
            request.body.readUtf8(),
        )
        assertEquals(DewormingType.Internal, deworming.type)
    }

    @Test
    fun `el historial del dueno llega paginado y dice quien lo registro`() = runTest(dispatcher) {
        server.enqueue(
            response(
                200,
                """{"items":[
                     {"kind":"Consultation","id":"c-1","date":"2026-11-11","title":"Vomito","origin":$origin},
                     {"kind":"Vaccine","id":"v-1","date":"2026-10-01","title":"Rabia","origin":{"type":"Owner","businessId":null,"businessName":null}}
                   ],"page":1,"pageSize":50,"totalCount":2,"totalPages":1}""",
            ),
        )

        val items = (repository.getHistory("p-1") as AppResult.Success).data

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/api/v1/history/get-all?petId=p-1"))
        assertEquals("Bearer acc-1", request.getHeader("Authorization"))
        assertEquals(2, items.size)
        assertTrue(items.first().origin.byBusiness)
        assertFalse(items.last().origin.byBusiness)
    }

    @Test
    fun `el detalle de una consulta trae su receta`() = runTest(dispatcher) {
        server.enqueue(response(200, consultationJson))

        val consultation = (repository.getConsultationById("c-1") as AppResult.Success).data

        assertEquals("/api/v1/consultations/get-by-id/c-1", server.takeRequest().path)
        assertEquals("1 tableta", consultation.medications.single().dose)
    }

    @Test
    fun `sin autorizacion vigente la veterinaria recibe 403`() = runTest(dispatcher) {
        server.enqueue(response(403, "null"))

        val result = repository.createConsultation(ConsultationDraft("p-1", "Revision", "Sano"))

        assertEquals(AppError.Server(403), (result as AppResult.Failure).error)
    }
}
