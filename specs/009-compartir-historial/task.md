# Tareas 009 — Compartir historial

> Lista de tareas de la unidad `009-compartir-historial`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **3 / 11 tareas** (2 verificadas; 1 implementada, por verificar; 2 sin marcar: implementadas, falta prueba instrumentada, al 26/11/2026) · Fases completadas: **0 / 5**

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

- [x] **T-001** — Modelo de compartición y casos de uso
  - Depende de: 008/T-001, 001/T-018
  - Aceptación: existen `ShareSelection`, `ShareDuration` (30 min, 1 día,
    7 días), `ShareToken` y `ShareState`; los casos de uso emiten, regeneran y
    consultan; no importa Android; hay pruebas de la selección.
  - Verificación: `./gradlew test` y `grep` de imports prohibidos en
    `domain/sharing`.
  - Archivos: `domain/sharing/ShareSelection.kt`,
    `domain/sharing/ShareDuration.kt`,
    `domain/sharing/ShareToken.kt`,
    `domain/sharing/usecase/CreateShareUseCase.kt`,
    `test/.../domain/sharing/ShareSelectionTest.kt`

- [ ] **T-002** — Regla de secciones y alergias obligatorias
  - Depende de: T-001, 008/T-003
  - Aceptación: las secciones elegibles son vacunas, desparasitación, peso,
    condiciones e historial; las alergias se agregan siempre desde
    `GetCriticalAllergiesUseCase` y no se pueden desmarcar; hay prueba de que
    una selección sin alergias las incluye igual.
  - Verificación: `./gradlew test` (C-03).
  - Archivos: `domain/sharing/ShareSections.kt`,
    `test/.../domain/sharing/ShareSectionsTest.kt`

> **Punto de verificación F1:** selección tipada y alergias inyectadas.

---

## Fase F2 — Red y directorio

- [x] **T-003** — Cliente de emisión y resolución del QR
  - Depende de: T-001, 001/T-012 · Bloqueada por: **Q-01, Q-02, Q-03**
  - Aceptación: el cliente emite un token y resuelve su contenido; el QR solo
    lleva el token; la expiración viene del servidor; hay pruebas de
    serialización con servidor local.
  - Verificación: `./gradlew test` con servidor local (C-04, C-06).
  - Archivos: `data/sharing/remote/ShareApi.kt`,
    `data/sharing/remote/ShareDtos.kt`,
    `data/sharing/remote/ShareRemoteDataSource.kt`,
    `test/.../data/sharing/remote/ShareContractTest.kt`

- [ ] **T-004** — Directorio de negocios y opción "cualquier veterinaria" _(implementada, falta prueba instrumentada)_
  - Depende de: T-001, 001/T-012 · Bloqueada por: **Q-07, Q-08**
  - Aceptación: el selector muestra los negocios del directorio con nombre y
    tipo, más la opción "cualquier veterinaria"; no permite crear ni editar
    negocios; hay estado vacío y error.
  - Verificación: prueba de interfaz con doble de directorio (C-02).
  - Archivos: `data/sharing/BusinessDirectory.kt`,
    `ui/sharing/BusinessSelector.kt`,
    `test/.../ui/sharing/BusinessSelectorTest.kt`

> **Punto de verificación F2:** contrato aislado y directorio de solo lectura.

---

## Fase F3 — Tiempo

- [ ] **T-005** — Duración, expiración y cuenta regresiva
  - Depende de: T-003
  - Aceptación: la duración se elige entre las tres opciones; la expiración se
    toma del servidor; la cuenta regresiva usa esa marca y no el reloj del
    dispositivo; al llegar a cero el QR deja de mostrarse.
  - Verificación: `./gradlew test` con reloj fijo (C-06, C-07).
  - Archivos: `domain/sharing/ShareExpiry.kt`,
    `ui/sharing/ShareCountdown.kt`,
    `test/.../domain/sharing/ShareExpiryTest.kt`

> **Punto de verificación F3:** el tiempo lo manda el servidor.

---

