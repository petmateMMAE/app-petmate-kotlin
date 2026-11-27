# Tareas 003 — Permisos

> Lista de tareas de la unidad `003-permisos`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **3 / 10 tareas** (3 verificadas; 6 sin marcar: implementadas, falta prueba instrumentada, al 27/11/2026) · Fases completadas: **0 / 4**

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

Los componentes de 001 se citan como `001/K-xx` y las tareas como `001/T-xx`.

---

## Fase F1 — Contrato

- [x] **T-001** — Catálogo de permisos y contrato del gateway
  - Depende de: 001/T-007, 001/T-009
  - Aceptación: existen `PermissionType` (ubicación, cámara, fotos,
    notificaciones), `PermissionStatus` (concedido, negado, permanente, no
    solicitado) y `PermissionGateway` con consultar, solicitar y observar;
    el catálogo declara el momento y la justificación de cada permiso; no
    importa Android ni red; hay pruebas del catálogo.
  - Verificación: `./gradlew test` y `grep` de imports prohibidos en
    `domain/permissions`.
  - Archivos: `domain/permissions/PermissionType.kt`,
    `domain/permissions/PermissionStatus.kt`,
    `domain/permissions/PermissionGateway.kt`,
    `domain/permissions/PermissionCatalog.kt`,
    `test/.../domain/permissions/PermissionCatalogTest.kt`

- [x] **T-002** — Implementación Android del gateway _(verificada 27/11/2026: manifiesto corregido con autorización, AndroidPermissionGatewayTest en verde y `lintDebug` con 0 errores)_
  - Depende de: T-001 · Requiere autorización para tocar el manifiesto
  - Aceptación: `AndroidPermissionGateway` implementa el contrato con las APIs
    del sistema; el manifiesto declara solo los permisos del catálogo; la
    consulta y la solicitud no bloquean el hilo principal; se prueba con
    instrumentación en un dispositivo.
  - Verificación: prueba instrumentada en verde y `./gradlew lint`.
  - Archivos: `core/permissions/AndroidPermissionGateway.kt`,
    `di/PermissionsModule.kt`, `AndroidManifest.xml`,
    `androidTest/.../core/permissions/AndroidPermissionGatewayTest.kt`
  - ✅ **Defecto encontrado y corregido (27/11/2026), con autorización de
    Eduardo para tocar el manifiesto.** Lo que estaba mal y ya no:
    1. **`POST_NOTIFICATIONS` no estaba declarado, y era un defecto de
       producto.** `AndroidPermissionGateway.systemPermissions()` lo devuelve
       en API 33+ y `NotificationActivation` consulta su estado, pero ningún
       manifiesto lo declaraba (ni el de la app ni el fusionado). En Android
       13+ pedir un permiso no declarado no muestra diálogo: se niega en
       silencio, así que **encender una categoría de notificación en Ajustes no
       podía funcionar** en un teléfono moderno. Afectaba a 006 y a 017/T-005.
       Nada fallaba al compilar y las pruebas pasaban, porque ninguna ejerce el
       diálogo real del sistema. **Ya está declarado.**
    2. **`CAMERA` solo llegaba por una librería.** Estaba en el manifiesto
       fusionado, no en el de la app, así que el escáner (013) dependía de que
       esa dependencia transitiva siguiera declarándolo. **Ya se declara
       explícitamente**, con `uses-feature android:required="false"`: declarar
       el permiso hace que Google Play exija cámara por omisión, y el escáner
       tiene la alternativa del código manual (spec 003, Q-02), así que excluir
       teléfonos sin cámara habría sido un daño colateral de cara a 020.
    3. `INTERNET` y `ACCESS_NETWORK_STATE` siguen en el manifiesto y no en el
       catálogo. Eso es correcto —son permisos normales, no de ejecución—, pero
       **el criterio de aceptación está mal redactado**: debería hablar de los
       permisos de ejecución, no de «solo los permisos del catálogo». Se deja
       anotado en lugar de reescribir la spec.
    - **Evidencia:** el manifiesto fusionado declara los cuatro permisos de
      ejecución del catálogo; `dumpsys package mx.petmate.app` ya lista
      `POST_NOTIFICATIONS` entre los solicitados (antes no aparecía); las 71
      pruebas instrumentadas y `lintDebug` siguen en verde después del cambio.

- [ ] **T-003** — Justificación previa reutilizable _(implementada, falta prueba instrumentada)_
  - Depende de: T-001, 001/T-020
  - Aceptación: existe un componente que muestra el motivo antes del diálogo
    del sistema; los textos salen de `strings.xml`; si el permiso ya está
    concedido no se muestra; hay prueba de interfaz de la secuencia.
  - Verificación: prueba de interfaz en verde y `grep` de literales en
    `ui/components/permissions`.
  - Archivos: `ui/components/permissions/PermissionRationale.kt`,
    `res/values/strings.xml`,
    `test/.../ui/components/permissions/PermissionRationaleTest.kt`

> **Punto de verificación F1:** contrato puro, gateway instrumentado y
> justificación probada antes del diálogo del sistema.

---

## Fase F2 — Flujo y ajustes

