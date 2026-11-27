package mx.petmate.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.ConsultationDraft

/**
 * Guardar, leer y borrar el borrador de una consulta (spec 014, T-004, D-05).
 *
 * **Por que es un contrato aparte y no se le agregaron metodos a `VeterinaryRepository`:** ese ya
 * declara `createConsultation`, lo implementa `VeterinaryRepositoryImpl`, lo delega
 * `OfflineFirstFollowUpRepository` con `by remote` y lo doblan cinco pruebas. Agregarle metodos
 * obligaria a tocar todo eso para una responsabilidad distinta: el borrador vive **solo en el
 * telefono** y no se manda al servidor hasta que la consulta se cierra. Separarlos deja claro que
 * guardar el avance no es guardar la consulta.
 *
 * La tarea lo llama `ConsultationRepository`; aqui se llamo `ConsultationDraftRepository` porque
 * "guardar la consulta" ya es de `VeterinaryRepository` y dos nombres parecidos para dos cosas
 * distintas se confunden al leer el codigo.
 *
 * **Ninguna firma recibe `ownerId`**: lo resuelve la implementacion desde la sesion. Si lo
 * recibiera, cualquier pantalla podria pasar el de otra cuenta por error.
 */
interface ConsultationDraftRepository {

    /** Guarda o reemplaza el avance de la consulta de esa mascota. */
    suspend fun save(draft: ConsultationDraft): AppResult<Unit>

    /** El borrador de esa mascota, o null si no hay ninguno. */
    suspend fun get(petId: String): AppResult<ConsultationDraft?>

    /** El borrador en vivo, para que la pantalla se entere si cambia. */
    fun observe(petId: String): Flow<ConsultationDraft?>

    /** Los borradores a medias de la cuenta, del mas reciente al mas viejo. */
    suspend fun getAll(): AppResult<List<ConsultationDraft>>

    /**
     * Borra el borrador de esa mascota.
     *
     * Se llama al cerrar la consulta (D-05): si no, la proxima vez la pantalla abriria con los
     * datos de una consulta que ya se guardo y el veterinario la mandaria dos veces.
     */
    suspend fun delete(petId: String): AppResult<Unit>
}
