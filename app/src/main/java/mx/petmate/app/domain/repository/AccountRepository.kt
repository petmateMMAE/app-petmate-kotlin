package mx.petmate.app.domain.repository

import mx.petmate.app.core.result.AppResult

/**
 * Operaciones irreversibles sobre la cuenta (spec 017, T-002). Esta separado de [SettingsRepository] a proposito:
 * cambiar un ajuste se deshace con otro toque; eliminar la cuenta no.
 *
 * La ruta y lo que borra en cascada esperan a Miguel (Q-01); el periodo de gracia, al equipo (Q-03).
 */
interface AccountRepository {
    /** Elimina la cuenta en el servidor. Pide la contrasena para reautenticar (R-05). */
    suspend fun deleteAccount(password: String): AppResult<Unit>
}