- [ ] **T-004** — Flujo de negativa y negativa permanente _(implementada, falta prueba instrumentada)_
  - Depende de: T-002, T-003
  - Aceptación: el flujo cuenta negativas por permiso, distingue la negativa
    permanente y a la segunda negativa ofrece ajustes en vez de un tercer
    diálogo; el estado sobrevive a la rotación; hay pruebas de los tres
    caminos.
  - Verificación: `./gradlew test` y prueba instrumentada de rotación (C-07,
    C-09).
  - Archivos: `core/permissions/PermissionFlow.kt`,
    `core/permissions/PermissionFlowState.kt`,
    `test/.../core/permissions/PermissionFlowTest.kt`,
    `androidTest/.../core/permissions/PermissionFlowRotationTest.kt`

- [ ] **T-005** — Lanzador de ajustes y retorno _(implementada, falta prueba instrumentada)_
  - Depende de: T-004
  - Aceptación: se abre la pantalla de ajustes de la app y, al volver, se
    vuelve a consultar el permiso y la función se activa sin reiniciar; si el
    usuario no concede, el estado negado se mantiene.
  - Verificación: prueba instrumentada del retorno desde ajustes (C-08).
  - Archivos: `core/permissions/AppSettingsLauncher.kt`,
    `androidTest/.../core/permissions/AppSettingsLauncherTest.kt`

> **Punto de verificación F2:** el flujo no insiste, ofrece ajustes y se
> recupera al volver.

---

## Fase F3 — Degradación

- [ ] **T-006** — Degradación de ubicación _(implementada, falta prueba instrumentada)_
  - Depende de: T-004, 001/T-020
  - Aceptación: existe un componente que, sin ubicación, ofrece centro por
    defecto, lista y búsqueda manual, con texto explicativo; no bloquea la
    pantalla ni vuelve a pedir el permiso en cada intento.
  - Verificación: prueba de interfaz con el gateway denegado (C-03).
  - Archivos: `ui/components/permissions/LocationFallback.kt`,
    `res/values/strings.xml`,
    `test/.../ui/components/permissions/LocationFallbackTest.kt`

- [ ] **T-007** — Degradación de cámara y fotos _(implementada, falta prueba instrumentada)_
  - Depende de: T-004, 001/T-020
  - Aceptación: sin cámara se ofrece la entrada manual del código; sin fotos
    se muestran las iniciales y se permite reintentar; ambos estados explican
    qué se pierde y cómo recuperarlo.
  - Verificación: pruebas de interfaz con cámara y fotos denegadas (C-04,
    C-05).
  - Archivos: `ui/components/permissions/CameraFallback.kt`,
    `ui/components/permissions/PhotoFallback.kt`,
    `res/values/strings.xml`,
    `test/.../ui/components/permissions/CameraPhotoFallbackTest.kt`

- [ ] **T-008** — Degradación de notificaciones y aviso in-app _(implementada, falta prueba instrumentada)_
  - Depende de: T-004, 001/T-020
  - Aceptación: sin permiso de notificaciones la app sigue usable y muestra un
    aviso dentro de la interfaz; el permiso se pide solo cuando la pantalla
    activa notificaciones, nunca al arrancar.
  - Verificación: prueba de interfaz con notificaciones denegadas (C-06).
  - Archivos: `ui/components/permissions/NotificationFallback.kt`,
    `ui/components/permissions/InAppNotice.kt`,
    `test/.../ui/components/permissions/NotificationFallbackTest.kt`

- [x] **T-009** — Pantalla de prueba de la matriz de permisos _(verificada 27/11/2026 en emulador: PermissionMatrixTest, suite instrumentada completa en verde)_
  - Depende de: T-006, T-007, T-008
  - Aceptación: una pantalla de prueba permite alternar los cuatro permisos
    entre concedido y denegado y muestra cada degradación; **no** se registra
    en el grafo de navegación de producto.
  - Verificación: prueba instrumentada de la matriz completa (C-03…C-06) y
    revisión del grafo.
  - Archivos: `ui/permissionsdebug/PermissionHarnessScreen.kt`,
    `androidTest/.../ui/permissionsdebug/PermissionMatrixTest.kt`

> **Punto de verificación F3:** las cuatro negativas dejan la app usable y
> explicada.

---

## Fase F4 — Cierre

- [ ] **T-010** — Cierre de la unidad
  - Depende de: T-005, T-009
  - Aceptación: no se pide ningún permiso al arrancar (C-01); el manifiesto no
    declara permisos sin uso (R-14); no hay literales de permisos en `ui/`
    (C-10); la pantalla de prueba no está en el grafo de producto; las capas
    están limpias; `memoria.md` queda actualizada.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas`,
    `grep` del manifiesto y de literales.
  - Archivos: `docs/evidencia-003.md`, `memoria.md`,
    `ui/navigation/AppNavHost.kt`, `AndroidManifest.xml`

> **Punto de verificación F4:** unidad cerrada con la matriz de permisos en
> verde y sin permisos en frío.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Contrato | T-001 … T-003 | Catálogo, gateway y justificación |
| F2 Flujo | T-004 … T-005 | Negativa, permanente y ajustes |
| F3 Degradación | T-006 … T-009 | Ubicación, cámara, fotos, notificaciones y matriz |
| F4 Cierre | T-010 | Evidencia y limpieza |

**Tareas bloqueadas por preguntas abiertas:** T-002 (Q-05), T-006 (Q-01),
T-007 (Q-02, Q-05), T-008 (Q-03), T-003 y T-010 (Q-04) y T-010 (Q-06).

Esas preguntas deben resolverse con el equipo y, cuando corresponda, con el
profesor, antes de ejecutar las tareas correspondientes. Las demás pueden
empezar en cuanto 001 esté implementada.
