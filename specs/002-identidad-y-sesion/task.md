# Tareas 002 — Identidad y sesión

> Lista de tareas de la unidad `002-identidad-y-sesion`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **11 / 18 tareas** (implementadas, por verificar al 17/11/2026) · Fases completadas: **0 / 6**

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

Los componentes de 001 se citan como `001/T-xx` para no confundirlos con las
tareas de esta unidad.

---

## Fase F1 — Dominio

- [x] **T-001** — Modelos de identidad y estado de sesión _(implementada, por verificar · 17/11/2026)_
  - Depende de: 001/T-007, 001/T-008
  - Aceptación: existen `Account`, `AuthTokens`, `VerificationChallenge`,
    `IdentityError` y `SessionState` como modelos inmutables de Kotlin puro;
    `SessionState` cubre sin sesión, restaurando, autenticado y expirado;
    `IdentityError` es un conjunto cerrado; ninguna clase importa Android ni
    red; hay pruebas de sus invariantes.
  - Verificación: `./gradlew test` y `grep` de imports prohibidos en
    `domain/identity`.
  - Archivos: `domain/identity/model/Account.kt`,
    `domain/identity/model/AuthTokens.kt`,
    `domain/identity/model/VerificationChallenge.kt`,
    `domain/identity/model/IdentityError.kt`,
    `domain/identity/model/SessionState.kt`

- [x] **T-002** — Contratos de repositorio de identidad y sesión _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001, 001/T-015
  - Aceptación: `IdentityRepository`, `SessionRepository` y `TokenRefresher`
    declaran las operaciones de alta, verificación, reenvío, login,
    recuperación, logout, restauración y renovación; usan `AppResult`, `Flow` y
    modelos de dominio; no aparece ningún DTO ni tipo de red.
  - Verificación: `./gradlew test` (compilación) y revisión de capas.
  - Archivos: `domain/identity/repository/IdentityRepository.kt`,
    `domain/identity/repository/SessionRepository.kt`,
    `domain/identity/repository/TokenRefresher.kt`

- [ ] **T-003** — Casos de uso de alta y verificación
  - Depende de: T-002
  - Aceptación: `SignUpUseCase`, `VerifyEmailUseCase` y `ResendCodeUseCase`
    devuelven `AppResult`, validan solo lo mínimo del contrato y no conocen
    Android; se prueban con dobles de los repositorios.
  - Verificación: `./gradlew test`.
  - Archivos: `domain/identity/usecase/SignUpUseCase.kt`,
    `domain/identity/usecase/VerifyEmailUseCase.kt`,
    `domain/identity/usecase/ResendCodeUseCase.kt`

- [ ] **T-004** — Casos de uso de login, recuperación y logout
  - Depende de: T-002
  - Aceptación: `LoginUseCase`, `RecoverAccessUseCase`, `LogoutUseCase` y
    `RestoreSessionUseCase` devuelven `AppResult`; el logout no decide la
    política local (eso es T-010); se prueban con dobles.
  - Verificación: `./gradlew test`.
  - Archivos: `domain/identity/usecase/LoginUseCase.kt`,
    `domain/identity/usecase/RecoverAccessUseCase.kt`,
    `domain/identity/usecase/LogoutUseCase.kt`,
    `domain/identity/usecase/RestoreSessionUseCase.kt`

> **Punto de verificación F1:** dominio puro, contratos completos y casos de
> uso probados. `grep` de `android.`, `retrofit`, `okhttp` y `compose` en
> `domain/identity` no devuelve nada.

---

## Fase F2 — Red y errores

- [x] **T-005** — Cliente remoto y DTOs de identidad _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001, 001/T-012, 001/T-013 · Bloqueada por: **Q-01**
  - Aceptación: existe una interfaz de API para registro, verificación,
    reenvío, login, renovación, logout y recuperación; los DTOs son
    serializables y se mapean a modelos de dominio; las rutas y los campos
    salen del contrato acordado, no de una suposición; hay pruebas de
    serialización con servidor local.
  - Verificación: `./gradlew test` con servidor local de prueba.
  - Archivos: `data/identity/remote/IdentityApi.kt`,
    `data/identity/remote/IdentityDtos.kt`,
    `data/identity/remote/IdentityRemoteDataSource.kt`,
    `data/identity/mapper/IdentityDtoMapper.kt`,
    `test/.../data/identity/remote/IdentityContractTest.kt`
  - Nota: **no se inventan rutas ni campos**. Si Q-01 sigue abierta, la tarea
    no se ejecuta.

- [x] **T-006** — Errores y textos de identidad _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001, 001/T-008, 001/T-013 · Bloqueada por: **Q-10**
  - Aceptación: cada falla del backend se traduce a un `IdentityError` del
    conjunto cerrado y a un `UiText` propio; credenciales inválidas y correo
    inexistente comparten mensaje; ningún mensaje crudo del backend llega a la
    interfaz.
  - Verificación: `./gradlew test` con los dos casos comparados (C-06, C-17).
  - Archivos: `data/identity/error/IdentityErrorMapper.kt`,
    `core/text/IdentityMessages.kt`,
    `test/.../data/identity/error/IdentityErrorMapperTest.kt`

