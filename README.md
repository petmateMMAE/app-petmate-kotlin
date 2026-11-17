# PetMate — App móvil Android

App de cuidado de mascotas: carnet de salud, QR, mapa de negocios afiliados,
consultas veterinarias y seguimiento de tratamientos.

Kotlin + Jetpack Compose, arquitectura limpia con MVVM. Desarrollada con
**Spec Driven Development**: cada funcionalidad tiene su `spec.md`, `plan.md` y
`task.md` en [`specs/`](specs/), y `constitution.md` manda sobre todo lo demás.

---

## Arrancar el proyecto

### 1. Clonar

```bash
git clone https://github.com/petmateMMAE/app-petmate-kotlin.git
cd app-petmate-kotlin
git checkout develop
```

`develop` es la rama de trabajo. `main` es lo que está liberado.

### 2. Crear `local.properties`

Este archivo **no se versiona** porque cada máquina tiene sus rutas. Créalo en
la raíz con esto:

```properties
sdk.dir=C:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk

# Backend en Azure (Chile Central)
BASE_URL_DEBUG=https://petmate-api.purplefield-b84659a6.chilecentral.azurecontainerapps.io/api/v1/
BASE_URL_RELEASE=https://petmate-api.purplefield-b84659a6.chilecentral.azurecontainerapps.io/api/v1/
```

Cambia `TU_USUARIO` por el tuyo. Si Android Studio ya abrió el proyecto, el
`sdk.dir` lo genera él solo y basta con agregar las dos URLs.

### 3. El JDK — esto es lo que más falla

**AGP 8.7 no acepta el JDK 25 del sistema.** Si compilas desde la terminal sin
configurarlo, Gradle truena con un mensaje inútil:

```
* What went wrong:
25.0.2
```

Eso es todo lo que dice. No menciona Java ni el JDK.

**Desde Android Studio** no pasa: usa su JDK incluido y funciona sin tocar nada.

**Desde la terminal** hay que apuntarle al JDK de Android Studio:

```bash
# Git Bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest
```

```powershell
# PowerShell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
./gradlew testDebugUnitTest
```

Si tu Android Studio está en otra carpeta, ajusta la ruta. Para no repetirlo
cada vez, puedes dejar `JAVA_HOME` fijo en las variables de entorno de Windows.

### 4. Compilar y probar

```bash
./gradlew assembleDebug        # genera el APK de depuración
./gradlew testDebugUnitTest    # 101 pruebas unitarias
```

---

## El backend

La app pega a una API en Azure Container Apps.

| | |
|---|---|
| URL | `https://petmate-api.purplefield-b84659a6.chilecentral.azurecontainerapps.io/api/v1/` |
| Repo | [`backend-petmate-sdd-aspnetcore`](https://github.com/petmateMMAE/backend-petmate-sdd-aspnetcore) |
| Salud | `GET /api/v1/health/live` y `/health/ready` |

### Si la API tarda en responder

Puede estar en **modo ahorro**: con poco tráfico escala a cero y la primera
petición tarda ~40 segundos en despertarla. Las siguientes son normales.

Para comprobar que está viva:

```bash
curl https://petmate-api.purplefield-b84659a6.chilecentral.azurecontainerapps.io/api/v1/health/live
```

### Lo que todavía no funciona

**Las rutas de foto responden `503`.** El código de subida de imágenes está
desplegado, pero falta crear la cuenta de almacenamiento en Azure
(`terraform apply` en el repo del backend). El `503` es la respuesta correcta
mientras no exista el almacenamiento: no es un bug de la app.

Todo lo demás funciona: login, registro, mascotas, carnet, vacunas, QR,
negocios, afiliación y escaneo.

### Trabajar sin backend

El flujo de identidad tiene un repositorio falso en memoria. Para usarlo, en
[`app/build.gradle.kts`](app/build.gradle.kts) cambia:

```kotlin
buildConfigField("boolean", "USE_FAKE_BACKEND", "true")
```

Cuenta de prueba: `demo@petmate.mx` / `petmate123`.

---

## Cómo está organizado el código

```
app/src/main/java/mx/petmate/app/
├── core/          utilidades transversales (AppResult, UiText, dispatchers)
├── domain/        modelos y casos de uso — Kotlin puro, sin Android
├── data/          repositorios, API de red, almacenamiento local
├── di/            módulos de Hilt
└── ui/            pantallas de Compose y ViewModels
```

**La regla que no se rompe** (`constitution.md` P3): las dependencias van
`ui → domain ← data`. `domain` nunca importa nada de Android ni de `data`. Si
necesitas romperla, el diseño está mal.

Las pantallas siguen un patrón de dos capas:

- `<Nombre>Content` — privado, sin estado, con `@Preview`
- `<Nombre>Screen` — público, lee el ViewModel

Así las previsualizaciones funcionan sin backend ni inyección.

---

## Ramas y commits

| Rama | Para qué |
|---|---|
| `main` | Liberado. Lo que está o va a Google Play |
| `develop` | Integración. De aquí sale el trabajo |
| `feature/<lo-que-sea>` | Una funcionalidad. Sale de `develop` y vuelve por PR |

Antes de abrir el PR, que `./gradlew testDebugUnitTest` pase. El pipeline
también corre gitleaks y mobsfscan, así que no metas tokens ni llaves ni de
mentiras en las pruebas: si lo necesitas, hay que documentar la excepción.

---

## Antes de cambiar algo

Lee [`constitution.md`](constitution.md). Hay cosas que piden autorización
antes de tocarlas:

- Agregar, quitar o subir de versión una dependencia
- Cambiar el esquema de Room
- Tocar `AndroidManifest.xml`
- Cambiar `minSdk`, `targetSdk`, `applicationId` o `namespace`
- Cambiar la configuración de CI

Y cosas que nunca se hacen: meter secretos en archivos versionados, borrar o
saltarse pruebas para que el build pase.

---

## Documentos del proyecto

| Archivo | Qué es |
|---|---|
| [`constitution.md`](constitution.md) | La ley: principios, stack, límites |
| [`AGENTS.md`](AGENTS.md) | Reglas para agentes de IA |
| [`memoria.md`](memoria.md) | Bitácora, equipo, decisiones, deudas |
| [`specs/MAPA-CAPACIDADES.md`](specs/MAPA-CAPACIDADES.md) | Las 20 unidades y su orden |
