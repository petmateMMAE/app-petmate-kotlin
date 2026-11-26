pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                // KSP (com.google.devtools.ksp) encaja en el patron com.google.* de arriba,
                // pero NO se publica en Google Maven: vive en Gradle Plugin Portal. Sin esta
                // exclusion el filtro lo atrapa y la resolucion falla en un entorno limpio
                // (el CI), aunque en una maquina que ya lo tiene en cache compile sin problema.
                excludeGroupByRegex("com\\.google\\.devtools.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PetMateEdel"
include(":app")
 