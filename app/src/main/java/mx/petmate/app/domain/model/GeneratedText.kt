package mx.petmate.app.domain.model

import java.security.MessageDigest

/**
 * Texto generado por IA (spec 019, T-001). Siempre se muestra marcado como IA y junto al original (T-007, T-008).
 * [sourceFingerprint] es la huella del texto original: si el original cambia, este texto ya no le corresponde.
 */
data class GeneratedText(
    val text: String,
    val generatedAtMillis: Long,
    val sourceFingerprint: String,
) {
    /** Origen fijo: todo [GeneratedText] viene de una IA y la interfaz lo dice (R de 019). */
    val isAiGenerated: Boolean get() = true
}

/** Huella SHA-256 del contenido original (spec 019, D-04). Pura JVM: no depende de Android. */
object ContentFingerprint {
    fun of(source: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
