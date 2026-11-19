# memoria.md — Estado del proyecto PetMate Móvil

> Memoria viva del repositorio. Los agentes la leen al empezar y la actualizan
> al terminar cada tarea. Si algo se decidió, está aquí.
>
> Última actualización: 19/11/2026

---

## 1. Dónde vamos

| Campo | Estado |
|---|---|
| Fase SDD actual | **SPECIFY / PLAN / TASK de las 20 unidades — redactados, pendientes de aprobación** |
| Código implementado | **Identidad y sesión funcionando**: bienvenida, registro, login, logout y pantalla interna provisional. Backend simulado en memoria. |
| Tareas ejecutadas | Parte de 001 y 002 (identidad). El resto sigue sin ejecutar. |
| Siguiente paso | Revisión y aprobación de los documentos. **Urgente: resolver las preguntas abiertas con Miguel y arrancar la unidad 020 (Google Play), cuya fecha límite para iniciar la prueba cerrada es el 08/11/2026.** |

### Documentos listos
- [x] `constitution.md` (v1.1.0)
- [x] `AGENTS.md`
- [x] `memoria.md` (este archivo)
- [x] `.opencode/agents/` — 5 subagentes
- [x] `.agents/skills/` — 4 skills
- [x] `specs/001-arquitectura-base/` — `spec.md`, `plan.md`, `task.md` (42 tareas)
- [x] `specs/002-identidad-y-sesion/` — `spec.md`, `plan.md`, `task.md` (18 tareas)
- [x] `specs/003-permisos/` — `spec.md`, `plan.md`, `task.md` (10 tareas)
- [x] `specs/004-moderacion-contenido/` — `spec.md`, `plan.md`, `task.md` (12 tareas)
- [x] `specs/005-sincronizacion-offline/` — `spec.md`, `plan.md`, `task.md` (12 tareas)
- [x] `specs/006-notificaciones-push/` — `spec.md`, `plan.md`, `task.md` (11 tareas)
- [x] `specs/007-mis-mascotas/` — `spec.md`, `plan.md`, `task.md` (13 tareas)
- [x] `specs/008-carnet-salud/` — `spec.md`, `plan.md`, `task.md` (14 tareas)
- [x] `specs/009-compartir-historial/` — `spec.md`, `plan.md`, `task.md` (11 tareas)
- [x] `specs/010-mapa-e-inicio/` — `spec.md`, `plan.md`, `task.md` (14 tareas)
- [x] `specs/MAPA-CAPACIDADES.md` — 20 unidades, dependencias y olas
- [ ] `specs/011-*` a `specs/020-*` — **no abrir todavía**

---

## 2. El producto en dos párrafos

PetMate es una app Android que funciona como **carnet digital de salud de la
mascota**. El dueño registra a sus mascotas con vacunas, desparasitaciones,
peso, alergias e historial médico, y puede **compartir ese historial mediante
un QR** con duración y secciones limitadas. La pantalla de inicio es un **mapa
de veterinarias, tiendas y estéticas afiliadas**.

Hay dos roles: **Usuario** (dueño) y **Afiliado** (negocio). Un usuario se
afilia desde su perfil eligiendo uno o varios tipos de negocio y entra en
"modo negocio" sin perder sus mascotas. El negocio escanea QRs, registra
consultas con cobro desglosado, da seguimiento al tratamiento y chatea con el
dueño.

---

## 3. Arquitectura decidida

- **Kotlin nativo + Jetpack Compose** (Material 3). *Queda descartado Flutter
  para este repositorio: el proyecto ya existe en Kotlin.*
- **MVVM + Clean Architecture** en tres capas: `ui` → `domain` ← `data`.
- **Hilt** para inyección de dependencias.
- **Room (SQLite)** como fuente de verdad local; la app es **offline-first**.
- **Retrofit + OkHttp + kotlinx.serialization** contra la API en `/api/v1`.
- **Navigation Compose** con un solo `NavHost` y rutas tipadas.
- **JWT**: token de acceso de 15 min + refresh rotativo de 30 días. La sesión
  no expira sola; solo el logout la cierra.
- **Caché frontend** (nivel 3 de los 3 exigidos): memoria + disco con
  expiración explícita.
- **FCM** para notificaciones push.
- Backend en **ASP.NET Core**, nube **Azure**, base **MongoDB Atlas + Redis**
  (fuera de este repositorio).

### Lo que falta decidir
- [ ] Función de **ML** definitiva (candidata: raza por foto con ML Kit/TFLite).
- [ ] Función de **IA generativa** definitiva (candidatas: "explícame fácil"
      la receta, resumen del historial).
- [ ] Proveedor de mapas (Google Maps vs. alternativa) — queda en `010/Q-01`
      y bloquea el SDK de esa unidad, no la spec ni el resto del plan.
- [ ] `minSdk` final: el andamio trae 24; los lineamientos piden Android 8+
      (API 26). **Confirmar con el profesor antes de fijarlo.**

---

## 4. Equipo y reparto

| Quién | Qué le toca |
|---|---|
| **Adrián Adonai García Jiménez** | App móvil (mapa, mascotas, carnet, QR, chat, cobro) + toda la nube en Azure (Terraform, 2 instancias, balanceador, HTTPS, Redis, CDN, moderación, observabilidad) + CI/CD + matriz de versiones + publicación en Google Play + **estructura SDD** |
| **Eduardo Olvera Camacho** | App móvil (seguimiento, notificaciones, afiliación, panel, nueva mascota, vacunas, alergias, permisos), SQLite sin internet, ML, IA generativa, landing con Google Analytics |
| **Miguel Angel Nuñez Gómez** | Backend (API) y NoSQL: MongoDB Atlas + Redis, JWT, caché, rate limit, moderación, chat, push |
| **Emanuel Cruz Cruz** | Pruebas unitarias, de seguridad, de integración, de nube y de regresión |

