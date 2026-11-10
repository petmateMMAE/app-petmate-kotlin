import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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
