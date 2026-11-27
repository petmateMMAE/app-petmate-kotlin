package mx.petmate.app.security

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprobaciones de seguridad sobre el manifiesto, ejecutables en cada PR (001/T-042, R-31).
 *
 * Leen el XML real del proyecto, no una copia ni una constante de Kotlin: una prueba que se comprueba a sí
 * misma no sirve de nada. Si alguien afloja una de estas protecciones, la build falla en vez de que el
 * problema llegue a producción y se descubra en una auditoría manual meses después.
 */
class ManifestSecurityTest {

    private val manifest: String by lazy {
        val file = File("src/main/AndroidManifest.xml")
        assertTrue("No se encontró el manifiesto en ${file.absolutePath}", file.exists())
        file.readText()
    }

    @Test
    fun `el trafico en claro esta prohibido`() {
        // Sin esto, cualquier http:// mal escrito en una URL viajaría sin cifrar y nadie se enteraría.
        assertTrue(
            "El manifiesto debe declarar android:usesCleartextTraffic=\"false\".",
            manifest.contains("android:usesCleartextTraffic=\"false\""),
        )
        assertTrue(
            "No debe habilitarse el tráfico en claro.",
            !manifest.contains("android:usesCleartextTraffic=\"true\""),
        )
    }

    @Test
    fun `el respaldo automatico esta desactivado`() {
        // allowBackup="true" deja que adb backup —o la copia de Google -- se lleve la base de datos completa,
        // con el carnet de salud de las mascotas y los tokens de sesión.
        assertTrue(
            "El manifiesto debe declarar android:allowBackup=\"false\".",
            manifest.contains("android:allowBackup=\"false\""),
        )
    }

    @Test
    fun `solo MainActivity esta exportada`() {
        // Cada componente exported es una puerta que otra app del teléfono puede tocar. MainActivity tiene que
        // estarlo para aparecer en el lanzador; cualquier otra necesita justificarse aquí a propósito.
        val exported = Regex("android:exported=\"true\"").findAll(manifest).count()
        assertEquals(
            "Solo MainActivity puede estar exportada. Si agregas otro componente exported, documenta por qué " +
                "y actualiza esta prueba a propósito.",
            1,
            exported,
        )
        assertTrue(
            "El único exported debe ser MainActivity.",
            manifest.substringAfter("android:name=\".MainActivity\"").substringBefore("</activity>")
                .contains("android:exported=\"true\""),
        )
    }

    @Test
    fun `MainActivity se protege contra el secuestro de tareas`() {
        // StrandHogg 2.0: sin launchMode singleInstance y taskAffinity vacío, una app maliciosa puede colocar
        // su pantalla encima de la nuestra dentro de la misma pila y cosechar la contraseña.
        val activity = manifest.substringAfter("android:name=\".MainActivity\"").substringBefore("</activity>")
        assertTrue("MainActivity debe usar launchMode=\"singleInstance\".", activity.contains("android:launchMode=\"singleInstance\""))
        assertTrue("MainActivity debe declarar taskAffinity vacío.", activity.contains("android:taskAffinity=\"\""))
    }

    @Test
    fun `no se piden permisos peligrosos fuera del catalogo`() {
        // El catálogo de 003. Un permiso nuevo que no esté aquí es una decisión de producto, no un descuido:
        // tiene que pasar por esta lista a propósito.
        val permitidos = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CAMERA",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.VIBRATE",
        )
        val pedidos = Regex("<uses-permission android:name=\"([^\"]+)\"").findAll(manifest)
            .map { it.groupValues[1] }
            .toSet()
        val desconocidos = pedidos - permitidos
        assertTrue(
            "Permisos fuera del catálogo: $desconocidos. Si uno es legítimo, agrégalo a esta lista y " +
                "documéntalo en la spec 003.",
            desconocidos.isEmpty(),
        )
    }

    @Test
    fun `la camara no es obligatoria para instalar`() {
        // Declarar CAMERA hace que Google Play exija cámara por omisión y excluya teléfonos sin ella. El
        // escáner tiene la alternativa del código manual, así que required="false" evita perder dispositivos.
        if (manifest.contains("android.permission.CAMERA")) {
            assertTrue(
                "Si se declara CAMERA, debe ir con <uses-feature android:name=\"android.hardware.camera\" " +
                    "android:required=\"false\" /> para no excluir teléfonos sin cámara.",
                Regex("android.hardware.camera\"[^>]*android:required=\"false\"").containsMatchIn(manifest) ||
                    Regex("android:required=\"false\"[^>]*android.hardware.camera\"").containsMatchIn(manifest),
            )
        }
    }
}
