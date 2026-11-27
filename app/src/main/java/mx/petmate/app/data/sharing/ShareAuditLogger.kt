package mx.petmate.app.data.sharing

import javax.inject.Inject
import javax.inject.Singleton
import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.model.AccessGrant
import mx.petmate.app.domain.sharing.ShareContent

/**
 * Registro de quien compartio el historial de que mascota y con que negocio (spec 009, T-009, C-11).
 *
 * **Que se registra:** identificadores (mascota, negocio, autorizacion), las secciones elegidas y
 * la expiracion. Con eso se reconstruye *que permiso se dio*, que es para lo que sirve una
 * auditoria.
 *
 * **Que NO se registra, nunca:** contenido clinico. Ni el nombre de la mascota, ni el de una
 * alergia, ni sus notas, ni un diagnostico, ni una vacuna concreta. La constitucion P6 lo prohibe,
 * y el motivo practico es que el registro de Android acaba en los informes de fallos y, en algunos
 * dispositivos, al alcance de otras aplicaciones. Un historial clinico en un registro es una
 * filtracion, aunque la pantalla este bien hecha.
 *
 * Por eso las alergias se anotan como `allergies=included` y **sin numero**: R-04 dice que van
 * siempre, asi que decir que van no revela nada nuevo; decir *cuantas* ya seria un dato clinico de
 * esa mascota.
 */
@Singleton
class ShareAuditLogger @Inject constructor(
    private val logger: Logger,
) {

    /**
     * Se emitio una autorizacion de acceso.
     *
     * [businessId] es null cuando el QR se genera antes de saber quien lo va a escanear.
     */
    fun issued(
        petId: String,
        businessId: String?,
        content: ShareContent,
        expiresAt: String?,
    ) {
        logger.debug(
            TAG,
            linea(
                "emitida",
                "pet" to petId,
                "business" to (businessId ?: SIN_DESTINO),
                "sections" to secciones(content.selection.sections.map { it.name }),
                "allergies" to ALERGIAS_INCLUIDAS,
                "expires" to (expiresAt ?: SIN_EXPIRACION),
            ),
        )
    }

    /** Variante para cuando ya llego la respuesta del servidor. */
    fun issued(petId: String, grant: AccessGrant) {
        logger.debug(
            TAG,
            linea(
                "emitida",
                "pet" to petId,
                "business" to (grant.businessId ?: SIN_DESTINO),
                "grant" to grant.id,
                "sections" to secciones(grant.data.map { it.name }),
                "allergies" to ALERGIAS_INCLUIDAS,
                "expires" to grant.expiresAt,
            ),
        )
    }

    /** El dueno revoco el acceso antes de que expirara. */
    fun revoked(petId: String, grantId: String) {
        logger.debug(TAG, linea("revocada", "pet" to petId, "grant" to grantId))
    }

    /**
     * No se pudo emitir.
     *
     * [reason] es un motivo tecnico corto de nuestra propia cosecha (`sin_red`, `http_409`), nunca
     * el mensaje del servidor: ese podria traer datos de la mascota.
     */
    fun issueFailed(petId: String, businessId: String?, reason: String) {
        logger.error(
            TAG,
            linea(
                "fallo",
                "pet" to petId,
                "business" to (businessId ?: SIN_DESTINO),
                "reason" to reason,
            ),
        )
    }

    /** Nombres de seccion en orden estable, para que dos registros del mismo permiso se parezcan. */
    private fun secciones(nombres: List<String>): String =
        if (nombres.isEmpty()) {
            SIN_SECCIONES
        } else {
            nombres.sorted().joinToString(",", prefix = "[", postfix = "]")
        }

    private fun linea(evento: String, vararg campos: Pair<String, String>): String =
        "comparticion $evento " + campos.joinToString(" ") { "${it.first}=${it.second}" }

    private companion object {
        const val TAG = "ShareAudit"

        /** R-04: las alergias van siempre. Se anota que van, nunca cuantas ni cuales. */
        const val ALERGIAS_INCLUIDAS = "included"

        const val SIN_DESTINO = "cualquiera"
        const val SIN_SECCIONES = "[solo-alergias]"
        const val SIN_EXPIRACION = "desconocida"
    }
}