Organización GitHub: **petmateMMAE**.

| Repositorio | Qué es | Visibilidad |
|---|---|---|
| `app-petmate-kotlin` | **Este**: app móvil Android (Kotlin + Compose) | privado |
| `backend-petmate-nosdd-aspnetcore` | Backend (API REST en ASP.NET Core) | privado |

URL de este repo: https://github.com/petmateMMAE/app-petmate-kotlin
Ramas: `main` (estable) y `develop`.

> ⚠️ Hay otras dos organizaciones de GitHub que **no** se usan para este
> proyecto y con las que no se mezcla nada (repositorios, commits, tableros ni
> entregas):
>
> | Organización | Qué es | Estado |
> |---|---|---|
> | `petmateMMAE` | **La correcta.** Aquí vive todo el trabajo de esta clase. | en uso |
> | `PETMATE-APP-S` | Primer intento del equipo; su estructura quedó mal armada y se abandonó el 04/11/2026. | abandonada |
> | `PetMate-AMEL` | Proyecto gemelo con otra profesora. | ajena |

---

## 5. Restricciones que no se negocian

- Spec Driven Development + DevSecOps; desarrollo con agentes de IA.
- Pruebas unitarias, de seguridad y de integración; CI/CD para backend y móvil.
- Caché en tres niveles (base de datos, backend, frontend).
- Logout real en backend y móvil; sesión siempre viva mientras no haya logout.
- Matriz de versionamiento definida **antes** de desarrollar y aplicada en cada
  despliegue.
- Nube con balanceador, mínimo 2 instancias, Terraform, salida HTTPS, costo cero.
- Base de datos NoSQL.
- Móvil: despliegue desde GitHub a Google Play, moderación de contenido antes
  de guardar, push, aviso de privacidad ligado a la landing, eliminar cuenta,
  manejo de permisos, SQLite con sincronización, al menos una función de ML y
  una de IA generativa.
- Entrega en `.rar` + script de la base de datos.
- **Fecha de evaluación: 02/12/2026.**

---

## 6. Bitácora

### 27/10/2026
- Arranca el proyecto en la organización `PETMATE-APP-S` (abandonada el
  04/11/2026, ver esa fecha); el repositorio del backend en ASP.NET Core ya
  existe.
- Se crea la estructura de **Spec Driven Development** de la app móvil:
  `constitution.md`, `AGENTS.md`, `memoria.md`, subagentes, skills y la unidad
  `specs/001-arquitectura-base/` con `spec.md`, `plan.md` y `task.md`.
- Las specs cubren **solo la arquitectura base**, sin funcionalidad de PetMate,
  por indicación del profesor.
- **No se ejecuta ninguna tarea**: el profesor dio instrucción explícita de
  esperar.
- Se verifica en PowerShell que la estructura está completa y que **0 de 42**
  tareas se ejecutaron (evidencia en `docs/evidencia-verificacion.md`).
- Se crea el repositorio **`app-petmate-kotlin`** (privado) en la organización
  `PETMATE-APP-S`, con ramas `main` y `develop`. Primer commit: la estructura
  SDD completa, sin código de las capas.
- No se pudo correr la auditoría en **OpenCode**: la cuenta devuelve
  `Free usage limit reached` incluso con un mensaje de una palabra y tras
  cambiar de modelo. El prompt queda listo en
  `docs/prompt-auditoria-opencode.md` para cuando se restablezca la cuota.
- Pendiente para la próxima clase: instrucciones sobre las specs de
  funcionalidad específica de backend y móvil.

### 04/11/2026
- Se integra el **mapa real**: `osmdroid` 6.1.20 con teselas de OpenStreetMap.
  Se arrastra y hace zoom, y los marcadores de la maqueta se pintan encima,
  reposicionados en cada movimiento.
- **Decisión del proveedor** (cierra la Q-01 de la spec 010): osmdroid porque
  no pide llave de API, registro ni tarjeta. Google Maps no cobra la carga del
  mapa en Android, pero **sí exige habilitar facturación**, lo que choca con
  el costo cero del profesor. MapLibre está activo, pero su wrapper de Compose
  no está publicado en Maven Central y su API está en beta.
- **Riesgo asumido**: osmdroid está archivado desde noviembre de 2024. Funciona
  y es estable, pero no recibe actualizaciones. Si un Android futuro lo rompe,
  se migra a MapLibre.
- La política de OpenStreetMap se cumple en `OsmMapConfig`: User-Agent propio
  (el genérico está prohibido y bloquean el tráfico), atribución visible que no
  se puede ocultar, caché de 7 días y nada de descarga masiva.
- `MapBusiness` pasa de `mapX`/`mapY` a `latitude`/`longitude` reales. El mapa
  ilustrado de la maqueta se conserva para las previsualizaciones, que no
  pueden cargar un mapa de red.
- Permisos de ubicación agregados al manifiesto. La app funciona sin ellos:
  centra el mapa en una posición por defecto.

### 03/11/2026
- Se implementa el flujo de identidad: bienvenida, registro, login, logout y
  una pantalla interna provisional. **24 pruebas unitarias en verde.**
- **Backend simulado**: `FakeAuthRepository` guarda las cuentas en memoria
  mientras la API no existe. La sesión sí se guarda cifrada, para cumplir
  "sesión siempre viva". Para conectar el backend real basta poner
  `USE_FAKE_BACKEND = false` en `app/build.gradle.kts`.
  Cuenta de ejemplo: `demo@petmate.mx` / `petmate123`.
- `AuthRepositoryImpl` (contra la API) queda escrito pero inactivo. Al
  conectarlo hay que verificar rutas y nombres de campo contra el contrato
  real (spec 002, Q-01).