## Fase F4 — QR y pantalla

- [x] **T-006** — Generación del QR y caché del vigente _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-005 · Requiere autorización si se agrega librería de QR
  - Aceptación: el QR se dibuja a partir del token; no contiene datos clínicos
    ni personales; un QR vigente se conserva en caché y se muestra sin red; al
    expirar se borra.
  - Verificación: decodificación del QR (C-04) y prueba sin red (C-09).
  - Archivos: `ui/sharing/QrRenderer.kt`, `data/sharing/ShareCache.kt`,
    `test/.../ui/sharing/QrRendererTest.kt`

- [ ] **T-007** — Regeneración e invalidación
  - Depende de: T-003, T-006 · Bloqueada por: **Q-04, Q-05, Q-06**
  - Aceptación: regenerar emite un token nuevo e invalida el anterior; el
    anterior ya no resuelve contenido; se respeta el máximo de QR vigentes.
  - Verificación: `./gradlew test` con dos emisiones (C-05).
  - Archivos: `domain/sharing/RegenerateShareUseCase.kt`,
    `test/.../domain/sharing/RegenerateShareTest.kt`

- [ ] **T-008** — Pantalla de compartir _(implementada, falta prueba instrumentada)_
  - Depende de: T-002, T-004, T-005, T-006
  - Aceptación: el flujo permite elegir mascota, negocio, secciones y duración;
    muestra el QR y la cuenta regresiva; sin red explica que no se puede emitir
    y ofrece mostrar el vigente; los textos están en recursos.
  - Verificación: prueba de interfaz del flujo completo (C-01, C-08).
  - Archivos: `ui/sharing/ShareScreen.kt`, `ui/sharing/ShareViewModel.kt`,
    `ui/sharing/ShareUiState.kt`,
    `test/.../ui/sharing/ShareScreenTest.kt`

- [ ] **T-009** — Auditoría de emisión
  - Depende de: T-003, 001/T-006
  - Aceptación: se registra mascota, negocio, secciones y expiración; no se
    registra contenido clínico; hay prueba de que el historial no aparece en el
    registro.
  - Verificación: `./gradlew test` (C-11).
  - Archivos: `data/sharing/ShareAuditLogger.kt`,
    `test/.../data/sharing/ShareAuditLoggerTest.kt`

> **Punto de verificación F4:** QR seguro, regenerable y con expiración real.

---

## Fase F5 — Cierre

- [ ] **T-010** — Matriz de pruebas de compartir
  - Depende de: T-007, T-008, T-009
  - Aceptación: una suite cubre selección, alergias obligatorias, expiración,
    regeneración, caché sin red, secciones autorizadas y auditoría; el
    decodificador de QR confirma que no hay datos sensibles.
  - Verificación: `./gradlew test` (C-01…C-11).
  - Archivos: `test/.../sharing/ShareMatrixTest.kt`

- [ ] **T-011** — Cierre de la unidad
  - Depende de: T-010
  - Aceptación: las capas están limpias; las alergias vienen de 008; no se
    agregó ninguna librería sin autorización; `memoria.md` queda actualizada y
    la evidencia escrita.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas` y
    revisión de evidencia.
  - Archivos: `docs/evidencia-009.md`, `memoria.md`

> **Punto de verificación F5:** unidad cerrada y lista para que 013 escanee.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-002 | Selección y regla de alergias |
| F2 Red | T-003 … T-004 | Contrato y directorio |
| F3 Tiempo | T-005 | Duración y expiración |
| F4 QR | T-006 … T-009 | QR, regeneración, pantalla y auditoría |
| F5 Cierre | T-010 … T-011 | Matriz y evidencia |

**Tareas bloqueadas por preguntas abiertas:** T-003 (Q-01, Q-02, Q-03),
T-004 (Q-07, Q-08), T-007 (Q-04, Q-05, Q-06), T-008 (Q-09, Q-10).

Esas preguntas deben resolverse con el responsable del backend y con el equipo
antes de ejecutar las tareas correspondientes.
