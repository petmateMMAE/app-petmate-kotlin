package mx.petmate.app.data.remote

import mx.petmate.app.data.remote.dto.AllergyCreateRequestDto
import mx.petmate.app.data.remote.dto.AllergyDto
import mx.petmate.app.data.remote.dto.AllergyUpdateRequestDto
import mx.petmate.app.data.remote.dto.ApiResponse
import mx.petmate.app.data.remote.dto.DewormingDto
import mx.petmate.app.data.remote.dto.DewormingRequestDto
import mx.petmate.app.data.remote.dto.PagedDto
import mx.petmate.app.data.remote.dto.PetDto
import mx.petmate.app.data.remote.dto.PetQrDto
import mx.petmate.app.data.remote.dto.PetRequestDto
import mx.petmate.app.data.remote.dto.VaccineDto
import mx.petmate.app.data.remote.dto.VaccineRequestDto
import mx.petmate.app.data.remote.dto.WeightDto
import mx.petmate.app.data.remote.dto.WeightRequestDto
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mascotas, QR y carnet de salud del backend (pm-040 a pm-103). La BASE_URL ya termina en `/api/v1/`.
 * Todas exigen `Authorization: Bearer <access token>`; el dueno sale del token, nunca del cuerpo.
 */
interface PetsApiService {

    // ---------- Mascotas ----------
    @POST("pets/create")
    suspend fun createPet(@Header("Authorization") bearer: String, @Body body: PetRequestDto): ApiResponse<PetDto>

    @GET("pets/get-all")
    suspend fun getPets(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = MAX_PAGE_SIZE,
    ): ApiResponse<PagedDto<PetDto>>

    @GET("pets/get-by-id/{id}")
    suspend fun getPet(@Header("Authorization") bearer: String, @Path("id") id: String): ApiResponse<PetDto>

    @PUT("pets/update/{id}")
    suspend fun updatePet(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: PetRequestDto,
    ): ApiResponse<PetDto>

    @DELETE("pets/delete/{id}")
    suspend fun deletePet(@Header("Authorization") bearer: String, @Path("id") id: String): ApiResponse<JsonElement?>

    // ---------- QR ----------
    @GET("pets/get-qr/{id}")
    suspend fun getQr(@Header("Authorization") bearer: String, @Path("id") id: String): ApiResponse<PetQrDto>

    @POST("pets/regenerate-qr/{id}")
    suspend fun regenerateQr(@Header("Authorization") bearer: String, @Path("id") id: String): ApiResponse<PetQrDto>

    // ---------- Vacunas ----------
    @POST("vaccines/create")
    suspend fun createVaccine(@Header("Authorization") bearer: String, @Body body: VaccineRequestDto): ApiResponse<VaccineDto>

    @GET("vaccines/get-all")
    suspend fun getVaccines(
        @Header("Authorization") bearer: String,
        @Query("petId") petId: String,
        @Query("pageSize") pageSize: Int = MAX_PAGE_SIZE,
    ): ApiResponse<PagedDto<VaccineDto>>

    @DELETE("vaccines/delete/{id}")
    suspend fun deleteVaccine(@Header("Authorization") bearer: String, @Path("id") id: String): ApiResponse<JsonElement?>

    // ---------- Desparasitaciones ----------
    @POST("dewormings/create")
    suspend fun createDeworming(@Header("Authorization") bearer: String, @Body body: DewormingRequestDto): ApiResponse<DewormingDto>

    @GET("dewormings/get-all")
    suspend fun getDewormings(
        @Header("Authorization") bearer: String,
        @Query("petId") petId: String,
        @Query("pageSize") pageSize: Int = MAX_PAGE_SIZE,
    ): ApiResponse<PagedDto<DewormingDto>>

    @DELETE("dewormings/delete/{id}")
    suspend fun deleteDeworming(@Header("Authorization") bearer: String, @Path("id") id: String): ApiResponse<JsonElement?>

    // ---------- Pesos ----------
    @POST("weights/create")
    suspend fun createWeight(@Header("Authorization") bearer: String, @Body body: WeightRequestDto): ApiResponse<WeightDto>

    @GET("weights/get-all")
    suspend fun getWeights(
        @Header("Authorization") bearer: String,
        @Query("petId") petId: String,
        @Query("pageSize") pageSize: Int = MAX_PAGE_SIZE,
    ): ApiResponse<PagedDto<WeightDto>>

    // ---------- Alergias y condiciones ----------
    @POST("allergies/create")
    suspend fun createAllergy(@Header("Authorization") bearer: String, @Body body: AllergyCreateRequestDto): ApiResponse<AllergyDto>

    @GET("allergies/get-all")
    suspend fun getAllergies(
        @Header("Authorization") bearer: String,
        @Query("petId") petId: String,
        @Query("pageSize") pageSize: Int = MAX_PAGE_SIZE,
    ): ApiResponse<PagedDto<AllergyDto>>

    @PUT("allergies/update/{id}")
    suspend fun updateAllergy(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: AllergyUpdateRequestDto,
    ): ApiResponse<AllergyDto>

    companion object {
        /** Maximo que acepta el backend (pm-047). */
        const val MAX_PAGE_SIZE = 50
    }
}
