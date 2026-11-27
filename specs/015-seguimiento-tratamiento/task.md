# Tareas 015 — Seguimiento del tratamiento

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **4 / 11 tareas** (4 verificadas; 3 sin marcar: implementadas, falta prueba instrumentada, al 26/11/2026)

---

## Fase F1 — Máquina de estados

- [x] **T-001** — Modelos del seguimiento
  - Depende de: —
  - Aceptación: `FollowUp` (estado, fecha de revisión, bitácora),
    `FollowUpStatus` (En tratamiento, Mejorando, Dado de alta) y
    `FollowUpEntry` (estado, fecha, autor, nota) en `domain/model`, en Kotlin
    puro.
  - Verificación: `./gradlew test`; `grep` sobre `domain/` sin framework.
  - Archivos: `domain/model/FollowUp.kt`, `domain/model/FollowUpStatus.kt`,
    `domain/model/FollowUpEntry.kt`

- [x] **T-002** — Tabla de transiciones válidas
  - Depende de: T-001 · Bloqueada por: **Q-05** (reapertura)
  - Aceptación: `FollowUpTransition` declara las transiciones permitidas:
    En tratamiento → Mejorando, En tratamiento → Dado de alta,
    Mejorando → En tratamiento, Mejorando → Dado de alta; **desde Dado de alta
    no sale ninguna**; un seguimiento nuevo arranca En tratamiento.
  - Verificación: `./gradlew test` con **todas** las combinaciones de estado
    origen y destino, no solo las válidas (C-02, C-03, C-04, C-05).
  - Archivos: `domain/usecase/FollowUpTransition.kt`,
    `test/.../domain/usecase/FollowUpTransitionTest.kt`
  - Nota: mitiga G-01. La prueba debe cubrir la matriz completa.

- [x] **T-003** — Clasificación de la fecha de revisión
  - Depende de: T-001
  - Aceptación: `ClassifyReviewDateUseCase` **importa la constante de umbrales
    compartida** con 008, 010 y 012; no define umbrales propios; devuelve
    atrasada, próxima en rojo (≤3 días), próxima en amarillo (≤14) o normal.
  - Verificación: `./gradlew test` con fronteras: vencida, 3 días, 4 días,
    14 días, 15 días (C-08, C-09).
  - Archivos: `domain/usecase/ClassifyReviewDateUseCase.kt`,
    `test/.../domain/usecase/ClassifyReviewDateUseCaseTest.kt`

> **Verificación F1:** matriz de transiciones completa; umbrales importados.

---

## Fase F2 — Dominio

- [x] **T-004** — Contrato del repositorio
  - Depende de: T-001
  - Aceptación: `FollowUpRepository` declara crear seguimiento desde una
    consulta, observar un seguimiento, cambiar su estado y actualizar la fecha
    de revisión.
  - Verificación: `./gradlew test` con doble del contrato.
  - Archivos: `domain/repository/FollowUpRepository.kt`

- [ ] **T-005** — Cambio de estado con validación y moderación
  - Depende de: T-004, T-002 · Bloqueada por: **Q-03** (revisión obligatoria)
  - Aceptación: `ChangeFollowUpStatusUseCase` rechaza transiciones inválidas
    antes de llamar a la red; modera la nota por el punto de 004; una nota
    rechazada no se envía y devuelve el error correspondiente.
  - Verificación: `./gradlew test` con transición inválida, nota aprobada y
    nota rechazada (C-05, C-13).
  - Archivos: `domain/usecase/ChangeFollowUpStatusUseCase.kt`,
    `test/.../domain/usecase/ChangeFollowUpStatusUseCaseTest.kt`

> **Verificación F2:** transiciones inválidas no llegan a la red; notas
> moderadas.

---

## Fase F3 — Datos

- [ ] **T-006** — DTOs, servicio remoto y mappers
  - Depende de: T-004 · Bloqueada por: **Q-01, Q-02**
  - Aceptación: DTOs del seguimiento y de la bitácora con su serialización;
    `FollowUpApiService` declara crear, consultar y cambiar estado; mappers en
    ambos sentidos; **el orden de la bitácora lo fija la fecha del servidor**,
    no la del dispositivo (RT-05).
  - Verificación: `./gradlew test` con `MockWebServer` y prueba de ordenación.
  - Archivos: `data/remote/dto/FollowUpDto.kt`,
    `data/remote/FollowUpApiService.kt`, `data/mapper/FollowUpMapper.kt`,
    `test/.../data/mapper/FollowUpMapperTest.kt`

