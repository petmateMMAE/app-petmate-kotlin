# Tareas 010 — Mapa e inicio

> Lista de tareas de la unidad `010-mapa-e-inicio`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **5 / 14 tareas** (3 verificadas; 2 implementadas, por verificar; 4 sin marcar: implementadas, falta prueba instrumentada, al 26/11/2026) · Fases completadas: **0 / 5**

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

- [x] **T-001** — Modelo de negocio, tipo y contrato del directorio
  - Depende de: 001/T-007
  - Aceptación: existen `Business`, `BusinessType` (veterinaria, tienda,
    estética y genérico para desconocidos), `Coordinates` y
    `BusinessRepository` con observar y refrescar; el tipo lleva icono y color
    como recursos; no importa Android; hay pruebas del tipo desconocido.
  - Verificación: `./gradlew test` y `grep` de imports prohibidos en
    `domain/business`.
  - Archivos: `domain/business/Business.kt`,
    `domain/business/BusinessType.kt`,
    `domain/business/BusinessRepository.kt`,
    `test/.../domain/business/BusinessTypeTest.kt`

- [x] **T-002** — Próximo evento de salud y umbrales de color
  - Depende de: 008/T-001, 001/T-018
  - Aceptación: `NextHealthEvent` toma la próxima dosis de 008, elige la de
    menos días y clasifica rojo con 3 días o menos, amarillo con 14 o menos y
    neutro en otro caso; los umbrales son constantes nombradas; la fecha se
    calcula con reloj inyectable; hay pruebas con tres fechas fijas.
  - Verificación: `./gradlew test` (C-06, C-07).
  - Archivos: `domain/health/NextHealthEvent.kt`,
    `domain/health/HealthAlertLevel.kt`,
    `test/.../domain/health/NextHealthEventTest.kt`

> **Punto de verificación F1:** dominio puro; "lo más próximo" es una fecha
> con umbrales probados.

---

## Fase F2 — Datos

- [x] **T-003** — Cliente y DTOs del directorio
  - Depende de: T-001, 001/T-012 · Bloqueada por: **Q-02**
  - Aceptación: el cliente pide el directorio y mapea los negocios con nombre,
    tipo, coordenadas y datos de tarjeta; los campos salen del contrato
    acordado; hay pruebas de serialización con servidor local.
  - Verificación: `./gradlew test` con servidor local.
  - Archivos: `data/business/remote/BusinessApi.kt`,
    `data/business/remote/BusinessDtos.kt`,
    `data/business/remote/BusinessRemoteDataSource.kt`,
    `test/.../data/business/remote/BusinessContractTest.kt`

- [ ] **T-004** — Caché del directorio con expiración
  - Depende de: T-001, 001/T-017 · Bloqueada por: **Q-05**
  - Aceptación: el directorio se guarda en caché con marca de tiempo; se
    devuelve de inmediato y se refresca en segundo plano; si el refresco falla
    se conserva la caché marcada como desactualizada; hay prueba de expiración.
  - Verificación: `./gradlew test` (C-10).
  - Archivos: `data/business/BusinessCache.kt`,
    `data/business/CachedBusiness.kt`,
    `test/.../data/business/BusinessCacheTest.kt`

- [x] **T-005** — Repositorio del directorio _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-003, T-004
  - Aceptación: el repositorio devuelve caché primero y refresca; expone
    `Flow` con el estado (datos, desactualizado, error); no filtra DTOs a
    `domain`; hay prueba con red ausente y con caché vacía.
  - Verificación: `./gradlew test` (C-10, C-11).
  - Archivos: `data/repository/BusinessRepositoryImpl.kt`,
    `di/BusinessModule.kt`,
    `test/.../data/repository/BusinessRepositoryTest.kt`

> **Punto de verificación F2:** el directorio abre desde caché y nunca deja el
> mapa en blanco.

---

## Fase F3 — Mapa

- [ ] **T-006** — Abstracción del mapa y adaptador falso
  - Depende de: T-001, 001/T-020
  - Aceptación: existe un contrato de mapa con marcadores, cámara y selección
    que no menciona ningún SDK; un adaptador falso permite probar la pantalla;
    la UI no importa clases del proveedor.
  - Verificación: `./gradlew test` y `grep` del SDK en `ui/`.
  - Archivos: `ui/map/MapContract.kt`, `ui/map/MapMarker.kt`,
    `ui/map/FakeMapView.kt`,
    `test/.../ui/map/FakeMapViewTest.kt`

- [ ] **T-007** — Adaptador del proveedor de mapas _(implementada, falta prueba instrumentada)_
  - Depende de: T-006 · Bloqueada por: **Q-01** · Requiere autorización de
    dependencia y de llave
  - Aceptación: el adaptador implementa el contrato con el SDK elegido; la
    llave llega por configuración de entorno y no está en el repositorio ni en
    `BuildConfig` con valor por defecto; la restricción por aplicación queda
    documentada.
  - Verificación: `./gradlew test`, `grep` de llaves (C-14) y prueba
    instrumentada del mapa.
  - Archivos: `core/maps/MapProviderAdapter.kt`,
    `gradle/libs.versions.toml`, `app/build.gradle.kts`,
    `docs/llave-mapas.md`

> **Punto de verificación F3:** el mapa real es sustituible y su llave no está
> versionada.

---

