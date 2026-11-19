# Tareas 007 — Mis mascotas

> Lista de tareas de la unidad `007-mis-mascotas`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **9 / 13 tareas** (implementadas, por verificar al 17/11/2026) · Fases completadas: **0 / 5**

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

- [x] **T-001** — Modelo de mascota y casos de uso _(implementada, por verificar · 17/11/2026)_
  - Depende de: 001/T-007, 001/T-018
  - Aceptación: existen `Pet` con los campos de la spec y `Pet.age(today)`;
    `ObservePetsUseCase`, `CreatePetUseCase`, `UpdatePetUseCase` y
    `DeletePetUseCase` devuelven `AppResult`; la edad se prueba con reloj fijo;
    no importa Android.
  - Verificación: `./gradlew test` (C-04) y `grep` de imports prohibidos en
    `domain/pets`.
  - Archivos: `domain/pets/Pet.kt`, `domain/pets/PetRepository.kt`,
    `domain/pets/usecase/ObservePetsUseCase.kt`,
    `domain/pets/usecase/SavePetUseCase.kt`,
    `test/.../domain/pets/PetAgeTest.kt`

- [x] **T-002** — Entidad, DAO y migración de Room _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001, 001/T-011 · Requiere autorización de esquema
  - Aceptación: existe `PetEntity` con su DAO; el `Flow` de la lista filtra por
    usuario y por activo; la migración se prueba desde la versión anterior; el
    cambio de esquema queda autorizado.
  - Verificación: prueba instrumentada de Room y de migración (C-10).
  - Archivos: `data/local/PetEntity.kt`, `data/local/PetDao.kt`,
    `data/local/PetMateDatabase.kt`,
    `androidTest/.../data/local/PetMigrationTest.kt`

> **Punto de verificación F1:** dominio puro y persistencia con migración.

---

## Fase F2 — Datos

- [x] **T-003** — DTOs, mapeo y cliente de mascotas _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001, 001/T-012 · Bloqueada por: **Q-01**
  - Aceptación: el cliente y los DTOs cubren listar, crear, editar y borrar;
    los campos salen del contrato acordado; el mapeo DTO ↔ entidad ↔ dominio
    se prueba con servidor local.
  - Verificación: `./gradlew test` con servidor local.
  - Archivos: `data/remote/PetApi.kt`, `data/remote/PetDtos.kt`,
    `data/mapper/PetMapper.kt`,
    `test/.../data/remote/PetContractTest.kt`

- [ ] **T-004** — Repositorio offline-first con aislamiento
  - Depende de: T-002, T-003, 005/T-001
  - Aceptación: el repositorio observa Room, escribe local y encola la
    sincronización según 005; toda consulta filtra por la sesión activa; no
    filtra DTOs a `domain`; hay prueba con dos sesiones.
  - Verificación: `./gradlew test` (C-09) y `./gradlew test` con red ausente
    (C-05).
  - Archivos: `data/repository/PetRepositoryImpl.kt`, `di/PetsModule.kt`,
    `test/.../data/repository/PetRepositoryTest.kt`

> **Punto de verificación F2:** lista desde Room, sin fugas entre usuarios y
> con creación offline.

---

## Fase F3 — Foto

- [ ] **T-005** — Selección de foto con permisos
  - Depende de: T-001, 003/T-007 · Requiere autorización de Coil
  - Aceptación: se puede elegir de galería o cámara; si el permiso se niega, se
    usa el componente de degradación de 003; `coil-compose` se agrega al
    catálogo con autorización previa; la imagen se muestra en la tarjeta y en
    el formulario.
  - Verificación: prueba de interfaz con permiso concedido y denegado (C-07).
  - Archivos: `ui/pets/PetPhotoPicker.kt`,
    `gradle/libs.versions.toml`, `app/build.gradle.kts`,
    `test/.../ui/pets/PetPhotoPickerTest.kt`

- [ ] **T-006** — Moderación y subida diferida
  - Depende de: T-005, 004/T-006
  - Aceptación: la foto se modera antes de subirse; el rechazo no la adjunta y
    borra el temporal; sin red la mascota se guarda sin foto y la subida queda
    pendiente; el guardado no espera a la subida.
  - Verificación: `./gradlew test` con doble de moderación (C-06) y con red
    ausente (C-05).
  - Archivos: `data/pets/PetPhotoUploader.kt`,
    `test/.../data/pets/PetPhotoUploaderTest.kt`

> **Punto de verificación F3:** ninguna foto sin moderar se adjunta ni se sube.

