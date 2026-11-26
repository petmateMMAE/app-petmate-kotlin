# Tareas 012 — Panel del afiliado

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **4 / 13 tareas** (1 verificada; 3 implementadas, por verificar; 2 sin marcar: implementadas, falta prueba instrumentada, al 26/11/2026)

---

## Fase F1 — Dominio

- [x] **T-001** — Modelos del panel
  - Depende de: —
  - Aceptación: `BusinessDashboard`, `FollowUpAlert`, `AttendedPet`,
    `AlertSeverity` y `DayFilter` existen en `domain/model`, en Kotlin puro,
    sin referencias a Android ni a framework.
  - Verificación: `./gradlew test`; `grep` sobre `domain/` sin importaciones
    de `android`, `androidx`, Room, Retrofit ni Compose.
  - Archivos: `domain/model/BusinessDashboard.kt`,
    `domain/model/FollowUpAlert.kt`, `domain/model/AttendedPet.kt`,
    `domain/model/DayFilter.kt`

- [x] **T-002** — Contrato del repositorio del panel _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001
  - Aceptación: `DashboardRepository` declara observar el panel, refrescarlo y
    consultar mascotas atendidas por rango; devuelve `Flow` y `AppResult`.
  - Verificación: `./gradlew test` con doble del contrato.
  - Archivos: `domain/repository/DashboardRepository.kt`

- [ ] **T-003** — Clasificación de alertas por fecha
  - Depende de: T-001 · Bloqueada por: **Q-02** (umbral configurable)
  - Aceptación: `ClassifyAlertUseCase` recibe fecha de revisión y reloj y
    devuelve atrasada (vencida), próxima (≤ 3 días rojo, ≤ 14 amarillo) o
    normal; los umbrales son constantes compartidas con 008 y 010, no
    redefinidas aquí.
  - Verificación: `./gradlew test` adelantando el reloj inyectado, con un caso
    por frontera: vencida, exactamente 3 días, 4 días, 14 días y 15 días
    (C-03, C-04, C-05).
  - Archivos: `domain/usecase/ClassifyAlertUseCase.kt`,
    `test/.../domain/usecase/ClassifyAlertUseCaseTest.kt`

- [ ] **T-004** — Casos de uso del panel y del filtro por día
  - Depende de: T-002, T-003
  - Aceptación: `GetDashboardUseCase` observa el panel desde el repositorio;
    `GetAttendedPetsUseCase` traduce `DayFilter` a un rango de fechas con el
    reloj inyectado y respeta la zona horaria.
  - Verificación: `./gradlew test` con pruebas de Hoy, Ayer, Semana y fecha
    concreta, incluida una con zona horaria distinta (C-08, C-09).
  - Archivos: `domain/usecase/GetDashboardUseCase.kt`,
    `domain/usecase/GetAttendedPetsUseCase.kt`, `test/.../domain/usecase/`

> **Verificación F1:** clasificación y filtros probados con reloj controlado;
> dominio sin framework.

---

## Fase F2 — Datos

- [x] **T-005** — DTOs, servicio remoto y mappers _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-002 · Bloqueada por: **Q-01, Q-05**
  - Aceptación: DTOs del resumen, de las alertas y de las mascotas atendidas,
    con su serialización; `DashboardApiService` declara las llamadas; los
    mappers traducen en ambos sentidos sin perder datos.
  - Verificación: `./gradlew test` con `MockWebServer` y pruebas de mapeo.
  - Archivos: `data/remote/dto/DashboardDto.kt`,
    `data/remote/DashboardApiService.kt`, `data/mapper/DashboardMapper.kt`,
    `test/.../data/mapper/DashboardMapperTest.kt`

- [ ] **T-006** — Caché local del panel
  - Depende de: T-001
  - Aceptación: entidades y DAOs guardan el resumen, las alertas y las
    mascotas atendidas; **la caché guarda identificadores y fechas, no
    contenido clínico** (decisión D-05); los DAOs exponen `Flow`.
  - Verificación: `./gradlew connectedAndroidTest` con base en memoria;
    inspección que confirme que no se persiste diagnóstico ni receta.
  - Archivos: `data/local/entity/DashboardEntity.kt`,
    `data/local/dao/DashboardDao.kt`,
    `androidTest/.../data/local/DashboardDaoTest.kt`
  - Nota: cambiar el esquema de Room requiere autorización.

> **Verificación F2:** mapeo sin pérdida; la caché no contiene datos clínicos.

---

## Fase F3 — Repositorio

- [ ] **T-007** — `DashboardRepositoryImpl`
  - Depende de: T-005, T-006
  - Aceptación: la UI observa Room y la red refresca Room según la política de
    caché de 001; un error de red no borra los datos locales y se reporta; el
    detalle clínico no se sirve desde caché, se pide al backend (D-05).
  - Verificación: `./gradlew test` con los casos: sin datos locales y red
    exitosa; con datos locales vigentes; con datos locales y red fallida
    (se conservan y se reporta el error) (C-11).
  - Archivos: `data/repository/DashboardRepositoryImpl.kt`,
    `di/DashboardModule.kt`,
    `test/.../data/repository/DashboardRepositoryImplTest.kt`

