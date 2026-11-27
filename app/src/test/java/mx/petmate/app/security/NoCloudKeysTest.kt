package mx.petmate.app.security

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ninguna llave de nube viaja dentro del APK (spec 004, T-010, C-12).
 *
 * La moderacion la hace el backend, no la app: la app manda el contenido y recibe un veredicto. Por eso aqui
 * no debe existir ninguna credencial de Azure, y menos de Content Safety. Una llave incrustada en un APK es
 * publica: basta descomprimirlo para leerla, y con ella se puede gastar la cuota de la cuenta o usarla para
 * otra cosa. No es un riesgo teorico, es la fuga mas comun en Android.
 *
 * Se revisan los archivos reales: el codigo de produccion, los recursos y el build.gradle.kts que genera
 * BuildConfig. Si alguien mete una llave, la build falla.
 */
class NoCloudKeysTest {

    private val gradleFile = File("build.gradle.kts")
    private val mainSources = File("src/main")

    /** Nombres de credencial que no tienen por que existir en la app. */
    private val forbiddenNames = listOf(
        "AZURE_KEY", "AZURE_SUBSCRIPTION_KEY", "COGNITIVE_SERVICES_KEY", "CONTENT_SAFETY_KEY",
        "OCP_APIM_SUBSCRIPTION_KEY", "Ocp-Apim-Subscription-Key",
        "AZURE_CLIENT_SECRET", "ACCOUNT_KEY", "SharedAccessSignature",
    )

    /** Dominios de servicio que la app nunca debe llamar directo: eso lo hace el backend. */
    private val forbiddenHosts = listOf(
        "cognitiveservices.azure.com",
        "api.cognitive.microsoft.com",
        "contentsafety.azure.com",
        "blob.core.windows.net",
    )

    private fun kotlinSources(): List<File> =
        mainSources.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun `BuildConfig no expone ninguna llave de nube`() {
        assertTrue("No se encontro build.gradle.kts en ${gradleFile.absolutePath}", gradleFile.exists())
        val campos = Regex("""buildConfigField\([^)]*\)""").findAll(gradleFile.readText())
            .map { it.value }
            .toList()

        val ofensores = campos.filter { campo ->
            forbiddenNames.any { campo.contains(it, ignoreCase = true) } ||
                campo.contains("KEY", ignoreCase = false) && !campo.contains("USE_FAKE")
        }

        assertTrue(
            "BuildConfig no puede llevar llaves. La moderacion la hace el backend; la app solo manda " +
                "contenido y recibe un veredicto. Campos: $ofensores",
            ofensores.isEmpty(),
        )
    }

    @Test
    fun `los recursos no contienen llaves de nube`() {
        val recursos = File("src/main/res").walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .toList()

        val ofensores = recursos.mapNotNull { archivo ->
            val texto = archivo.readText()
            val hits = forbiddenNames.filter { texto.contains(it, ignoreCase = true) }
            if (hits.isEmpty()) null else "${archivo.name}: ${hits.joinToString()}"
        }

        assertTrue("Hay llaves en los recursos:\n" + ofensores.joinToString("\n"), ofensores.isEmpty())
    }

    @Test
    fun `el codigo no llama directo a los servicios de nube`() {
        // Si la app hablara directo con Azure necesitaria la llave, y volveriamos al problema de arriba.
        val ofensores = kotlinSources().mapNotNull { archivo ->
            val texto = archivo.readText()
            val hits = forbiddenHosts.filter { texto.contains(it, ignoreCase = true) }
            if (hits.isEmpty()) null else "${archivo.name}: ${hits.joinToString()}"
        }

        assertTrue(
            "La app no debe llamar directo a los servicios de nube; eso lo hace el backend:\n" +
                ofensores.joinToString("\n"),
            ofensores.isEmpty(),
        )
    }

    @Test
    fun `la URL del backend sale de la configuracion por entorno`() {
        val texto = gradleFile.readText()

        // El criterio pide que la URL venga de la configuracion por entorno de 001, no incrustada a mano.
        assertTrue(
            "BASE_URL debe declararse como buildConfigField, una por variante.",
            Regex("""buildConfigField\("String",\s*"BASE_URL"""").containsMatchIn(texto),
        )
        assertTrue(
            "La URL debe leerse de una propiedad del entorno (BASE_URL_DEBUG / BASE_URL_RELEASE), con un " +
                "valor por omision solo como respaldo.",
            texto.contains("BASE_URL_DEBUG") && texto.contains("BASE_URL_RELEASE"),
        )
    }

    @Test
    fun `no hay credenciales con forma de secreto en el codigo`() {
        // Formas que no tienen explicacion posible en codigo de produccion.
        val formas = listOf(
            Regex("""eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}"""),  // JWT
            Regex("""AIza[0-9A-Za-z_-]{35}"""),                       // llave de Google
            Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----"""),          // llave privada
            Regex("""DefaultEndpointsProtocol=.*AccountKey="""),      // cadena de Azure Storage
        )

        val ofensores = kotlinSources().mapNotNull { archivo ->
            val texto = archivo.readText()
            if (formas.any { it.containsMatchIn(texto) }) archivo.name else null
        }

        assertTrue("Hay algo con forma de credencial en:\n" + ofensores.joinToString("\n"), ofensores.isEmpty())
    }
}
