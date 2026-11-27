package mx.petmate.app.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las reglas de la skill `revisar-capas`, convertidas en pruebas (001/T-026, C-02, C-03, C-20).
 *
 * ```
 * ui  -->  domain  <--  data
 * ```
 *
 * Por que una prueba y no una auditoria a mano: la skill se corre cuando alguien se acuerda, y la
 * arquitectura se degrada justo cuando nadie se acuerda. Esto recorre los archivos reales de
 * `src/main` en cada PR, asi que una regresion futura falla sola en vez de aparecer en la
 * siguiente revision manual.
 *
 * Aviso para quien la lea: la skill del repositorio apunta al paquete viejo
 * `com.example.petmateedel`. El paquete real es `mx.petmate.app` (deuda D-01 de memoria.md), y
 * estas pruebas usan el real.
 */
class ArchitectureLayersTest {

    private val raiz = File("src/main/java/mx/petmate/app")

    private fun kotlinDe(capa: String): List<File> = File(raiz, capa)
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    private fun File.ruta(): String = relativeTo(raiz).path.replace('\\', '/')

    /** Las lineas de codigo, sin comentarios: un comentario que menciona algo no lo usa. */
    private fun File.lineasDeCodigo(): List<IndexedValue<String>> = readLines()
        .withIndex()
        .filterNot { (_, linea) ->
            val limpia = linea.trimStart()
            limpia.startsWith("//") || limpia.startsWith("*") || limpia.startsWith("/*")
        }
        .toList()

    private fun violaciones(capa: String, patron: Regex): List<String> =
        kotlinDe(capa).flatMap { archivo ->
            archivo.lineasDeCodigo()
                .filter { (_, linea) -> patron.containsMatchIn(linea) }
                .map { (indice, linea) -> "${archivo.ruta()}:${indice + 1}: ${linea.trim()}" }
        }

    private fun exigirLimpio(regla: String, porQue: String, encontradas: List<String>) {
        assertTrue(
            "$regla\n" + encontradas.joinToString("\n") { "  $it" } + "\n\n$porQue",
            encontradas.isEmpty(),
        )
    }

    @Test
    fun `el escaneo encuentra las cinco capas`() {
        // Si la ruta cambiara, todas las pruebas de abajo pasarian en vacio sin mirar un archivo.
        listOf("core", "data", "di", "domain", "ui").forEach { capa ->
            assertTrue(
                "No se encontro la capa $capa en ${File(raiz, capa).absolutePath}",
                kotlinDe(capa).size > 2,
            )
        }
    }

    @Test
    fun `regla 1 - domain es Kotlin puro`() {
        // Si domain conoce Android, Room o Retrofit, deja de poder probarse sin emulador y las
        // reglas de negocio quedan atadas al framework.
        exigirLimpio(
            regla = "domain no puede importar framework:",
            porQue = "Saca el tipo del framework a `data` y deja en `domain` una interfaz o un " +
                "modelo de Kotlin puro.",
            encontradas = violaciones(
                "domain",
                Regex(
                    "^import (android|androidx|kotlinx\\.coroutines\\.android)\\.|" +
                        "^import .*(room|retrofit2|okhttp3|compose|sqlite)",
                    RegexOption.IGNORE_CASE,
                ),
            ),
        )
    }

    @Test
    fun `regla 1 - domain no usa kotlinx serialization`() {
        // La serializacion pertenece a los DTO de `data`: un modelo de dominio anotado para JSON
        // acaba moldeado por la forma que le conviene al backend.
        exigirLimpio(
            regla = "domain no puede importar kotlinx.serialization:",
            porQue = "Anota el DTO en `data/remote` y mapealo al modelo de dominio.",
            encontradas = violaciones("domain", Regex("^import kotlinx\\.serialization")),
        )
    }

    @Test
    fun `regla 2 - ui no importa data`() {
        val encontradas = violaciones("ui", Regex("^import mx\\.petmate\\.app\\.data"))
            .filterNot { linea -> EXCEPCIONES_UI_IMPORTA_DATA.any { linea.startsWith(it) } }

        exigirLimpio(
            regla = "ui no puede importar data:",
            porQue = "La interfaz habla con `domain` (casos de uso o interfaces de repositorio), " +
                "nunca con repositorios concretos, DAO, entidades de Room ni DTO. Si una " +
                "violacion es de ui/ y no es tuya, avisale a quien lleva ui/ en vez de moverla " +
                "de lugar; si la aceptas por ahora, agregala a EXCEPCIONES_UI_IMPORTA_DATA con " +
                "la fecha y el motivo.",
            encontradas = encontradas,
        )
    }

