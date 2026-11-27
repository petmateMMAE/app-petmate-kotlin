package mx.petmate.app.data.remote

import retrofit2.Response
import retrofit2.http.GET

/**
 * Salud del backend en Azure Container Apps (spec 001, T-033; Q-02 resuelta el 27/11/2026).
 *
 * Las rutas son `health/live` y `health/ready`, relativas a la base `/api/v1/`. Verificadas: las dos
 * responden 200.
 *
 * Devuelve `Response<Unit>` y no un sobre: lo que importa es **el código**, no el cuerpo. Un
 * balanceador puede responder un cuerpo vacío o un HTML de error, y si esto esperara el sobre JSON
 * del backend, la comprobación fallaría al deserializar justo cuando el servidor está en problemas,
 * que es exactamente cuando hace falta que funcione.
 */
interface HealthApiService {

    /** El proceso está vivo. Lo que mira el balanceador para no matar el contenedor. */
    @GET("health/live")
    suspend fun live(): Response<Unit>

    /**
     * El backend está listo para atender: ya levantó sus dependencias (base de datos, etc.).
     *
     * Es la que de verdad importa para la app: `live` puede estar en 200 mientras el servidor
     * todavía no puede responder una consulta.
     */
    @GET("health/ready")
    suspend fun ready(): Response<Unit>
}
