# Tareas 005 — Sincronización offline

> Lista de tareas de la unidad `005-sincronizacion-offline`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **2 / 12 tareas** (2 verificadas; 2 sin marcar: implementadas, falta prueba instrumentada, al 26/11/2026) · Fases completadas: **0 / 5**

---

## Cómo leer una tarea

```
- [ ] T-0NN — descripción
  - Depende de · Criterio de aceptación · Verificación · Archivos
```

Reglas: se ejecutan **en orden de dependencia**, una a la vez; una tarea toca
a lo más ~5 archivos; se marca `[x]` **solo** cuando su verificación pasó de
verdad; al cerrarla se actualiza `memoria.md`; después de cada tarea el agente
**se detiene y reporta**.

Puerta de calidad de toda tarea que toque código:
```bash
./gradlew ktlintCheck detekt test
```

---

## Fase F1 — Política

- [x] **T-001** — Política por entidad y contrato de entidad sincronizable
  - Depende de: 001/T-029, 001/T-007
  - Aceptación: existe `SyncPolicy` con la tabla de mascota, vacuna,
    desparasitación, peso, alergia y condición (crear, editar, borrar y
    conflicto); existe `SyncableEntity` con identidad, dependencia y
    serialización; no importa Android ni Room; hay pruebas de la tabla.
  - Verificación: `./gradlew test` y `grep` de imports prohibidos en
    `domain/sync`.
  - Archivos: `domain/sync/SyncPolicy.kt`,
    `domain/sync/SyncableEntity.kt`,
    `domain/sync/SyncOperation.kt`,
    `test/.../domain/sync/SyncPolicyTest.kt`

- [ ] **T-002** — Extensión del outbox de 001 _(implementada, falta prueba instrumentada)_
  - Depende de: T-001, 001/T-029
  - Aceptación: la cola de 001 guarda estado por operación (pendiente,
    enviando, error permanente), dependencia y clave de idempotencia; sobrevive
    a la muerte del proceso; no se crea una segunda cola.
  - Verificación: prueba instrumentada de persistencia (C-09).
  - Archivos: `data/sync/SyncOutbox.kt`, `data/sync/SyncOperationEntity.kt`,
    `data/sync/SyncOperationDao.kt`,
    `androidTest/.../data/sync/SyncOutboxPersistenceTest.kt`

> **Punto de verificación F1:** política única y cola persistente.

---

## Fase F2 — Motor

- [x] **T-003** — Planificador con dependencias
  - Depende de: T-002
  - Aceptación: el planificador respeta el grafo (una vacuna no sale antes que
    su mascota) y aplica FIFO dentro de cada nivel; detecta ciclos al registrar
    una entidad y falla al arrancar si los hay.
  - Verificación: `./gradlew test` (C-04).
  - Archivos: `data/sync/SyncScheduler.kt`, `data/sync/SyncGraph.kt`,
    `test/.../data/sync/SyncSchedulerTest.kt`

- [ ] **T-004** — Idempotencia
  - Depende de: T-003 · Bloqueada por: **Q-01**
  - Aceptación: cada operación lleva una clave estable derivada del id de
    cliente; repetir diez veces produce un solo registro en el servidor; el
    servidor recibe la misma clave en cada reintento.
  - Verificación: `./gradlew test` con contador de peticiones (C-03).
  - Archivos: `data/sync/IdempotencyKey.kt`,
    `test/.../data/sync/IdempotencyKeyTest.kt`

- [ ] **T-005** — Reintentos y errores permanentes
  - Depende de: T-003 · Bloqueada por: **Q-08**
  - Aceptación: 5xx, timeout y corte de red se reintentan con backoff; un 4xx
    se marca como error permanente, no se reintenta y queda visible; el
    usuario puede corregir o descartar.
  - Verificación: `./gradlew test` con 400, 500 y timeout (C-05).
  - Archivos: `data/sync/SyncRetryPolicy.kt`,
    `data/sync/PermanentSyncError.kt`,
    `test/.../data/sync/SyncRetryPolicyTest.kt`

> **Punto de verificación F2:** orden, idempotencia y errores separados.

---

## Fase F3 — Garantías

