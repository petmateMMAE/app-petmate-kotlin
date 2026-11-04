# Constitución — PetMate Móvil (Android)

> Documento raíz del proyecto. Todo `spec.md`, `plan.md`, `task.md`, agente y skill
> debe ser coherente con este archivo. Si algo contradice la constitución, la
> constitución gana y el otro documento se corrige.
>
> Versión: 1.1.0 · Fecha: 29/10/2026 · Ratificada por: Adrián Adonai García Jiménez
> Proyecto: app móvil Android de PetMate · Org GitHub: petmateMMAE

---

## 0. Identidad del proyecto

| Campo | Valor |
|---|---|
| Producto | PetMate — carnet digital de salud de mascotas, QR para compartir historial y mapa de negocios afiliados |
| Este repositorio | Aplicación móvil Android (`app-petmate-kotlin`, carpeta local `PetMateEdel`) |
| Repositorio hermano | `backend-petmate-nosdd-aspnetcore` (API REST en ASP.NET Core) |
| Metodología | Spec Driven Development (SDD) + DevSecOps |
| Desarrollo | Mediante prompts y agentes de IA |
| Idioma de documentos | Español |
| Idioma del código | Inglés (identificadores, commits técnicos) |
| Fecha de evaluación | 02/12/2026 |

---

## 1. Principios (no negociables)

### P1 — La especificación precede al código
Ninguna línea de código de producción se escribe sin una spec aprobada que la
justifique. El orden es siempre: `spec.md` → `plan.md` → `task.md` →
implementación. Cada fase se detiene hasta que el humano la aprueba.

Corolario: si durante la implementación se descubre que la spec está mal,
**se corrige la spec primero** y luego el código. Nunca al revés.

### P2 — Arquitectura antes que funcionalidad
La arquitectura base (capas, inyección de dependencias, navegación,
persistencia, red, pruebas, CI/CD) se construye y se verifica **antes** de
cualquier funcionalidad de PetMate. Las specs de arquitectura no contienen
reglas de negocio del producto.

### P3 — Capas con dependencias en una sola dirección
Arquitectura limpia en tres capas. Las flechas apuntan hacia adentro y nunca
al revés:

```
ui (Compose, ViewModels)  ──→  domain (modelos, casos de uso, interfaces)
data (Room, Retrofit, repos) ──→  domain
```

- `domain` **no** conoce Android, Room, Retrofit, Compose ni ninguna librería de framework. Es Kotlin puro.
- `ui` **no** importa nada de `data`. Habla con `domain`.
- `data` implementa las interfaces que declara `domain`.
- Violar esta regla es un error de compilación o de revisión, no una opinión.

### P4 — Offline-first
La base de datos local (Room/SQLite) es la fuente de verdad para la UI. La red
actualiza la base local; la UI observa la base local. La app debe abrir y
mostrar datos sin conexión. Las escrituras hechas sin conexión se encolan y se
reconcilian al recuperar la red; ninguna se pierde ni se duplica en silencio.

### P5 — Estado unidireccional e inmutable
Un `ViewModel` por pantalla. Expone **un** `StateFlow<UiState>` inmutable y
recibe eventos de la UI. Los Composables son funciones sin estado propio de
negocio y no conocen repositorios.

### P6 — Nada de secretos en el repositorio
Llaves, tokens, cadenas de conexión y credenciales de firma nunca se escriben
en el código ni en archivos versionados. Van en `local.properties`,
variables de entorno o GitHub Secrets. `local.properties` y los `.jks` están
en `.gitignore`.

### P7 — Toda entrega pasa por el pipeline
Ningún artefacto llega a Google Play sin pasar compilación, pruebas unitarias,
pruebas de seguridad y pruebas de integración en GitHub Actions.

### P8 — Caché en tres niveles
El sistema completo cachea en base de datos, backend y frontend. La
responsabilidad de esta app es el **nivel frontend**: caché en memoria y caché
en disco (Room), con política de expiración explícita y documentada.

### P9 — La sesión no se cae sola
La sesión del usuario permanece viva indefinidamente mediante refresh token
rotativo. La única forma de cerrarla es un logout explícito, que invalida el
token en el backend y borra las credenciales locales.