    @Test
    fun `regla 3 - domain no conoce a data ni a ui`() {
        // domain esta en el centro: no conoce a nadie. Declara interfaces y `data` las implementa.
        exigirLimpio(
            regla = "domain no puede importar data ni ui:",
            porQue = "Invierte la dependencia: declara la interfaz en `domain` y que `data` la " +
                "implemente.",
            encontradas = violaciones("domain", Regex("^import mx\\.petmate\\.app\\.(data|ui)\\.")),
        )
    }

    @Test
    fun `regla 4 - Entity y Dto viven en data`() {
        val fuera = listOf("core", "di", "domain", "ui").flatMap { capa ->
            violaciones(capa, Regex("\\b(class|data class) [A-Za-z]+(Entity|Dto)\\b"))
        }

        exigirLimpio(
            regla = "Las clases *Entity y *Dto solo viven en data:",
            porQue = "`*Entity` va en `data/local`, `*Dto` en `data/remote`. Si una pieza no cabe " +
                "en ninguna capa, la clasificacion esta mal: no la muevas, replantea el tipo.",
            encontradas = fuera,
        )
    }

    @Test
    fun `regla 5 - no hay println en produccion`() {
        // Un println no se puede apagar, no lleva etiqueta y puede acabar imprimiendo datos del
        // usuario en el Logcat de un build de release. Para eso esta Logger.
        val todas = listOf("core", "data", "di", "domain", "ui").flatMap { capa ->
            violaciones(capa, Regex("println\\("))
        }

        exigirLimpio(
            regla = "No puede haber println en el codigo de produccion:",
            porQue = "Usa `Logger`, que se puede apagar y no viaja en release.",
            encontradas = todas,
        )
    }

    @Test
    fun `regla 5 - los dispatchers se inyectan, no se escriben a mano`() {
        // Un Dispatchers.IO escrito dentro de una clase la vuelve no determinista en pruebas: ya
        // no se puede controlar el tiempo. Por eso existe DispatcherProvider, y es el unico lugar
        // donde esta permitido nombrarlos.
        val todas = listOf("core", "data", "di", "domain", "ui").flatMap { capa ->
            violaciones(capa, Regex("Dispatchers\\.(IO|Default)"))
        }.filterNot { it.startsWith(PROVEEDOR_DE_DISPATCHERS) }

        exigirLimpio(
            regla = "Los dispatchers se inyectan por DispatcherProvider:",
            porQue = "Inyecta `DispatcherProvider` y usa `dispatchers.io` o `dispatchers.default`, " +
                "para que una prueba pueda sustituirlos.",
            encontradas = todas,
        )
    }

    @Test
    fun `regla 5 - no hay supresiones ni pendientes escondidos`() {
        // @SuppressLint con su motivo escrito si se permite; lo que no se permite es apagar un
        // aviso del compilador o dejar un TODO() que truena en ejecucion.
        val todas = listOf("core", "data", "di", "domain", "ui").flatMap { capa ->
            violaciones(capa, Regex("@Suppress\\(|@Ignore|TODO\\(\\)"))
        }

        exigirLimpio(
            regla = "Supresiones de avisos y TODO() pendientes:",
            porQue = "Arregla la causa. Si de verdad hace falta suprimir, usa `@SuppressLint` con " +
                "el motivo escrito al lado, que es lo que ya se hace en el resto del proyecto.",
            encontradas = todas,
        )
    }

    private companion object {
        const val PROVEEDOR_DE_DISPATCHERS = "core/coroutines/DispatcherProvider.kt"

        /**
         * Violaciones de la regla 2 que ya existian al escribir esta prueba (27/11/2026).
         *
         * Las dos estan en `ui/`, que es territorio de Eduardo, y corregirlas significa cambiar
         * sus pantallas: no se tocan desde aqui. Quedan anotadas para que la prueba proteja contra
         * violaciones **nuevas** en vez de estar en rojo por estas dos, y para que no se olviden.
         *
         * Que hay que hacer con cada una:
         *  - `label` es una funcion de presentacion que vive en `data/repository`; su lugar es
         *    `ui/` o un mapper de `domain`.
         *  - `SamplePetRepository` es un repositorio concreto usado desde un componente de
         *    Compose; deberia llegar por un caso de uso o como parametro de vista previa.
         */
        val EXCEPCIONES_UI_IMPORTA_DATA = listOf(
            "ui/business/OwnerBusinessScreens.kt",
            "ui/pets/PetComponents.kt",
        )
    }
}
