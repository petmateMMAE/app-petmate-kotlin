# Tareas 019 — IA generativa

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **3 / 10 tareas** (2 verificadas el 19/11/2026; 1 implementada, por verificar en dispositivo)

---

## Fase F1 — Dominio

- [x] **T-001** — Modelos de generación
  - Depende de: —
  - Aceptación: `GeneratedText` (texto, origen marcado como IA, fecha y huella
    del contenido original), `GenerationRequest` (explicar receta o resumir
    historial) y `GenerationResult` con **tres casos** —generado, no
    disponible, fallido— en `domain/model`, en Kotlin puro.
  - Verificación: `./gradlew test`; `grep` sobre `domain/` sin framework.
  - Archivos: `domain/model/GeneratedText.kt`,
    `domain/model/GenerationRequest.kt`,
    `domain/model/GenerationResult.kt`

- [x] **T-002** — Contrato del repositorio
  - Depende de: T-001
  - Aceptación: `AiRepository` declara pedir explicación y resumen, con la
    opción de forzar regeneración; **no declara ninguna llamada a un proveedor
    externo**, solo al backend.
  - Verificación: `./gradlew test` con doble del contrato.
  - Archivos: `domain/repository/AiRepository.kt`

> **Verificación F1:** el contrato no contempla proveedores externos.

---

## Fase F2 — Datos

- [ ] **T-003** — DTOs y servicio remoto hacia el backend
  - Depende de: T-002 · Bloqueada por: **Q-01**
  - Aceptación: `AiApiService` apunta **al endpoint del backend**; no existe
    ningún cliente de proveedor de IA ni ninguna llave en la app (D-01); los
    DTOs y mappers traducen en ambos sentidos.
  - Verificación: `./gradlew test` con `MockWebServer`; búsqueda en el
    repositorio que confirme que no hay llaves ni URLs de proveedores (C-08).
  - Archivos: `data/remote/dto/GenerationDto.kt`,
    `data/remote/AiApiService.kt`, `data/mapper/GenerationMapper.kt`,
    `test/.../data/mapper/GenerationMapperTest.kt`

- [x] **T-004** — Caché por huella del contenido _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001
  - Aceptación: `GeneratedTextEntity` y su DAO guardan el texto generado
    asociado al identificador **y a una huella del contenido original**
    (D-04); si el original cambia, la huella cambia y la caché no sirve el
    texto viejo.
  - Verificación: `./gradlew connectedAndroidTest` con el mismo contenido
    (sirve caché) y con contenido modificado (no la sirve).
  - Archivos: `data/local/entity/GeneratedTextEntity.kt`,
    `data/local/dao/GeneratedTextDao.kt`,
    `androidTest/.../data/local/GeneratedTextDaoTest.kt`
  - Nota: cambiar el esquema de Room requiere autorización.

- [ ] **T-005** — `AiRepositoryImpl`
  - Depende de: T-003, T-004
  - Aceptación: consulta la caché primero y la red después; sin conexión
    devuelve "no disponible" sin intentar generar; un fallo del backend
    devuelve "fallido" sin excepción; la caché se limpia junto con los datos
    de la mascota y en el borrado de cuenta de 017 (RT-06).
  - Verificación: `./gradlew test` con: caché presente, caché ausente, sin red
    y fallo del backend (C-06, C-10, C-11).
  - Archivos: `data/repository/AiRepositoryImpl.kt`, `di/AiModule.kt`,
    `test/.../data/repository/AiRepositoryImplTest.kt`

> **Verificación F2:** solo se llama al backend; la caché respeta cambios del
> original.

---

## Fase F3 — Casos de uso

- [ ] **T-006** — Explicar receta y resumir historial
  - Depende de: T-005 · Bloqueada por: **Q-03, Q-07**
  - Aceptación: `ExplainPrescriptionUseCase` y `SummarizeHistoryUseCase` usan
    la caché por omisión y regeneran solo si se pide de forma explícita;
    **el resumen no incluye las alergias** (D-05), que se muestran aparte por
    008.
  - Verificación: `./gradlew test` con conteo de llamadas (C-06, C-07) y una
    prueba que confirme que el resumen generado no contiene la sección de
    alergias.
  - Archivos: `domain/usecase/ExplainPrescriptionUseCase.kt`,
    `domain/usecase/SummarizeHistoryUseCase.kt`, `test/.../domain/usecase/`

