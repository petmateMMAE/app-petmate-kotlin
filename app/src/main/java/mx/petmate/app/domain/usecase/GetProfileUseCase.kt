package mx.petmate.app.domain.usecase

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.UserProfile
import mx.petmate.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Obtiene el perfil propio desde el servidor. */
class GetProfileUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<UserProfile> = repository.loadProfile()
}
