# Tareas 006 — Notificaciones push

> Lista de tareas de la unidad `006-notificaciones-push`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **2 / 11 tareas** (2 verificadas; 2 sin marcar: implementadas, falta prueba instrumentada, al 26/11/2026)

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

## Fase F1 — Modelo

- [x] **T-001** — Categorías y modelo de preferencias
  - Depende de: 001/T-039, 001/T-007
  - Aceptación: `NotificationCategory` es un conjunto cerrado con salud,
    seguimiento, chat, novedades y seguridad; `NotificationPreferences` define
    valores por defecto y la regla de que seguridad no se apaga; no importa
    Android; hay pruebas del modelo.
  - Verificación: `./gradlew test` y `grep` de imports prohibidos en
    `domain/push`.
  - Archivos: `domain/push/NotificationCategory.kt`,
    `domain/push/NotificationPreferences.kt`,
    `test/.../domain/push/NotificationPreferencesTest.kt`

- [ ] **T-002** — Contrato y parser del payload
  - Depende de: T-001 · Bloqueada por: **Q-01**
  - Aceptación: existe un contrato de mensaje con categoría, destino y clave de
    evento; el parser descarta un payload inválido o con campos prohibidos y no
    cierra la app; hay pruebas con mensajes válidos, incompletos y malformados.
  - Verificación: `./gradlew test` (C-04, C-11).
  - Archivos: `domain/push/PushMessage.kt`,
    `data/push/PushPayloadParser.kt`,
    `test/.../data/push/PushPayloadParserTest.kt`

> **Punto de verificación F1:** categorías cerradas y payload validado.

---

## Fase F2 — Recepción

- [ ] **T-003** — Recepción en primer y segundo plano
  - Depende de: T-002, 001/T-039
  - Aceptación: con la app abierta se muestra un aviso interno; en segundo
    plano se muestra la notificación del sistema; la recepción no bloquea el
    hilo principal ni depende de que la app esté abierta.
  - Verificación: prueba instrumentada con mensajes simulados (C-01).
  - Archivos: `data/push/PushReceiver.kt`, `data/push/ForegroundNotice.kt`,
    `androidTest/.../data/push/PushReceiverTest.kt`

- [x] **T-004** — Presentación con texto genérico
  - Depende de: T-003
  - Aceptación: cada categoría tiene un texto fijo en `strings.xml`; el
    payload no se muestra crudo; el detalle se carga al abrir; hay prueba de
    que ningún campo clínico llega a la notificación.
  - Verificación: prueba de interfaz y de esquema (C-04).
  - Archivos: `data/push/NotificationPresenter.kt`,
    `res/values/strings.xml`,
    `test/.../data/push/NotificationPresenterTest.kt`

- [ ] **T-005** — Deduplicación por clave de evento
  - Depende de: T-003 · Bloqueada por: **Q-08**
  - Aceptación: un mensaje con una clave ya mostrada se ignora; dos eventos
    distintos con el mismo texto se muestran ambos; hay prueba con entrega
    duplicada.
  - Verificación: `./gradlew test` (C-11).
  - Archivos: `data/push/PushDeduplicator.kt`,
    `test/.../data/push/PushDeduplicatorTest.kt`

> **Punto de verificación F2:** recepción correcta y sin duplicados.

---

## Fase F3 — Destinos

- [ ] **T-006** — Deep links a las pantallas
  - Depende de: T-002, 001/T-022 · Bloqueada por: **Q-07**
  - Aceptación: cada categoría abre su destino (mascota/carnet, chat,
    seguimiento, mapa); un destino desconocido abre el inicio sin cerrar la
    app; las rutas son tipadas.
  - Verificación: prueba instrumentada de deep links (C-05, C-06).
  - Archivos: `ui/navigation/PushDeepLinks.kt`,
    `androidTest/.../ui/navigation/PushDeepLinksTest.kt`

> **Punto de verificación F3:** ningún enlace deja la app rota.

---

## Fase F4 — Preferencias

- [ ] **T-007** — Persistencia local de preferencias _(implementada, falta prueba instrumentada)_
  - Depende de: T-001
  - Aceptación: el interruptor cambia de inmediato y sobrevive al cierre de la
    app; la preferencia de seguridad no se puede apagar; hay prueba de
    persistencia.
  - Verificación: `./gradlew test` e instrumentada (C-02, C-03, C-09).
  - Archivos: `data/push/NotificationPreferencesStore.kt`,
    `test/.../data/push/NotificationPreferencesStoreTest.kt`

- [ ] **T-008** — Sincronización de preferencias con el servidor
  - Depende de: T-007 · Bloqueada por: **Q-02**
  - Aceptación: al cambiar una preferencia se envía al servidor; si falla,
    queda pendiente y se reintenta; el servidor recibe el mismo valor que ve el
    usuario.
  - Verificación: `./gradlew test` con doble de API (C-03).
  - Archivos: `data/push/PreferencesSync.kt`,
    `test/.../data/push/PreferencesSyncTest.kt`

- [ ] **T-009** — Permiso al activar una categoría _(implementada, falta prueba instrumentada)_
  - Depende de: T-007, 003/T-004
  - Aceptación: al encender la primera categoría se usa el flujo de 003; si se
    niega, la categoría queda activa y la app avisa dentro de la interfaz; el
    permiso no se pide al arrancar.
  - Verificación: prueba de interfaz (C-07, C-08).
  - Archivos: `data/push/NotificationActivation.kt`,
    `test/.../data/push/NotificationActivationTest.kt`

> **Punto de verificación F4:** preferencias consistentes y permiso en el
> momento correcto.

---

## Fase F5 — Cierre

- [ ] **T-010** — Matriz de pruebas de notificaciones
  - Depende de: T-004, T-005, T-006, T-008, T-009
  - Aceptación: una sola suite cubre cada categoría, preferencia apagada,
    payload inválido, duplicado, deep link y permiso denegado; el doble de FCM
    entrega los mensajes de ejemplo.
  - Verificación: `./gradlew test` (C-01…C-11).
  - Archivos: `test/.../push/PushMatrixTest.kt`

- [ ] **T-011** — Cierre de la unidad
  - Depende de: T-010
  - Aceptación: las capas están limpias; ningún payload de ejemplo contiene
    datos clínicos; el token se registra y se borra con la sesión; `memoria.md`
    queda actualizada y la evidencia escrita.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas` y
    revisión de evidencia.
  - Archivos: `docs/evidencia-006.md`, `memoria.md`

> **Punto de verificación F5:** unidad cerrada con la matriz en verde.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Modelo | T-001 … T-002 | Categorías y payload |
| F2 Recepción | T-003 … T-005 | Primer plano, segundo plano y dedupe |
| F3 Destinos | T-006 | Deep links |
| F4 Preferencias | T-007 … T-009 | Persistencia, sincronización y permiso |
| F5 Cierre | T-010 … T-011 | Matriz y evidencia |

**Tareas bloqueadas por preguntas abiertas:** T-002 (Q-01), T-005 (Q-08),
T-006 (Q-07), T-008 (Q-02), T-003 y T-004 (Q-06), T-007 y T-009 (Q-03), T-010
(Q-04, Q-05, Q-09, Q-10).

Esas preguntas deben resolverse con el responsable del backend y con el equipo
antes de ejecutar las tareas correspondientes.