- **`applicationId` cambiado a `mx.petmate.app`** (antes `com.example.…`, que
  Google Play rechaza). Resuelve la deuda D-01.
- **Bajada de AGP 9.2.1 a 8.7.3**: Hilt todavía no soporta AGP 9 y el build
  fallaba con "Android BaseExtension not found". También se bajó Gradle a
  8.11.1, Compose BOM a 2024.12.01 y se fijó `org.gradle.java.home` al JDK de
  Android Studio, porque AGP 8.7 no soporta JDK 25. Queda registrado por si
  alguien se pregunta por qué no están las versiones más nuevas.
- Dependencias agregadas con autorización: Hilt, Navigation Compose, Room,
  Retrofit, OkHttp, kotlinx.serialization, DataStore, security-crypto, MockK,
  Turbine y coroutines-test.

### 04/11/2026
- **Diseño de pantallas igual a la maqueta** (`PetMate_App.html`), sin lógica
  nueva: splash, bienvenida (3 slides), login, registro y el **mapa como
  pantalla de inicio** (spec 010, A-01). Login y registro conservan sus
  ViewModels; solo cambió su `Content`.
- **Tema de marca**: paleta clara y oscura de la maqueta en `ui/theme`, colores
  extra en `MaterialTheme.petMateColors` y sin color dinámico. Fuentes
  **Baloo 2** (títulos) y **Figtree** (texto) en `res/font`, convertidas de la
  maqueta (licencia OFL). Íconos de la maqueta como vectores `ic_pm_*`.
- Componentes compartidos en `ui/components` (botones, campo de texto, barra
  inferior, `@PetMatePreviews` para ver claro y oscuro a la vez).
- El mapa es **ilustrado** (el dibujo de la maqueta) con datos de ejemplo en
  `MapSampleData`; `MapScreen` lleva un TODO para su `MapViewModel`. El mapa
  real espera la decisión del SDK (`010/Q-01`).
- Perfil aún no existe: la pestaña "Perfil" abre la pantalla provisional con
  "Cerrar sesión" para no perder el logout.
- No se pudo compilar en el entorno de la sesión (sin acceso a Google Maven);
  falta correr `./gradlew build` y revisar las previews en Android Studio.

### 29/10/2026
- El profesor autoriza crear **todas** las specs, no solo la arquitectura base.
- Se escribe `specs/MAPA-CAPACIDADES.md` con las 20 unidades numeradas, sus
  dependencias y el orden de construcción en olas.
- Se amplía la unidad 001 de 32 a 42 tareas: Azure, Google Play, moderación,
  push, permisos y sincronización offline.
- Se redactan las unidades **002 a 020** con sus tres archivos cada una.
  Total: **280 tareas definidas, 0 ejecutadas.**
- Todas las unidades se generaron con agentes de IA, siguiendo las puertas de
  aprobación de la metodología.
- **Hallazgo de calendario:** la unidad 020 exige iniciar la prueba cerrada de
  Google Play (12 testers, 14 días) **a más tardar el 08/11/2026** para llegar
  publicados a la evaluación del 02/12/2026. Las tareas T-001, T-002, T-003 y
  T-009 de esa unidad no dependen de código y pueden empezar de inmediato.
- Se rectifica la unidad `001-arquitectura-base` tras la auditoría contra los
  lineamientos del profesor. Se sube `constitution.md` a **v1.1.0**: se agrega
  el principio P13 (moderación antes de almacenar), se amplía P4 con las
  escrituras offline, se añaden al stack el consumo de Azure, la publicación en
  Play y la moderación, y se documentan en §3 las carpetas `session`, `cache`,
  `sync`, `moderation` y `push`.
- La spec 001 pasa de 15 a 22 capacidades y de 22 a 30 criterios de éxito. El
  plan pasa de 25 a 32 componentes y de 14 a 20 decisiones. `task.md` pasa de
  32 a **42 tareas** (nuevas T-033 a T-042), todas sin marcar.
- Nuevas áreas cubiertas: publicación en Google Play desde GitHub Actions
  (track interno → prueba cerrada), consumo del backend en Azure con
  reintentos e idempotencia, punto de intercepción de moderación, base de FCM,
  mecanismo de permisos y outbox de sincronización offline.
- Se corrigen desalineaciones menores: nombre de la organización (entonces
  `PETMATE-APP-S`), nombre real del repositorio (`app-petmate-kotlin`), R-14
  condicionada a Q-01 y advertencia de autorización en T-010 por el manifiesto.
- La revisión del `git diff` detectó y corrigió cuatro textos que contradecían
  la rectificación: G-06 y D-12 aún decían que la subida a Play era de la spec
  de liberación, la nota de T-031 lo repetía, y D-10 citaba R-22 en lugar de
  R-25. También se alineó el árbol de `core/` del plan con las rutas
  `core/permissions` y `core/connectivity`.
- **Siguen sin ejecutarse tareas**: 0 / 42.
- Se crea `specs/MAPA-CAPACIDADES.md` con las **20 unidades** del producto, sus
  dependencias en una sola dirección, 13 olas de construcción y el camino
  crítico `001 → 002 → 007 → 008 → 010 → 011 → 012 → 013 → 014 → 015 → 016 →
  020`. Se anota en §4 el riesgo de calendario de 020: la prueba cerrada de
  Google Play exige 12 testers durante 14 días antes de producción, así que la
  cuenta, la ficha y el primer AAB deben arrancar en paralelo desde ya.
- Se redactan las unidades **002 a 010** completas (`spec.md`, `plan.md` y
  `task.md`), todas sin tareas marcadas: 002 (18 tareas), 003 (10), 004 (12),
  005 (12), 006 (11), 007 (13), 008 (14), 009 (11) y 010 (14). Total de las
  nueve unidades: **115 tareas**, 0 ejecutadas (más las 42 de 001).