> **Verificación F3:** caché por omisión; alergias fuera de lo generado.

---

## Fase F4 — Interfaz

- [ ] **T-007** — Texto original siempre visible
  - Depende de: T-006
  - Aceptación: `OriginalTextSection` muestra la receta o el historial tal
    como los escribió el veterinario, **siempre presente**; la tarjeta
    generada se monta debajo y se puede contraer, pero nunca sustituye al
    original (D-02).
  - Verificación: `./gradlew connectedAndroidTest` con la explicación
    presente y ausente (C-02).
  - Archivos: `ui/ia/OriginalTextSection.kt`,
    `androidTest/.../ui/ia/OriginalTextSectionTest.kt`

- [ ] **T-008** — Tarjeta generada, marca de IA y advertencia
  - Depende de: T-007
  - Aceptación: `AiExplanationCard` muestra el texto generado;
    `AiDisclaimer` lo marca como generado por IA y advierte consultar al
    veterinario, **sin opción de descartarlo permanentemente** (D-03);
    mientras genera hay indicador y la pantalla responde; pasados 10 s se
    avisa y se puede cancelar.
  - Verificación: `./gradlew connectedAndroidTest` (C-03, C-04, C-12) y prueba
    con servidor lento (C-13).
  - Archivos: `ui/ia/AiExplanationCard.kt`, `ui/ia/AiDisclaimer.kt`,
    `androidTest/.../ui/ia/AiExplanationCardTest.kt`

- [ ] **T-009** — Integración en el carnet y la receta
  - Depende de: T-008
  - Aceptación: la acción "explícamelo fácil" aparece en la receta de 014 y el
    resumen en el historial de 008; **no existe ningún campo de texto libre
    hacia la IA** (D-06); si la generación falla, se muestra el original con
    un aviso discreto y no un error bloqueante.
  - Verificación: `./gradlew connectedAndroidTest` con doble que falla (C-10)
    y revisión de la interfaz (C-14).
  - Archivos: `ui/carnet/HealthRecordScreen.kt`,
    `ui/consulta/PrescriptionView.kt`, `androidTest/.../ui/ia/`

> **Verificación F4:** el original manda; la IA se presenta como lo que es.

---

## Fase F5 — Verificación y cierre

- [ ] **T-010** — Auditoría de privacidad y cierre
  - Depende de: T-009 · Bloqueada por: **Q-04, Q-05**
  - Aceptación: **no existe ninguna llave de proveedor en el repositorio ni en
    el artefacto**; durante una generación el tráfico muestra **solo llamadas
    al backend** (C-09); el uso de IA y los datos que viajan están declarados
    en el aviso de privacidad de 017; con un historial conocido, el resumen no
    inventa datos; C-01 a C-14 comprobados; `revisar-capas` sin violaciones;
    revisión del agente `seguridad` sin hallazgos críticos ni altos;
    `memoria.md` actualizada.
  - Verificación: reporte del agente `seguridad` (C-08), inspección del
    tráfico (C-09), revisión del contenido generado con historial conocido
    (C-05) y `./gradlew ktlintCheck detekt test`.
  - Archivos: `memoria.md`

> **Verificación F5:** sin llaves, sin llamadas directas al proveedor, y el
> contenido generado no inventa.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001, T-002 | Modelos y contrato |
| F2 Datos | T-003 … T-005 | Backend, caché por huella y degradación |
| F3 Casos de uso | T-006 | Explicación y resumen |
| F4 Interfaz | T-007 … T-009 | Original visible, marca y advertencia |
| F5 Verificación | T-010 | Privacidad y contenido |

**Bloqueadas:** T-003 (Q-01), T-006 (Q-03, Q-07), T-010 (Q-04, Q-05).

**Nota:** Q-04 (qué datos clínicos viajan al proveedor) debe resolverse
**antes** de empezar, porque determina qué hay que declarar en el aviso de
privacidad y si se requiere anonimizar. No es un detalle técnico.
