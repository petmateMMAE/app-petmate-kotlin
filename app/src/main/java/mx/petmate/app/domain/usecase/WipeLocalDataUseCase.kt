package mx.petmate.app.domain.usecase

/** Lo que guarda la app en el telefono y se puede borrar de golpe. Lo implementa la capa de datos. */
interface LocalDataWiper {
    /** Borra todas las tablas de Room, las preferencias de DataStore y las credenciales cifradas. */
    suspend fun wipeAll()
}

/**
 * Deja el telefono sin ningun dato del usuario (spec 017, T-009, R-11, D-03): todas las tablas de Room, las
 * preferencias y las credenciales cifradas, en una sola operacion. Lo usa la eliminacion de cuenta (T-010).
 */
class WipeLocalDataUseCase(private val wiper: LocalDataWiper) {
    suspend operator fun invoke() = wiper.wipeAll()
}
