# Tareas 018 — ML: identificar la raza por foto

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **1 / 9 tareas** (1 verificada, al 26/11/2026)

---

## Fase F1 — Contrato, sin modelo

> Se construye y se prueba con un clasificador doble, antes de elegir la
> tecnología (decisión D-01).

- [x] **T-001** — Modelos y contrato del clasificador
  - Depende de: —
  - Aceptación: `BreedSuggestion` (raza y confianza), `ClassificationResult`
    con **tres casos distintos** —sugerencias, no identificada, modelo no
    disponible— (D-03) y la interfaz `BreedClassifier` en `domain`, que recibe
    la imagen como un tipo propio, **no como un `Bitmap` de Android** (D-01).
  - Verificación: `./gradlew test`; `grep` sobre `domain/` sin importaciones
    de `android`, `androidx` ni de ninguna librería de inferencia.
  - Archivos: `domain/model/BreedSuggestion.kt`,
    `domain/model/ClassificationResult.kt`,
    `domain/classifier/BreedClassifier.kt`

- [ ] **T-002** — Umbral y ordenación de sugerencias
  - Depende de: T-001 · Bloqueada por: **Q-02** (umbral)
  - Aceptación: `SuggestBreedUseCase` descarta lo que no llega al umbral,
    ordena por confianza descendente y **limita a tres**; si nada supera el
    umbral devuelve "no identificada", no una lista vacía.
  - Verificación: `./gradlew test` con clasificador doble: cinco resultados
    sobre el umbral (devuelve tres), todos por debajo (no identificada),
    exactamente en el umbral, y clasificador no disponible (C-02, C-06).
  - Archivos: `domain/usecase/SuggestBreedUseCase.kt`,
    `test/.../domain/usecase/SuggestBreedUseCaseTest.kt`

> **Verificación F1:** reglas probadas sin que exista aún ningún modelo.

---

## Fase F2 — Modelo

- [ ] **T-003** — Elegir la tecnología de inferencia
  - Depende de: T-002 · Bloqueada por: **Q-01, Q-03, Q-04**
  - Aceptación: se evalúan al menos dos opciones y se documenta la elegida con
    su justificación, su tamaño, si requiere servicios de Google y su licencia;
    se sigue la skill `agregar-dependencia` y se obtiene autorización antes de
    tocar el catálogo.
  - Verificación: propuesta escrita y aprobada por el responsable; registro en
    `memoria.md`.
  - Archivos: `memoria.md`, `gradle/libs.versions.toml`
  - Nota: no se escribe código de inferencia hasta que esta tarea esté
    aprobada.

- [ ] **T-004** — Implementación del clasificador
  - Depende de: T-003
  - Aceptación: `BreedClassifierImpl` implementa la interfaz con el modelo
    elegido; **no existe ninguna llamada de red en el camino de la
    inferencia** (D-02); corre en el dispatcher inyectado, nunca en el hilo
    principal; `ModelAvailability` reporta si el modelo está listo.
  - Verificación: `./gradlew connectedAndroidTest` con una foto de prueba;
    inspección del código que confirme que no hay cliente HTTP en la ruta.
  - Archivos: `data/classifier/BreedClassifierImpl.kt`,
    `data/classifier/ModelAvailability.kt`, `di/ClassifierModule.kt`,
    `androidTest/.../data/classifier/BreedClassifierImplTest.kt`

- [ ] **T-005** — Medir el impacto en el tamaño de la app
  - Depende de: T-004 · Bloqueada por: **Q-03**
  - Aceptación: se mide el artefacto con y sin el modelo y se compara contra
    el límite acordado; si lo excede, se implementa descarga bajo demanda
    (D-07); el resultado se registra.
  - Verificación: `./gradlew bundleRelease` en ambas configuraciones y
    comparación de tamaños (C-13).
  - Archivos: `memoria.md`, `app/build.gradle.kts`

> **Verificación F2:** el modelo responde, no usa red y cabe en el presupuesto
> de tamaño.

