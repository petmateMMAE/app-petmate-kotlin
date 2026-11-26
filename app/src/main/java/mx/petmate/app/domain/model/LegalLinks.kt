package mx.petmate.app.domain.model

/**
 * Enlaces al aviso de privacidad y a los terminos, alojados en la landing (spec 017, T-001, R-03, R-12).
 *
 * Las URLs reales esperan a Q-02. Mientras falten, [isConfigured] es falso y la pantalla no muestra un enlace
 * roto. El perfil de usuario de esta unidad es `UserProfile` (ya existia, con `isAffiliate`), no se duplica.
 */
data class LegalLinks(
    val privacyUrl: String?,
    val termsUrl: String?,
) {
    /** Solo cuentan enlaces https: Google Play exige que el aviso abra en una pagina segura. */
    val isConfigured: Boolean get() = privacyUrl.isHttps() && termsUrl.isHttps()

    private fun String?.isHttps() = this != null && startsWith("https://") && length > "https://".length

    companion object {
        /** Sin URLs todavia (Q-02). */
        val PENDING = LegalLinks(privacyUrl = null, termsUrl = null)
    }
}
