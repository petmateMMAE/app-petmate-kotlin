# Tareas 011 — Afiliación de negocios

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **9 / 14 tareas** (implementadas, por verificar al 17/11/2026)

---

## Fase F1 — Dominio

- [x] **T-001** — Modelos de negocio, tipo y servicio _(implementada, por verificar · 17/11/2026)_
  - Depende de: —
  - Aceptación: `Business`, `BusinessType` (enumeración con veterinaria,
    tienda y estética), `Service` y `Money` existen en `domain/model`, en
    Kotlin puro; `Money` guarda el monto en unidad mínima como entero y su
    moneda, y expone suma y formato sin perder precisión.
  - Verificación: `./gradlew test` con pruebas de suma de `Money` que
    comprueben que 10.10 + 20.20 da exactamente 30.30.
  - Archivos: `domain/model/Business.kt`, `domain/model/Service.kt`,
    `domain/model/Money.kt`, `test/.../domain/model/MoneyTest.kt`

- [ ] **T-002** — Borrador de afiliación y contrato del repositorio
  - Depende de: T-001
  - Aceptación: `AffiliationDraft` representa el avance de los tres pasos;
    `BusinessRepository` declara crear negocio, consultar el propio, guardar y
    leer borrador, y borrar borrador; todo en `domain`, sin framework.
  - Verificación: `./gradlew test`; `grep` sobre `domain/` sin importaciones
    de Android, Room ni Retrofit.
  - Archivos: `domain/model/AffiliationDraft.kt`,
    `domain/repository/BusinessRepository.kt`

- [x] **T-003** — Validación por paso _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-002
  - Aceptación: `ValidateAffiliationStepUseCase` rechaza paso 1 sin nombre o
    sin tipo, paso 2 con cero servicios o con precio vacío, negativo o no
    numérico, y paso 3 sin pin; devuelve el motivo como `UiText`.
  - Verificación: `./gradlew test` con un caso por regla (C-02, C-04, C-06,
    C-07).
  - Archivos: `domain/usecase/ValidateAffiliationStepUseCase.kt`,
    `test/.../domain/usecase/ValidateAffiliationStepUseCaseTest.kt`

- [ ] **T-004** — Casos de uso de borrador y envío
  - Depende de: T-003
  - Aceptación: `SaveAffiliationDraftUseCase` persiste el avance;
    `SubmitAffiliationUseCase` valida los tres pasos antes de enviar y
    devuelve `AppResult`.
  - Verificación: `./gradlew test` con repositorio doble.
  - Archivos: `domain/usecase/SaveAffiliationDraftUseCase.kt`,
    `domain/usecase/SubmitAffiliationUseCase.kt`, `test/.../domain/usecase/`

> **Verificación F1:** reglas de validación probadas; dominio sin framework.

---

## Fase F2 — Datos

- [x] **T-005** — DTOs, servicio remoto y mappers _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-002 · Bloqueada por: **Q-01**
  - Aceptación: `BusinessDto` y `ServiceDto` con su serialización;
    `BusinessApiService` declara crear y consultar negocio; los mappers
    traducen DTO ↔ dominio sin que el dominio conozca el transporte.
  - Verificación: `./gradlew test` con `MockWebServer` y pruebas de mapeo en
    ambos sentidos.
  - Archivos: `data/remote/dto/BusinessDto.kt`,
    `data/remote/BusinessApiService.kt`, `data/mapper/BusinessMapper.kt`,
    `test/.../data/mapper/BusinessMapperTest.kt`

- [ ] **T-006** — Persistencia del borrador
  - Depende de: T-002
  - Aceptación: `AffiliationDraftEntity` y su DAO guardan el borrador asociado
    al identificador del usuario; el borrador sobrevive a cerrar la app.
  - Verificación: `./gradlew connectedAndroidTest` con base en memoria (C-08).
  - Archivos: `data/local/entity/AffiliationDraftEntity.kt`,
    `data/local/dao/AffiliationDraftDao.kt`,
    `androidTest/.../data/local/AffiliationDraftDaoTest.kt`
  - Nota: cambiar el esquema de Room requiere autorización.

> **Verificación F2:** el borrador persiste; los mappers no pierden datos.

---

## Fase F3 — Repositorio

- [x] **T-007** — `BusinessRepositoryImpl` _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-005, T-006
  - Aceptación: implementa el contrato; guarda y lee borrador local; al enviar,
    modera textos e imagen por el punto de 004 antes de llamar al backend; un
    rechazo de moderación no borra el borrador; sin red devuelve el error
    correspondiente sin perder lo capturado.
  - Verificación: `./gradlew test` con dobles de red y de moderación (C-11,
    C-12, C-14).
  - Archivos: `data/repository/BusinessRepositoryImpl.kt`,
    `di/BusinessModule.kt`, `test/.../data/repository/BusinessRepositoryImplTest.kt`