- Decisiones cerradas con el humano en esta sesión:
  - `010`: "lo más próximo" es el próximo **evento de salud** (vacuna o
    desparasitación), no el negocio más cercano; rojo ≤ 3 días, amarillo ≤ 14.
    La tarjeta depende de 008 y por eso 010 va después en el mapa.
  - `009`: el selector de negocios es una lista de **solo lectura** del
    directorio del backend, con la opción "cualquier veterinaria"; no crea
    negocios ni toca el mapa.
  - `004`: la app llama al **backend** y el backend llama a Azure; la llave
    nunca vive en el dispositivo. El texto se guarda offline y se modera al
    sincronizar (si se rechaza queda marcado); imágenes y videos no se suben
    sin moderar.
  - `005`: la política de sincronización por entidad se fija por adelantado,
    incluidas mascota y carnet; una entidad nueva se agrega ampliando 005 con
    aprobación. Conflictos: en datos clínicos gana el servidor con aviso; en
    datos de la mascota gana el más reciente.
  - `008`: los registros de origen veterinario no se editan ni se borran desde
    el carnet.
  - `003` y `006`: ningún permiso se pide al arrancar; el payload push no lleva
    datos clínicos.
- Dependencias que quedan bloqueadas por preguntas abiertas, no por falta de
  diseño: el SDK de mapas (`010/Q-01`), `coil-compose` en 007 (autorización
  pendiente) y el contrato de red de cada unidad con el backend.

### 04/11/2026 — cambio de organización en GitHub
- El equipo revisó la organización `PETMATE-APP-S` y concluyó que su estructura
  de repositorios estaba mal armada. **Se abandona** y todo el trabajo de la
  materia pasa a la organización **`petmateMMAE`**.
- Se crea `petmateMMAE/app-petmate-kotlin` (privado) y se reapunta el `origin`
  local. Se suben las tres ramas (`main`, `develop` y la rama de diseño) con
  todo su historial, de modo que no se pierde ningún commit anterior.
- Se actualizan las menciones de la organización en `constitution.md`,
  `memoria.md`, `specs/001-arquitectura-base/spec.md` y
  `docs/prompt-specs-completas-opencode.md`. Las menciones **históricas** de la
  bitácora se dejan con el nombre viejo a propósito: en esas fechas la
  organización nueva no existía.
- `PETMATE-APP-S` **no se borra**: queda como respaldo hasta que el equipo
  confirme que en `petmateMMAE` está todo.
- Se aprueba el diseño de pantallas y el mapa real; la rama
  `feature/010-diseno-pantallas-maqueta` se integra a `develop` y a `main`.
- Verificado antes de subir: `assembleDebug` correcto y **24 pruebas, 0 fallos**.

---

## 7. Deudas y defectos conocidos

| # | Qué | Severidad | Nota |
|---|---|---|---|
| D-01 | `app/src/main/java/.../` sigue en `com.example.petmateedel` | Media | Debería ser un `applicationId` real (p. ej. `mx.petmate.app`) antes de publicar en Google Play. Requiere permiso para cambiarlo. |
| D-02 | ~~Quedaron en el repositorio `hs_err_pid26380.log` y `replay_pid26380.log`~~ | Cerrada 04/11/2026 | Se verificó que **nunca estuvieron versionados**; la deuda estaba mal redactada. Siguen en la carpeta local (ahora son cuatro: `pid24740` y `pid26380`) y ya tienen regla en `.gitignore`. Se pueden borrar a mano cuando se quiera. |
| D-03 | `minSdk` 24 vs. Android 8+ (API 26) de los lineamientos | Media | Confirmar con el profesor (ver §3). |
| D-04 | No hay matriz de versionamiento escrita todavía | Alta | Los lineamientos la exigen **antes** de desarrollar. Es la tarea T-001. |
| D-06 | ~~Umbrales de fechas distintos~~ | Resuelta 17/11/2026 | Lista, carnet y registros usan `DueThresholds` (rojo ≤ 3, amarillo ≤ 14, la decisión aprobada en 010 R-07) con `dueColors()`. |
| D-05 | `ui` importa de `data`: `OwnerBusinessScreens.kt` (`data.repository.label`) y `PetComponents.kt` (`SamplePetRepository`) | Media | Rompe P3. Encontrado el 17/11/2026 al revisar capas en la spec 005; no se corrigió por estar fuera de la tarea. |

---

## Conexión con el backend real (06/11/2026)

- **Backend:** PetMate API v0.1.0 en Azure Container Apps, Chile Central (México Central no pudo crear el ambiente). URL en `local.properties` (`BASE_URL_DEBUG` / `BASE_URL_RELEASE`, terminando en `/api/v1/`); no se versiona.
- **La app ya usa el backend real por defecto.** Para volver al simulado: `USE_FAKE_BACKEND=true` en `local.properties`.
- **Contrato aplicado** (spec 002, sección «Resoluciones»): envoltura `{statusCode,message,data}`, registro solo con correo y contraseña (12-128), logout con Bearer + refresh token y renovación automática si el access token expiró, rol `Business` = afiliado, nombre guardado solo en el dispositivo.
- **Pruebas:** 34 en verde (7 nuevas de `AuthRepositoryImplTest` con MockWebServer y el formato real).
- **Lo que no se puede conectar todavía:** mascotas, carnet, mapa de afiliados, QR, etc. El backend aún no tiene esos endpoints (están «Planeado» en su catálogo). Se conectan unidad por unidad conforme Miguel los publique.
- **Avisos de uso:** en modo ahorro la API tarda 10-20 s en la primera petición y no hay servicio de 23:00 a 7:00 (hora de México). Timeout de red: 30 s.
## Perfil real y cuenta demo (06/11/2026)

