import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

/*
 * Puerta de estilo (001/T-004). Autorizado por Adrian el 27/11/2026.
 *
 * **Por que con linea base y no reformateando todo.** `ktlintFormat` arreglaria las violaciones de
 * golpe, pero reescribiria cientos de archivos, entre ellos todo `ui/`, que es de otra sesion. Un
 * diff asi no se puede revisar y pisaria trabajo ajeno. La forma sana de meter un linter en un
 * proyecto que ya existe es registrar lo que hay hoy en una **linea base** y exigir limpieza de ahi
 * en adelante: lo nuevo entra limpio, lo viejo queda anotado como deuda y se va limpiando por
 * archivo cuando alguien lo toque de todos modos.
 *
 * Para limpiar un archivo de la deuda: arreglarlo (`ktlintFormat` sobre ese archivo) y regenerar la
 * linea base con `./gradlew :app:ktlintGenerateBaseline` y `:app:detektBaseline`. La linea base solo
 * debe encogerse; si crece, alguien metio codigo sucio y eso es justo lo que hay que notar en la
 * revision.
 */
ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
    // Sin color en CI: los codigos de escape ensucian el log de Actions.
    outputColorName.set("RED")
    ignoreFailures.set(false)
    baseline.set(file("$rootDir/config/ktlint/baseline.xml"))
    filter {
        // Lo generado no es codigo de nadie: R.java, BuildConfig, Hilt, Room.
        exclude { it.file.path.replace('\\', '/').contains("/build/") }
    }
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    source.setFrom(
        files(
            "src/main/java",
            "src/test/java",
            "src/androidTest/java",
        ),
    )
    ignoreFailures = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(false)
        md.required.set(false)
        txt.required.set(false)
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "17"
}

// La URL base sale de local.properties o de una variable de entorno.
// Nunca se escribe en el codigo (constitution.md P6).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun baseUrl(key: String, fallback: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: fallback

// Backend real por defecto (v0.1.0 desplegado en Azure el 06/11/2026). Para volver al
// backend simulado: USE_FAKE_BACKEND=true en local.properties o como variable de entorno.
val useFakeBackend: String =
    (localProperties.getProperty("USE_FAKE_BACKEND") ?: System.getenv("USE_FAKE_BACKEND") ?: "false")
        .trim().lowercase().let { if (it == "true") "true" else "false" }

// Aviso de una cuenta de prueba con contrasena en el login. Apagado por defecto: la demo del producto
// es "Probar sin cuenta" en la bienvenida. Solo para pruebas internas: SHOW_DEMO_ACCOUNT=true.
val showDemoAccount: String =
    (localProperties.getProperty("SHOW_DEMO_ACCOUNT") ?: System.getenv("SHOW_DEMO_ACCOUNT") ?: "false")
        .trim().lowercase().let { if (it == "true") "true" else "false" }

// La llave de subida sale de keystore.properties (ignorado por git) o de
// variables de entorno en GitHub Actions. Nunca se versiona (constitution.md P6).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(key)