- [ ] **T-008** — Limpieza del borrador al cambiar de sesión
  - Depende de: T-007
  - Aceptación: el logout de 002 borra el borrador de afiliación; un usuario
    distinto no ve el borrador del anterior.
  - Verificación: `./gradlew test` con cambio de sesión simulado (C-13).
  - Archivos: `data/repository/BusinessRepositoryImpl.kt`,
    `test/.../data/repository/`

> **Verificación F3:** envío, moderación y limpieza de sesión probados.

---

## Fase F4 — Pasos 1 y 2

- [x] **T-009** — ViewModel del flujo y entrada desde el perfil _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004, T-007
  - Aceptación: `AffiliationFlowViewModel` expone un `StateFlow` con el paso
    activo, el borrador y los errores; el perfil muestra la entrada solo si el
    usuario no está afiliado.
  - Verificación: `./gradlew test` con Turbine (C-01).
  - Archivos: `ui/afiliacion/AffiliationFlowViewModel.kt`,
    `ui/afiliacion/AffiliationUiState.kt`,
    `test/.../ui/afiliacion/AffiliationFlowViewModelTest.kt`

- [x] **T-010** — Paso 1: datos y tipos _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-009
  - Aceptación: captura nombre, descripción, teléfono y horario; permite
    seleccionar uno o varios tipos; no avanza sin nombre ni tipo; los textos
    salen de `strings.xml`.
  - Verificación: `./gradlew connectedAndroidTest` con prueba de Compose
    (C-02, C-03).
  - Archivos: `ui/afiliacion/AffiliationStep1Screen.kt`,
    `androidTest/.../ui/afiliacion/AffiliationStep1Test.kt`

- [x] **T-011** — Paso 2: servicios con precio _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-010
  - Aceptación: agrega, edita y elimina servicios antes de confirmar; exige al
    menos uno; valida el precio con `Money`; muestra el total capturado.
  - Verificación: `./gradlew connectedAndroidTest` (C-04, C-05, C-06).
  - Archivos: `ui/afiliacion/AffiliationStep2Screen.kt`,
    `androidTest/.../ui/afiliacion/AffiliationStep2Test.kt`

> **Verificación F4:** pasos 1 y 2 completos y validados; el borrador persiste
> al salir (C-08).

---

## Fase F5 — Paso 3

- [x] **T-012** — Paso 3: pin en el mapa _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-011 · Bloqueada por: **010 Q-07** (proveedor de mapas)
  - Aceptación: monta el componente de mapa de 010 en modo selección; pide el
    permiso de ubicación según 003 y funciona sin él con centro por defecto;
    no confirma sin pin; muestra la dirección para que el usuario la verifique.
  - Verificación: `./gradlew connectedAndroidTest` (C-07).
  - Archivos: `ui/afiliacion/AffiliationStep3Screen.kt`,
    `androidTest/.../ui/afiliacion/AffiliationStep3Test.kt`

> **Verificación F5:** el pin se coloca y se confirma; sin permiso no se
> bloquea la pantalla.

---

## Fase F6 — Modos y cierre

- [x] **T-013** — Alternancia entre modo dueño y modo negocio _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-012
  - Aceptación: al completar la afiliación el usuario entra al panel (012) o a
    su destino provisional si 012 no existe; puede volver a "Mis mascotas" sin
    perderlas; un indicador visible señala el modo activo.
  - Verificación: `./gradlew connectedAndroidTest` de navegación (C-09, C-10).
  - Archivos: `ui/navigation/ModeSwitcher.kt`, `ui/navigation/AppNavHost.kt`,
    `androidTest/.../ui/navigation/ModeSwitcherTest.kt`

- [ ] **T-014** — Cierre de la unidad
  - Depende de: T-013
  - Aceptación: recorrido completo de los tres pasos con doble de backend;
    C-01 a C-14 comprobados; la skill `revisar-capas` sin violaciones;
    `memoria.md` actualizada.
  - Verificación: `./gradlew ktlintCheck detekt test` y recorrido manual.
  - Archivos: `memoria.md`

> **Verificación F6:** flujo completo de punta a punta; capas limpias.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001 … T-004 | Modelos, `Money`, contratos y validación |
| F2 Datos | T-005, T-006 | Transporte y borrador persistente |
| F3 Repositorio | T-007, T-008 | Envío, moderación y limpieza de sesión |
| F4 Pasos 1 y 2 | T-009 … T-011 | Datos, tipos y servicios |
| F5 Paso 3 | T-012 | Pin en el mapa |
| F6 Modos | T-013, T-014 | Alternancia y cierre |

**Bloqueadas:** T-005 (Q-01), T-012 (010 Q-07). Las demás solo requieren que
001, 002, 003, 004 y 010 estén implementadas.