- **Perfil** (pestaña Perfil del mapa): nombre, correo, tipo de cuenta (dueño / negocio), miembro desde, «Sesión iniciada» y cerrar sesión. Datos de `GET users/me/get` del backend (pm-037). Botón «Mapa» para volver.
- **El nombre ya vive en el servidor:** al registrarse se envía con `PUT users/me/update` (pm-038); al iniciar sesión se lee del perfil. Si el servidor falla, se conserva el nombre local (Q-09 resuelta).
- **Llamadas autenticadas:** si el access token expiró se renueva una vez con el refresh token; si eso también falla, la sesión se cierra y la app vuelve al login.
- **Cuenta demo** (backend real y simulado): `demo@petmate.mx` / `PetMateDemo2026`, nombre «Usuario Demo». En el login, tocar el aviso llena los campos. Se oculta con `SHOW_DEMO_ACCOUNT=false` en `local.properties`.
- **Pruebas:** 38 unitarias en verde.
- Pendiente aparte (no subido por decisión del equipo): el aviso «Despertando el servidor…» y el timeout de 60 s están guardados en `git stash`.
## Modo demo y Perfil de la maqueta (06/11/2026)

- **Probar sin cuenta** (`SessionState.Guest`): se entra sin registro, con la franja amarilla DEMO arriba; crear cuenta / ya tengo cuenta / salir de la demo llevan a registro, login o bienvenida. La cuenta demo con contraseña ya no se muestra por defecto.
- **Perfil** igual a `SCREENS.profile` de la maqueta, más las pantallas Privacidad y términos y Eliminar mi cuenta (sin endpoint de borrado todavía). 40 pruebas unitarias.

## Pipeline CI/CD DevSecOps de la app (10/11/2026)

- Un solo workflow `.github/workflows/pipeline.yml`, mismo flujo que el del backend: secretos (gitleaks) -> compilación + Android Lint -> SAST CodeQL, SAST móvil (mobsfscan), dependencias (Trivy) + SBOM -> una prueba por funcionalidad (matriz) -> puerta de calidad -> APK/AAB de release -> entrega (Release manual, Google Play interna si hay llaves) -> prueba de humo.
- En PR corre hasta el APK/AAB; en push a `main` entrega. Detalle y diagrama en `docs/ci-cd/pipeline.md`.
- Variable del repositorio `API_BASE_URL` = URL del backend en Azure (`.../api/v1/`). Firma y Google Play quedan pendientes: hacen falta los secretos de la llave y la cuenta de servicio.

## Mascotas, carnet y QR conectados al backend (10/11/2026)

- **Registro con nombre:** el backend del primer hito (pm-085) exige `displayName` en `auth/register`; ya va en el mismo registro (sin el PUT aparte). El login toma el nombre de la sesión y solo pide el perfil si no viene.
- **Mis mascotas** (spec 007, maqueta `pets`/`petsEmpty`/`petForm`): lista con próxima vacuna/desparasitación y alergias, alta, edición y borrado con confirmación. Especie solo Perro/Gato (lista cerrada del backend), sexo opcional, raza ≤ 60, nombre ≤ 50, nacimiento no futuro ni de hace más de 50 años.
- **Carnet** (spec 008, maqueta `carnet`): resumen, vacunas, desparasitaciones, peso con gráfica y alergias/condiciones (activas y superadas). Lo que registró un negocio se ve con candado y no se edita (403 del backend).
- **QR** (spec 009, pm-093 a pm-095): se dibuja con ZXing (`com.google.zxing:core`, solo codificador) el `qrToken`; abajo el código corto; "Generar QR nuevo" con confirmación.
- **Modo demo:** "Mascotas de ejemplo" en memoria (`SamplePetRepository`); guardar o editar pide crear cuenta.
- **Sesión:** `AuthorizedCaller` renueva el access token una vez con el refresh (con Mutex) y cierra la sesión solo si la renovación falla; un 403 no cierra sesión.
- **Fuera de alcance hoy:** fotos (pm-079), copia local sin conexión (spec 005), historial clínico (spec 014) y avisos (spec 006).
- **Pruebas:** 64 unitarias en verde; 3 jobs nuevos en la matriz del pipeline.

## Base local y cola offline (17/11/2026)

