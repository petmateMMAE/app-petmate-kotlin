# Tareas 008 — Carnet de salud

> Lista de tareas de la unidad `008-carnet-salud`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **2 / 14 tareas** (2 verificadas; 6 sin marcar: implementadas, falta prueba instrumentada, al 26/11/2026) · Fases completadas: **0 / 5**

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

## Fase F1 — Dominio

- [x] **T-001** — Modelos del carnet y casos de uso
  - Depende de: 007/T-001, 001/T-018
  - Aceptación: existen `Vaccine`, `Deworming`, `WeightEntry`, `Allergy`,
    `Condition` y `MedicalRecord`, con `HealthEvent` como vista unificada;
    los casos de uso observan, registran, editan y borran; no importa Android;
    hay pruebas de orden y de campos.
  - Verificación: `./gradlew test` y `grep` de imports prohibidos en
    `domain/health`.
  - Archivos: `domain/health/HealthEvent.kt`,
    `domain/health/Vaccine.kt`, `domain/health/Deworming.kt`,
    `domain/health/WeightEntry.kt`,
    `test/.../domain/health/HealthEventTest.kt`

- [ ] **T-002** — Entidades, DAOs y migración _(implementada, falta prueba instrumentada)_
  - Depende de: T-001, 001/T-011 · Requiere autorización de esquema
  - Aceptación: las seis secciones tienen entidad y DAO; los `Flow` filtran por
    mascota y por activo; la migración desde la versión anterior se prueba; el
    cambio de esquema queda autorizado.
  - Verificación: prueba instrumentada de Room y migración (C-01).
  - Archivos: `data/local/HealthEntities.kt`, `data/local/HealthDao.kt`,
    `data/local/PetMateDatabase.kt`,
    `androidTest/.../data/local/HealthMigrationTest.kt`

- [ ] **T-003** — Alergias críticas y regla de origen
  - Depende de: T-001
  - Aceptación: `GetCriticalAllergiesUseCase` devuelve todas las alergias
    activas sin aceptar filtros; el caso de uso de edición y borrado rechaza
    los registros de origen veterinario; hay pruebas de ambos.
  - Verificación: `./gradlew test` (C-06, C-07).
  - Archivos: `domain/health/usecase/GetCriticalAllergiesUseCase.kt`,
    `domain/health/usecase/EditHealthRecordUseCase.kt`,
    `test/.../domain/health/HealthRulesTest.kt`

> **Punto de verificación F1:** dominio puro, alergias sin filtros y origen
> respetado.

---

## Fase F2 — Datos

- [x] **T-004** — DTOs, mapeo y cliente del carnet
  - Depende de: T-001, 001/T-012 · Bloqueada por: **Q-01**
  - Aceptación: el cliente cubre las seis secciones; los campos salen del
    contrato acordado; el mapeo se prueba con servidor local; el origen viaja
    en el DTO.
  - Verificación: `./gradlew test` con servidor local.
  - Archivos: `data/remote/HealthApi.kt`, `data/remote/HealthDtos.kt`,
    `data/mapper/HealthMapper.kt`,
    `test/.../data/remote/HealthContractTest.kt`

- [ ] **T-005** — Repositorio del carnet
  - Depende de: T-002, T-004, 005/T-003
  - Aceptación: el repositorio lee de Room, escribe local y encola según 005;
    ninguna lectura dispara red; el carnet completo se sirve desde Room; hay
    prueba con red ausente y contador de llamadas en cero.
  - Verificación: `./gradlew test` (C-01, C-02).
  - Archivos: `data/repository/HealthRepositoryImpl.kt`,
    `di/HealthModule.kt`,
    `test/.../data/repository/HealthRepositoryTest.kt`

> **Punto de verificación F2:** el carnet abre completo sin red y sin
> peticiones.

---

## Fase F3 — Vista

- [ ] **T-006** — Línea de tiempo y filtros
  - Depende de: T-001
  - Aceptación: un solo flujo une las seis secciones, ordena por fecha e id y
    aplica filtros por tipo y rango; las fechas se muestran en formato local;
    hay pruebas con datos fijos.
  - Verificación: `./gradlew test` (C-03, C-09, C-10).
  - Archivos: `domain/health/HealthTimeline.kt`,
    `domain/health/HealthFilter.kt`,
    `test/.../domain/health/HealthTimelineTest.kt`

> **Punto de verificación F3:** una sola línea de tiempo con filtros
> verificables.

---

## Fase F4 — Pantallas

- [ ] **T-007** — Pantalla del carnet y secciones _(implementada, falta prueba instrumentada)_
  - Depende de: T-005, T-006, 001/T-020
  - Aceptación: la pantalla muestra la línea de tiempo, las seis secciones y
    las alergias con distintivo crítico; el origen se ve en cada evento; hay
    estado vacío explicativo en historial médico.
  - Verificación: prueba de interfaz (C-03, C-05, C-10).
  - Archivos: `ui/health/CarnetScreen.kt`, `ui/health/CarnetViewModel.kt`,
    `ui/health/CarnetUiState.kt`, `ui/health/HealthEventCard.kt`,
    `test/.../ui/health/CarnetScreenTest.kt`

