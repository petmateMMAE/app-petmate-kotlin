package mx.petmate.app.domain.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Conversation
import mx.petmate.app.domain.model.Message
import mx.petmate.app.domain.model.MessageContent
import mx.petmate.app.domain.model.MessageStatus
import mx.petmate.app.domain.repository.ChatRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Modelos y contrato del chat de seguimiento (spec 016, T-001 y T-002). */
class ChatModelsTest {

    private val message = Message(
        id = "m-1",
        conversationId = "conv-1",
        authorId = "ana",
        content = MessageContent.Text("Come mejor"),
        sentAt = "2026-11-17T10:00:00Z",
        status = MessageStatus.Pending,
    )

    @Test
    fun `el autor se reconoce`() {
        assertTrue(message.isFrom("ana"))
        assertFalse(message.isFrom("vet"))
    }

    @Test
    fun `la conversacion sabe quien participa`() {
        val conversation = Conversation("conv-1", "f-1", setOf("ana", "vet"), unreadCount = 2)

        assertTrue(conversation.includes("vet"))
        assertFalse(conversation.includes("beto"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no hay no leidos negativos`() {
        Conversation("conv-1", "f-1", setOf("ana"), unreadCount = -1)
    }

    @Test
    fun `el contrato se puede doblar con Flow y AppResult`() = runTest {
        val fake = object : ChatRepository {
            override fun observeMessages(conversationId: String): Flow<List<Message>> =
                flowOf(listOf(message))
            override suspend fun refresh(conversationId: String) = AppResult.Success(0)
            override suspend fun send(conversationId: String, content: MessageContent) = AppResult.Success(message)
            override suspend fun markAsRead(conversationId: String, upToMessageId: String) = AppResult.Success(0)
            override fun observeUnreadCount(): Flow<Int> = flowOf(0)
        }

        assertEquals(listOf(message), fake.observeMessages("conv-1").first())
        assertEquals(30, ChatRepository.PAGE_SIZE)
    }
}
