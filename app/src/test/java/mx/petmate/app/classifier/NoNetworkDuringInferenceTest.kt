package mx.petmate.app.classifier

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La foto de la mascota nunca sale del telefono (spec 018, T-008, riesgo G-07).
 *
 * Esta es la tarea de privacidad de la unidad: el usuario fotografia a su mascota en su casa y esa imagen no
 * puede acabar en el servidor de nadie. La spec pide inspeccionar el trafico durante una inferencia, pero
 * mirar el trafico solo demuestra que HOY no se envio nada; manana alguien agrega una llamada y nadie repite
 * la captura. Esta prueba comprueba algo mas fuerte y que no caduca: que en la ruta de inferencia no EXISTE
 * con que enviar nada.
 *
 * Lee los archivos reales de `src/main`. Si alguien mete un cliente HTTP en el clasificador, la build falla.
 */
class NoNetworkDuringInferenceTest {

    /** Todo lo que participa en clasificar una foto: el contrato, sus modelos y la implementacion. */
    private val inferenceSources: List<File> by lazy {
        val roots = listOf(
            File("src/main/java/mx/petmate/app/domain/classifier"),
            File("src/main/java/mx/petmate/app/data/classifier"),
        )
        val models = listOf("PetImage", "BreedSuggestion", "ClassificationResult", "ModelAvailability")
            .mapNotNull { name ->
                File("src/main/java/mx/petmate/app/domain/model/$name.kt").takeIf { it.exists() }
            }
        roots.filter { it.isDirectory }
            .flatMap { it.walkTopDown().filter { f -> f.isFile && f.extension == "kt" } }
            .plus(models)
    }

    /** Formas de salir a la red. Si aparece alguna en la ruta de inferencia, la foto puede viajar. */
    private val networkMarkers = listOf(
        "retrofit2",
        "okhttp3",
        "java.net.URL",
        "java.net.HttpURLConnection",
        "javax.net.ssl",
        "io.ktor.client",
        "ApiService",
        "HttpClient",
        "WebView",
    )

    @Test
    fun `la ruta de inferencia no puede salir a la red`() {
        assertTrue(
            "No se encontro ningun archivo del clasificador. Si se movieron de carpeta, actualiza esta " +
                "prueba: sin archivos que mirar, no esta comprobando nada.",
            inferenceSources.isNotEmpty(),
        )

        val offenders = inferenceSources.mapNotNull { file ->
            val hits = networkMarkers.filter { marker -> file.readText().contains(marker) }
            if (hits.isEmpty()) null else "${file.name}: ${hits.joinToString()}"
        }

        assertTrue(
            "La foto de la mascota no puede salir del telefono (G-07). Estos archivos de la ruta de " +
                "inferencia tienen con que enviarla:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `el clasificador no recibe por donde enviar la foto`() {
        // Aunque el archivo no importe un cliente HTTP, podrian inyectarselo por el constructor. Un
        // repositorio o un servicio de API en las dependencias del clasificador es la misma fuga por otra
        // puerta.
        val sospechosos = listOf("Repository", "ApiService", "Uploader", "RemoteDataSource")

        val offenders = inferenceSources.mapNotNull { file ->
            val text = file.readText()
            val constructorArgs = Regex("""class\s+\w+\s*(?:@\w+\s*)*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
                .findAll(text)
                .joinToString(" ") { it.groupValues[1] }
            val hits = sospechosos.filter { constructorArgs.contains(it) }
            if (hits.isEmpty()) null else "${file.name}: recibe ${hits.joinToString()}"
        }

        assertTrue(
            "El clasificador no debe recibir nada que hable con la red:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `la imagen viaja como bytes y no como un tipo de Android`() {
        // D-01: PetImage lleva bytes, no un Bitmap ni una Uri. Una Uri es una referencia a un archivo del
        // telefono que otro componente podria leer y subir; los bytes se quedan donde el dominio los pone, y
        // ademas permiten probar todo esto en la JVM sin emulador.
        val petImage = File("src/main/java/mx/petmate/app/domain/classifier/BreedClassifier.kt")
        assertTrue("No se encontro BreedClassifier.kt", petImage.exists())

        val text = petImage.readText()
        assertTrue(
            "PetImage debe llevar los bytes de la foto (ByteArray).",
            Regex("""class PetImage\([^)]*ByteArray""").containsMatchIn(text),
        )
        listOf("android.graphics.Bitmap", "android.net.Uri", "java.io.File").forEach { tipo ->
            assertTrue(
                "El contrato del clasificador no debe usar $tipo: domain no conoce Android (D-01), y una " +
                    "referencia a un archivo es algo que otro componente puede leer y subir.",
                !text.contains(tipo),
            )
        }
    }
}
