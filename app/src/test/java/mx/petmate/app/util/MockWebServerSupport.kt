package mx.petmate.app.util

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit

/**
 * Utilidades de [MockWebServer] compartidas (001/T-023).
 *
 * Cuatro pruebas de repositorio repetian el mismo armado de Retrofit y el mismo
 * `MockResponse().setResponseCode(...).setHeader(...).setBody(...)`. Lo importante no es ahorrar
 * lineas: es que el `Json` de las pruebas sea **el mismo** que el de produccion. Si la prueba usa
 * una configuracion mas permisiva que `NetworkModule`, pasa en verde y la app falla con la misma
 * respuesta del servidor.
 */

/**
 * La misma configuracion de `Json` que arma `NetworkModule`.
 *
 * Si algun dia cambia alla, tiene que cambiar aqui: por eso esta en un solo lugar y no copiada en
 * cada prueba.
 */
val jsonDePrueba: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Ruta base del backend, igual que en `BASE_URL`. */
const val RUTA_BASE_API = "/api/v1/"

/** Crea un servicio de Retrofit apuntado a este servidor simulado. */
inline fun <reified T> MockWebServer.servicio(ruta: String = RUTA_BASE_API): T = Retrofit.Builder()
    .baseUrl(url(ruta))
    .addConverterFactory(jsonDePrueba.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(T::class.java)

/** Encola una respuesta JSON con su `Content-Type`, que es lo que el convertidor necesita. */
fun MockWebServer.encolarJson(codigo: Int, cuerpo: String) {
    enqueue(
        MockResponse()
            .setResponseCode(codigo)
            .setHeader("Content-Type", "application/json")
            .setBody(cuerpo),
    )
}

/**
 * El sobre que usa el backend en todas sus respuestas:
 * `{"statusCode":...,"message":"...","data":...}` (ver `docs/api/endpoints.md` del backend).
 *
 * [datos] se inserta tal cual, asi que puede ser un objeto, un arreglo o `null`.
 */
fun sobre(codigo: Int, datos: String, mensaje: String = "ok"): String =
    """{"statusCode":$codigo,"message":"$mensaje","data":$datos}"""

/** Encola una respuesta ya envuelta en el sobre del backend. */
fun MockWebServer.encolarSobre(codigo: Int, datos: String, mensaje: String = "ok") {
    encolarJson(codigo, sobre(codigo, datos, mensaje))
}

/** Encola un error del backend: sobre con `data` en `null`. */
fun MockWebServer.encolarError(codigo: Int, mensaje: String) {
    encolarJson(codigo, sobre(codigo, "null", mensaje))
}
