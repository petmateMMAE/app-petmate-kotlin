package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.LoginCredentials
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.push.DeviceRegistration
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Inicia sesion despues de validar las credenciales. */
class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val validate: ValidateCredentialsUseCase,
    private val devices: DeviceRegistration,
) {
    suspend operator fun invoke(credentials: LoginCredentials): AppResult<User> {
        val errors = validate.validateLogin(credentials)
        if (errors.isNotEmpty()) return AppResult.Failure(errors.first())
        // El registro del dispositivo va DESPUES de que la sesion quedo guardada: necesita el
        // access token (D-19). Solo si el login salio bien; no lanza, asi que no puede tumbarlo.
        return repository.login(credentials.copy(email = credentials.email.trim()))
            .also { if (it is AppResult.Success) devices.register() }
    }
}