### P10 — Privacidad y control del usuario
El aviso de privacidad es accesible desde la app y enlaza a la landing. El
usuario puede eliminar su cuenta. Los permisos del sistema se piden en el
momento en que se usan, con explicación previa, y la app funciona
razonablemente si se niegan.

### P11 — Versionamiento gobernado por la matriz
Existe una matriz de versionamiento. `versionCode` y `versionName` salen de
ella, nunca de un incremento improvisado. Cada despliegue registra su versión.

### P12 — Trazabilidad
Todo cambio se puede rastrear: tarea → spec → requisito (`RF-xx` / `RNF-xx`).
Un commit que no se puede ligar a una tarea no debería existir.

### P13 — Moderación antes de almacenar
Todo texto, imagen o video aportado por el usuario pasa por el servicio de
moderación de la nube **antes** de persistirse o enviarse. Si es inapropiado,
se rechaza con un mensaje claro y no se guarda. La arquitectura deja un único
punto de intercepción; la política, el proveedor y los umbrales se definen en
su propia spec.

---

## 2. Stack tecnológico obligatorio

| Capa | Tecnología | Nota |
|---|---|---|
| Lenguaje | Kotlin | JVM target 11 |
| UI | Jetpack Compose + Material 3 | Compose BOM administrado en `libs.versions.toml` |
| Arquitectura | MVVM + Clean Architecture | 3 capas (P3) |
| Asincronía | Coroutines + Flow | Sin `RxJava`, sin callbacks crudos |
| Inyección de dependencias | Hilt | Obligatoria; nada de singletons manuales |
| Navegación | Navigation Compose | Rutas tipadas, un solo `NavHost` |
| Persistencia local | Room (SQLite) | Fuente de verdad de la UI (P4) |
| Red | Retrofit + OkHttp + kotlinx.serialization | Base URL `/api/v1` |
| Imágenes | Coil | |
| Preferencias/sesión | DataStore + EncryptedSharedPreferences | Tokens cifrados |
| Push | Firebase Cloud Messaging | |
| Pruebas unitarias | JUnit4 + MockK + Turbine + kotlinx-coroutines-test | |
| Pruebas de UI | Compose UI Test + Espresso | |
| Calidad | ktlint, detekt | Bloquean el pipeline |
| CI/CD | GitHub Actions | |
| Nube (consumo) | Azure Container Apps por HTTPS | La infraestructura vive en otro repositorio; la app solo consume |
| Publicación | Google Play (AAB firmado) desde GitHub Actions | Track interno → prueba cerrada antes de producción |
| Moderación | API de moderación de la nube (Azure) | Proveedor y umbrales se fijan en su spec; la app solo llama |

**Compatibilidad:** `minSdk` 24 (Android 7.0) o superior según lo exijan los
lineamientos; `targetSdk` y `compileSdk` en la versión estable más reciente.

**Para agregar cualquier dependencia nueva se pide autorización** (ver §4).
Todas las versiones viven en `gradle/libs.versions.toml`. Está prohibido
escribir versiones literales en un `build.gradle.kts`.

---

## 3. Estructura de archivos

```
PetMateEdel/
├── constitution.md              ← este archivo
├── AGENTS.md                    ← reglas operativas para los agentes de IA
├── memoria.md                   ← estado y bitácora del proyecto
├── .opencode/
│   ├── agents/                  ← subagentes (especialistas)
│   └── skills/                  ← skills (procedimientos reutilizables)
├── specs/
│   └── 001-arquitectura-base/
│       ├── spec.md              ← QUÉ y POR QUÉ
│       ├── plan.md              ← CÓMO (técnico)
│       └── task.md              ← tareas ordenadas y verificables
└── app/src/
    ├── main/java/com/example/petmateedel/
    │   ├── PetMateApplication.kt
    │   ├── MainActivity.kt
    │   ├── core/                ← Result, errores, permisos, conectividad, tiempo
    │   ├── di/                  ← módulos de Hilt
    │   ├── domain/              ← modelos, interfaces de repositorio, casos de uso
    │   ├── data/
    │   │   ├── local/           ← Room: entidades, DAOs, base de datos
    │   │   ├── remote/          ← Retrofit: servicios, DTOs, interceptores
    │   │   ├── mapper/          ← DTO ↔ entidad ↔ modelo de dominio
    │   │   ├── repository/      ← implementaciones de los repositorios
    │   │   ├── session/         ← credenciales cifradas y estado de sesión
    │   │   ├── cache/           ← política de expiración (memoria + disco)
    │   │   ├── sync/            ← cola de escrituras y reconciliación offline
    │   │   ├── moderation/      ← punto de intercepción de contenido
    │   │   └── push/            ← token FCM y recepción de mensajes
    │   └── ui/
    │       ├── navigation/      ← rutas y NavHost
    │       ├── theme/           ← colores, tipografía, tema
    │       ├── components/      ← Composables reutilizables
    │       └── <feature>/       ← pantalla + ViewModel + UiState
    ├── test/                    ← pruebas unitarias (JVM)
    └── androidTest/             ← pruebas instrumentadas (dispositivo)
```