> **Punto de verificación F2:** contrato de red aislado y errores cerrados.
> Cambiar un DTO no obliga a tocar `domain` ni `ui`.

---

## Fase F3 — Sesión

- [x] **T-007** — Estado de sesión observable _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001, T-002, 001/T-014, 001/T-015
  - Aceptación: la sesión se expone como `Flow<SessionState>` con los cuatro
    estados; se guarda cifrada y se restaura al arrancar; el cierre la deja
    vacía; una sesión corrupta o ilegible se descarta y pide login, sin
    respaldo en texto claro.
  - Verificación: `./gradlew test` y prueba instrumentada de almacenamiento
    (C-07, C-12).
  - Archivos: `data/session/SessionStateHolder.kt`,
    `data/session/SessionRepositoryImpl.kt`, `di/SessionModule.kt`,
    `test/.../data/session/SessionRepositoryTest.kt`

- [x] **T-008** — Renovación single-flight _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-007, 001/T-017 · Bloqueada por: **Q-05**
  - Aceptación: la renovación se serializa; diez peticiones simultáneas con el
    token expirado producen una sola renovación; cada petición original se
    reintenta una vez; un segundo 401 deja la sesión como expirada.
  - Verificación: `./gradlew test` con servidor local y prueba de concurrencia
    (C-08, C-09).
  - Archivos: `data/remote/TokenRefreshCoordinator.kt`,
    `data/remote/TokenAuthenticator.kt`,
    `test/.../data/remote/TokenRefreshCoordinatorTest.kt`
  - Nota: el transporte del refresh lo define Q-05; no se fija en la app.

> **Punto de verificación F3:** la sesión sobrevive a un reinicio simulado y
> la renovación no se duplica.

---

## Fase F4 — Repositorio

- [x] **T-009** — Repositorio de identidad _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-003, T-004, T-005, T-006, T-007
  - Aceptación: `IdentityRepositoryImpl` implementa los contratos y orquesta
    red, sesión y errores; guarda la sesión solo después de un login o una
    verificación correctos; se prueba con API y sesión falsas.
  - Verificación: `./gradlew test`.
  - Archivos: `data/identity/repository/IdentityRepositoryImpl.kt`,
    `data/identity/mapper/IdentityMapper.kt`, `di/IdentityModule.kt`,
    `test/.../data/identity/repository/IdentityRepositoryTest.kt`

- [ ] **T-010** — Logout local-first e invalidación pendiente
  - Depende de: T-009, T-007 · Bloqueada por: **Q-06**
  - Aceptación: el logout borra primero lo local y cambia el estado a sin
    sesión; después intenta invalidar en el servidor; sin red, deja una marca
    pendiente y la reintenta en el siguiente arranque con red; la interfaz no
    espera al servidor.
  - Verificación: `./gradlew test` con red simulada como ausente (C-10, C-11).
  - Archivos: `data/session/PendingLogoutStore.kt`,
    `data/session/LogoutCoordinator.kt`,
    `data/session/SessionRepositoryImpl.kt`,
    `test/.../data/session/LogoutCoordinatorTest.kt`
  - Nota: la política de la marca pendiente depende de Q-06.

> **Punto de verificación F4:** el repositorio no filtra DTOs ni decisiones de
> sesión a la UI; el logout deja el almacén vacío.

---

## Fase F5 — Presentación

- [x] **T-011** — Bienvenida y grafo de identidad _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004, T-009, 001/T-022
  - Aceptación: la bienvenida ofrece iniciar sesión y crear cuenta, y **no**
    muestra "probar demo"; las rutas son tipadas y llevan a alta, verificación,
    login y recuperación; no se puede volver al login con el botón atrás
    después de autenticarse.
  - Verificación: prueba de interfaz y de navegación (C-01).
  - Archivos: `ui/identity/welcome/WelcomeScreen.kt`,
    `ui/identity/welcome/WelcomeViewModel.kt`,
    `ui/navigation/IdentityNavGraph.kt`, `ui/navigation/AppNavHost.kt`,
    `test/.../ui/identity/WelcomeTest.kt`

- [x] **T-012** — Pantalla de alta _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-003, T-006, T-011 · Bloqueada por: **Q-02, Q-07, Q-09**
  - Aceptación: pide correo y contraseña, exige aceptar el aviso de privacidad
    y muestra su enlace; no continúa sin aceptación; con un servidor lento
    muestra estado de carga antes de 300 ms; los errores son comprensibles.
  - Verificación: prueba de interfaz con doble de backend (C-02, C-03, C-14).
  - Archivos: `ui/identity/signup/SignUpScreen.kt`,
    `ui/identity/signup/SignUpViewModel.kt`,
    `ui/identity/signup/SignUpUiState.kt`,
    `test/.../ui/identity/SignUpTest.kt`
  - Nota: los datos del alta y la URL del aviso dependen de Q-02, Q-07 y Q-09.

