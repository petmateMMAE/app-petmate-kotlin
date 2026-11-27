package mx.petmate.app.data.moderation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.moderation.ModerationApproval
import mx.petmate.app.domain.moderation.ModerationGateway
import mx.petmate.app.domain.moderation.ModerationRejected
import mx.petmate.app.domain.moderation.ModerationRequest
import mx.petmate.app.domain.moderation.ModerationVerdict

/**
 * Camino unico por el que pasa todo el contenido que sube el usuario (001/T-038, C-25;
 * constitucion P13).
 *
 * La regla es una sola: primero se modera, y **solo si aprueba** se ejecuta [onApproved]. Si el
 * veredicto no deja publicar, [onApproved] no se llama nunca, asi que no hay nada que escribir en
 * Room ni que enviar al servidor: no queda rastro porque la escritura no ocurrio, no porque
 * alguien la deshaga despues. Deshacerla seria frágil; no hacerla no se puede equivocar.
 *
 * El [ModerationApproval] que recibe [onApproved] es el comprobante: las firmas que guardan o
 * envian contenido lo exigen, de modo que no se puede llamar a una escritura moderada sin haber
 * pasado por aqui.
 *
 * Ejemplo de uso:
 * ```
 * moderatedContent.publish(ModerationRequest.Text(descripcion)) { aprobacion ->
 *     repository.savePost(descripcion, aprobacion)
 * }
 * ```
 */
@Singleton
class ModeratedContent @Inject constructor(
    private val gateway: ModerationGateway,
) {

    /**
     * Modera [request] y, si aprueba, ejecuta [onApproved] y devuelve su resultado.
     *
     * Si no aprueba devuelve `AppResult.Failure` con un [ModerationRejected] dentro, y
     * [onApproved] no se ejecuta.
     */
    suspend fun <T> publish(
        request: ModerationRequest,
        onApproved: suspend (ModerationApproval) -> AppResult<T>,
    ): AppResult<T> {
        val veredicto = moderar(request)

        return when (veredicto) {
            is ModerationVerdict.Approved -> onApproved(ModerationApproval())

            is ModerationVerdict.Rejected -> rechazo(ModerationRejected.NotAllowed(veredicto.reason))

            is ModerationVerdict.NotValidated -> rechazo(ModerationRejected.CouldNotValidate)
        }
    }

    /**
     * El contrato de [ModerationGateway] dice que no lanza excepciones, pero esto no se apoya en
     * esa promesa: si una implementacion futura lanza, se trata como "no se pudo validar" y no se
     * publica (R-08). Lo que no se atrapa es la cancelacion, porque esa tiene que seguir subiendo
     * o se rompe la corrutina que la pidio.
     */
    private suspend fun moderar(request: ModerationRequest): ModerationVerdict = try {
        gateway.moderate(request)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        ModerationVerdict.NotValidated
    }

    private fun rechazo(causa: ModerationRejected): AppResult<Nothing> =
        AppResult.Failure(AppError.Unknown(causa))
}
