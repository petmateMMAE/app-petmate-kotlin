package mx.petmate.app.data.remote

import kotlinx.serialization.json.JsonElement
import mx.petmate.app.data.remote.dto.ApiResponse
import mx.petmate.app.data.remote.dto.AuthDataDto
import mx.petmate.app.data.remote.dto.LoginRequestDto
import mx.petmate.app.data.remote.dto.LogoutRequestDto
import mx.petmate.app.data.remote.dto.ProfileDto
import mx.petmate.app.data.remote.dto.RefreshRequestDto
import mx.petmate.app.data.remote.dto.RegisterRequestDto
import mx.petmate.app.data.remote.dto.UpdateProfileRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Endpoints de identidad del backend PetMate v0.1.0 (base `/api/v1/`).
 *
 * Contrato: docs/api/endpoints.md del repositorio backend (spec 002, Q-01).
 */
interface AuthApiService {

    /** 201 con AuthData; 400 validacion; 409 correo registrado; 429; 503. */
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): ApiResponse<AuthDataDto>

    /** 200 con AuthData; 401 credenciales invalidas; 429; 503. */
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): ApiResponse<AuthDataDto>

    /** 200 con tokens nuevos (rotacion); 401 si el token ya no sirve. */
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): ApiResponse<AuthDataDto>

    /** 200 con data null; 401 si el access token expiro (renovar y reintentar). */
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String,
        @Body body: LogoutRequestDto,
    ): ApiResponse<JsonElement>

    /** Perfil propio (pm-037): 200; 401 si el token expiro; 404 si la cuenta ya no existe. */
    @GET("users/me/get")
    suspend fun getProfile(@Header("Authorization") authorization: String): ApiResponse<ProfileDto>

    /** Nombre visible (pm-038): 200; 400 si el nombre no es valido; 401 si el token expiro. */
    @PUT("users/me/update")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body body: UpdateProfileRequestDto,
    ): ApiResponse<ProfileDto>
}