- [ ] **T-006** — Resolución de conflictos
  - Depende de: T-003 · Bloqueada por: **Q-02, Q-05, Q-07**
  - Aceptación: en entidades clínicas gana el servidor y se emite un aviso al
    usuario; en la mascota gana la marca más reciente; el resultado es el mismo
    con reloj fijo y con marcas invertidas.
  - Verificación: `./gradlew test` (C-06, C-07).
  - Archivos: `data/sync/ConflictResolver.kt`,
    `data/sync/ConflictNotice.kt`,
    `test/.../data/sync/ConflictResolverTest.kt`

- [ ] **T-007** — Lápidas de borrado
  - Depende de: T-002 · Bloqueada por: **Q-06**
  - Aceptación: borrar sin red deja una lápida; al sincronizar se envía y no
    se recrea el elemento; una lectura posterior no lo devuelve.
  - Verificación: `./gradlew test` (C-08).
  - Archivos: `data/sync/TombstoneStore.kt`,
    `test/.../data/sync/TombstoneStoreTest.kt`

> **Punto de verificación F3:** conflictos y borrados no pierden ni resucitan
> datos.

---

## Fase F4 — Visibilidad

- [ ] **T-008** — Estado observable por elemento _(implementada, falta prueba instrumentada)_
  - Depende de: T-002, 001/T-020
  - Aceptación: el outbox expone `Flow<SyncStatus>` por operación; existe un
    indicador visual con pendiente, sincronizando y error, y un botón de
    reintento; la UI no conoce el outbox directamente.
  - Verificación: prueba de interfaz del indicador (C-11).
  - Archivos: `data/sync/SyncStatus.kt`, `data/sync/SyncStatusRepository.kt`,
    `ui/components/sync/SyncStatusIndicator.kt`,
    `test/.../ui/components/sync/SyncStatusIndicatorTest.kt`

- [ ] **T-009** — Integración con la moderación diferida
  - Depende de: T-003, 004/T-005
  - Aceptación: antes de enviar un texto, el planificador consulta el estado
    de moderación; si no hay veredicto, la operación espera; si el veredicto es
    rechazo, no se envía y queda marcada.
  - Verificación: `./gradlew test` (C-12).
  - Archivos: `data/sync/ModerationGate.kt`,
    `test/.../data/sync/ModerationGateTest.kt`

> **Punto de verificación F4:** el usuario ve el estado y nada se publica sin
> moderar.

---

## Fase F5 — Cierre

- [ ] **T-010** — Entidad de prueba de extremo a extremo
  - Depende de: T-003, T-004, T-005, T-006, T-007
  - Aceptación: una entidad de prueba con la forma de una mascota recorre
    crear → editar → borrar sin red, sincroniza y reconcilia; demuestra que el
    motor no depende de 007 ni de 008.
  - Verificación: `./gradlew test` con el recorrido completo (C-01, C-02).
  - Archivos: `test/.../data/sync/FakeSyncableEntity.kt`,
    `test/.../data/sync/SyncEngineEndToEndTest.kt`

- [ ] **T-011** — Matriz de pruebas de sincronización
  - Depende de: T-008, T-009, T-010
  - Aceptación: una sola suite cubre orden, idempotencia, 4xx, conflictos,
    lápidas, proceso muerto, lectura offline y moderación; el doble de API
    cuenta las peticiones.
  - Verificación: `./gradlew test` (C-01…C-12).
  - Archivos: `test/.../sync/SyncMatrixTest.kt`

- [ ] **T-012** — Cierre de la unidad
  - Depende de: T-011
  - Aceptación: las capas están limpias; la política vive solo en `domain`; no
    hay una segunda cola; `memoria.md` queda actualizada y la evidencia escrita.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas` y
    revisión de evidencia.
  - Archivos: `docs/evidencia-005.md`, `memoria.md`

> **Punto de verificación F5:** motor probado sin depender de las entidades
> reales y unidad cerrada.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Política | T-001 … T-002 | Tabla por entidad y cola persistente |
| F2 Motor | T-003 … T-005 | Orden, idempotencia y errores |
| F3 Garantías | T-006 … T-007 | Conflictos y lápidas |
| F4 Visibilidad | T-008 … T-009 | Estado e integración con moderación |
| F5 Cierre | T-010 … T-012 | Entidad de prueba, matriz y evidencia |

**Tareas bloqueadas por preguntas abiertas:** T-004 (Q-01), T-005 (Q-08),
T-006 (Q-02, Q-05, Q-07), T-007 (Q-06), T-002 y T-011 (Q-03, Q-04), T-012
(Q-09, Q-10).

Esas preguntas deben resolverse con el responsable del backend y con el equipo
antes de ejecutar las tareas correspondientes.