- **Room arranca** (spec 001, T-011): `PetMateDatabase` (versión 1, `petmate.db`) con una entidad de prueba desechable (`ProbeEntity`), su DAO con `Flow`, conversor de fechas a milisegundos y `DatabaseModule` en Hilt. `exportSchema = false` mientras ninguna versión publicada traiga la base.
- **Política de sincronización** (spec 005, T-001): `domain/sync/SyncPolicy` es el único lugar que decide qué se crea, edita o borra sin conexión. La tabla sale de las operaciones que el backend expone hoy: mascota crea/edita/borra y gana el más reciente; vacuna y desparasitación crean y borran; peso solo crea; alergia y condición crean y editan; en todo lo clínico gana el servidor y dependen de la mascota.
- **Cola persistente** (spec 005, T-002): tabla `sync_operations` con estado (`Pending`, `Sending`, `PermanentError`), padre, intentos, último error y clave de idempotencia única que se genera una vez al encolar. `SyncOutbox` es la única cola. Al abrir la app, lo que quedó `Sending` porque el proceso murió vuelve a `Pending` (`PetMateApplication`).
- **Pruebas nuevas:** 7 de la política y 6 de la cola en JVM, registradas en la matriz del pipeline (`politica-sincronizacion`, `cola-offline`); 2 instrumentadas de la base y 1 de persistencia al reabrir la base en disco (C-09).
- **Planificador** (spec 005, T-003): `SyncGraph` saca los niveles de `SyncPolicy` (mascota 0, carnet 1) y falla al arrancar si la política tiene un ciclo. `SyncScheduler` ordena por nivel y luego FIFO; un elemento con una operación enviándose o con error permanente bloquea sus operaciones siguientes y las de sus hijos. Es lógica pura: no envía ni toca la base. 9 pruebas (`SyncGraphTest`, `SyncSchedulerTest`), registradas en la matriz (`grafo-sincronizacion`, `planificador-sincronizacion`).
- **Estado visible** (spec 005, T-008): `SyncStatus` (sincronizado, pendiente, sincronizando, error) y la interfaz `SyncStatusRepository` viven en `domain/sync` y no en `data/sync` como dice la tarea, porque la UI no puede importar de `data` (P3 gana). `SyncStatusRepositoryImpl` toma el estado más grave de la cola por elemento y reintenta solo los errores de ese elemento. `ui/components/sync/SyncStatusIndicator` no pinta nada si está sincronizado y solo el error muestra «Reintentar». 5 pruebas JVM (`SyncStatusRepositoryImplTest`, matriz `estado-sincronizacion`) y 4 de interfaz en `androidTest`.
- **Verificado el 19/11/2026:** `assembleDebug`, `testDebugUnitTest` (las 36 pruebas JVM de esta sección en verde) y `lintDebug` (0 errores). 005/T-001 y T-003 quedan verificadas. T-011 de 001 y T-002 y T-008 de 005 piden prueba instrumentada: compilan (`assembleDebugAndroidTest`), pero no había dispositivo y siguen por verificar.
- **Room 2.6.1 → 2.7.2 (autorizado por Eduardo el 19/11/2026):** con 2.6.1 el build fallaba en `kspDebugKotlin` con `unexpected jvm signature V`. Room 2.6.1 no es compatible con KSP2 (`ksp` 2.2.10-2.0.2) y estos fueron los primeros DAOs del proyecto. Se descartó volver a KSP1 (`ksp.useKSP2=false`), que sí compilaba, porque KSP1 es obsoleto y desaparece en KSP 2.3.
- **Bloqueadas por el backend:** T-004 (Q-01), T-005 (Q-08), T-006 (Q-02, Q-05, Q-07) y T-007 (Q-06). Las tareas de 005 sin bloqueo ya están escritas; T-009 espera a 004 (moderación) y T-010 a T-012 esperan a T-004…T-007.

## Permisos (17/11/2026)

- **Spec 003 escrita completa (T-001…T-010).** Catálogo y contrato en `domain/permissions`; `AndroidPermissionGateway` y `PermissionFlow` (lógica pura) en `core/permissions`; explicación previa, diálogo de ajustes, `rememberPermissionRequest` y las cuatro alternativas en `ui/components/permissions`; pantalla de prueba `ui/permissionsdebug/PermissionHarnessScreen` **fuera** del grafo de navegación.
- **Pantallas conectadas:** `rememberLocationRequest` (mapa, afiliación, mi negocio) y el escáner pasan por el flujo. Antes, ZXing pedía la cámara sin explicación; ahora hay explicación previa y, si se niega, un aviso que lleva al código manual.
- **Decisiones:** Q-01 se piden precisa y aproximada (basta la aproximada); Q-02 código de 8 caracteres; Q-03 notificaciones solo al activarlas; Q-05 selector de fotos del sistema sin permiso. Q-04 (aprobación de textos) y Q-06 (analítica) siguen abiertas. Detalle en `docs/evidencia-003.md`.
- **Desviaciones de la tarea:** las pruebas de interfaz van en `androidTest` y no en `test` (Compose necesita dispositivo sin Robolectric); el gateway no persiste negativas en disco: el sistema las recuerda y el flujo guarda su contador con `rememberSaveable`.
- **Pruebas:** 18 JVM (`PermissionCatalogTest`, `PermissionFlowTest`, `PhotoFallbackInitialsTest`, en la matriz como `catalogo-permisos`, `flujo-permisos`, `alternativa-foto`) y 14 instrumentadas.
- **Verificado el 19/11/2026:** las 18 pruebas JVM en verde y `lintDebug` sin errores; T-001 queda verificada. T-002 a T-009 piden pruebas instrumentadas: compilan, pero no había dispositivo. Siguen pendientes las 14 instrumentadas y la prueba manual de volver desde ajustes.

## Seguimiento de tratamiento (17/11/2026)

- **Spec 015, lo que se escribió:** `FollowUpStatus` (Treating, Improving, Discharged, los textos del backend) y `Followup.statusKind`; `FollowUpTransition` con la tabla de la spec (desde el alta no sale nada; Q-05 queda: no se reabre, se abre uno nuevo); `DueThresholds` (3 y 14 días) y `ClassifyReviewDateUseCase` con «hoy» inyectado; `ChangeFollowUpStatusUseCase` valida la transición y los días (1–365) antes de llamar al servidor; `ui/seguimiento` con `FollowUpViewModel`, `FollowUpScreen`, `StatusTimeline` y `ReviewDateBadge`.
- **Integración (T-011):** al guardar una consulta desde la ficha, la veterinaria ve la pantalla de seguimiento para abrirlo o saltarlo. Cambios mínimos en archivos de 014: `VeterinaryActionUiState.lastConsultationId`, el parámetro opcional `onSavedWithId` de `ConsultationFormScreen` y el estado `followUpFor` en `FichaScreen`.
- **Reutilizado y no duplicado:** el modelo `Followup`, los DTOs, `VeterinaryApiService` y los métodos de `VeterinaryRepository` ya existían (014). Por eso T-004 y T-006 no crearon archivos nuevos, y Q-01 queda respondida por el código: `followups/create`, `update/{id}`, `add-note`, `get-by-consultation/{id}`.
- **Decisiones:** Q-03, la fecha de revisión es opcional (el backend acepta null). Al dar de alta no se manda fecha.
- **Pendiente:** R-09 (moderar notas) espera a 004; R-05 (alerta al dueño) depende de Q-02 y de 006. **Pregunta nueva para Miguel:** las notas solo traen fecha y texto; la bitácora de la spec pide también autor y estado de cada cambio.
- **Pruebas:** 24 JVM (`FollowUpTransitionTest`, `ClassifyReviewDateUseCaseTest`, `ChangeFollowUpStatusUseCaseTest`, `FollowUpViewModelTest`), registradas en la matriz.
- **Verificado el 19/11/2026:** las 24 pruebas JVM en verde. T-001 a T-004 quedan verificadas. **Desmarcadas** porque su verificación no existe todavía: **T-006**, que pide `MockWebServer` para los endpoints de seguimiento y prueba de ordenación, y `VeterinaryRepositoryImplTest` no tiene ninguna de seguimiento; y **T-008**, que pide los casos "cambio sin red" y "cambio rechazado por el backend" en el repositorio, y `OfflineFirstFollowUpRepositoryTest` solo cubre lectura sin red, cambio confirmado y aislamiento por cuenta (el cambio sin red además espera a la cola de 005). T-007, T-009 y T-010 piden instrumentadas y siguen por verificar, igual que la prueba en el teléfono del flujo consulta → seguimiento.