- [ ] **T-008** — Aislamiento por cuenta
  - Depende de: T-007
  - Aceptación: los datos del panel se asocian al negocio del usuario activo y
    se limpian en el logout; tras cambiar de cuenta no se ven datos del
    negocio anterior.
  - Verificación: `./gradlew test` con cambio de sesión simulado (C-13).
  - Archivos: `data/repository/DashboardRepositoryImpl.kt`,
    `test/.../data/repository/`

> **Verificación F3:** offline-first probado; sin fuga entre cuentas.

---

## Fase F4 — Presentación

- [x] **T-009** — ViewModel y pantalla del panel _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004, T-007
  - Aceptación: `DashboardViewModel` expone un `StateFlow` con **un estado por
    bloque** (portada, alertas, atendidas), no un `isLoading` global (D-03);
    `DashboardScreen` monta los tres bloques; en modo negocio es el destino
    inicial.
  - Verificación: `./gradlew test` con Turbine y prueba de navegación (C-01).
  - Archivos: `ui/panel/DashboardViewModel.kt`, `ui/panel/DashboardUiState.kt`,
    `ui/panel/DashboardScreen.kt`,
    `test/.../ui/panel/DashboardViewModelTest.kt`

- [ ] **T-010** — Portada del negocio y resumen _(implementada, falta prueba instrumentada)_
  - Depende de: T-009
  - Aceptación: muestra nombre, tipos y foto del negocio, y los contadores de
    atrasadas, próximas y mensajes sin leer; los textos salen de
    `strings.xml`; con cero mensajes el contador muestra cero, no se oculta.
  - Verificación: `./gradlew connectedAndroidTest` (C-02, C-06).
  - Archivos: `ui/panel/BusinessHeader.kt`,
    `androidTest/.../ui/panel/BusinessHeaderTest.kt`

> **Verificación F4:** el panel abre en modo negocio con portada y contadores.

---

## Fase F5 — Bloques

- [ ] **T-011** — Lista de alertas con color y navegación
  - Depende de: T-010
  - Aceptación: las alertas se pintan con el color de su severidad; tocar una
    navega al seguimiento con el identificador correcto; si 015 no existe, la
    navegación apunta a un destino provisional sin romper; con cero alertas se
    muestra el estado vacío explicativo.
  - Verificación: `./gradlew connectedAndroidTest` con prueba de navegación
    con argumento (C-07, C-10).
  - Archivos: `ui/panel/AlertList.kt`,
    `androidTest/.../ui/panel/AlertListTest.kt`

- [ ] **T-012** — Mascotas atendidas con filtro por día _(implementada, falta prueba instrumentada)_
  - Depende de: T-011 · Bloqueada por: **Q-03** (tamaño de página)
  - Aceptación: lista paginada con filtro Hoy, Ayer, Semana y selección de
    fecha; tocar una mascota navega a su consulta o a un destino provisional
    si 014 no existe; refrescar no duplica elementos; con cero resultados se
    muestra el estado vacío del filtro aplicado.
  - Verificación: `./gradlew connectedAndroidTest` con datos de varios días y
    prueba de refresco sin duplicados (C-08, C-09, C-10).
  - Archivos: `ui/panel/AttendedPetList.kt`,
    `androidTest/.../ui/panel/AttendedPetListTest.kt`

> **Verificación F5:** los dos bloques funcionan y navegan; estados vacíos
> correctos.

---

## Fase F6 — Cierre

- [ ] **T-013** — Integración y cierre de la unidad
  - Depende de: T-012
  - Aceptación: el panel abre sin conexión con los últimos datos y avisa que
    pueden estar desactualizados; pinta contenido local antes de 1 s con
    servidor lento; el botón central abre el escáner; C-01 a C-14 comprobados;
    la skill `revisar-capas` sin violaciones; `memoria.md` actualizada.
  - Verificación: `./gradlew ktlintCheck detekt test`, prueba con servidor
    lento (C-12), prueba con red ausente (C-11) y prueba de navegación al
    escáner (C-14).
  - Archivos: `memoria.md`

> **Verificación F6:** panel completo, sin conexión y con capas limpias.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-004 | Modelos, contrato, clasificación y filtros |
| F2 Datos | T-005, T-006 | Transporte y caché sin contenido clínico |
| F3 Repositorio | T-007, T-008 | Offline-first y aislamiento por cuenta |
| F4 Presentación | T-009, T-010 | Panel, portada y contadores |
| F5 Bloques | T-011, T-012 | Alertas y mascotas atendidas |
| F6 Cierre | T-013 | Integración y verificación |

**Bloqueadas:** T-003 (Q-02), T-005 (Q-01, Q-05), T-012 (Q-03).

**Nota de dependencias parciales:** esta unidad se puede cerrar sin que 014,
015 y 016 existan. Sus bloques se muestran vacíos con el estado explicativo y
la navegación apunta a destinos provisionales (decisión D-03).