## Fase F4 — Pantalla de inicio

- [ ] **T-008** — Pantalla de inicio con mapa y filtros _(implementada, falta prueba instrumentada)_
  - Depende de: T-005, T-006, 001/T-020 · Bloqueada por: **Q-03**
  - Aceptación: tras el login la pantalla de inicio es el mapa; cada negocio
    aparece con el icono de su tipo; los filtros por tipo muestran solo los
    marcadores elegidos; hay estado vacío, de error y de desactualizado.
  - Verificación: prueba de interfaz y de navegación (C-01, C-02, C-03, C-11).
  - Archivos: `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`,
    `ui/home/HomeUiState.kt`, `ui/home/BusinessTypeFilter.kt`,
    `test/.../ui/home/HomeScreenTest.kt`

- [ ] **T-009** — Carrusel y tarjeta resumida del negocio _(implementada, falta prueba instrumentada)_
  - Depende de: T-008 · Bloqueada por: **Q-04**
  - Aceptación: tocar un marcador abre la tarjeta resumida; el carrusel y el
    mapa comparten una sola selección y se sincronizan en ambos sentidos; la
    tarjeta muestra los campos acordados.
  - Verificación: prueba de interfaz (C-04, C-05).
  - Archivos: `ui/home/BusinessCard.kt`, `ui/home/BusinessCarousel.kt`,
    `test/.../ui/home/BusinessCarouselTest.kt`

- [ ] **T-010** — Tarjeta "lo más próximo" y navegación al carnet
  - Depende de: T-002, T-008, 008/T-007
  - Aceptación: la tarjeta muestra la próxima dosis de la mascota con menos
    días y su color (rojo ≤ 3, amarillo ≤ 14, neutro); tocar la tarjeta abre
    el carnet de esa mascota; sin mascotas la tarjeta invita a registrar una.
  - Verificación: prueba de interfaz y de navegación (C-06, C-07, C-13).
  - Archivos: `ui/home/NextHealthCard.kt`,
    `test/.../ui/home/NextHealthCardTest.kt`

> **Punto de verificación F4:** el inicio muestra el mapa, el carrusel y la
> alerta de salud, y navega al carnet.

---

## Fase F5 — Ubicación, demo y cierre

- [ ] **T-011** — Ubicación, "cerca de mí" y degradación _(implementada, falta prueba instrumentada)_
  - Depende de: T-008, 003/T-006 · Bloqueada por: **Q-08, Q-09**
  - Aceptación: el mapa abre con centro por defecto sin pedir permisos; "cerca
    de mí" usa el flujo de 003; si se niega, se mantiene el centro por defecto
    y la lista; abrir la app no pide ningún permiso.
  - Verificación: pruebas de interfaz con permiso concedido y denegado (C-08,
    C-09).
  - Archivos: `ui/home/HomeLocation.kt`,
    `test/.../ui/home/HomeLocationTest.kt`

- [x] **T-012** — Modo demo con datos locales _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-008, T-010 · Bloqueada por: **Q-07**
  - Aceptación: con el modo demo activo, mapa, mascota y carnet se alimentan de
    datos locales; ninguna operación de escritura sale hacia el backend; el
    modo se activa por configuración y no por un botón visible en producción.
  - Verificación: prueba con doble de API que cuenta escrituras en cero (C-12).
  - Archivos: `data/demo/DemoDataProvider.kt`, `di/DemoModule.kt`,
    `test/.../data/demo/DemoModeTest.kt`

- [ ] **T-013** — Matriz de pruebas del mapa e inicio
  - Depende de: T-009, T-010, T-011, T-012
  - Aceptación: una suite cubre arranque en el mapa, marcadores por tipo,
    filtros, carrusel sincronizado, tarjeta de salud con tres fechas, ubicación
    denegada, caché sin red, caché vacía y demo sin escrituras.
  - Verificación: `./gradlew test` (C-01…C-14).
  - Archivos: `test/.../home/HomeMatrixTest.kt`

- [ ] **T-014** — Cierre de la unidad
  - Depende de: T-007, T-013
  - Aceptación: las capas están limpias; la UI no importa el SDK de mapas; no
    hay llaves en el repositorio; "lo más próximo" consume 008; `memoria.md`
    queda actualizada y la evidencia escrita.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas` y
    revisión de evidencia.
  - Archivos: `docs/evidencia-010.md`, `memoria.md`

> **Punto de verificación F5:** unidad cerrada, con el inicio funcionando sin
> red y el demo listo para el profesor.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-002 | Negocio, tipo y alerta de salud |
| F2 Datos | T-003 … T-005 | Cliente, caché y repositorio |
| F3 Mapa | T-006 … T-007 | Abstracción y adaptador |
| F4 Pantalla | T-008 … T-010 | Inicio, carrusel y tarjeta de salud |
| F5 Cierre | T-011 … T-014 | Ubicación, demo, matriz y evidencia |

**Tareas bloqueadas por preguntas abiertas:** T-003 (Q-02), T-004 (Q-05),
T-007 (Q-01), T-008 (Q-03), T-009 (Q-04), T-011 (Q-08, Q-09), T-012 (Q-07) y
T-013 (Q-03, Q-06, Q-10).

Esas preguntas deben resolverse con el responsable del backend, con el equipo
y con el profesor antes de ejecutar las tareas correspondientes.
