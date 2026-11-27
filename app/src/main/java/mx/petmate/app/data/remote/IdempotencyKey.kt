package mx.petmate.app.data.remote

import java.util.UUID
import okhttp3.Interceptor
import okhttp3.Response

/** Cabecera con la que el servidor descarta una escritura repetida (D-15). */
const val IDEMPOTENCY_HEADER = "Idempotency-Key"

/** Metodos que cambian estado en el servidor y por eso llevan clave. Un GET no la necesita. */
private val METODOS_DE_ESCRITURA = setOf("POST", "PUT", "PATCH", "DELETE")

/**
 * Pone una clave de idempotencia en cada escritura (001/T-033, D-15).
 *
 * El orden en el cliente importa y no es un detalle: este interceptor va **antes** de
 * [RetryInterceptor], asi que la clave se genera UNA vez por peticion logica y todos los
 * reintentos mandan la misma. Si se generara en cada intento, un reintento despues de un timeout
 * crearia dos mascotas en vez de una, que es exactamente lo que D-15 quiere evitar: el timeout no
 * dice si el servidor alcanzo a guardar o no.
 *
 * Si quien llama ya puso la cabecera, se respeta. La cola de envios sin conexion necesita que la
 * clave sobreviva entre reintentos de dias distintos, no solo entre reintentos de red, y esa
 * clave la elige ella.
 */
class IdempotencyKeyInterceptor(
    private val generarClave: () -> String = { UUID.randomUUID().toString() },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        val esEscritura = peticion.method in METODOS_DE_ESCRITURA
        val yaTieneClave = peticion.header(IDEMPOTENCY_HEADER) != null

        if (!esEscritura || yaTieneClave) return chain.proceed(peticion)

        return chain.proceed(
            peticion.newBuilder()
                .header(IDEMPOTENCY_HEADER, generarClave())
                .build(),
        )
    }
}
