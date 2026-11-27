package mx.petmate.app.data.push

import mx.petmate.app.R
import mx.petmate.app.domain.push.NotificationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Texto fijo por categoria y nada del payload en la notificacion
 * (spec 006, T-004, R-04 y C-04).
 */
class NotificationPresenterTest {

    /** Payload con todo lo clinico que el backend podria mandar. */
    private val clinicalPayload = mapOf(
        "category" to "followup",
        "targetId" to "f1",
        "eventKey" to "evt-1",
        "diagnosis" to "Traqueobronquitis infecciosa",
        "prescription" to "Amoxicilina 250 mg cada 12 h",
        "notes" to "La mascota sigue tosiendo por las noches",
        "petName" to "Nube",
        "ownerEmail" to "ana@petmate.mx",
        "body" to "Texto crudo que el servidor quiso mostrar",
        "title" to "Titulo crudo del servidor",
    )

    // ---- Cada categoria tiene su texto fijo ----

    @Test
    fun `cada categoria tiene titulo y cuerpo propios en strings`() {
        NotificationCategory.entries.forEach { category ->
            val notification = NotificationPresenter.present(mapOf("category" to category.key))

            assertNotNull("$category", notification)
            assertTrue("$category sin titulo", notification!!.titleRes != 0)
            assertTrue("$category sin cuerpo", notification.bodyRes != 0)
        }
    }

    @Test
    fun `ninguna categoria comparte texto con otra`() {
        val titles = NotificationCategory.entries.map(NotificationPresenter::titleOf)
        val bodies = NotificationCategory.entries.map(NotificationPresenter::bodyOf)

        assertEquals("los titulos deben ser distintos", titles.size, titles.toSet().size)
        assertEquals("los cuerpos deben ser distintos", bodies.size, bodies.toSet().size)
    }

    @Test
    fun `el seguimiento usa su propio texto`() {
        val notification = NotificationPresenter.present(clinicalPayload)!!

        assertEquals(NotificationCategory.FollowUp, notification.category)
        assertEquals(R.string.notifications_followup_title, notification.titleRes)
        assertEquals(R.string.notifications_followup_body, notification.bodyRes)
    }

    // ---- C-04: ningun campo clinico llega a la notificacion ----

    @Test
    fun `la notificacion no lleva ningun texto del payload`() {
        val notification = NotificationPresenter.present(clinicalPayload)!!

        // Lo unico que se guarda del payload es el identificador para abrir el
        // detalle; no hay un solo campo de texto donde pudiera colarse algo.
        assertEquals("f1", notification.targetId)

        // Se revisan los campos por nombre: si alguien agrega un `body` o un
        // `title` de texto libre, esta prueba lo caza. Se ignoran los campos
        // que mete la instrumentacion de cobertura, que empiezan con '$'.
        val fields = PushNotification::class.java.declaredFields
            .filterNot { it.isSynthetic || it.name.startsWith("$") }
            .map { it.name }
            .toSet()
        assertEquals(setOf("category", "titleRes", "bodyRes", "targetId"), fields)

        // Y el unico campo de texto que queda es el identificador.
        val textFields = PushNotification::class.java.declaredFields
            .filterNot { it.isSynthetic || it.name.startsWith("$") }
            .filter { it.type == String::class.java }
            .map { it.name }
        assertEquals(listOf("targetId"), textFields)
    }

    @Test
    fun `el esquema solo permite tres claves tecnicas`() {
        assertEquals(setOf("category", "targetId", "eventKey"), NotificationPresenter.ALLOWED_KEYS)
    }

    @Test
    fun `las claves clinicas del payload no estan permitidas`() {
        val clinical = listOf("diagnosis", "prescription", "notes", "petName", "ownerEmail", "body", "title")

        clinical.forEach {
            assertTrue("$it no debe estar permitida", it !in NotificationPresenter.ALLOWED_KEYS)
        }
    }

    @Test
    fun `dos payloads de la misma categoria dan exactamente el mismo texto`() {
        val first = NotificationPresenter.present(clinicalPayload)!!
        val second = NotificationPresenter.present(
            mapOf("category" to "followup", "targetId" to "f2", "diagnosis" to "Otra cosa"),
        )!!

        assertEquals(first.titleRes, second.titleRes)
        assertEquals(first.bodyRes, second.bodyRes)
    }

    // ---- El detalle se carga al abrir ----

    @Test
    fun `guarda el identificador para cargar el detalle al abrir`() {
        val notification = NotificationPresenter.present(mapOf("category" to "chat", "targetId" to "c9"))!!

        assertEquals("c9", notification.targetId)
    }

    @Test
    fun `sin identificador el detalle queda nulo y no truena`() {
        val notification = NotificationPresenter.present(mapOf("category" to "news"))!!

        assertNull(notification.targetId)
    }

    @Test
    fun `un identificador vacio o con espacios cuenta como ausente`() {
        listOf("", "   ").forEach {
            val notification = NotificationPresenter.present(mapOf("category" to "news", "targetId" to it))!!
            assertNull("targetId '$it'", notification.targetId)
        }
    }

    // ---- Payloads que no se reconocen ----

    @Test
    fun `una categoria desconocida no muestra nada`() {
        assertNull(NotificationPresenter.present(mapOf("category" to "promociones_raras")))
    }

    @Test
    fun `un payload sin categoria no muestra nada`() {
        assertNull(NotificationPresenter.present(emptyMap()))
        assertNull(NotificationPresenter.present(mapOf("targetId" to "x")))
        assertNull(NotificationPresenter.present(mapOf("category" to "")))
    }

    @Test
    fun `la categoria tolera espacios alrededor`() {
        assertEquals(
            NotificationCategory.Health,
            NotificationPresenter.present(mapOf("category" to "  health  "))?.category,
        )
    }

    // ---- Clave de deduplicacion ----

    @Test
    fun `lee la clave de evento cuando viene`() {
        assertEquals("evt-1", NotificationPresenter.eventKeyOf(clinicalPayload))
    }

    @Test
    fun `sin clave de evento devuelve nulo`() {
        assertNull(NotificationPresenter.eventKeyOf(mapOf("category" to "news")))
        assertNull(NotificationPresenter.eventKeyOf(mapOf("eventKey" to "  ")))
    }
}
