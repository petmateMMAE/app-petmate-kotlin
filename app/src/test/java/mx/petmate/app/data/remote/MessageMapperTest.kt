package mx.petmate.app.data.remote

import kotlinx.coroutines.test.runTest
import mx.petmate.app.data.remote.dto.ConversationSummaryDto
import mx.petmate.app.data.remote.dto.MarkReadRequestDto
import mx.petmate.app.data.remote.dto.MessageDto
import mx.petmate.app.data.remote.dto.MessageListDto
import mx.petmate.app.data.remote.dto.SendMessageRequestDto
import mx.petmate.app.domain.model.MessageContent
import mx.petmate.app.domain.model.MessageStatus
import mx.petmate.app.util.encolarSobre
import mx.petmate.app.util.servicio
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Transporte y mapeo del chat (spec 016, T-003).
 *
 * Las pruebas de deserializacion pasan por [MockWebServer] con el mismo `Json` que arma
 * `NetworkModule`: una configuracion mas permisiva en la prueba dejaria pasar un DTO que
 * la app no sabe leer.
 */
class MessageMapperTest {

    private val server = MockWebServer()
    private val api: ChatApiService = server.servicio()

    @After
    fun tearDown() = server.shutdown()

    private fun mensajeJson(
        id: String,
        seq: String,
        createdAt: String,
        kind: String = MessageDto.KIND_TEXT,
        text: String = "hola",
        isMine: Boolean = false,
        hasPhoto: Boolean = false,
    ) = """{"id":"$id","conversationId":"c1","seq":$seq,"kind":"$kind",""" +
        """"isMine":$isMine,"text":"$text","createdAt":"$createdAt","hasPhoto":$hasPhoto}"""

    /**
     * El orden lo fija `seq` del servidor y no la fecha. El servidor manda los mensajes
     * desordenados y con **fechas que contradicen la secuencia**: si el mapeo ordenara por
     * fecha, el resultado saldria al reves. Es el caso que pide Q-02.
     */
    @Test
    fun `ordena por la secuencia del servidor y no por la fecha`() = runTest {
        val items = listOf(
            mensajeJson(id = "m3", seq = "3", createdAt = "2026-11-27T10:00:00Z", text = "tercero"),
            mensajeJson(id = "m1", seq = "1", createdAt = "2026-11-27T12:00:00Z", text = "primero"),
            mensajeJson(id = "m2", seq = "2", createdAt = "2026-11-27T11:00:00Z", text = "segundo"),
        )
        server.encolarSobre(200, """{"items":[${items.joinToString(",")}],"hasMore":true}""")

        val mensajes = api.getMessages("Bearer t", "c1").data!!.toDomainList()

        assertEquals(listOf("m1", "m2", "m3"), mensajes.map { it.id })
        assertEquals(
            listOf("primero", "segundo", "tercero"),
            mensajes.map { (it.content as MessageContent.Text).text },
        )
    }

    /**
     * `seq` se declara `type: ["integer","string"]` en el contrato por ser `int64`, asi que el
     * backend puede mandarlo entre comillas. Las dos formas tienen que ordenar igual; si el DTO
     * lo declarara `Long`, la variante con comillas reventaria la deserializacion.
     */
    @Test
    fun `acepta la secuencia como entero y como cadena`() = runTest {
        server.encolarSobre(
            200,
            """{"items":[""" +
                mensajeJson(id = "b", seq = "\"20\"", createdAt = "2026-11-27T10:00:00Z") + "," +
                mensajeJson(id = "a", seq = "3", createdAt = "2026-11-27T10:00:01Z") +
                """],"hasMore":false}""",
        )

        val pagina = api.getMessages("Bearer t", "c1").data!!

        // 3 antes que 20: si se comparara como texto, "20" iria primero.
        assertEquals(listOf("a", "b"), pagina.toDomainList().map { it.id })
    }

    /**
     * El cursor de la siguiente pagina es el **id** del mensaje mas avanzado, no su `seq`:
     * el backend recibe `after` como uuid. Mandar el `seq` romperia el parseo del Guid.
     */
    @Test
    fun `el cursor es el id del mensaje mas avanzado`() {
        val pagina = MessageListDto(
            items = listOf(
                MessageDto("m1", "c1", "1", MessageDto.KIND_TEXT, false, "a", "2026-11-27T10:00:00Z"),
                MessageDto("m9", "c1", "9", MessageDto.KIND_TEXT, false, "b", "2026-11-27T09:00:00Z"),
            ),
            hasMore = true,
        )

        assertEquals("m9", pagina.nextCursor())
        assertNull("una pagina vacia no tiene cursor", MessageListDto().nextCursor())
    }

