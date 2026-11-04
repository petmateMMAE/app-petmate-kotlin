# memoria.md — Estado del proyecto PetMate Móvil

> Memoria viva del repositorio. Los agentes la leen al empezar y la actualizan
> al terminar cada tarea. Si algo se decidió, está aquí.
>
> Última actualización: 29/10/2026

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
