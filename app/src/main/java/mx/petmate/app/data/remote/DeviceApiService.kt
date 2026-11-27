package mx.petmate.app.data.remote

import kotlinx.serialization.Serializable
import mx.petmate.app.data.remote.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Registro del dispositivo para notificaciones (spec 001, T-039; spec 006).
 *
 * Rutas de la respuesta de Adrián del 27/11/2026 a Q-02 de la spec 006: el dispositivo se registra
 * con `devices/register` y se da de baja con `devices/unregister`, sobre la base `/api/v1/`.
 */
interface DeviceApiService {

    /** Asocia el token de este dispositivo a la cuenta de la sesión. */
    @POST("devices/register")
    suspend fun register(
        @Header("Authorization") authorization: String,
        @Body body: DeviceTokenRequestDto,
    ): ApiResponse<Unit>

    /**
     * Desasocia el token.
     *
     * Lleva el token en el cuerpo y no solo la cuenta porque una persona puede tener la app en
     * varios teléfonos: al cerrar sesión en uno, los otros tienen que seguir recibiendo.
     */
    @POST("devices/unregister")
    suspend fun unregister(
        @Header("Authorization") authorization: String,
        @Body body: DeviceTokenRequestDto,
    ): ApiResponse<Unit>
}

/**
 * El token del dispositivo.
 *
 * `platform` va fijo en "android": el backend sirve a varias plataformas y necesita saber por cuál
 * mandar. No lleva modelo de teléfono ni identificadores: nada que no haga falta (R-12).
 */
@Serializable
data class DeviceTokenRequestDto(
    val token: String,
    val platform: String = PLATAFORMA,
) {
    companion object {
        const val PLATAFORMA = "android"
    }
}