- [ ] **T-007** — Persistencia para lectura sin conexión _(implementada, falta prueba instrumentada)_
  - Depende de: T-001
  - Aceptación: entidad y DAO guardan estado, fechas, autor y notas; **no
    guardan diagnóstico ni receta** (D-06); el DAO expone `Flow`.
  - Verificación: `./gradlew connectedAndroidTest` con base en memoria;
    inspección que confirme que no se persiste contenido clínico de 014.
  - Archivos: `data/local/entity/FollowUpEntity.kt`,
    `data/local/dao/FollowUpDao.kt`,
    `androidTest/.../data/local/FollowUpDaoTest.kt`
  - Nota: cambiar el esquema de Room requiere autorización.
  - 27/11/2026: la prueba instrumentada ya existe en
    `androidTest/.../data/local/FollowUpDaoTest.kt`, 8 casos: guardado y lectura, upsert que
    reemplaza en vez de duplicar, aislamiento entre cuentas (dos cuentas con el mismo
    consultationId no se pisan), notas guardadas tal cual, fecha de revisión opcional, y dos que
    revisan el esquema real con `PRAGMA table_info` para confirmar D-06: ninguna columna clínica,
    y la lista exacta de columnas fijada para que agregar una obligue a pasar por la prueba.
    Compila, pero NO se ha ejecutado: no hay emulador levantado en esta máquina. La tarea sigue
    en [ ] porque su criterio es `connectedAndroidTest` y marcarla sin correrla sería falso.
  - 27/11/2026, parte del `Flow`: **HECHA.** `FollowUpDao` ya expone dos consultas observables,
    `observe(ownerId, consultationId): Flow<FollowUpEntity?>` y `observeAll(ownerId):
    Flow<List<FollowUpEntity>>`. **No cambió el esquema**: agregar un `@Query` que devuelve `Flow`
    no toca ninguna columna, así que no hizo falta migración ni subir la versión de la base.
    Por qué importa el `Flow` y no es adorno: con solo `get` suspend, alguien tiene que acordarse
    de volver a leer después de cada escritura, y el día que se olvide la veterinaria verá el
    estado viejo y creerá que su cambio no se guardó. Con el flujo, el repositorio escribe en Room
    y la pantalla se entera sola.
    El orden de `observeAll` lo decide el DAO —primero los que tienen fecha de revisión, de la más
    próxima a la más lejana, y al final los que no tienen— porque es el orden en que hay que
    atenderlos y repetir esa decisión en cada pantalla es cómo se desincronizan.
    Probado con 10 casos nuevos en `OfflineFirstFollowUpRepositoryTest` (pasó de 4 a 14, todos en
    verde), con un doble de DAO que **reemite** como Room: si el doble solo devolviera un valor y
    cerrara el flujo, la prueba pasaría sin comprobar lo único que importa.
  - **Lo único que falta para marcar T-007** es correr `connectedAndroidTest`, y eso necesita un
    emulador. No se levantó: esta máquina ya se quedó sin memoria una vez esta noche con el
    emulador encendido, y tumbarla a media sesión cuesta más que la marca.

> **Verificación F3:** bitácora ordenada por el servidor; sin duplicar
> expediente clínico.

---

## Fase F4 — Repositorio

- [x] **T-008** — `FollowUpRepositoryImpl` _(hecha 27/11/2026)_
  - Depende de: T-006, T-007, T-005
  - Estado (27/11/2026): **los cuatro casos de la verificación corren en verde**
    con `./gradlew test`. 14 pruebas en
    `test/.../data/repository/OfflineFirstFollowUpRepositoryTest.kt`.
    - Lectura sin red: lo leído con red se vuelve a leer sin red, con sus notas.
    - Cambio sin red: devuelve el fallo y **lo guardado no se mueve**. Un cambio
      que no llegó al servidor no puede verse como aplicado.
    - Cambio exitoso: queda guardado y el flujo observable lo reemite.
    - Cambio rechazado por el backend (409): no pisa lo local.
    Más: una nota que no llegó al servidor no aparece en la bitácora local, y
    la lectura observable respeta el filtro por `ownerId` igual que `get`.
    **D-05 con una prueba estructural, no de comportamiento.** El criterio dice
    que el cambio de estado no se encola en el outbox de 005. Comprobar eso por
    comportamiento solo demuestra que **hoy** no se encola. La prueba
    `este repositorio no conoce el outbox de sincronizacion` lee el archivo real
    y falla si aparece `SyncOutbox`, `SyncScheduler`, `outbox` o `enqueue`: el día
    que alguien se lo inyecte, falla sola, sin que nadie tenga que acordarse de
    D-05. Por qué importa: si el alta se encolara, la veterinaria daría de alta a
    una mascota, se iría sin red, y el alta se aplicaría horas después sin que
    nadie la confirme.
    **Diferencia de nombres con la spec, a propósito.** La tarea nombra
    `data/repository/FollowUpRepositoryImpl.kt`, `di/FollowUpModule.kt` y
    `FollowUpRepositoryImplTest.kt`. El proyecto los tiene como
    `OfflineFirstFollowUpRepository.kt`, dentro de `di/VeterinaryModule.kt`, y
    `OfflineFirstFollowUpRepositoryTest.kt`. No se renombró nada: el seguimiento
    no es un repositorio aparte, es el de veterinaria (014) con lectura local
    encima, y por eso delega con `by remote`. Partirlo en un repositorio propio
    duplicaría los doce métodos del contrato para no ganar nada.
    **Lo que queda y no es mío:** que el `FollowUpViewModel` consuma
    `observeFollowup()` en vez de pedir una sola vez. Eso es `ui/` (Eduardo). El
    repositorio ya expone el flujo y está probado.
  - Aceptación: la lectura observa Room y la red la refresca; el cambio de
    estado **requiere conexión** y no se encola en el outbox de 005 (D-05);
    sin red, la lectura funciona y el cambio se explica como no disponible; un
    error de red no borra lo local.
  - Verificación: `./gradlew test` con los casos: lectura sin red, cambio sin
    red, cambio exitoso y cambio rechazado por el backend (C-14).
  - Archivos: `data/repository/FollowUpRepositoryImpl.kt`,
    `di/FollowUpModule.kt`,
    `test/.../data/repository/FollowUpRepositoryImplTest.kt`

