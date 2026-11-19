# Tareas 014 — Consulta veterinaria y cobro

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **8 / 15 tareas** (implementadas, por verificar al 17/11/2026)

---

## Fase F1 — Cálculo del cobro

> Esta fase va primero a propósito: es donde un error cuesta dinero real y es
> la más fácil de probar aislada (decisión D-01).

- [ ] **T-001** — Renglón de cobro y resumen
  - Depende de: —
  - Aceptación: `ChargeLine` (concepto, tipo —servicio, vacuna o medicina— y
    `Money` de 011), `ChargeSummary` (subtotal, IVA y total) y `TaxRate`
    existen en `domain/model`, en Kotlin puro; la tasa **no es una constante
    en el código** (D-03).
  - Verificación: `./gradlew test`; `grep` sobre `domain/` sin importaciones
    de framework.
  - Archivos: `domain/model/ChargeLine.kt`, `domain/model/ChargeSummary.kt`,
    `domain/model/TaxRate.kt`

- [ ] **T-002** — Cálculo del cobro con redondeo documentado
  - Depende de: T-001 · Bloqueada por: **Q-03** (tasa de IVA)
  - Aceptación: `CalculateChargeUseCase` suma los renglones en enteros, aplica
    la tasa sobre el subtotal y redondea **una sola vez al final**, al centavo
    más cercano con medio hacia arriba (D-02); el mismo caso de uso se usa en
    la pantalla y al guardar.
  - Verificación: `./gradlew test` con al menos estos casos: suma de
    10.10 + 20.20 + 0.70 = 31.00 exacto (C-07); montos que producen fracciones
    de centavo en el IVA (C-08); cero renglones; un solo renglón; veinte
    renglones; renglón con precio cero.
  - Archivos: `domain/usecase/CalculateChargeUseCase.kt`,
    `test/.../domain/usecase/CalculateChargeUseCaseTest.kt`
  - Nota: tarea crítica. Mitiga G-01. No se cierra sin las pruebas de
    fracciones.

> **Verificación F1:** el cobro calcula correctamente antes de construir nada
> encima.

---

## Fase F2 — Dominio

- [x] **T-003** — Modelos de la consulta y la receta _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001
  - Aceptación: `Consultation` (motivo, diagnóstico, peso, receta, renglones,
    forma de pago), `Prescription` con varios `PrescriptionItem`
    (medicamento, dosis, frecuencia, duración) y `PaymentMethod`, en Kotlin
    puro.
  - Verificación: `./gradlew test`
  - Archivos: `domain/model/Consultation.kt`,
    `domain/model/Prescription.kt`, `domain/model/PaymentMethod.kt`

- [ ] **T-004** — Borrador y contrato del repositorio
  - Depende de: T-003
  - Aceptación: `ConsultationDraft` guarda el avance; `ConsultationRepository`
    declara guardar consulta, crear servicio nuevo, y guardar, leer y **borrar**
    el borrador.
  - Verificación: `./gradlew test` con doble del contrato.
  - Archivos: `domain/model/ConsultationDraft.kt`,
    `domain/repository/ConsultationRepository.kt`

- [x] **T-005** — Validación y guardado _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-004, T-002 · Bloqueada por: **Q-04** (formas de pago)
  - Aceptación: `ValidateConsultationUseCase` rechaza guardar sin forma de
    pago y sin renglones de cobro; `SaveConsultationUseCase` guarda la
    consulta **y propaga el peso al carnet de 008 en la misma operación**
    (D-04), con clave de idempotencia para que un reintento no duplique.
  - Verificación: `./gradlew test` con dobles (C-10, C-11) y una prueba de
    reintento que confirme que no se duplica.
  - Archivos: `domain/usecase/ValidateConsultationUseCase.kt`,
    `domain/usecase/SaveConsultationUseCase.kt`, `test/.../domain/usecase/`

> **Verificación F2:** validaciones probadas; el peso se propaga una sola vez.

