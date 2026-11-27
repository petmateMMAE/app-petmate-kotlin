package mx.petmate.app.data.moderation

import mx.petmate.app.domain.moderation.ModerationContentType
import mx.petmate.app.domain.moderation.ModerationGateway
import mx.petmate.app.domain.moderation.ModerationVerdict

/**
 * Modera un texto ANTES de guardarlo o enviarlo (spec 004, T-004).
 *
 * El orden importa y es la razon de que esto exista: primero se modera, despues se guarda. Al reves, un texto
 * rechazado ya habria quedado en la base y habria que borrarlo, con la ventana en la que alguien pudo leerlo.
 *
 * Lo aprobado se guarda UNA vez: el bloque de guardado recibe el texto y se invoca una sola vez, nunca dos,
 * aunque el llamador reintente el veredicto.
 */
class InlineTextModerator(
    private val gateway: ModerationGateway,
    private val audit: ModerationAuditLogger,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /**
     * Modera [text] y, solo si se aprueba, ejecuta [onApproved].
     *
     * @return el veredicto. Quien llama lo traduce a mensaje con `ModerationMessages`.
     */
    suspend fun moderateThenSave(text: String, onApproved: suspend (String) -> Unit): ModerationVerdict {
        val start = clock()
        val verdict = runCatching { gateway.moderateText(text) }
            .getOrElse { ModerationPolicy.fromFailure() }

        audit.record(ModerationContentType.Text, verdict, clock() - start, text)

        if (verdict.isPublishable) {
            onApproved(text)
        }
        return verdict
    }
}
