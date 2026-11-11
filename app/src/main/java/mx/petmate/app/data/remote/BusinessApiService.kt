package mx.petmate.app.data.remote

import kotlinx.serialization.json.JsonElement
import mx.petmate.app.data.remote.dto.AccessGrantDto
import mx.petmate.app.data.remote.dto.AccessGrantRequestDto
import mx.petmate.app.data.remote.dto.ApiResponse
import mx.petmate.app.data.remote.dto.AttendedPetDto
import mx.petmate.app.data.remote.dto.BusinessDto
import mx.petmate.app.data.remote.dto.CreateBusinessRequestDto
import mx.petmate.app.data.remote.dto.NearbyBusinessDto
import mx.petmate.app.data.remote.dto.PagedDto
import mx.petmate.app.data.remote.dto.PublicBusinessDto
import mx.petmate.app.data.remote.dto.ResolveScanRequestDto
import mx.petmate.app.data.remote.dto.ScanFichaDto
import mx.petmate.app.data.remote.dto.ServiceDto
import mx.petmate.app.data.remote.dto.ServiceRequestDto
import mx.petmate.app.data.remote.dto.UpdateBusinessRequestDto
import mx.petmate.app.data.remote.dto.UpdateLocationRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Negocio afiliado, mapa publico, autorizaciones y escaneo (backend 0.4.0). */
interface BusinessApiService {

    // ---------- Mapa publico (sin cuenta, pm-116 a pm-119) ----------
    @GET("businesses/get-nearby")
    suspend fun getNearby(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double = 50.0,
        @Query("pageSize") pageSize: Int = 50,
    ): ApiResponse<PagedDto<NearbyBusinessDto>>

    @GET("businesses/get-by-id/{id}")
    suspend fun getPublicBusiness(@Path("id") id: String): ApiResponse<PublicBusinessDto>

    // ---------- Negocio propio (pm-107 a pm-110) ----------
    @POST("businesses/create")
    suspend fun createBusiness(@Header("Authorization") bearer: String, @Body body: CreateBusinessRequestDto): ApiResponse<BusinessDto>

    @PUT("businesses/update")
    suspend fun updateBusiness(@Header("Authorization") bearer: String, @Body body: UpdateBusinessRequestDto): ApiResponse<BusinessDto>

    @PUT("businesses/update-location")
    suspend fun updateLocation(@Header("Authorization") bearer: String, @Body body: UpdateLocationRequestDto): ApiResponse<BusinessDto>

    @DELETE("businesses/delete")
    suspend fun deleteBusiness(@Header("Authorization") bearer: String): ApiResponse<JsonElement?>

    // ---------- Catalogo (pm-112 a pm-115) ----------
    @POST("services/create")
    suspend fun createService(@Header("Authorization") bearer: String, @Body body: ServiceRequestDto): ApiResponse<ServiceDto>

    @GET("services/get-all")
    suspend fun getServices(@Header("Authorization") bearer: String, @Query("pageSize") pageSize: Int = 50): ApiResponse<PagedDto<ServiceDto>>

    @PUT("services/update/{id}")
    suspend fun updateService(@Header("Authorization") bearer: String, @Path("id") id: String, @Body body: ServiceRequestDto): ApiResponse<ServiceDto>

    @DELETE("services/delete/{id}")
    suspend fun deleteService(@Header("Authorization") bearer: String, @Path("id") id: String): ApiResponse<JsonElement?>

    // ---------- Autorizacion del dueno y escaneo del negocio (pm-122 a pm-129) ----------
    @POST("access-grants/create")
    suspend fun createAccessGrant(@Header("Authorization") bearer: String, @Body body: AccessGrantRequestDto): ApiResponse<AccessGrantDto>

    @POST("scans/resolve")
    suspend fun resolveScan(@Header("Authorization") bearer: String, @Body body: ResolveScanRequestDto): ApiResponse<ScanFichaDto>

    @GET("scans/get-attended")
    suspend fun getAttended(
        @Header("Authorization") bearer: String,
        @Query("period") period: String = "week",
        @Query("pageSize") pageSize: Int = 50,
    ): ApiResponse<PagedDto<AttendedPetDto>>
}
