package mx.petmate.app.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

/**
 * Lee un `int64` del backend venga como numero o entre comillas.
 *
 * El contrato declara estos campos `type: ["integer","string"]` (por ejemplo
 * `MessageResponse.seq` y `ConversationSummaryResponse.unreadCount`): un `long` de .NET se
 * serializa a veces como entero y a veces como cadena, porque en JavaScript un entero de 64
 * bits no cabe en un `Number` sin perder precision.
 *
 * `kotlinx.serialization` es estricto: un campo `String` exige comillas y un `Long` las
 * rechaza, asi que **cualquiera de las dos declaraciones falla con la mitad de las respuestas
 * legitimas del servidor**. Este serializador lee el primitivo tal como venga y devuelve su
 * texto; quien lo use lo convierte con `toLongOrNull`.
 *
 * Se descubrio con una prueba, no en produccion: ver `MessageMapperTest`.
 */
object Int64AsStringSerializer : KSerializer<String> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Int64AsString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String = when (decoder) {
        // El primitivo sirve para las dos formas: `content` da el texto sin las comillas.
        is JsonDecoder -> decoder.decodeJsonElement().jsonPrimitive.content
        else -> decoder.decodeString()
    }

    /** Se escribe como cadena, que es la forma que el backend acepta sin perder precision. */
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}
