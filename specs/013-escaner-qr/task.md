# Tareas 013 — Escáner de QR y ficha de la mascota

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **10 / 12 tareas** (implementadas, por verificar al 17/11/2026)

> **Antes de empezar esta unidad hay que resolver Q-02.** Si el backend no
> filtra por tipo de negocio, la unidad no se puede implementar con seguridad
> y hay que corregir el backend primero.

---

## Fase F1 — Dominio

- [x] **T-001** — Modelos de la ficha y del token _(implementada, por verificar · 17/11/2026)_
  - Depende de: —
  - Aceptación: `ScannedPetCard` (básicos, alergias y secciones autorizadas),
    `ScanToken` y `ScanError` (expirado, inválido, revocado, sin red, no
    autorizado) en `domain/model`, en Kotlin puro; `ScanToken` **sobrescribe su
    representación textual** para no exponer el valor al imprimirse.
  - Verificación: `./gradlew test` con una prueba que confirme que convertir
    el token a texto no revela su contenido.
  - Archivos: `domain/model/ScannedPetCard.kt`, `domain/model/ScanToken.kt`,
    `domain/model/ScanError.kt`, `test/.../domain/model/ScanTokenTest.kt`

- [x] **T-002** — Contrato del repositorio de escaneo _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001
  - Aceptación: `ScanRepository` declara canjear un token por una ficha y
    devuelve `AppResult<ScannedPetCard>`; **no declara ninguna operación de
    guardado local** (decisión D-02).
  - Verificación: `./gradlew test` con doble del contrato.
  - Archivos: `domain/repository/ScanRepository.kt`

- [x] **T-003** — Canje del token y traducción de errores _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-002 · Bloqueada por: **Q-01, Q-03**
  - Aceptación: `RedeemScanTokenUseCase` canjea el token y traduce cada
    respuesta de error a su `ScanError` concreto; ninguna excepción sale del
    caso de uso.
  - Verificación: `./gradlew test` con un caso por tipo de error: expirado,
    inválido, revocado y sin red (C-06, C-07, C-08).
  - Archivos: `domain/usecase/RedeemScanTokenUseCase.kt`,
    `test/.../domain/usecase/RedeemScanTokenUseCaseTest.kt`

> **Verificación F1:** errores traducidos; el token no se revela al imprimirse.

---

## Fase F2 — Datos

- [x] **T-004** — DTO, servicio remoto y mapper _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-002 · Bloqueada por: **Q-01, Q-02**
  - Aceptación: `ScannedPetCardDto` y su mapper; `ScanApiService` declara el
    canje enviando el token y la credencial de sesión; el interceptor de
    registro **omite el token** (D-03).
  - Verificación: `./gradlew test` con `MockWebServer`; prueba que confirme
    que el token no aparece en la salida de registro (C-13).
  - Archivos: `data/remote/dto/ScannedPetCardDto.kt`,
    `data/remote/ScanApiService.kt`, `data/mapper/ScanMapper.kt`,
    `test/.../data/remote/ScanApiServiceTest.kt`

- [x] **T-005** — `ScanRepositoryImpl` sin persistencia _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004
  - Aceptación: canjea el token y devuelve la ficha; **no escribe en Room ni
    en ningún almacenamiento**; sin red devuelve el error correspondiente sin
    quedarse esperando.
  - Verificación: `./gradlew test` con dobles; inspección que confirme que no
    existe ninguna llamada de escritura local en la implementación (C-11).
  - Archivos: `data/repository/ScanRepositoryImpl.kt`, `di/ScanModule.kt`,
    `test/.../data/repository/ScanRepositoryImplTest.kt`

- [ ] **T-006** — **Prueba de privacidad por tipo de negocio**
  - Depende de: T-005 · Bloqueada por: **Q-02, Q-05**
  - Aceptación: con el doble respondiendo como para una veterinaria, la
    respuesta **contiene** historial clínico; con el doble respondiendo como
    para tienda o estética, la respuesta **no contiene** historial clínico,
    solo básicos y alergias. La prueba inspecciona **la respuesta recibida**,
    no lo que se pinta en pantalla.
  - Verificación: `./gradlew test` con ambos escenarios (C-03, C-04).
  - Archivos: `test/.../data/repository/ScanPrivacyTest.kt`
  - Nota: esta es la tarea más importante de la unidad. Mitiga el riesgo
    crítico G-01. Si falla, se detiene la unidad y se corrige el backend.

> **Verificación F2:** la respuesta para tienda y estética no trae datos
> clínicos; nada se persiste; el token no se registra.

---

## Fase F3 — Escáner