> **Verificación F4:** se lee sin conexión; no se cambia sin conexión.

---

## Fase F5 — Presentación

- [ ] **T-009** — ViewModel y pantalla con permisos por rol _(implementada, falta prueba instrumentada)_
  - Depende de: T-008
  - Aceptación: `FollowUpViewModel` expone un `StateFlow` con el seguimiento,
    la bitácora y si el usuario puede editar; **el dueño no ve los controles
    de cambio de estado** y el backend también lo rechaza (D-04); la
    veterinaria sí los ve.
  - Verificación: `./gradlew test` con Turbine y
    `./gradlew connectedAndroidTest` con sesión de dueño y de veterinaria
    (C-11).
  - Archivos: `ui/seguimiento/FollowUpViewModel.kt`,
    `ui/seguimiento/FollowUpUiState.kt`,
    `ui/seguimiento/FollowUpScreen.kt`,
    `test/.../ui/seguimiento/FollowUpViewModelTest.kt`

- [ ] **T-010** — Bitácora y distintivo de la fecha de revisión _(implementada, falta prueba instrumentada)_
  - Depende de: T-009
  - Aceptación: `StatusTimeline` muestra los cambios en orden, el más reciente
    arriba, con fecha, autor y nota; `ReviewDateBadge` pinta la fecha con el
    color de su clasificación; dar de alta marca el seguimiento como cerrado y
    deshabilita los controles.
  - Verificación: `./gradlew connectedAndroidTest` con tres cambios
    consecutivos y con cada color (C-06, C-12).
  - Archivos: `ui/seguimiento/StatusTimeline.kt`,
    `ui/seguimiento/ReviewDateBadge.kt`,
    `androidTest/.../ui/seguimiento/StatusTimelineTest.kt`

> **Verificación F5:** el dueño no edita; la bitácora refleja los cambios.

---

## Fase F6 — Cierre

- [ ] **T-011** — Integración y cierre de la unidad
  - Depende de: T-010
  - Aceptación: el seguimiento se abre desde una consulta de 014; el dueño
    llega desde el carnet de 008; cada cambio dispara la alerta del dueño; el
    resultado de `ClassifyReviewDateUseCase` **coincide con el de 012** para
    las mismas entradas; C-01 a C-15 comprobados; `revisar-capas` sin
    violaciones; `memoria.md` actualizada.
  - Verificación: `./gradlew ktlintCheck detekt test`, prueba de navegación
    desde 014 y 008 (C-01, C-10), prueba del disparo de alerta con doble
    (C-07) y prueba comparativa de umbrales (C-15).
  - Archivos: `memoria.md`

> **Verificación F6:** alertas disparadas; umbrales consistentes con 012.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Máquina de estados | T-001 … T-003 | Modelos, transiciones y clasificación |
| F2 Dominio | T-004, T-005 | Contrato y cambio validado |
| F3 Datos | T-006, T-007 | Transporte y lectura local |
| F4 Repositorio | T-008 | Offline para leer, red para cambiar |
| F5 Presentación | T-009, T-010 | Vista por rol y bitácora |
| F6 Cierre | T-011 | Integración y consistencia |

**Bloqueadas:** T-002 (Q-05), T-005 (Q-03), T-006 (Q-01, Q-02).

**Nota:** T-002 es la tarea más importante. La matriz de transiciones mal
hecha deja seguimientos en estados imposibles que luego nadie sabe corregir.