---

## Fase F3 — Datos

- [x] **T-006** — DTOs, servicio remoto y mappers _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-003 · Bloqueada por: **Q-01, Q-05**
  - Aceptación: DTOs de consulta, receta y renglones con su serialización;
    `ConsultationApiService` declara crear consulta y crear servicio; mappers
    en ambos sentidos sin pérdida de precisión en los montos.
  - Verificación: `./gradlew test` con `MockWebServer` y prueba de ida y
    vuelta que confirme que los montos no se alteran.
  - Archivos: `data/remote/dto/ConsultationDto.kt`,
    `data/remote/ConsultationApiService.kt`,
    `data/mapper/ConsultationMapper.kt`,
    `test/.../data/mapper/ConsultationMapperTest.kt`

- [ ] **T-007** — Borrador persistente
  - Depende de: T-004
  - Aceptación: `ConsultationDraftEntity` y su DAO guardan el borrador en
    curso asociado al negocio y a la mascota; el borrador sobrevive al cierre
    de la app.
  - Verificación: `./gradlew connectedAndroidTest` con base en memoria (C-12).
  - Archivos: `data/local/entity/ConsultationDraftEntity.kt`,
    `data/local/dao/ConsultationDraftDao.kt`,
    `androidTest/.../data/local/ConsultationDraftDaoTest.kt`
  - Nota: cambiar el esquema de Room requiere autorización.

> **Verificación F3:** el borrador persiste; los montos viajan sin alterarse.

---

## Fase F4 — Repositorio

- [x] **T-008** — `ConsultationRepositoryImpl` _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-006, T-007 · Bloqueada por: **Q-02**
  - Aceptación: guarda la consulta enviando el token del escaneo de 013; un
    permiso expirado se traduce a un error claro y **no se guarda** (D-06);
    sin red devuelve el error correspondiente sin perder el borrador; crear un
    servicio nuevo es una operación independiente del guardado (RT-06).
  - Verificación: `./gradlew test` con dobles para: guardado exitoso, permiso
    expirado (C-13), sin red (C-14) y servicio nuevo (C-09).
  - Archivos: `data/repository/ConsultationRepositoryImpl.kt`,
    `di/ConsultationModule.kt`,
    `test/.../data/repository/ConsultationRepositoryImplTest.kt`

- [ ] **T-009** — Borrado del borrador al cerrar
  - Depende de: T-008
  - Aceptación: el borrador se elimina al guardar con éxito **y** al descartar
    la consulta; tras cualquiera de los dos, no queda diagnóstico, receta ni
    peso en el almacenamiento local (D-05).
  - Verificación: `./gradlew test` y comprobación del almacenamiento en ambos
    caminos (C-15).
  - Archivos: `data/repository/ConsultationRepositoryImpl.kt`,
    `test/.../data/repository/`

> **Verificación F4:** permiso expirado rechazado; sin datos clínicos
> residuales.

---

## Fase F5 — Sección clínica

- [x] **T-010** — ViewModel de la consulta _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-005, T-008
  - Aceptación: `ConsultationViewModel` expone un `StateFlow` con la parte
    clínica, la receta, los renglones y el resumen; recalcula el resumen con
    `CalculateChargeUseCase`, sin duplicar la lógica; guarda el borrador al
    cambiar cualquier campo.
  - Verificación: `./gradlew test` con Turbine, comprobando que el resumen
    cambia al agregar y quitar renglones (C-05, C-06).
  - Archivos: `ui/consulta/ConsultationViewModel.kt`,
    `ui/consulta/ConsultationUiState.kt`,
    `test/.../ui/consulta/ConsultationViewModelTest.kt`

- [x] **T-011** — Motivo, diagnóstico y peso _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-010
  - Aceptación: captura motivo, diagnóstico y peso en kilogramos; valida que
    el peso sea un número positivo razonable; los textos salen de
    `strings.xml`; la sección se puede plegar (D-08).
  - Verificación: `./gradlew connectedAndroidTest` (C-02).
  - Archivos: `ui/consulta/ClinicalSection.kt`,
    `androidTest/.../ui/consulta/ClinicalSectionTest.kt`

