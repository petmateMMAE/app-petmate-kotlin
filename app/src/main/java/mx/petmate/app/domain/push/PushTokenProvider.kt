package mx.petmate.app.domain.push

/**
 * De dónde sale el token de este dispositivo (spec 001, T-039; spec 006).
 *
 * **Firebase vive detrás de esta interfaz y de ningún otro lado.** El motivo es D-19: "si Firebase
 * no está configurado, la app arranca igual". Con `FirebaseMessaging` llamado directamente desde el
 * registrador, esa regla depende de que nadie olvide un try/catch; con la interfaz, la app sin
 * Firebase simplemente recibe una implementación que devuelve null, y el resto del código ni se
 * entera.
 *
 * El otro motivo es poder probarlo: `FirebaseMessaging.getInstance()` es un singleton estático que
 * necesita un `Context` inicializado, así que una prueba de JVM no lo puede tocar.
 *
 * Es `domain` y por eso no menciona Android ni Firebase: solo texto.
 */
interface PushTokenProvider {

    /**
     * El token de este dispositivo, o `null` si no se puede obtener.
     *
     * `null` cubre los dos casos y a propósito no los distingue, porque la reacción es la misma:
     * Firebase no está configurado en esta compilación, o lo está y falló al pedir el token.
     * Quien llama no puede hacer nada distinto en uno u otro caso.
     *
     * **No lanza.** Una implementación que lance rompe D-19.
     */
    suspend fun currentToken(): String?

    /**
     * Borra el token de este dispositivo en el proveedor.
     *
     * Se llama al cerrar sesión: si no se borrara, Firebase seguiría mandando notificaciones de la
     * cuenta anterior a un teléfono donde ya entró otra persona.
     *
     * **No lanza**, por lo mismo.
     */
    suspend fun deleteToken()
}

/**
 * El proveedor cuando no hay Firebase (D-19).
 *
 * Es el que se inyecta mientras Q-08 siga abierta —qué proyecto de Firebase se usa y quién
 * administra `google-services.json`—, y se queda como el de las compilaciones que no llevan
 * Firebase. No es un hueco por llenar: es el comportamiento correcto para esas compilaciones.
 */
object NoPushTokenProvider : PushTokenProvider {
    override suspend fun currentToken(): String? = null
    override suspend fun deleteToken() = Unit
}