---

## Fase F3 — Interfaz en 007

- [ ] **T-006** — Sugerencias tocables en el registro de mascota
  - Depende de: T-004 · Bloqueada por: **Q-07** (catálogo de razas)
  - Aceptación: tras elegir la foto en 007, aparecen hasta tres sugerencias
    con su confianza; tocar una **escribe la raza en el campo**; el usuario
    puede borrarla y escribir otra; mientras infiere se muestra un indicador y
    la pantalla sigue respondiendo (D-06).
  - Verificación: `./gradlew connectedAndroidTest` (C-02, C-03, C-04, C-08).
  - Archivos: `ui/mascotas/BreedSuggestionChips.kt`,
    `ui/mascotas/PetFormScreen.kt`,
    `androidTest/.../ui/mascotas/BreedSuggestionChipsTest.kt`

- [ ] **T-007** — Aviso de sugerencia y degradación
  - Depende de: T-006
  - Aceptación: la pantalla indica que la raza es una **sugerencia**, no una
    determinación (R-06); si el modelo no está disponible, las sugerencias se
    ocultan y **el registro de mascota sigue funcionando igual** (D-05); con
    una foto que no es de una mascota se muestra "no identificada" sin error.
  - Verificación: `./gradlew connectedAndroidTest` con el modelo deshabilitado
    y con fotos de objetos (C-05, C-11, C-12).
  - Archivos: `ui/mascotas/SuggestionDisclaimer.kt`,
    `androidTest/.../ui/mascotas/`

> **Verificación F3:** la sugerencia ayuda pero nunca estorba ni bloquea 007.

---

## Fase F4 — Verificación

- [ ] **T-008** — **Comprobar que la foto no sale del dispositivo**
  - Depende de: T-007
  - Aceptación: durante una inferencia completa **no se registra ningún
    tráfico de red**; la revisión del agente `seguridad` confirma que no hay
    envío de imágenes a servicios externos en esta ruta.
  - Verificación: inspección del tráfico durante la operación (C-09) y reporte
    del agente `seguridad`.
  - Archivos: `androidTest/.../data/classifier/NoNetworkDuringInferenceTest.kt`
  - Nota: mitiga el riesgo alto G-07. Es la tarea de privacidad de esta unidad.

- [ ] **T-009** — Precisión, rendimiento y cierre
  - Depende de: T-008 · Bloqueada por: **Q-05, Q-06**
  - Aceptación: con el conjunto de fotos etiquetadas, la raza correcta aparece
    entre las tres primeras en la proporción mínima acordada; la inferencia
    tarda menos de 3 s en el dispositivo de referencia; funciona sin conexión;
    C-01 a C-13 comprobados; `revisar-capas` sin violaciones; `memoria.md`
    actualizada con la precisión medida.
  - Verificación: prueba con fotos etiquetadas (C-01), medición de tiempo
    (C-07), prueba con red ausente (C-10) y
    `./gradlew ktlintCheck detekt test`.
  - Archivos: `androidTest/.../data/classifier/BreedAccuracyTest.kt`,
    `memoria.md`

> **Verificación F4:** precisión medida y registrada; nada sale del
> dispositivo.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Contrato | T-001, T-002 | Interfaz y reglas, sin modelo |
| F2 Modelo | T-003 … T-005 | Tecnología elegida, implementada y medida |
| F3 Interfaz | T-006, T-007 | Sugerencias en 007 con degradación |
| F4 Verificación | T-008, T-009 | Privacidad, precisión y rendimiento |

**Bloqueadas:** T-002 (Q-02), T-003 (Q-01, Q-03, Q-04), T-005 (Q-03),
T-006 (Q-07), T-009 (Q-05, Q-06).

**Nota:** T-003 bloquea toda la fase F2. Es la decisión que hay que tomar
primero, y conviene resolverla con el profesor porque afecta el tamaño del
artefacto que se sube a Google Play (020).
