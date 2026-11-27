package mx.petmate.app.data.sharing

import mx.petmate.app.domain.model.AccessGrant
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.RecordOrigin
import mx.petmate.app.domain.model.SharedData
import mx.petmate.app.domain.sharing.ShareContent
import mx.petmate.app.domain.sharing.ShareSection
import mx.petmate.app.domain.sharing.ShareSelection
import mx.petmate.app.util.RecordingLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Auditoria de emision de comparticiones (spec 009, T-009, C-11).
 *
 * La prueba que importa es la ultima: **el historial clinico no aparece en el registro**. Las
 * alergias de esta prueba llevan nombres y notas inventados pero muy distinguibles a proposito, de
 * forma que si alguna se colara al registro, la comparacion lo caza sin ambiguedad.
 */
class ShareAuditLoggerTest {

    private val logger = RecordingLogger()
    private val auditor = ShareAuditLogger(logger)

    private val alergiaSecreta = Allergy(
        id = "a-1",
        name = "ALERGIA-SECRETA-AL-POLLO",
        kind = AllergyKind.Allergy,
        notes = "NOTA-CLINICA-CONFIDENCIAL: reaccion severa en 2025",
        active = true,
        origin = RecordOrigin(false, null),
    )

    private val condicionSecreta = Allergy(
        id = "a-2",
        name = "CONDICION-SECRETA-DIABETES",
        kind = AllergyKind.Condition,
        notes = "OTRA-NOTA-CLINICA",
        active = true,
        origin = RecordOrigin(true, "NOMBRE-DE-VETERINARIA"),
    )

    private fun contenido(vararg secciones: ShareSection) = ShareContent.of(
        selection = ShareSelection(secciones.toSet()),
        criticalAllergies = listOf(alergiaSecreta, condicionSecreta),
    )

    private val registro: String get() = logger.mensajes.joinToString(" | ")

    // ---- Lo que si se registra ----

    @Test
    fun `se registra mascota, negocio, secciones y expiracion`() {
        auditor.issued(
            petId = "p-1",
            businessId = "b-9",
            content = contenido(ShareSection.Vaccines, ShareSection.Weights),
            expiresAt = "2026-11-27T10:00:00Z",
        )

        assertEquals(1, logger.mensajes.size)
        assertTrue("falta la mascota: $registro", registro.contains("pet=p-1"))
        assertTrue("falta el negocio: $registro", registro.contains("business=b-9"))
        assertTrue("faltan las secciones: $registro", registro.contains("sections=[Vaccines,Weights]"))
        assertTrue("falta la expiracion: $registro", registro.contains("expires=2026-11-27T10:00:00Z"))
    }

    @Test
    fun `las secciones se ordenan igual siempre`() {
        // Dos registros del mismo permiso deben parecerse, aunque el Set llegue en otro orden.
        auditor.issued("p-1", "b-9", contenido(ShareSection.Weights, ShareSection.Vaccines), "x")
        auditor.issued("p-1", "b-9", contenido(ShareSection.Vaccines, ShareSection.Weights), "x")

        assertEquals(logger.mensajes[0], logger.mensajes[1])
    }

    @Test
    fun `un QR sin destino se registra como para cualquiera`() {
        // El QR se genera antes de saber quien lo va a escanear.
        auditor.issued("p-1", businessId = null, content = contenido(ShareSection.Vaccines), expiresAt = "x")

        assertTrue(registro.contains("business=cualquiera"))
    }

    @Test
    fun `compartir solo las alergias se distingue de no compartir nada`() {
        // Seleccion vacia no es un error: R-04 dice que las alergias van siempre.
        auditor.issued("p-1", "b-9", contenido(), "x")

        assertTrue("$registro", registro.contains("sections=[solo-alergias]"))
        assertTrue(registro.contains("allergies=included"))
    }

    @Test
    fun `desde la respuesta del servidor se registra tambien el id de la autorizacion`() {
        val grant = AccessGrant(
            id = "g-77",
            businessId = "b-9",
            data = listOf(SharedData.Consultations, SharedData.Vaccines),
            expiresAt = "2026-11-27T10:00:00Z",
        )

        auditor.issued("p-1", grant)

        assertTrue(registro.contains("grant=g-77"))
        assertTrue(registro.contains("sections=[Consultations,Vaccines]"))
    }

    @Test
    fun `una revocacion queda registrada`() {
        auditor.revoked("p-1", "g-77")

        assertTrue(registro.contains("comparticion revocada"))
        assertTrue(registro.contains("pet=p-1"))
        assertTrue(registro.contains("grant=g-77"))
    }

    @Test
    fun `un fallo se registra como error y con un motivo tecnico`() {
        auditor.issueFailed("p-1", "b-9", "http_409")

        assertEquals(1, logger.errores.size)
        assertTrue(registro.contains("comparticion fallo"))
        assertTrue(registro.contains("reason=http_409"))
    }

    // ---- Lo que NUNCA se registra ----

    @Test
    fun `el historial clinico no aparece en el registro`() {
        // Esta es C-11. Se llama a todo lo que registra, con el contenido clinico mas
        // distinguible posible, y despues se exige que nada de eso este en el registro.
        auditor.issued("p-1", "b-9", contenido(ShareSection.Vaccines, ShareSection.Consultations), "2026-11-27T10:00:00Z")
        auditor.issued("p-1", AccessGrant("g-77", "b-9", listOf(SharedData.Vaccines), "2026-11-27T10:00:00Z"))
        auditor.revoked("p-1", "g-77")
        auditor.issueFailed("p-1", "b-9", "sin_red")

        listOf(
            "ALERGIA-SECRETA-AL-POLLO",
            "CONDICION-SECRETA-DIABETES",
            "NOTA-CLINICA-CONFIDENCIAL",
            "OTRA-NOTA-CLINICA",
            "NOMBRE-DE-VETERINARIA",
            "reaccion severa",
        ).forEach { secreto ->
            assertTrue(
                "Se filtro contenido clinico al registro: \"$secreto\" aparece en: $registro",
                logger.noRegistro(secreto),
            )
        }
    }

    @Test
    fun `no se registra cuantas alergias tiene la mascota`() {
        // Decir que las alergias van no revela nada, porque van siempre (R-04). Decir cuantas si
        // seria un dato clinico de esa mascota, asi que el numero no se anota.
        auditor.issued("p-1", "b-9", contenido(ShareSection.Vaccines), "x")

        assertTrue(registro.contains("allergies=included"))
        assertTrue("no debe aparecer un conteo", logger.noRegistro("allergies=2"))
    }

    @Test
    fun `el nombre de la mascota no se registra porque nunca se le pasa`() {
        // La firma solo acepta identificadores: no hay forma de pasarle el nombre aunque se quiera.
        // Esta prueba documenta esa decision, para que un cambio de firma tenga que pensarlo.
        auditor.issued("p-1", "b-9", contenido(ShareSection.Vaccines), "x")

        assertTrue(logger.noRegistro("Firulais"))
        assertTrue(registro.contains("pet=p-1"))
    }
}
