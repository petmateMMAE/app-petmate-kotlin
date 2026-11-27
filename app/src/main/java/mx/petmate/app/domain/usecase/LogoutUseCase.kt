package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.push.DeviceRegistration
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Cierra la sesion del usuario. */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val devices: DeviceRegistration,
) {
    /**
     * La baja del dispositivo va **antes** del logout a proposito: `devices/unregister` necesita el
     * access token, y el logout borra la sesion. Al reves, la peticion saldria sin credencial.
     *
     * Si la baja falla, el logout sigue: el usuario pidio salir y debe salir (spec 002, C-11).
     */
    suspend operator fun invoke(): AppResult<Unit> {
        devices.unregister()
        return repository.logout()
    }
}
