package mx.petmate.app.security

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprobacion de que no viajan secretos dentro del APK (001/T-042, R-31).
 *
 * Recorre el codigo de produccion real (`src/main`) y busca formas de secreto que no tienen
 * explicacion posible: claves de Google, JWT, llaves privadas, tokens de proveedores, cadenas de
 * conexion con contrasena. No mira `src/test` a proposito: ahi hay tokens falsos puestos a mano
 * para las pruebas y esto seria puro ruido.
 *
 * Una prueba de seguridad ruidosa termina con un `@Ignore` encima, y entonces no protege nada. Por
 * eso las excepciones conocidas estan en [EXCEPCIONES] con su motivo escrito, al estilo del
 * allowlist de `.gitleaks.toml`: si alguien incrusta una credencial nueva, esto falla; si una
 * excepcion es legitima, se anota aqui a proposito y queda por escrito quien decidio que lo era.
 */
class NoEmbeddedSecretsTest {

    private data class Hallazgo(val archivo: String, val linea: Int, val patron: String, val valor: String) {
        /** No se imprime el secreto completo en el log de la build. */
        override fun toString(): String {
            val pista = if (valor.length <= 4) "****" else valor.take(4) + "*".repeat(valor.length - 4)
            return "$archivo:$linea  [$patron]  $pista (${valor.length} caracteres)"
        }
    }

    private data class Patron(val nombre: String, val regex: Regex)

    private val patronesDeSecreto = listOf(
        Patron("clave de API de Google", Regex("AIza[0-9A-Za-z_-]{35}")),
        Patron("JWT incrustado", Regex("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}")),
        Patron("llave privada", Regex("-----BEGIN [A-Z ]*PRIVATE KEY-----")),
        Patron("credencial de AWS", Regex("\\b(AKIA|ASIA)[0-9A-Z]{16}\\b")),
        Patron("token de GitHub", Regex("\\b(ghp|gho|ghs|ghu)_[A-Za-z0-9]{36}\\b")),
        Patron("token de Slack", Regex("xox[baprs]-[0-9A-Za-z-]{10,}")),
        Patron("cadena de conexion con contrasena", Regex("[a-z+]{2,}://[^\\s:/@\"]+:[^\\s:/@\"]+@")),
    )

    /** `nombre = "valor"` escrito a mano, sin interpolacion. */
    private val credencialLiteral = Regex(
        "(?i)\\b(password|passwd|pwd|secret|api_?key|client_?secret|access_?token|refresh_?token)" +
            "\\s*=\\s*\"([^\"\\\\]{6,})\"",
    )

    private val raizProduccion = File("src/main")

    private fun archivosKotlin(): List<File> =
        raizProduccion.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun File.rutaRelativa(): String =
        relativeTo(raizProduccion).path.replace('\\', '/')

    @Test
    fun `el escaneo encuentra archivos y los patrones funcionan`() {
        // Sin esto, un escaner roto -- una ruta mal puesta, un walk que no recorre nada -- dejaria
        // pasar las dos pruebas de abajo en verde sin haber mirado un solo archivo.
        val archivos = archivosKotlin()
        assertTrue(
            "El escaneo no encontro codigo Kotlin en ${raizProduccion.absolutePath}. Si esto falla, " +
                "las otras pruebas de este archivo estan pasando en vacio.",
            archivos.size > 50,
        )
        val cebo = "AIzaSyA0000000000000000000000000000000x"
        assertTrue(
            "El patron de clave de Google no reconoce ni un ejemplo de juguete: la expresion esta mal.",
            patronesDeSecreto.first().regex.containsMatchIn(cebo),
        )
    }

    @Test
    fun `no hay secretos con forma reconocible en el codigo de produccion`() {
        val hallazgos = mutableListOf<Hallazgo>()
        archivosKotlin().forEach { archivo ->
            archivo.readLines().forEachIndexed { indice, linea ->
                patronesDeSecreto.forEach { patron ->
                    patron.regex.find(linea)?.let { coincidencia ->
                        hallazgos += Hallazgo(archivo.rutaRelativa(), indice + 1, patron.nombre, coincidencia.value)
                    }
                }
            }
        }
        assertTrue(
            "Hay secretos incrustados en el codigo que se compila en el APK:\n" +
                hallazgos.joinToString("\n") { "  $it" } +
                "\n\nUn secreto en el APK es publico: cualquiera lo saca con apktool. Saquenlo a " +
                "local.properties o a un buildConfigField y rotenlo, porque ya estuvo en el repositorio.",
            hallazgos.isEmpty(),
        )
    }