## Offline-first: mascotas, carnet y seguimiento en SQLite (17/11/2026)

- **Lo que pedía P4 y no estaba:** las pantallas de 007 y 008 leían directo del servidor; sin red no se veía nada. Se implementaron las tareas de Room que estaban sin hacer: **007/T-002 y T-004**, **008/T-002 y T-005** y **015/T-007 y T-008**.
- **Tablas nuevas en `petmate.db`:** `pets`, `vaccines`, `dewormings`, `weights`, `allergies` (las condiciones van con `kind = Condition`), `carnet_sync` (marca de carnet descargado) y `followups`. Todas llevan `ownerId`: dos cuentas en el mismo teléfono no se mezclan (007, C-09).
- **`OfflineFirstPetRepository`** envuelve a `PetRepositoryImpl` sin cambiarlo. Leer sale de Room y no toca la red (008, C-02); `refreshPets()` y `refreshCarnet()` bajan del servidor y guardan; las escrituras van al servidor y, si confirma, se guardan en Room. `PetRepository` ganó `observePets`, `observeCarnet`, `refreshPets` y `refreshCarnet` con valores por defecto, así `SamplePetRepository` (modo demo) no cambió; `PetRepositoryRouter` los reenvía.
- **ViewModels de 007 y 008:** `PetsViewModel` y `PetCarnetViewModel` muestran primero lo guardado y luego refrescan; sin red y con datos guardados no muestran error.
- **`OfflineFirstFollowUpRepository`** (015) envuelve a `VeterinaryRepositoryImpl`: al leer intenta el servidor y, sin red, devuelve lo guardado; todo seguimiento que llega se guarda. `VeterinaryModule` pasó de `@Binds` a `@Provides` para envolverlo.
- **Desviaciones:** la spec pide prueba de migración, pero la versión publicada (versionCode 3) no trae base: Room crea las tablas al instalar y la primera migración real será el primer cambio de esquema después de publicar. El repositorio nuevo es un envoltorio en vez de reescribir `PetRepositoryImpl`, para no romper sus pruebas. En 015 la lectura intenta primero la red (para no mostrarle a la veterinaria un estado viejo) y cae a Room sin conexión.
- **Pendiente:** escribir sin conexión y encolar (007/T-004 y 008/T-005, segunda mitad) espera a 005/T-004…T-007, que esperan a Miguel.
- **Pruebas:** 14 JVM (`OfflineFirstPetRepositoryTest`, `OfflineFirstFollowUpRepositoryTest`, en la matriz como `mascotas-carnet-sin-red` y `seguimiento-sin-red`) y 5 instrumentadas (`PetStorageTest`).
- **Verificado el 19/11/2026:** las 14 pruebas JVM en verde. 007/T-002, 008/T-002 y 015/T-007 piden instrumentadas (`PetStorageTest`): compilan, pero no había dispositivo y siguen por verificar, igual que la prueba en el teléfono (abrir con red, luego modo avión y volver a abrir mascotas, carnet y seguimiento).

## Lo que no depende de nadie: 006, 017, 018, 019 y D-06 (17/11/2026)

- **D-06 resuelta:** `DueThresholds.level()` es el único cálculo; `ui/components/DueColors.kt` pinta lista de mascotas, resumen del carnet y registros con rojo ≤ 3 y amarillo ≤ 14 (antes 7 y 30). La tarjeta «lo más próximo» del inicio (010/R-07) todavía no existe.
- **006 · T-001, T-007, T-009:** `domain/push` con las cinco categorías y `NotificationPreferences` (seguridad no se puede apagar). **Decisión:** por defecto todo encendido menos «novedades», que es publicidad; la spec no fijaba valores. `NotificationPreferencesStore` (DataStore) y `NotificationActivation`, que guarda y pide el permiso con el flujo de 003 al encender una categoría. Sin Firebase todavía (Q-01 a Q-03 de Miguel y el `google-services.json`).
- **017 · T-001, T-002, T-009:** `LegalLinks` (no muestra enlaces mientras falten las URLs, Q-02); `UserProfile` ya existía y no se duplicó. Contratos `SettingsRepository` y `AccountRepository`, separados porque eliminar la cuenta no se deshace. `WipeLocalDataUseCase` + `DeviceDataWiper` borran todas las tablas, las preferencias y la sesión cifrada; su prueba revisa **todas** las tablas de SQLite, así que cubre tablas futuras.
- **018 · T-001:** `BreedSuggestion`, `ClassificationResult` (sugerencias, no identificada, modelo no disponible) y `BreedClassifier`, que recibe `PetImage` (bytes) y no un `Bitmap`.
- **019 · T-001, T-002, T-004:** `GenerationRequest`, `GeneratedText` (siempre marcado como IA, con huella SHA-256 del original), `GenerationResult` y `AiRepository`, que solo habla con el backend. Tabla `generated_texts`: solo devuelve el texto si la huella del original coincide.
- **Pruebas:** 20 JVM (`NotificationPreferencesTest`, `NotificationPreferencesStoreTest`, `BreedClassifierModelsTest`, `GenerationModelsTest`, en la matriz) y 3 instrumentadas (`WipeLocalDataUseCaseTest`, `GeneratedTextDaoTest`).
- **Verificado el 19/11/2026:** las 20 pruebas JVM en verde y `grep` sin imports de framework en los paquetes nuevos de `domain`. Quedan verificadas 006/T-001, 017/T-001 (una sola definición de `NotificationCategory`), 018/T-001 y 019/T-001 y T-002. **Desmarcada 017/T-002:** pide "`./gradlew test` con dobles" y ninguna prueba usa `SettingsRepository` ni `AccountRepository`. 006/T-007 y T-009, 017/T-009 y 019/T-004 piden instrumentadas y siguen por verificar.

