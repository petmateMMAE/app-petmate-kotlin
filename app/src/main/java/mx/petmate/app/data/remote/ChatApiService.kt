package mx.petmate.app.data.remote

import mx.petmate.app.data.remote.dto.ApiResponse
import mx.petmate.app.data.remote.dto.ConversationSummaryDto
import mx.petmate.app.data.remote.dto.MarkReadRequestDto
import mx.petmate.app.data.remote.dto.MessageDto
import mx.petmate.app.data.remote.dto.MessageListDto
import mx.petmate.app.data.remote.dto.MessagePhotoDto
import mx.petmate.app.data.remote.dto.PagedDto
import mx.petmate.app.data.remote.dto.ReopenConversationRequestDto
import mx.petmate.app.data.remote.dto.SendMessageRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoints del chat de seguimiento (spec 016, T-003; pm-143 a pm-159).
 * La BASE_URL de Retrofit ya termina en `/api/v1/`.
 *
 * Todas exigen `Authorization: Bearer <access token>`; el participante sale del token,
 * nunca del cuerpo. Se llaman a traves de [AuthorizedCaller], que pone el token y
 * renueva en 401.
 */
interface ChatApiService {

    /**
     * Mensajes de la conversacion **en orden ascendente** (del mas viejo al mas nuevo),
     * desde el mensaje [after] en adelante.
     *
     * [after] es el **id** del ultimo mensaje que ya se tiene, no su `seq`: el backend
     * busca ese mensaje y usa su secuencia como corte. `null` trae desde el principio.
     * Si el id no pertenece a la conversacion, el backend responde 404.
     *
     * Se pagina **hacia adelante**: para traer lo nuevo se manda el id del ultimo
     * mensaje local. El historial hacia atras no lo ofrece este endpoint.
     */
    @GET("messages/get-all")
    suspend fun getMessages(
        @Header("Authorization") bearer: String,
        @Query("conversationId") conversationId: String,
        @Query("after") after: String? = null,
        @Query("pageSize") pageSize: Int = DEFAULT_PAGE_SIZE,
    ): ApiResponse<MessageListDto>

    /** Envia un mensaje de texto. La moderacion la aplica el backend (004). */
    @POST("messages/send")
    suspend fun sendMessage(
        @Header("Authorization") bearer: String,
        @Body body: SendMessageRequestDto,
    ): ApiResponse<MessageDto>

    /**
     * URL firmada de la foto de un mensaje. **Expira**: se pide cuando se va a mostrar,
     * no se guarda (D-05).
     */
    @GET("messages/get-photo-url")
    suspend fun getPhotoUrl(
        @Header("Authorization") bearer: String,
        @Query("conversationId") conversationId: String,
        @Query("messageId") messageId: String,
    ): ApiResponse<MessagePhotoDto>

    /** Conversaciones del usuario, paginadas por pagina (no por cursor, a diferencia de los mensajes). */
    @GET("conversations/get-all")
    suspend fun getConversations(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = DEFAULT_PAGE_SIZE,
    ): ApiResponse<PagedDto<ConversationSummaryDto>>

    /** Marca como leido hasta un mensaje concreto y devuelve el resumen con el contador ya actualizado. */
    @POST("conversations/mark-read")
    suspend fun markRead(
        @Header("Authorization") bearer: String,
        @Body body: MarkReadRequestDto,
    ): ApiResponse<ConversationSummaryDto>

    /** Reabre una conversacion cerrada al dar de alta el seguimiento. Solo del lado del negocio. */
    @POST("conversations/reopen")
    suspend fun reopen(
        @Header("Authorization") bearer: String,
        @Body body: ReopenConversationRequestDto,
    ): ApiResponse<Unit>

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
    }
}
