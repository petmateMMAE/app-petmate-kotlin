package mx.petmate.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Transporte del chat de seguimiento (spec 016, T-003). Contrato leido de
 * `openapi/openapi.json` del backend ya desplegado: `MessageResponse`,
 * `MessageListResponse`, `SendMessageRequest`, `ConversationSummaryResponse`,
 * `MessagePhotoResponse` y `MarkReadRequest`.
 *
 * Dos cosas del contrato que conviene no olvidar al leer esto:
 *
 * - **El cursor de paginacion es [MessageDto.id], no [MessageDto.seq].** El backend
 *   recibe `after` como uuid, busca ese mensaje y usa *su* `seq` como corte
 *   (`ListMessagesHandler`). Mandar el `seq` en `after` revienta el parseo del Guid.
 * - **`seq` llega como entero o como cadena.** El esquema lo declara
 *   `type: ["integer","string"]` por ser `int64`, asi que se recibe como [String] con
 *   [Int64AsStringSerializer] y se convierte al mapear. Ni `Long` ni `String` a secas
 *   sirven: cada uno rechaza una de las dos formas legitimas.
 */
@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    @Serializable(with = Int64AsStringSerializer::class)
    val seq: String,
    val kind: String,
    val isMine: Boolean,
    val text: String = "",
    val createdAt: String,
    val hasPhoto: Boolean = false,
) {
    companion object {
        /** Valores de `kind` en el backend (`MessageKind`: Text = 1, System = 2, Photo = 3). */
        const val KIND_TEXT = "Text"
        const val KIND_SYSTEM = "System"
        const val KIND_PHOTO = "Photo"
    }
}

/** Pagina de mensajes. [hasMore] lo calcula el backend pidiendo un elemento extra. */
@Serializable
data class MessageListDto(
    val items: List<MessageDto> = emptyList(),
    val hasMore: Boolean = false,
)

@Serializable
data class SendMessageRequestDto(
    val conversationId: String,
    val text: String,
)

/**
 * Marcar como leido. **`upToMessageId` es obligatorio** en el contrato: no se marca
 * "toda la conversacion", se marca hasta un mensaje concreto. Asi un mensaje que llego
 * mientras el usuario leia no se da por leido sin haberse visto.
 */
@Serializable
data class MarkReadRequestDto(
    val conversationId: String,
    val upToMessageId: String,
)

@Serializable
data class ReopenConversationRequestDto(
    val conversationId: String,
)

/**
 * Resumen de una conversacion. `unreadCount` es `int64` y viaja como entero o cadena,
 * igual que `seq`.
 */
@Serializable
data class ConversationSummaryDto(
    val id: String,
    val followupId: String,
    val isClosed: Boolean = false,
    val lastMessage: MessageDto? = null,
    @Serializable(with = Int64AsStringSerializer::class)
    val unreadCount: String = "0",
    val updatedAt: String,
)

/**
 * URL firmada de la foto de un mensaje. **Expira**: no se guarda en Room ni se
 * cachea mas alla de [expiresAt] (D-05 de la spec 016: la entidad guarda la
 * referencia, no la imagen).
 */
@Serializable
data class MessagePhotoDto(
    val messageId: String,
    val url: String,
    val expiresAt: String,
)