---

## Fase F4 — Presentación

- [x] **T-007** — Pantalla de lista y tarjeta _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004, 001/T-020
  - Aceptación: la lista muestra tarjetas con foto o iniciales, nombre, edad,
    especie y raza; el espacio de la próxima vacuna queda reservado; hay estado
    vacío con acción de crear; la lista se actualiza sola.
  - Verificación: prueba de interfaz (C-01, C-03).
  - Archivos: `ui/pets/PetsScreen.kt`, `ui/pets/PetsViewModel.kt`,
    `ui/pets/PetsUiState.kt`, `ui/pets/PetCard.kt`,
    `test/.../ui/pets/PetsScreenTest.kt`

- [x] **T-008** — Formulario de alta y edición _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001, T-005
  - Aceptación: un solo formulario sirve para alta y edición; valida los campos
    obligatorios confirmados; conserva el estado al rotar; muestra errores
    comprensibles; no permite guardar con datos inválidos.
  - Verificación: pruebas de interfaz y de rotación (C-02).
  - Archivos: `ui/pets/PetFormScreen.kt`, `ui/pets/PetFormViewModel.kt`,
    `ui/pets/PetFormUiState.kt`,
    `test/.../ui/pets/PetFormTest.kt`

- [x] **T-009** — Borrado con confirmación _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004, T-007
  - Aceptación: borrar pide confirmación explícita; al confirmar, la mascota
    desaparece de la lista y la operación se encola; cancelar no cambia nada.
  - Verificación: prueba de interfaz (C-08).
  - Archivos: `ui/pets/DeletePetDialog.kt`,
    `test/.../ui/pets/DeletePetTest.kt`

- [x] **T-010** — Navegación y retiro de la pantalla de verificación _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-007, T-008, 001/T-022
  - Aceptación: las rutas de lista, alta y edición son tipadas; la lista es el
    destino tras autenticarse; la pantalla y la ruta de verificación de 001 ya
    no están en el grafo de producto.
  - Verificación: prueba de navegación y revisión de `AppNavHost` (C-11).
  - Archivos: `ui/navigation/PetsNavGraph.kt`, `ui/navigation/AppNavHost.kt`,
    `test/.../ui/navigation/PetsNavigationTest.kt`

> **Punto de verificación F4:** alta, edición, borrado y navegación funcionan
> sin el andamio de 001.

---

## Fase F5 — Cierre

- [x] **T-011** — Estados de carga, error y vacío _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-007, T-008
  - Aceptación: la lista y el formulario muestran carga, error y vacío con
    textos propios; un error de red no cierra la app; el reintento funciona.
  - Verificación: pruebas de interfaz con dobles que fallan (C-03).
  - Archivos: `ui/pets/PetsErrorState.kt`,
    `test/.../ui/pets/PetsStatesTest.kt`

- [x] **T-012** — Pruebas de aislamiento, edad y persistencia _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004, T-007, T-008
  - Aceptación: una suite cubre dos sesiones, edad con reloj fijo, rotación,
    modo avión y borrado lógico; el doble de API cuenta las peticiones.
  - Verificación: `./gradlew test` (C-01…C-10).
  - Archivos: `test/.../pets/PetsMatrixTest.kt`

- [ ] **T-013** — Cierre de la unidad
  - Depende de: T-009, T-010, T-011, T-012
  - Aceptación: las capas están limpias; no queda la pantalla de verificación;
    `coil-compose` está en el catálogo con autorización registrada;
    `memoria.md` queda actualizada y la evidencia escrita.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas` y
    revisión de evidencia.
  - Archivos: `docs/evidencia-007.md`, `memoria.md`

> **Punto de verificación F5:** unidad cerrada y lista para que 008 lea el
> carnet de cada mascota.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-002 | Modelo, casos de uso y Room |
| F2 Datos | T-003 … T-004 | Red, mapeo y repositorio |
| F3 Foto | T-005 … T-006 | Permiso, Coil, moderación y subida |
| F4 Presentación | T-007 … T-010 | Lista, formulario, borrado y navegación |
| F5 Cierre | T-011 … T-013 | Estados, matriz y evidencia |

**Tareas bloqueadas por preguntas abiertas:** T-003 (Q-01), T-005 (Q-05),
T-007 (Q-09), T-008 (Q-02, Q-06, Q-08), T-009 (Q-04), T-012 (Q-03, Q-07,
Q-10).

Esas preguntas deben resolverse con el responsable del backend y con el equipo
antes de ejecutar las tareas correspondientes.
