package mx.petmate.app.domain.push

/**
 * Alta y baja de este dispositivo para notificaciones (spec 001, T-039; D-19).
 *
 * Existe para que `LoginUseCase` y `LogoutUseCase` puedan disparar el registro sin importar
 * `data`: la regla 3 de `ArchitectureLayersTest` lo prohibe, y con razon, porque `domain` tiene que
 * poder probarse sin Retrofit ni Android. `PushRegistrar` la implementa en `data`.
 *
 * **Ningun metodo lanza.** El push es un extra: si el registro falla, la persona entra y sale
 * igual. Por eso devuelven `Unit` y no un resultado, para que quien llama no tenga ni la
 * posibilidad de cancelar un login por esto.
 */
interface DeviceRegistration {

    /** Asocia este dispositivo a la sesion recien abierta. Se llama **despues** de guardarla. */
    suspend fun register()

    /** Da de baja este dispositivo y olvida su token. Se llama al cerrar sesion. */
    suspend fun unregister()
}
