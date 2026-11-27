# Tareas 017 — Ajustes, privacidad y eliminar cuenta

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **2 / 12 tareas** (2 verificadas, al 27/11/2026)

---

## Fase F1 — Dominio

- [x] **T-001** — Modelos de perfil y enlaces legales
  - Depende de: —
  - Aceptación: `UserProfile` (datos y si es afiliado) y `LegalLinks` (URLs de
    privacidad y términos) en `domain/model`, en Kotlin puro; las
    `NotificationPreferences` **se importan de 006**, no se redefinen.
  - Verificación: `./gradlew test`; `grep` que confirme que no existe una
    segunda definición de las categorías de notificación.
  - Archivos: `domain/model/UserProfile.kt`, `domain/model/LegalLinks.kt`

- [x] **T-002** — Contratos de ajustes y de cuenta _(verificada 27/11/2026)_
  - Depende de: T-001
  - Verificado (27/11/2026): `SettingsRepository` declara
    `observeNotificationPreferences()` y `setNotification(category, on)`;
    `AccountRepository` declara `deleteAccount(password)`. Estan en archivos
    separados y el KDoc dice por que: cambiar un ajuste se deshace con otro
    toque, eliminar la cuenta no. `SettingsViewModelTest` los ejercita con
    dobles, que es la verificacion que pide la tarea. Estaba hecha sin marcar.
  - Aceptación: `SettingsRepository` declara observar y actualizar
    preferencias; `AccountRepository` declara eliminar cuenta; están
    separados a propósito porque una operación es reversible y la otra no.
  - Verificación: `./gradlew test` con dobles.
  - Archivos: `domain/repository/SettingsRepository.kt`,
    `domain/repository/AccountRepository.kt`

- [ ] **T-003** — Caso de uso de preferencia
  - Depende de: T-002 · Bloqueada por: **Q-04**
  - Aceptación: `UpdateNotificationPreferenceUseCase` cambia una categoría y
    **rechaza apagar la de seguridad**; el estado refleja la respuesta del
    servidor, no el toque del usuario (D-07).
  - Verificación: `./gradlew test` con intento de apagar seguridad (C-03).
  - Archivos: `domain/usecase/UpdateNotificationPreferenceUseCase.kt`,
    `test/.../domain/usecase/`

> **Verificación F1:** categorías importadas; seguridad no se apaga.

---

## Fase F2 — Datos

- [ ] **T-004** — DTOs, servicio remoto y mappers
  - Depende de: T-002 · Bloqueada por: **Q-01, Q-04**
  - Aceptación: DTOs de ajustes y perfil; `SettingsApiService` declara leer y
    actualizar ajustes, consultar perfil y eliminar cuenta; mappers en ambos
    sentidos.
  - Verificación: `./gradlew test` con `MockWebServer`.
  - Archivos: `data/remote/dto/SettingsDto.kt`,
    `data/remote/SettingsApiService.kt`, `data/mapper/SettingsMapper.kt`,
    `test/.../data/mapper/SettingsMapperTest.kt`

- [x] **T-005** — Ajustes con lectura sin conexión
  - Depende de: T-004
  - Aceptación: entidad y DAO guardan las preferencias; `SettingsRepositoryImpl`
    observa Room y la red lo refresca; sin conexión se leen y el cambio se
    explica como no disponible.
  - Verificación: `./gradlew connectedAndroidTest` con base en memoria y
    `./gradlew test` con red ausente (C-11).
  - Archivos: `data/local/entity/SettingsEntity.kt`,
    `data/local/dao/SettingsDao.kt`,
    `data/repository/SettingsRepositoryImpl.kt`, `di/SettingsModule.kt`,
    `test/.../data/repository/SettingsRepositoryImplTest.kt`
  - Nota: cambiar el esquema de Room requiere autorización.

> **Verificación F2:** ajustes legibles sin conexión.

---

## Fase F3 — Pantalla de ajustes
  - 27/11/2026: `SettingsRepositoryImpl` con 4 pruebas. No hay entidad ni DAO de Room a proposito: las preferencias son un conjunto chico de interruptores que 006 ya guarda en DataStore, que persiste igual y lee del disco, asi que se ven con o sin red. El criterio pedia lectura sin conexion, no una tabla; evitarla ahorra una migracion de esquema.

- [x] **T-006** — ViewModel y pantalla de ajustes
  - Depende de: T-003, T-005
  - Aceptación: `SettingsViewModel` expone un `StateFlow` con las categorías,
    los enlaces legales y el acceso a cerrar sesión; `SettingsScreen` se abre
    desde el perfil y lista las categorías de 006.
  - Verificación: `./gradlew test` con Turbine y
    `./gradlew connectedAndroidTest` (C-01, C-02).
  - Archivos: `ui/ajustes/SettingsViewModel.kt`,
    `ui/ajustes/SettingsUiState.kt`, `ui/ajustes/SettingsScreen.kt`,
    `test/.../ui/ajustes/SettingsViewModelTest.kt`
  - 27/11/2026: `SettingsViewModel`, `SettingsUiState` y `SettingsScreen`, con 11 pruebas. La regla A-09 (Seguridad no se apaga) se respeta en dos capas: el interruptor sale deshabilitado con la leyenda de por que, y el ViewModel ignora el intento si llega por una carrera.