- [x] **T-007** — Interfaz de lectura y su implementación _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-003 · Bloqueada por: **Q-06** (librería)
  - Aceptación: `QrCodeReader` se declara en el dominio como interfaz; su
    implementación concreta vive en `data` y se inyecta; cambiar de librería
    no obliga a tocar la pantalla.
  - Verificación: `./gradlew test` con un doble del lector.
  - Archivos: `domain/scanner/QrCodeReader.kt`,
    `data/scanner/QrCodeReaderImpl.kt`, `di/ScannerModule.kt`
  - Nota: agregar la librería requiere autorización (skill
    `agregar-dependencia`).

- [x] **T-008** — Pantalla del escáner _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-007
  - Aceptación: abre la cámara pidiendo el permiso según 003; muestra guía
    visual y linterna; al leer un QR válido navega a la ficha; **libera la
    cámara al salir de la pantalla**; sin permiso muestra la alternativa
    manual sin bloquearse.
  - Verificación: `./gradlew connectedAndroidTest` con permiso concedido y
    denegado (C-01, C-02, C-09).
  - Archivos: `ui/escaner/QrScannerViewModel.kt`,
    `ui/escaner/QrScannerScreen.kt`,
    `androidTest/.../ui/escaner/QrScannerScreenTest.kt`

> **Verificación F3:** el escaneo abre la ficha correcta; la cámara se libera.

---

## Fase F4 — Entrada manual

- [x] **T-009** — Código corto escrito a mano _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-008 · Bloqueada por: **Q-04** (formato del código)
  - Aceptación: campo de entrada con el formato del código corto y validación
    de longitud; un código válido abre la ficha igual que el escaneo; un
    código mal escrito muestra error sin salir de la pantalla; esta ruta está
    disponible siempre, no solo cuando se niega la cámara.
  - Verificación: `./gradlew connectedAndroidTest` (C-10).
  - Archivos: `ui/escaner/ManualCodeEntry.kt`,
    `androidTest/.../ui/escaner/ManualCodeEntryTest.kt`

> **Verificación F4:** se puede llegar a la ficha sin usar la cámara.

---

## Fase F5 — Ficha

- [x] **T-010** — Alergias destacadas _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-006
  - Aceptación: `AllergyBanner` se pinta por encima del resto de la ficha y
    **es independiente de las secciones autorizadas**; aparece tanto para
    veterinaria como para tienda y estética; si no hay alergias registradas,
    lo dice explícitamente en vez de no mostrarse.
  - Verificación: `./gradlew connectedAndroidTest` con los dos tipos de
    negocio y con lista vacía (C-05).
  - Archivos: `ui/ficha/AllergyBanner.kt`,
    `androidTest/.../ui/ficha/AllergyBannerTest.kt`

- [x] **T-011** — Pantalla de la ficha _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-010
  - Aceptación: muestra los básicos de la mascota y las secciones que llegaron
    autorizadas; es de solo lectura; una veterinaria ve la entrada a registrar
    consulta y una tienda o estética **no**; los errores de token se muestran
    con su mensaje propio.
  - Verificación: `./gradlew connectedAndroidTest` con ambos tipos y con cada
    tipo de error (C-06, C-07, C-08, C-14, C-15).
  - Archivos: `ui/ficha/PetCardViewModel.kt`, `ui/ficha/PetCardScreen.kt`,
    `androidTest/.../ui/ficha/PetCardScreenTest.kt`

> **Verificación F5:** alergias siempre visibles; la consulta solo para
> veterinaria.

---

## Fase F6 — Cierre

- [ ] **T-012** — Verificación de privacidad y cierre
  - Depende de: T-011
  - Aceptación: tras cerrar la ficha, **no queda contenido clínico en el
    almacenamiento del dispositivo**; el token no aparece en los registros de
    una sesión completa; C-01 a C-15 comprobados; la skill `revisar-capas` sin
    violaciones; revisión del agente `seguridad` sin hallazgos críticos ni
    altos; `memoria.md` actualizada.
  - Verificación: inspección del almacenamiento tras cerrar la ficha (C-12),
    búsqueda sobre los registros (C-13), `./gradlew ktlintCheck detekt test` y
    reporte del agente `seguridad`.
  - Archivos: `memoria.md`

> **Verificación F6:** nada clínico persistido; auditoría de seguridad limpia.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-003 | Modelos, contrato y canje |
| F2 Datos | T-004 … T-006 | Transporte, sin persistencia y prueba de privacidad |
| F3 Escáner | T-007, T-008 | Lectura con cámara |
| F4 Manual | T-009 | Alternativa sin cámara |
| F5 Ficha | T-010, T-011 | Alergias y presentación por tipo |
| F6 Cierre | T-012 | Auditoría de privacidad |

**Bloqueadas:** T-003 (Q-01, Q-03), T-004 (Q-01, Q-02), T-006 (Q-02, Q-05),
T-007 (Q-06), T-009 (Q-04).

**Advertencia:** T-006 mitiga el riesgo crítico G-01. Si el backend no filtra
por tipo de negocio, **la unidad se detiene** hasta corregirlo. Mostrar menos
en pantalla mientras se recibe todo no es protección.
