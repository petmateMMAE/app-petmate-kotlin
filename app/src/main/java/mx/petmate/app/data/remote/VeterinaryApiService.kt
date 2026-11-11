package mx.petmate.app.data.remote

import mx.petmate.app.data.remote.dto.AddFollowupNoteRequestDto
import mx.petmate.app.data.remote.dto.AddMedicationRequestDto
import mx.petmate.app.data.remote.dto.ApiResponse
import mx.petmate.app.data.remote.dto.ConsultationResponseDto
import mx.petmate.app.data.remote.dto.ConsultationSummaryResponseDto
import mx.petmate.app.data.remote.dto.CreateConsultationRequestDto
import mx.petmate.app.data.remote.dto.CreateFollowupRequestDto
import mx.petmate.app.data.remote.dto.DewormingByBusinessRequestDto
import mx.petmate.app.data.remote.dto.DewormingDto
import mx.petmate.app.data.remote.dto.FollowupResponseDto
import mx.petmate.app.data.remote.dto.HistoryItemResponseDto
import mx.petmate.app.data.remote.dto.PagedDto
import mx.petmate.app.data.remote.dto.UpdateFollowupRequestDto
import mx.petmate.app.data.remote.dto.VaccineByBusinessRequestDto
import mx.petmate.app.data.remote.dto.VaccineDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints del backend para consultas, recetas, vacunas/desparasitaciones por veterinaria,
 * historial medico unificado y seguimiento (pm-130 a pm-142).
 * La BASE_URL de Retrofit ya termina en `/api/v1/`.
 */
interface VeterinaryApiService {

    // ---------- Consultas ----------
    @POST("consultations/create")
    suspend fun createConsultation(
        @Header("Authorization") bearer: String,
        @Body body: CreateConsultationRequestDto,
    ): ApiResponse<ConsultationResponseDto>

    @POST("consultations/add-medication")
    suspend fun addMedication(
        @Header("Authorization") bearer: String,
        @Body body: AddMedicationRequestDto,
    ): ApiResponse<ConsultationResponseDto>

    @GET("consultations/get-all")
    suspend fun getConsultations(
        @Header("Authorization") bearer: String,
        @Query("petId") petId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): ApiResponse<PagedDto<ConsultationSummaryResponseDto>>

    @GET("consultations/get-by-id/{id}")
    suspend fun getConsultationById(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
    ): ApiResponse<ConsultationResponseDto>

    // ---------- Vacunas y desparasitaciones por veterinaria ----------
    @POST("vaccines/create-by-business")
    suspend fun createVaccineByBusiness(
        @Header("Authorization") bearer: String,
        @Body body: VaccineByBusinessRequestDto,
    ): ApiResponse<VaccineDto>

    @POST("dewormings/create-by-business")
    suspend fun createDewormingByBusiness(
        @Header("Authorization") bearer: String,
        @Body body: DewormingByBusinessRequestDto,
    ): ApiResponse<DewormingDto>

    // ---------- Historial medico unificado ----------
    @GET("history/get-all")
    suspend fun getHistory(
        @Header("Authorization") bearer: String,
        @Query("petId") petId: String,
        @Query("types") types: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): ApiResponse<PagedDto<HistoryItemResponseDto>>

    // ---------- Seguimiento ----------
    @POST("followups/create")
    suspend fun createFollowup(
        @Header("Authorization") bearer: String,
        @Body body: CreateFollowupRequestDto,
    ): ApiResponse<FollowupResponseDto>

    @PUT("followups/update/{id}")
    suspend fun updateFollowup(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: UpdateFollowupRequestDto,
    ): ApiResponse<FollowupResponseDto>

    @POST("followups/add-note")
    suspend fun addFollowupNote(
        @Header("Authorization") bearer: String,
        @Body body: AddFollowupNoteRequestDto,
    ): ApiResponse<FollowupResponseDto>

    @GET("followups/get-by-consultation/{id}")
    suspend fun getFollowupByConsultation(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
    ): ApiResponse<FollowupResponseDto>

    @GET("followups/get-all")
    suspend fun getFollowups(
        @Header("Authorization") bearer: String,
        @Query("reviewDate") reviewDate: String? = null,
        @Query("onlyActive") onlyActive: Boolean? = true,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): ApiResponse<PagedDto<FollowupResponseDto>>
}