    /**
     * Un mensaje con foto **no trae la URL**: se pide aparte y expira (D-05). El mapeo deja la
     * URL vacia y conserva el texto como pie, para que nadie guarde una URL muerta en Room.
     */
    @Test
    fun `un mensaje con foto no trae la url embebida`() = runTest {
        server.encolarSobre(
            200,
            """{"items":[""" +
                mensajeJson(
                    id = "f1",
                    seq = "1",
                    createdAt = "2026-11-27T10:00:00Z",
                    kind = MessageDto.KIND_PHOTO,
                    text = "mira la herida",
                    hasPhoto = true,
                ) +
                """],"hasMore":false}""",
        )

        val contenido = api.getMessages("Bearer t", "c1").data!!.toDomainList().single().content

        assertTrue(contenido is MessageContent.Image)
        assertEquals("", (contenido as MessageContent.Image).url)
        assertEquals("mira la herida", contenido.caption)
    }

    /**
     * El backend tiene tres clases de mensaje y el dominio solo modela dos. Un `System` es texto
     * que nadie escribio: se conserva como texto en vez de descartarse de la conversacion.
     */
    @Test
    fun `un mensaje de sistema se conserva como texto`() {
        val dto = MessageDto(
            id = "s1",
            conversationId = "c1",
            seq = "1",
            kind = MessageDto.KIND_SYSTEM,
            isMine = false,
            text = "El seguimiento se dio de alta",
            createdAt = "2026-11-27T10:00:00Z",
        )

        val contenido = dto.toDomain().content

        assertEquals(MessageContent.Text("El seguimiento se dio de alta"), contenido)
    }

    /**
     * `isMine` es lo unico que el contrato dice del autor, asi que es lo unico que se afirma.
     * Y un mensaje que volvio del servidor esta `Sent`: `Pending` y `Failed` los pone la cola
     * local, no el transporte, y `Delivered` no se inventa porque nadie lo confirma.
     */
    @Test
    fun `distingue el autor y no inventa el acuse de recibo`() {
        val mio = MessageDto("a", "c1", "1", MessageDto.KIND_TEXT, true, "yo", "2026-11-27T10:00:00Z")
        val ajeno = mio.copy(id = "b", isMine = false)

        assertTrue(mio.toDomain().isFrom(AUTHOR_ME))
        assertTrue(!ajeno.toDomain().isFrom(AUTHOR_ME))
        assertEquals(MessageStatus.Sent, mio.toDomain().status)
    }

    /** `unreadCount` es `int64` y tambien puede llegar entre comillas; nunca produce negativos. */
    @Test
    fun `mapea el resumen de la conversacion con el contador`() {
        val dto = ConversationSummaryDto(
            id = "c1",
            followupId = "f1",
            isClosed = false,
            unreadCount = "7",
            updatedAt = "2026-11-27T10:00:00Z",
        )

        val conversacion = dto.toDomain(participantIds = setOf("u1", "u2"))

        assertEquals("f1", conversacion.followupId)
        assertEquals(7, conversacion.unreadCount)
        assertTrue(conversacion.includes("u1"))
        assertEquals(0, dto.copy(unreadCount = "ilegible").toDomain().unreadCount)
    }

    /**
     * Las rutas y los parametros son el contrato: se comprueban contra la peticion que sale de
     * verdad. `after` nulo no se manda, para que el backend entienda "desde el principio".
     */
    @Test
    fun `arma las peticiones segun el contrato del backend`() = runTest {
        server.encolarSobre(200, """{"items":[],"hasMore":false}""")
        api.getMessages("Bearer t", "c1", after = null, pageSize = 30)
        val sinCursor = server.takeRequest()
        assertEquals("/api/v1/messages/get-all?conversationId=c1&pageSize=30", sinCursor.path)
        assertEquals("Bearer t", sinCursor.getHeader("Authorization"))

        server.encolarSobre(200, """{"items":[],"hasMore":false}""")
        api.getMessages("Bearer t", "c1", after = "m9")
        assertTrue(server.takeRequest().path!!.contains("after=m9"))

        server.encolarSobre(200, "null")
        api.markRead("Bearer t", MarkReadRequestDto(conversationId = "c1", upToMessageId = "m9"))
        val leido = server.takeRequest()
        assertEquals("/api/v1/conversations/mark-read", leido.path)
        assertEquals(
            """{"conversationId":"c1","upToMessageId":"m9"}""",
            leido.body.readUtf8(),
        )

        server.encolarSobre(200, "null")
        api.sendMessage("Bearer t", SendMessageRequestDto(conversationId = "c1", text = "hola"))
        assertEquals("/api/v1/messages/send", server.takeRequest().path)
    }
}