- [ ] **T-008** — Formulario de vacuna _(implementada, falta prueba instrumentada)_
  - Depende de: T-001, T-007 · Bloqueada por: **Q-02, Q-06, Q-08, Q-10**
  - Aceptación: registra nombre, fecha, dosis, próxima dosis y lugar; valida lo
    mínimo; guarda la próxima dosis como fecha; funciona sin conexión según
    005.
  - Verificación: prueba de interfaz y de dominio (C-04, C-11).
  - Archivos: `ui/health/VaccineFormScreen.kt`,
    `ui/health/VaccineFormViewModel.kt`,
    `test/.../ui/health/VaccineFormTest.kt`

- [ ] **T-009** — Formularios de desparasitación y peso _(implementada, falta prueba instrumentada)_
  - Depende de: T-001, T-007 · Bloqueada por: **Q-02, Q-03**
  - Aceptación: la desparasitación distingue interna y externa con producto y
    fecha; el peso se registra en kilogramos con fecha y se muestra en orden
    cronológico; ambos funcionan sin conexión.
  - Verificación: pruebas de interfaz (C-10, C-11).
  - Archivos: `ui/health/DewormingFormScreen.kt`,
    `ui/health/WeightFormScreen.kt`,
    `test/.../ui/health/HealthFormsTest.kt`

- [ ] **T-010** — Formularios de alergia y condición _(implementada, falta prueba instrumentada)_
  - Depende de: T-001, T-007 · Bloqueada por: **Q-05**
  - Aceptación: la alergia se marca como crítica y se muestra siempre; la
    condición indica si es crónica y desde cuándo; ambas se pueden editar y
    borrar si son del dueño.
  - Verificación: pruebas de interfaz (C-05, C-08).
  - Archivos: `ui/health/AllergyFormScreen.kt`,
    `ui/health/ConditionFormScreen.kt`,
    `test/.../ui/health/AllergyConditionTest.kt`

- [ ] **T-011** — Inmutabilidad y etiqueta de origen _(implementada, falta prueba instrumentada)_
  - Depende de: T-003, T-007
  - Aceptación: un registro veterinario no muestra acciones de editar ni
    borrar; uno del dueño sí, con confirmación; la etiqueta de origen es
    visible en la tarjeta.
  - Verificación: prueba de interfaz (C-07, C-08).
  - Archivos: `ui/health/HealthEventActions.kt`,
    `test/.../ui/health/HealthOriginTest.kt`

- [ ] **T-012** — Comprobante con permiso y moderación
  - Depende de: T-008, 004/T-006, 003/T-007
  - Aceptación: el comprobante se elige con el permiso correspondiente, se
    modera antes de subirse y un rechazo no lo adjunta; sin red queda pendiente.
  - Verificación: `./gradlew test` con doble de moderación (C-12).
  - Archivos: `data/health/HealthAttachmentUploader.kt`,
    `test/.../data/health/HealthAttachmentUploaderTest.kt`

> **Punto de verificación F4:** las seis secciones se registran, se ven y
> respetan el origen.

---

## Fase F5 — Cierre

- [ ] **T-013** — Matriz de pruebas del carnet
  - Depende de: T-005, T-008, T-009, T-010, T-011, T-012
  - Aceptación: una suite cubre modo avión, cero peticiones al abrir, filtros,
    alergias críticas, inmutabilidad veterinaria, peso ordenado y escritura
    offline; el doble de API cuenta las llamadas.
  - Verificación: `./gradlew test` (C-01…C-12).
  - Archivos: `test/.../health/CarnetMatrixTest.kt`

- [ ] **T-014** — Cierre de la unidad
  - Depende de: T-013
  - Aceptación: las capas están limpias; el carnet no depende de la red para
    mostrarse; la regla de alergias es de dominio; `memoria.md` queda
    actualizada y la evidencia escrita.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas` y
    revisión de evidencia.
  - Archivos: `docs/evidencia-008.md`, `memoria.md`

> **Punto de verificación F5:** carnet cerrado y listo para que 009 lo
> comparta.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-003 | Modelos, Room y reglas |
| F2 Datos | T-004 … T-005 | Red, mapeo y repositorio |
| F3 Vista | T-006 | Línea de tiempo y filtros |
| F4 Pantallas | T-007 … T-012 | Carnet, formularios y adjuntos |
| F5 Cierre | T-013 … T-014 | Matriz y evidencia |

**Tareas bloqueadas por preguntas abiertas:** T-004 (Q-01), T-008 (Q-02, Q-06,
Q-08, Q-10), T-009 (Q-02, Q-03), T-010 (Q-05), T-012 (Q-07), T-013 (Q-04,
Q-09).

Esas preguntas deben resolverse con el responsable del backend y con el equipo
antes de ejecutar las tareas correspondientes.
