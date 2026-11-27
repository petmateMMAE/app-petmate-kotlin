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

Progreso: **3 / 12 tareas** (3 verificadas; 1 sin marcar: implementada, falta prueba instrumentada, al 27/11/2026) · Fases completadas: **1 / 5**

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

- [x] **T-002** — Extensión del outbox de 001 _(verificada 27/11/2026 en emulador: SyncOutboxPersistenceTest, suite instrumentada completa en verde)_
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

- [x] **T-004** — Idempotencia _(hecha 27/11/2026)_
  - Depende de: T-003 · ~~Bloqueada por: Q-01~~ **desbloqueada el 27/11/2026.**
  - Estado (27/11/2026): **hecha y en verde**, 5 de 5, y `SyncOutboxTest` sigue
    en 6 de 6 tras el cambio de la clave.
    Lo que se hizo:
    - `data/sync/SyncIdempotencyKey.kt`: la clave **se deriva de la operación en
      vez de sortearse**. Antes era `UUID.randomUUID()`. Un UUID también sirve
      mientras la fila viva en Room, porque la clave se guarda una vez y cada
      reintento manda la guardada; el problema es el caso que importa: si la fila
      se pierde (base reinstalada, backup restaurado, borrado de datos a medias)
      y la app vuelve a encolar la misma escritura, un UUID nuevo hace que el
      servidor la vea como otra operación y **la aplique dos veces**. El dueño
      acabaría con dos vacunas idénticas en el carnet sin saber cuál borrar.
    - La clave lleva tipo, acción, id de cliente e instante de encolado. Cada
      parte está ahí por una razón concreta, escrita en el archivo: sin el
      instante, dos pesos legítimos de la misma mascota se verían como uno
      repetido y el segundo se perdería.
    - `SyncOutbox` ahora recibe `newKey: (SyncOperation) -> String` con
      `SyncIdempotencyKey::of` por omisión, y sigue inyectable para las pruebas.
    - `test/.../data/sync/SyncIdempotencyKeyTest.kt`, 5 casos, con el **contador
      de peticiones** que pide C-03: un servidor falso que cuenta llamadas y
      registros aparte, y descarta por clave como el backend. Diez envíos, un
      registro.
    **Un fallo que valió la pena, por si alguien toca el saneado.** En la primera
    corrida el caso del id de cliente falló: el saneado convertía `p-1` en `p_1`
    porque el guion es el separador de la clave. Se arregló dejando pasar el
    guion, no cambiando la prueba: los ids del proyecto son `p-1`, `v-3`, y
    volverlos irreconocibles estorba justo cuando alguien busca una operación
    atorada en el registro del servidor. El guion repetido no molesta porque para
    el servidor la clave es un token opaco que nadie vuelve a partir en piezas.
  - Aceptación: cada operación lleva una clave estable derivada del id de
    cliente; repetir diez veces produce un solo registro en el servidor; el
    servidor recibe la misma clave en cada reintento.
  - Verificación: `./gradlew test` con contador de peticiones (C-03).
  - Archivos: `data/sync/IdempotencyKey.kt`,
    `test/.../data/sync/IdempotencyKeyTest.kt`

- [x] **T-005** — Reintentos y errores permanentes _(hecha 27/11/2026)_
  - Depende de: T-003 · ~~Bloqueada por: Q-08~~ **desbloqueada: Adrián respondió
    el 27/11/2026 que 4xx son permanentes, y 5xx y red transitorios.**
  - Estado: **hecha y en verde.** `data/sync/SyncRetryPolicy.kt` con
    `SyncRetryDecision` (`Retry`, `Permanent`, `GaveUp`), y 5 pruebas en
    `test/.../data/sync/SyncRetryPolicyTest.kt` (C-05 con 400, 403, 409, 500,
    503, timeout y red).
    La regla, en una frase: **lo que el servidor rechazó por su contenido no se
    reintenta.** Un 400 reenviado cinco veces sigue siendo un 400 y mientras
    tanto tapa la cola y gasta batería.
    Dos decisiones que conviene no deshacer sin pensarlas:
    - `Unauthorized` cuenta como permanente. No es el dato el que está mal, es
      la sesión, y de eso se encarga el renovador de sesión, no la cola. Si
      fuera transitorio, la operación se reenviaría en bucle con un token
      vencido.
    - `GaveUp` existe aparte de `Permanent` para cuando se agotan los intentos
      de un error transitorio. Ahí el dato probablemente está bien y falló la
      red, así que el reintento manual tiene sentido; en un `Permanent` no lo
      tiene hasta que alguien corrija el dato.
    No se reusó `RetryInterceptor` (001/T-033) a propósito: ese decide sobre
    **una petición HTTP** y este sobre **una operación de la cola**, que
    sobrevive al cierre de la app y lleva la cuenta de sus intentos en Room.
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

- [x] **T-006** — Resolución de conflictos _(hecha 27/11/2026)_
  - Depende de: T-003 · ~~Bloqueada por: Q-02, Q-05, Q-07~~ **desbloqueada:
    Adrián respondió el 27/11/2026 que el servidor devuelve `updatedAt` pero no
    una versión, así que la reconciliación va por `updatedAt`.**
  - Estado: **hecha y en verde.** `data/sync/ConflictResolver.kt` con
    `ConflictNotice` y `ConflictOutcome`, y 5 pruebas en
    `test/.../data/sync/ConflictResolverTest.kt` (C-06 y C-07).
    Las dos reglas, y la diferencia no es técnica sino de **quién puede
    equivocarse menos**:
    - Lo clínico: gana el servidor siempre y se avisa (R-07). Lo registra una
      veterinaria y el teléfono del dueño puede llevar días sin conexión; dejar
      ganar al teléfono borraría lo que escribió un profesional. La marca local
      ni se mira.
    - La mascota: gana la marca más reciente (R-08). Son datos del dueño y el
      último que la tocó sabe mejor cómo se llama su perro.
    **El empate se decidió a propósito, y es lo que hay que saber de esta
    tarea.** `updatedAt` llega en segundos, así que dos cambios del mismo
    segundo traen la misma marca. Con marcas iguales **gana el servidor**. Es la
    elección conservadora: ante la duda no se pierde lo que ya estaba guardado, y
    el reloj de un teléfono puede estar mal puesto. Lo mismo si la marca local es
    nula: si el teléfono no sabe cuándo se cambió, no puede alegar que es más
    nuevo.
    El aviso solo sale cuando el usuario **pierde** su cambio. Avisar cuando gana
    sería ruido. Y `ConflictNotice` es un dato, no un texto: el mensaje lo elige
    la interfaz desde `strings.xml`.
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