## Moderación y chat: lo que no depende del backend (17/11/2026)

- **Hecho a pedido de Eduardo, aunque en el reparto son unidades de Adrián:** avisar a Adrián para que no lo duplique.
- **004 · T-001:** `domain/moderation` con `ModerationContentType`, `ModerationRequest` (texto, o bytes + MIME para imagen y video), `ModerationVerdict` (aprobado, rechazado, no validado; solo aprobado deja publicar, R-08) y `ModerationGateway`. **Decisión:** motivos de rechazo cerrados y genéricos (contenido no permitido, archivo muy grande, formato no soportado), porque la spec no los enumera y R-07 prohíbe revelar categorías internas.
- **016 · T-001 y T-002:** `Message`, `MessageContent` (texto o imagen), `MessageStatus`, `Conversation` (ligada al seguimiento de 015) y `ChatRepository` con `Flow` y `AppResult`, páginas de 30.
- **Lo siguiente de ambas espera a Miguel:** 004/T-002 (Q-01) y 016/T-003 y T-004 (Q-01, Q-02).
- **Pruebas:** 8 JVM (`ModerationVerdictTest`, `ChatModelsTest`), en la matriz como `moderacion-modelos` y `chat-modelos`.
- **Verificado el 19/11/2026:** las 8 en verde; 004/T-001 y 016/T-001 y T-002 quedan verificadas.

## Estado de las tareas en los `task.md` (17/11/2026)

- **Qué se hizo:** los `task.md` no se habían actualizado. Se marcó `[x]` cada tarea cuyo código ya existe, con la nota _(implementada, por verificar · 17/11/2026)_, y se actualizó el contador «Progreso» de cada spec. **Nadie ha corrido todavía la verificación de esas tareas**: Adrián (las suyas) y la sesión de Claude Code (las de Eduardo) deben confirmarlas y quitar la nota, o desmarcarlas si no pasan.
- **Criterio:** se marcó solo si el título completo de la tarea está hecho; una tarea a medias quedó sin marcar (ej. 007/T-004: lee de Room pero no encola sin red).

| Spec | Marcadas | Spec | Marcadas |
|---|---|---|---|
| 001 Arquitectura | 23 / 42 | 011 Afiliación | 9 / 14 |
| 002 Identidad | 11 / 18 | 012 Panel | 6 / 13 |
| 003 Permisos | 9 / 10 | 013 Escáner | 10 / 12 |
| 004 Moderación | 1 / 12 | 014 Consulta | 8 / 15 |
| 005 Offline | 4 / 12 | 015 Seguimiento | 7 / 11 |
| 006 Push | 3 / 11 | 016 Chat | 2 / 13 |
| 007 Mascotas | 9 / 13 | 017 Privacidad | 2 / 12 |
| 008 Carnet | 8 / 14 | 018 ML | 1 / 9 |
| 009 Compartir | 5 / 11 | 019 IA | 3 / 10 |
| 010 Mapa e inicio | 9 / 14 | 020 Play | 4 / 14 |
| **Total** | **134 / 280 (48 %)** | | |

- **No existe en el código (por eso no se marcó):** verificación de correo y recuperación de contraseña (002), cobro de la consulta y borradores persistentes (011, 014), caché del directorio (010), línea de tiempo y comprobante del carnet (008), tarjeta «lo más próximo» del inicio (010/R-07), cuenta regresiva de la compartición (009), ktlint/detekt aplicados al build (001/T-004), matriz de versionamiento (001/T-001), `ConnectivityObserver` (001/T-035) y la base de push con FCM (001/T-039).
- **020:** solo se marcó lo que Eduardo confirmó (cuenta, nombre de paquete, testers, prueba cerrada). Él debe revisar el resto.
- **Verificación del 19/11/2026 (Claude Code, unidades de Eduardo y las del 17/11):** 15 tareas verificadas, sin la nota; 3 desmarcadas (015/T-006, 015/T-008 y 017/T-002, motivos en sus secciones); el resto de lo de 17/11 pide instrumentadas y sigue con la nota, porque no había dispositivo. Las de Adrián no se tocaron. Totales en la tabla: 137 → 134.
- **Puerta de calidad incompleta:** `ktlintCheck` y `detekt` no existen en el build (001/T-004 sin hacer), así que de `ktlintCheck detekt test` solo corre `test`.
- **Para compilar en Windows:** el lanzador `gradlew.bat` necesita `JAVA_HOME`. El JBR de Android Studio es JDK 25 y AGP no lo acepta; se usa el JDK 21 que ya apunta `org.gradle.java.home` (`%USERPROFILE%\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2`).