- [x] **T-012** — Receta con varios medicamentos _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-011 · Bloqueada por: **Q-05** (catálogo de medicamentos)
  - Aceptación: agrega, edita y elimina renglones de receta, cada uno con
    medicamento, dosis, frecuencia y duración; permite al menos tres sin
    degradarse; una receta vacía es válida (no toda consulta receta).
  - Verificación: `./gradlew connectedAndroidTest` con tres medicamentos
    (C-04).
  - Archivos: `ui/consulta/PrescriptionSection.kt`,
    `androidTest/.../ui/consulta/PrescriptionSectionTest.kt`

> **Verificación F5:** clínica y receta completas y plegables.

---

## Fase F6 — Cobro y cierre

- [ ] **T-013** — Sección de cobro con total en vivo
  - Depende de: T-012 · Bloqueada por: **Q-03, Q-04**
  - Aceptación: agrega renglones desde los servicios del negocio, vacunas y
    medicinas; muestra subtotal, IVA y total actualizados al momento; permite
    quitar renglones; registra la forma de pago; el recálculo tarda menos de
    100 ms con veinte renglones.
  - Verificación: `./gradlew connectedAndroidTest` (C-05, C-06, C-10) y
    medición del recálculo.
  - Archivos: `ui/consulta/ChargeSection.kt`,
    `androidTest/.../ui/consulta/ChargeSectionTest.kt`

- [ ] **T-014** — Servicio nuevo durante la consulta
  - Depende de: T-013 · Bloqueada por: **Q-02**
  - Aceptación: un diálogo permite crear un servicio con nombre y costo sin
    salir de la consulta; el servicio se agrega al cobro y **queda disponible
    para el negocio** después; si la creación falla, la consulta no se pierde.
  - Verificación: `./gradlew connectedAndroidTest` con doble de backend (C-09).
  - Archivos: `ui/consulta/NewServiceDialog.kt`,
    `androidTest/.../ui/consulta/NewServiceDialogTest.kt`

- [x] **T-015** — Guardado, seguimiento opcional y cierre _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-014
  - Aceptación: al guardar se ofrece abrir un seguimiento (015) y la consulta
    se guarda igual si se declina; si 015 no existe, el ofrecimiento apunta a
    un destino provisional sin romper; solo una veterinaria llega a esta
    pantalla; C-01 a C-16 comprobados; la skill `revisar-capas` sin
    violaciones; `memoria.md` actualizada.
  - Verificación: `./gradlew ktlintCheck detekt test`, recorrido completo con
    doble de backend (C-01, C-16) y comprobación del borrador limpio (C-15).
  - Archivos: `ui/consulta/ConsultationScreen.kt`, `memoria.md`

> **Verificación F6:** cobro correcto de punta a punta; sin datos residuales.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Cálculo | T-001, T-002 | El cobro calculado y probado |
| F2 Dominio | T-003 … T-005 | Modelos, contrato, validación y guardado |
| F3 Datos | T-006, T-007 | Transporte y borrador |
| F4 Repositorio | T-008, T-009 | Guardado, permisos y limpieza |
| F5 Clínica | T-010 … T-012 | Motivo, diagnóstico, peso y receta |
| F6 Cobro | T-013 … T-015 | Renglones, servicio nuevo y cierre |

**Bloqueadas:** T-002 (Q-03), T-005 (Q-04), T-006 (Q-01, Q-05), T-008 (Q-02),
T-012 (Q-05), T-013 (Q-03, Q-04), T-014 (Q-02).

**Nota:** Q-03 (la tasa de IVA y si los precios de 011 la incluyen) bloquea el
cálculo entero. Es la pregunta más urgente de esta unidad.