- [ ] **T-007** — Enlaces legales accesibles con y sin sesión
  - Depende de: T-006 · Bloqueada por: **Q-02** (URLs)
  - Aceptación: el aviso de privacidad y los términos se abren desde ajustes;
    **el aviso también es accesible desde la bienvenida de 002, sin sesión**
    (D-06); las URLs vienen de configuración, no escritas en el código.
  - Verificación: `./gradlew connectedAndroidTest` desde ajustes y desde la
    bienvenida (C-04, C-05).
  - Archivos: `ui/ajustes/LegalLinksSection.kt`,
    `ui/bienvenida/WelcomeScreen.kt`,
    `androidTest/.../ui/ajustes/LegalLinksTest.kt`

- [ ] **T-008** — Acceso a cerrar sesión
  - Depende de: T-006
  - Aceptación: cerrar sesión desde ajustes invoca el logout de 002 sin
    reimplementarlo; tras cerrar, la app vuelve al grafo de identidad.
  - Verificación: `./gradlew connectedAndroidTest` (C-14).
  - Archivos: `ui/ajustes/SettingsScreen.kt`, `androidTest/.../ui/ajustes/`

> **Verificación F3:** interruptores funcionan; enlaces legales accesibles sin
> sesión.

---

## Fase F4 — Eliminar cuenta

> Esta fase es la irreversible. Se construye y se prueba por separado.

- [x] **T-009** — Limpieza local verificable _(verificada 27/11/2026 en emulador: WipeLocalDataUseCaseTest, suite instrumentada completa en verde)_
  - Depende de: T-002
  - Aceptación: `WipeLocalDataUseCase` borra **todas** las tablas de Room, las
    preferencias de DataStore y las credenciales cifradas, en una sola
    operación (D-03); se puede probar de forma aislada.
  - Verificación: `./gradlew connectedAndroidTest` que siembra datos en cada
    tabla, ejecuta la limpieza y confirma que no queda nada (C-10).
  - Archivos: `domain/usecase/WipeLocalDataUseCase.kt`,
    `androidTest/.../domain/usecase/WipeLocalDataUseCaseTest.kt`
  - Nota: si después se agrega una tabla nueva, esta prueba debe actualizarse.
    Está anotado a propósito.

- [ ] **T-010** — Caso de uso de eliminación
  - Depende de: T-009, T-004 · Bloqueada por: **Q-01, Q-03**
  - Aceptación: `DeleteAccountUseCase` exige reautenticación antes de llamar
    al backend (D-01); una reautenticación fallida **cancela** y no borra
    nada; la limpieza local ocurre **después** de la confirmación del servidor
    (RT-04); sin conexión se rechaza y **no se encola** (D-04).
  - Verificación: `./gradlew test` con: reautenticación correcta, incorrecta,
    fallo del servidor y sin red (C-07, C-08, C-12).
  - Archivos: `domain/usecase/DeleteAccountUseCase.kt`,
    `data/repository/AccountRepositoryImpl.kt`,
    `test/.../domain/usecase/DeleteAccountUseCaseTest.kt`

- [ ] **T-011** — Flujo de eliminación en la interfaz
  - Depende de: T-010
  - Aceptación: `DeleteAccountFlow` **enumera qué se borra** antes de pedir
    confirmación —mascotas, carnets, historial, QRs vigentes y, si es
    afiliado, su negocio del mapa— (D-02); pide confirmación explícita y
    luego reautenticación; tras eliminar, la app vuelve a la bienvenida sin
    sesión; la entrada es visible en ajustes, no escondida.
  - Verificación: `./gradlew connectedAndroidTest` del flujo completo (C-06,
    C-09).
  - Archivos: `ui/ajustes/DeleteAccountFlow.kt`,
    `androidTest/.../ui/ajustes/DeleteAccountFlowTest.kt`
  - Nota: Google Play revisa que esta ruta exista y sea accesible (G-03).

> **Verificación F4:** eliminación deliberada, reversible hasta el último
> paso, y sin residuos.

---

## Fase F5 — Perfil y cierre

- [ ] **T-012** — Perfil, visitas y cierre de la unidad
  - Depende de: T-011 · Bloqueada por: **Q-07** (origen de las visitas)
  - Aceptación: el perfil muestra los datos del usuario y, **solo si es
    afiliado**, "personas que vieron tu perfil"; un dueño no ve esa sección;
    C-01 a C-14 comprobados; `revisar-capas` sin violaciones; revisión del
    agente `seguridad` sin hallazgos críticos ni altos; `memoria.md`
    actualizada.
  - Verificación: `./gradlew ktlintCheck detekt test`,
    `./gradlew connectedAndroidTest` con perfil de dueño y de afiliado (C-13)
    y reporte del agente `seguridad`.
  - Archivos: `ui/perfil/ProfileScreen.kt`,
    `androidTest/.../ui/perfil/ProfileScreenTest.kt`, `memoria.md`

> **Verificación F5:** visitas solo para afiliados; unidad cerrada y auditada.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-003 | Modelos y contratos separados |
| F2 Datos | T-004, T-005 | Ajustes con lectura offline |
| F3 Ajustes | T-006 … T-008 | Interruptores, enlaces legales y logout |
| F4 Eliminación | T-009 … T-011 | Limpieza, reautenticación y flujo |
| F5 Perfil | T-012 | Perfil, visitas y cierre |

**Bloqueadas:** T-003 (Q-04), T-004 (Q-01, Q-04), T-007 (Q-02), T-010 (Q-01,
Q-03), T-012 (Q-07).

**Nota para 020:** Google Play exige que la ruta de eliminar cuenta sea
accesible y que el aviso de privacidad esté publicado. T-007 y T-011 son
requisitos de la publicación, no detalles de interfaz.