- [ ] **T-013** — Pantalla de verificación
  - Depende de: T-003, T-012 · Bloqueada por: **Q-03**
  - Aceptación: acepta un código de 6 dígitos, permite reenviar y muestra el
    estado y el temporizador que devuelve el servidor; un código incorrecto o
    expirado no activa la sesión; no se inventan tiempos locales.
  - Verificación: prueba de interfaz con respuestas simuladas (C-04, C-05).
  - Archivos: `ui/identity/verification/VerificationScreen.kt`,
    `ui/identity/verification/VerificationViewModel.kt`,
    `test/.../ui/identity/VerificationTest.kt`

- [x] **T-014** — Pantalla de login _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004, T-006, T-011 · Bloqueada por: **Q-04, Q-10**
  - Aceptación: autentica con correo y contraseña; credenciales inválidas y
    correo inexistente muestran el mismo mensaje; con servidor lento muestra
    carga antes de 300 ms; sin red muestra un error comprensible y no cierra
    la app; al entrar navega al grafo principal.
  - Verificación: prueba de interfaz con doble (C-06, C-14, C-15).
  - Archivos: `ui/identity/login/LoginScreen.kt`,
    `ui/identity/login/LoginViewModel.kt`,
    `test/.../ui/identity/LoginTest.kt`

- [ ] **T-015** — Pantalla de recuperación
  - Depende de: T-004, T-006, T-011 · Bloqueada por: **Q-08**
  - Aceptación: solicita la recuperación por correo y muestra la misma
    respuesta exista o no la cuenta; el flujo sigue lo que defina Q-08; no
    revela si el correo está registrado.
  - Verificación: prueba de interfaz con ambos casos (C-17).
  - Archivos: `ui/identity/recovery/RecoveryScreen.kt`,
    `ui/identity/recovery/RecoveryViewModel.kt`,
    `test/.../ui/identity/RecoveryTest.kt`

- [x] **T-016** — Guardas de navegación y restauración de sesión _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-007, T-011, T-014
  - Aceptación: al arrancar se restaura la sesión antes de pintar la primera
    pantalla; no se ve el login y luego un salto; una sesión válida entra al
    grafo principal y una ausente al de identidad; el botón atrás no vuelve al
    login después de autenticarse.
  - Verificación: prueba instrumentada de navegación y de muerte del proceso
    (C-07).
  - Archivos: `ui/navigation/SessionGate.kt`, `ui/navigation/AppNavHost.kt`,
    `MainActivity.kt`, `androidTest/.../ui/navigation/SessionGateTest.kt`

> **Punto de verificación F5:** las cinco pantallas y las guardas funcionan
> con dobles; la bienvenida no muestra demo; la carga aparece antes de 300 ms.

---

## Fase F6 — Integración

- [ ] **T-017** — Registro de push por transición de sesión
  - Depende de: T-007, 001/T-039 · Bloqueada por: **Q-08 de 001**
  - Aceptación: al pasar a autenticado se registra el token de push; al pasar a
    sin sesión se borra; la operación es idempotente y no depende de que la
    pantalla de login esté abierta.
  - Verificación: `./gradlew test` con doble del registro de push (C-16).
  - Archivos: `data/push/SessionPushObserver.kt`, `di/PushModule.kt`,
    `test/.../data/push/SessionPushObserverTest.kt`
  - Nota: 001/T-039 aún no está implementada; esta tarea no se ejecuta antes.

- [ ] **T-018** — Cierre de la unidad
  - Depende de: T-010, T-016, T-017
  - Aceptación: el recorrido completo con doble de backend pasa (alta →
    verificación → login → reinicio → logout); C-01 a C-17 están comprobados;
    C-18 queda diferido y anotado; las capas están limpias; las coberturas
    mínimas se cumplen; `memoria.md` queda actualizada.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas` y
    tabla de criterios de éxito en `memoria.md`.
  - Archivos: `test/.../identity/IdentityEndToEndTest.kt`,
    `docs/evidencia-002.md`, `memoria.md`

> **Punto de verificación F6:** unidad cerrada con el recorrido completo en
> verde y C-18 marcado como pendiente contra el backend real.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-004 | Modelos, contratos y casos de uso |
| F2 Red | T-005 … T-006 | Cliente, DTOs y errores cerrados |
| F3 Sesión | T-007 … T-008 | Estado observable y renovación single-flight |
| F4 Repositorio | T-009 … T-010 | Orquestación y logout local-first |
| F5 Presentación | T-011 … T-016 | Bienvenida, alta, verificación, login, recuperación y guardas |
| F6 Integración | T-017 … T-018 | Push y cierre de la unidad |

**Tareas bloqueadas por preguntas abiertas:** T-005 (Q-01), T-006 (Q-10),
T-008 (Q-05), T-010 (Q-06), T-012 (Q-02, Q-07, Q-09), T-013 (Q-03),
T-014 (Q-04, Q-10), T-015 (Q-08) y T-017 (Q-08 de 001, vía 001/T-039).

Esas preguntas deben resolverse con el responsable del backend y con el equipo
antes de ejecutar las tareas correspondientes. Las demás pueden empezar en
cuanto 001 esté implementada.