    @Test
    fun `no hay credenciales escritas a mano fuera de la interfaz`() {
        // Se omite ui/ a proposito: ahi los @Preview de Compose llevan datos de ejemplo
        // (password = "petmate123") que no son credenciales de nadie, y perseguirlos convertiria
        // esta prueba en ruido cada vez que alguien agrega una vista previa. Un secreto de verdad en
        // ui/ lo sigue cazando la prueba de arriba, que si recorre todo src/main por forma.
        val hallazgos = archivosKotlin()
            .filterNot { it.rutaRelativa().contains("/ui/") }
            .flatMap { archivo ->
                archivo.readLines().mapIndexedNotNull { indice, linea ->
                    credencialLiteral.find(linea)?.let { coincidencia ->
                        val valor = coincidencia.groupValues[2]
                        // Una cadena interpolada como "fake-access-${user.id}" se arma en tiempo de
                        // ejecucion a partir de datos del usuario: no es un secreto incrustado, es
                        // codigo. Lo que se busca aqui es un valor fijo escrito a mano.
                        if (valor.contains('$')) {
                            null
                        } else {
                            Hallazgo(archivo.rutaRelativa(), indice + 1, "credencial escrita a mano", valor)
                        }
                    }
                }
            }
            .filterNot { hallazgo ->
                EXCEPCIONES.any { it.archivo == hallazgo.archivo && it.valor == hallazgo.valor }
            }

        assertTrue(
            "Hay credenciales escritas a mano en el codigo de produccion:\n" +
                hallazgos.joinToString("\n") { "  $it" } +
                "\n\nSi alguna es legitima, agregala a EXCEPCIONES con el motivo escrito. No la " +
                "borres de la prueba sin dejar dicho por que.",
            hallazgos.isEmpty(),
        )
    }

    @Test
    fun `la URL de produccion no esta incrustada en el codigo`() {
        // BASE_URL entra por local.properties y llega como buildConfigField. Si el host aparece
        // escrito en un .kt, alguien se salto ese camino y el APK apunta a produccion a la fuerza.
        val hallazgos = archivosKotlin().flatMap { archivo ->
            archivo.readLines().mapIndexedNotNull { indice, linea ->
                if (linea.contains(HOST_PRODUCCION)) {
                    Hallazgo(archivo.rutaRelativa(), indice + 1, "URL de produccion", HOST_PRODUCCION)
                } else {
                    null
                }
            }
        }
        assertTrue(
            "El host de produccion aparece escrito en el codigo:\n" +
                hallazgos.joinToString("\n") { "  $it" } +
                "\n\nDebe entrar por BASE_URL (local.properties -> buildConfigField), no a mano.",
            hallazgos.isEmpty(),
        )
    }

    @Test
    fun `local properties no se versiona`() {
        // Ahi vive BASE_URL y ahi va a terminar cualquier credencial de firma. Si se versiona, el
        // primer secreto que alguien ponga se sube sin que nadie lo note.
        val gitignore = File("../.gitignore")
        assertTrue("No se encontro .gitignore en ${gitignore.absolutePath}", gitignore.exists())
        val ignorado = gitignore.readLines()
            .map { it.trim() }
            .any { it == "local.properties" || it == "/local.properties" }
        assertTrue(
            "local.properties debe estar en .gitignore: ahi se guardan BASE_URL y las credenciales " +
                "de firma.",
            ignorado,
        )
    }

    private companion object {
        const val HOST_PRODUCCION = "api.petmate.mx"

        /**
         * Credenciales escritas a mano que se revisaron una por una y se aceptaron a proposito.
         * Cada una lleva su motivo: si el motivo deja de ser cierto, se quita de aqui y la prueba
         * vuelve a fallar sola.
         */
        val EXCEPCIONES = listOf(
            Excepcion(
                archivo = "java/mx/petmate/app/domain/model/UserProfile.kt",
                valor = "PetMateDemo2026",
                motivo = "Cuenta de demostracion publica (DemoAccount), documentada en el propio archivo: " +
                    "existe con las mismas credenciales en el backend real y en el simulado, y no tiene " +
                    "datos reales. Si algun dia deja de ser publica, quitala de esta lista y saca la " +
                    "constante del codigo.",
            ),
        )
    }

    private data class Excepcion(val archivo: String, val valor: String, val motivo: String)
}