Una carpeta por capa. Un paquete por feature dentro de `ui`. Nada de carpetas
`utils` o `helpers` sin dueño.

---

## 4. Límites de los agentes

### Siempre (sin preguntar)
- Leer cualquier archivo del repositorio.
- Correr `./gradlew build`, `test`, `lint`, `ktlintCheck`, `detekt`.
- Escribir y corregir pruebas.
- Respetar la dirección de dependencias entre capas (P3).
- Declarar versiones nuevas en `libs.versions.toml`, no en los `build.gradle.kts`.
- Actualizar `memoria.md` al terminar una tarea.
- Marcar una tarea como completada solo cuando su verificación pasó de verdad.

### Preguntar primero
- Agregar, quitar o subir de versión cualquier dependencia.
- Cambiar el esquema de Room (entidades, migraciones).
- Modificar `constitution.md`, `AGENTS.md` o cualquier `spec.md` aprobada.
- Tocar `AndroidManifest.xml` (permisos, componentes).
- Configurar credenciales de publicación en Google Play o en Firebase.
- Cambiar la configuración del pipeline de CI/CD.
- Cambiar `minSdk`, `targetSdk`, `applicationId` o el `namespace`.
- Hacer `git push`, abrir un PR o publicar en Google Play.

### Nunca
- Escribir secretos, llaves o credenciales en archivos versionados.
- Borrar, saltar (`@Ignore`) o vaciar una prueba para que el build pase.
- Silenciar errores con `@Suppress`, `try/catch` vacíos o `// TODO` como solución.
- Implementar funcionalidad que no esté en una spec aprobada.
- Ejecutar tareas de `task.md` sin aprobación explícita del humano.
- Firmar commits con líneas de coautoría ni atribuir autoría a la IA.
- Mezclar este proyecto con el repositorio o la organización del proyecto gemelo.
- Escribir `print`/`println` en código de producción; se usa el logger del proyecto.

---

## 5. Calidad mínima exigida

| Dimensión | Umbral |
|---|---|
| Cobertura de pruebas en `domain` | ≥ 80 % |
| Cobertura de pruebas en `data` (repos y mappers) | ≥ 70 % |
| ViewModels con prueba unitaria | 100 % |
| Advertencias de `ktlint` / `detekt` | 0 |
| Pruebas de seguridad | 0 hallazgos críticos o altos |
| Compilación con warnings nuevos | no se aceptan |
| Build de `release` | debe compilar en todo momento |
| Hilos | ninguna operación de I/O en el hilo principal |

---

## 6. Flujo de trabajo y Git

- Ramas: `main` (estable) ← `develop` ← `feature/<id>-<descripción>`.
- Commits en formato convencional: `feat:`, `fix:`, `docs:`, `test:`,
  `refactor:`, `chore:`, `ci:`.
- Los commits se firman **solo a nombre del integrante humano**.
- Un PR por tarea o grupo pequeño de tareas; referencia su `task.md`.
- `main` está protegida: nada entra sin que el pipeline esté verde.

---

## 7. Gobierno de este documento

- Cambiar la constitución requiere aprobación explícita del responsable del
  proyecto y subir la versión semántica de la cabecera.
- Al cambiar la constitución se revisan **todas** las specs, planes y tareas
  vigentes para volver a alinearlas. La desalineación es un defecto.
- Ante cualquier conflicto, el orden de autoridad es:
  **constitución → spec → plan → task → código.**
