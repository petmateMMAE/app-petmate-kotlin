package mx.petmate.app.security

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Comprobaciones de que las reglas de respaldo no se llevan datos del telefono (001/T-042, R-31).
 *
 * Parsean el XML real del proyecto con un parser de XML, no con `contains()`: asi lo que se
 * comprueba es la estructura que Android va a leer de verdad, y una regla escrita dentro de un
 * comentario no cuenta como regla. Eso es exactamente lo que estaba mal: los dos archivos existian,
 * el manifiesto los referenciaba, y dentro solo habia los comentarios de ejemplo que genera
 * Android Studio. Parecian configurados y no configuraban nada.
 */
class BackupRulesSecurityTest {

    /**
     * Los dominios que no pueden salir del telefono: `sharedpref` guarda `petmate_session` (el token
     * de sesion) y `database` guarda `petmate.db` (el carnet de salud de las mascotas).
     */
    private val dominiosSensibles = listOf("sharedpref", "database")

    private fun leer(nombre: String): Element {
        val file = File("src/main/res/xml/$nombre")
        assertTrue("No se encontro $nombre en ${file.absolutePath}", file.exists())
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            // Esta es una prueba de seguridad: seria ironico abrirla a XXE.
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        return factory.newDocumentBuilder().parse(file).documentElement
    }

    /** Dominios excluidos por completo (`path="."`) dentro del elemento dado. */
    private fun dominiosExcluidos(padre: Element): Set<String> {
        val nodos = padre.getElementsByTagName("exclude")
        return (0 until nodos.length)
            .map { nodos.item(it) as Element }
            .filter { it.getAttribute("path").trim() == "." }
            .map { it.getAttribute("domain").trim() }
            .toSet()
    }

    private fun hijo(padre: Element, nombre: String): Element? {
        val nodos = padre.getElementsByTagName(nombre)
        return if (nodos.length == 0) null else nodos.item(0) as Element
    }

    @Test
    fun `la transferencia entre dispositivos no se lleva la sesion ni la base de datos`() {
        // allowBackup="false" apaga el respaldo en la nube, pero la documentacion de Android avisa
        // que en dispositivos de algunos fabricantes NO apaga la transferencia entre dispositivos.
        // Y si falta <device-transfer>, ese modo queda habilitado para TODO el contenido. Sin esto,
        // al migrar a un telefono nuevo se copian el token de sesion y el carnet de salud.
        // https://developer.android.com/identity/data/autobackup
        val raiz = leer("data_extraction_rules.xml")
        val transferencia = hijo(raiz, "device-transfer")
        assertTrue(
            "data_extraction_rules.xml debe declarar <device-transfer>. Si falta, Android transfiere " +
                "todos los datos de la app al cambiar de telefono, aunque allowBackup sea false.",
            transferencia != null,
        )
        val faltantes = dominiosSensibles - dominiosExcluidos(transferencia!!)
        assertTrue(
            "Estos dominios deben excluirse de <device-transfer> con path=\".\": $faltantes",
            faltantes.isEmpty(),
        )
    }

    @Test
    fun `el respaldo en la nube tampoco se lleva la sesion ni la base de datos`() {
        // Defensa en profundidad: hoy allowBackup="false" ya lo impide, pero si alguien lo cambia,
        // estas exclusiones siguen de pie.
        val raiz = leer("data_extraction_rules.xml")
        val nube = hijo(raiz, "cloud-backup")
        assertTrue("data_extraction_rules.xml debe declarar <cloud-backup>.", nube != null)
        val faltantes = dominiosSensibles - dominiosExcluidos(nube!!)
        assertTrue(
            "Estos dominios deben excluirse de <cloud-backup> con path=\".\": $faltantes",
            faltantes.isEmpty(),
        )
    }

    @Test
    fun `las reglas de Android 11 o anterior tampoco filtran`() {
        // minSdk es 24: esos telefonos siguen en el alcance. Hoy allowBackup="false" ya los cubre,
        // pero la exclusion queda puesta para el dia en que alguien lo ponga en true sin pensarlo.
        val raiz = leer("backup_rules.xml")
        val faltantes = dominiosSensibles - dominiosExcluidos(raiz)
        assertTrue(
            "Estos dominios deben excluirse en backup_rules.xml con path=\".\": $faltantes",
            faltantes.isEmpty(),
        )
    }

    @Test
    fun `los archivos de reglas no quedaron con la plantilla de Android Studio`() {
        // Esta es la prueba que habria cazado el problema original. Un archivo de reglas sin ninguna
        // regla efectiva es peor que no tenerlo, porque desde el manifiesto parece configurado.
        listOf("data_extraction_rules.xml", "backup_rules.xml").forEach { nombre ->
            val raiz = leer(nombre)
            val reglas = raiz.getElementsByTagName("exclude").length +
                raiz.getElementsByTagName("include").length
            assertTrue(
                "$nombre no declara ninguna regla real, solo comentarios. Android no lee las reglas " +
                    "comentadas: configuralo de verdad o quita la referencia del manifiesto.",
                reglas > 0,
            )
        }
    }

    @Test
    fun `el manifiesto sigue apuntando a los dos archivos de reglas`() {
        // Si alguien borra el atributo, las exclusiones de arriba dejan de aplicarse sin que ninguna
        // de las otras pruebas se entere.
        val manifiesto = File("src/main/AndroidManifest.xml")
        assertTrue("No se encontro el manifiesto en ${manifiesto.absolutePath}", manifiesto.exists())
        val texto = manifiesto.readText()
        assertTrue(
            "El manifiesto debe declarar android:dataExtractionRules=\"@xml/data_extraction_rules\" " +
                "(Android 12 y superior).",
            texto.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""),
        )
        assertTrue(
            "El manifiesto debe declarar android:fullBackupContent=\"@xml/backup_rules\" " +
                "(Android 11 y anterior).",
            texto.contains("android:fullBackupContent=\"@xml/backup_rules\""),
        )
    }
}