android {
    namespace = "mx.petmate.app"
    // Google Play exige targetSdk 36 desde el 31/08/2026 (AGP 8.9.1 o superior).
    compileSdk = 36

    signingConfigs {
        create("release") {
            val storePath = signingValue("PETMATE_STORE_FILE")
            if (storePath != null) {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("PETMATE_STORE_PASSWORD")
                keyAlias = signingValue("PETMATE_KEY_ALIAS")
                keyPassword = signingValue("PETMATE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "mx.petmate.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // URL en local.properties (BASE_URL_DEBUG), nunca en el codigo. Debe terminar en /api/v1/.
            buildConfigField("String", "BASE_URL", "\"${baseUrl("BASE_URL_DEBUG", "http://10.0.2.2:8080/api/v1/")}\"")
            buildConfigField("boolean", "USE_FAKE_BACKEND", useFakeBackend)
            buildConfigField("boolean", "SHOW_DEMO_ACCOUNT", showDemoAccount)
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            // Sin llave configurada el AAB sale sin firmar (Play lo rechaza).
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "BASE_URL", "\"${baseUrl("BASE_URL_RELEASE", "https://api.petmate.mx/api/v1/")}\"")
            buildConfigField("boolean", "USE_FAKE_BACKEND", useFakeBackend)
            buildConfigField("boolean", "SHOW_DEMO_ACCOUNT", showDemoAccount)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Las pruebas de seguridad (001/T-042) no miran constantes de Kotlin: abren con File(...) el
// manifiesto y las reglas de respaldo reales del proyecto. Gradle no tiene forma de saber que
// esos archivos son entradas de la tarea de pruebas, asi que sin esto la deja en UP-TO-DATE y un
// cambio hecho SOLO en ellos no vuelve a ejecutarlas. Es decir: alguien podria poner
// usesCleartextTraffic="true", correr la suite en local, verla en verde y subirlo.
// Comprobado el 27/11/2026: antes de declarar esto, cambiar res/xml no reejecutaba nada.
tasks.withType<Test>().configureEach {
    inputs.file(layout.projectDirectory.file("src/main/AndroidManifest.xml"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("manifiestoQueLeenLasPruebasDeSeguridad")
    inputs.dir(layout.projectDirectory.dir("src/main/res/xml"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("reglasDeRespaldoQueLeenLasPruebasDeSeguridad")
}

/**
 * Cobertura (001/T-023 y T-027; constitucion parrafo 5). Autorizado por Adrian el 27/11/2026.
 *
 * Que se excluye y por que: codigo que nadie escribio a mano. Los modulos de Hilt, los `_Factory`,
 * los `_Impl` de Room, el `BuildConfig` y los serializadores de kotlinx los genera el compilador;
 * contarlos sube o baja el porcentaje sin que nadie haya probado nada.
 *
 * Los `@Composable` tambien se excluyen, y esta es la decision que hay que entender: una pantalla
 * de Compose no se puede ejercitar con una prueba JVM, solo con una instrumentada, y en esta
 * maquina las instrumentadas no corren. Si se contaran, el numero diria 19 % y no significaria
 * nada. Excluyendolas, el porcentaje mide lo que de verdad se puede probar aqui: dominio, datos y
 * ViewModels, que es justo lo que pide la constitucion.
 */
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*Hilt_*",
                    "*_HiltModules*",
                    "*_Factory",
                    "*_MembersInjector",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.*",
                    "*_Impl",
                    "*ComposableSingletons*",
                    "*\$\$serializer",
                )
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }

        /**
         * Suelo general, para que la cobertura total no se desplome sin que nadie se entere.
         * Los umbrales por capa que pide la constitucion se comprueban en
         * `verificarCoberturaPorCapa`, mas abajo.
         */
        verify {
            rule("Cobertura total: suelo del 40 %") {
                bound {
                    minValue = 40
                    coverageUnits = CoverageUnit.LINE
                }
            }
        }
    }
}

/**
 * Umbrales de cobertura por capa (001/T-027, constitucion parrafo 5).
 *
 * **Por que una tarea a mano y no el `verify` de Kover:** en Kover 0.9.1 una regla de verificacion
 * (`KoverVerifyRule`) solo expone `groupBy` y cotas; **no tiene `filters`**. Se comprobo en el
 * propio jar del plugin con `javap`. Sin filtros por regla no hay forma de decir "domain al 80 y
 * data/repository al 70" con su DSL, asi que esta tarea lee el XML que Kover ya genera y comprueba
 * cada capa. De paso imprime los numeros en cada corrida, que es lo que pide T-023: "la cobertura
 * se mide y se reporta".
 *
 * Medido el 27/11/2026 al cerrar T-027: total 57.4 %, domain 94.1 %, data/repository 71.0 %,
 * ViewModels 100.0 %. Las tres capas con umbral bloquean el pipeline.
 *
 * Aviso para quien lea esto despues: "ViewModels" son las clases cuyo nombre termina en
 * `ViewModel`, no sus lambdas internas (`...ViewModel$load$1`), que caen en el total. El 100 % es
 * de las clases; algunas lambdas de corrutina solo se ejercitan con pruebas instrumentadas.
 */
tasks.register("verificarCoberturaPorCapa") {
    group = "verification"
    description = "Comprueba los umbrales de cobertura por capa (001/T-027)."
    dependsOn("koverXmlReportDebug")

    val reporte = layout.buildDirectory.file("reports/kover/reportDebug.xml")
    inputs.file(reporte)

    doLast {
        val fabrica = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val doc = fabrica.newDocumentBuilder().parse(reporte.get().asFile)

        // Umbral de los ViewModels. El 27/11/2026 llego al 100 %, que es lo que pide la
        // constitucion, asi que dejo de ser un suelo movil y pasa a ser el requisito: cada
        // ViewModel nuevo entra con su prueba o el pipeline reprueba.
        // Historia del suelo, por si hay que bajarlo con razon: 35 -> 55 -> 100 el mismo dia.
        // Va aqui dentro y no como propiedad del script, porque la cache de configuracion de
        // Gradle no sabe serializar referencias a objetos del script.
        val sueloViewModels = 100.0

        val grupos = listOf("total", "domain", "data/repository", "ViewModels")
        val cubiertas = grupos.associateWith { 0 }.toMutableMap()
        val perdidas = grupos.associateWith { 0 }.toMutableMap()

        fun sumar(grupo: String, c: Int, p: Int) {
            cubiertas[grupo] = cubiertas.getValue(grupo) + c
            perdidas[grupo] = perdidas.getValue(grupo) + p
        }

        val paquetes = doc.getElementsByTagName("package")
        for (i in 0 until paquetes.length) {
            val paquete = paquetes.item(i) as org.w3c.dom.Element
            val nombrePaquete = paquete.getAttribute("name")
            val clases = paquete.getElementsByTagName("class")
            for (j in 0 until clases.length) {
                val clase = clases.item(j) as org.w3c.dom.Element
                var c = 0
                var p = 0
                // Solo los `counter` hijos directos de la clase: los de `method` contarian doble.
                val hijos = clase.childNodes
                for (k in 0 until hijos.length) {
                    val hijo = hijos.item(k)
                    if (hijo.nodeName != "counter") continue
                    val contador = hijo as org.w3c.dom.Element
                    if (contador.getAttribute("type") != "LINE") continue
                    c = contador.getAttribute("covered").toInt()
                    p = contador.getAttribute("missed").toInt()
                }
                if (c + p == 0) continue

                sumar("total", c, p)
                if (nombrePaquete.startsWith("mx/petmate/app/domain")) sumar("domain", c, p)
                if (nombrePaquete.startsWith("mx/petmate/app/data/repository")) sumar("data/repository", c, p)
                if (clase.getAttribute("name").substringAfterLast('/').endsWith("ViewModel")) {
                    sumar("ViewModels", c, p)
                }
            }
        }

        fun porcentaje(grupo: String): Double {
            val total = cubiertas.getValue(grupo) + perdidas.getValue(grupo)
            return if (total == 0) 0.0 else 100.0 * cubiertas.getValue(grupo) / total
        }

        logger.lifecycle("")
        logger.lifecycle("Cobertura de linea por capa (001/T-027):")
        grupos.forEach { grupo ->
            logger.lifecycle(
                "  %-16s %5.1f %%  (%d de %d lineas)".format(
                    grupo,
                    porcentaje(grupo),
                    cubiertas.getValue(grupo),
                    cubiertas.getValue(grupo) + perdidas.getValue(grupo),
                ),
            )
        }

        val fallos = mutableListOf<String>()
        mapOf("domain" to 80.0, "data/repository" to 70.0).forEach { (grupo, minimo) ->
            if (porcentaje(grupo) < minimo) {
                fallos += "%s esta en %.1f %% y la constitucion pide %.0f %%".format(grupo, porcentaje(grupo), minimo)
            }
        }
        if (porcentaje("ViewModels") < sueloViewModels) {
            fallos += ("ViewModels esta en %.1f %% y la constitucion pide %.0f %%. " +
                "Lo mas probable: un ViewModel nuevo sin su clase de prueba.")
                .format(porcentaje("ViewModels"), sueloViewModels)
        }

        if (fallos.isNotEmpty()) {
            throw GradleException(
                "Cobertura por debajo del umbral:\n" + fallos.joinToString("\n") { "  - $it" },
            )
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.okhttp.logging.interceptor)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    implementation(libs.osmdroid.android)

    // Dibuja el QR de cada mascota (spec 009). Solo el codificador, sin camara.
    implementation(libs.zxing.core)
    // Lector de QR con la camara del negocio (spec 012); trae su propia pantalla de captura.
    implementation(libs.zxing.android.embedded)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.navigation.testing)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
