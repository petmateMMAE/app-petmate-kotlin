# Tareas 004 — Moderación de contenido

> Lista de tareas de la unidad `004-moderacion-contenido`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 29/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Las tareas se redactan y
se ejecutan después, con autorización explícita del humano. Un agente que
implemente estas tareas sin aprobación viola la constitución §4.

Progreso: **1 / 12 tareas** (1 verificada, al 26/11/2026) · Fases completadas: **0 / 5**

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

Los componentes de 001 se citan como `001/K-xx` y los de 002 como `002/ID-xx`.

---

## Fase F1 — Contrato

- [x] **T-001** — Modelos y contrato de moderación
  - Depende de: 001/T-030, 001/T-007
  - Aceptación: existen `ModerationContentType`, `ModerationRequest`,
    `ModerationVerdict` (aprobado, rechazado, no validado) y
    `ModerationRejection` como conjunto cerrado; `ModerationGateway` declara
    moderar texto, imagen y video; no importa Android ni red; hay pruebas de
    los estados.
  - Verificación: `./gradlew test` y `grep` de imports prohibidos en
    `domain/moderation`.
  - Archivos: `domain/moderation/ModerationContentType.kt`,
    `domain/moderation/ModerationRequest.kt`,
    `domain/moderation/ModerationVerdict.kt`,
    `domain/moderation/ModerationGateway.kt`,
    `test/.../domain/moderation/ModerationVerdictTest.kt`

- [ ] **T-002** — Cliente del backend y DTOs
  - Depende de: T-001, 001/T-012 · Bloqueada por: **Q-01**
  - Aceptación: existe un cliente que envía texto, imagen o video al endpoint
    del backend y mapea la respuesta a `ModerationVerdict`; las rutas y los
    campos salen del contrato acordado; hay pruebas de serialización con
    servidor local; la app no contiene llaves de Azure.
  - Verificación: `./gradlew test` con servidor local.
  - Archivos: `data/moderation/remote/ModerationApi.kt`,
    `data/moderation/remote/ModerationDtos.kt`,
    `data/moderation/remote/ModerationRemoteDataSource.kt`,
    `test/.../data/moderation/remote/ModerationContractTest.kt`
  - Nota: **no se inventan rutas ni campos**. Si Q-01 sigue abierta, la tarea
    no se ejecuta.

- [x] **T-003** — Veredicto, errores y mensaje único _(27/11/2026)_
  - Depende de: T-002, 001/T-008, 001/T-013
  - Aceptación: cada respuesta se traduce a aprobado, rechazado o no validado;
    el mensaje visible es único para todas las categorías; no se muestra el
    puntaje ni el proveedor; hay prueba de comparación entre dos categorías.
  - Verificación: `./gradlew test` (C-13).
  - Archivos: `data/moderation/ModerationPolicy.kt`,
    `core/text/ModerationMessages.kt`,
    `test/.../data/moderation/ModerationPolicyTest.kt`
  - Hecho: `ModerationPolicy` traduce la respuesta del moderador a Approved, Rejected o NotValidated,
    y descarta la categoria interna y el puntaje ahi mismo, en la frontera, para que no puedan
    filtrarse a la pantalla ni al log. `ModerationMessages` da un UNICO texto para todo lo no
    permitido; tamano y formato si tienen el suyo, porque ahi la persona puede actuar.
    14 pruebas, incluida la comparacion entre dos categorias que exige el criterio y una que
    verifica que el mensaje es un id de recurso sin argumentos, de modo que no hay por donde colar
    un puntaje. Un fallo del servicio NUNCA se traduce a aprobado (R-08): si lo hiciera, tirar el
    moderador seria la forma mas facil de saltarselo.

> **Punto de verificación F1:** contrato cerrado y veredicto sin detalles
> internos.

---

## Fase F2 — Texto

- [x] **T-004** — Moderación de texto en línea
  - Depende de: T-003
  - Aceptación: con red, el texto se modera antes de guardarse y enviarse; el
    aprobado se guarda una vez; el rechazado no se guarda ni se envía; la UI
    recibe el estado.
  - Verificación: `./gradlew test` con doble de moderación (C-02, C-03).
  - Archivos: `data/moderation/InlineTextModerator.kt`,
    `test/.../data/moderation/InlineTextModeratorTest.kt`

  - 27/11/2026: `InlineTextModerator` con 4 pruebas. Modera ANTES de guardar: al reves, un texto rechazado ya habria quedado en la base, con la ventana en la que alguien pudo leerlo. Una de las pruebas verifica justo ese orden. Si el servicio falla no se guarda (R-08).
- [ ] **T-005** — Moderación de texto diferida al sincronizar
  - Depende de: T-004, 005/T-009 (contrato de sincronización)
  - Aceptación: sin red, el texto se guarda marcado como pendiente; al
    sincronizar se modera antes de enviar; si se rechaza, queda marcado como
    rechazado, no se envía y el usuario puede corregirlo; hay prueba del ciclo
    completo.
  - Verificación: `./gradlew test` con red simulada y doble de moderación
    (C-04, C-05).
  - Archivos: `data/moderation/DeferredTextModerator.kt`,
    `data/moderation/ModerationState.kt`,
    `test/.../data/moderation/DeferredTextModeratorTest.kt`
  - Nota: depende de que 005 exponga el punto de sincronización; si 005 aún no
    está implementada, la tarea espera.

> **Punto de verificación F2:** texto aprobado se publica; rechazado no, ni en
> línea ni al sincronizar.

---

## Fase F3 — Archivos

- [x] **T-006** — Moderación de imagen antes de subir _(27/11/2026)_
  - Depende de: T-003, 001/T-012
  - Aceptación: la imagen se modera antes de la subida; el aprobado se sube una
    sola vez; el rechazado no se sube y el temporal se borra; sin red la
    entidad se guarda sin archivo y el archivo no se encola.
  - Verificación: `./gradlew test` e inspección de caché (C-06…C-08).
  - Archivos: `data/moderation/ImageModerator.kt`,
    `data/moderation/TemporaryFileStore.kt`,
    `test/.../data/moderation/ImageModeratorTest.kt`

  - 27/11/2026: `ImageModerator` + `TemporaryFileStore`, 5 pruebas. El temporal se borra en un
    `finally`: pase lo que pase —rechazo, fallo de red o excepcion a media subida— el archivo no se
    queda en la cache del telefono. El usuario cree que no se guardo nada y tiene que ser cierto
    tambien ahi. Sin red no se sube NI se encola: encolar un archivo sin moderar seria subirlo mas
    tarde sin revisar.
- [~] **T-007** — Moderación de video antes de subir _(FUERA DE ALCANCE · 27/11/2026)_
  - Depende de: T-006 · Bloqueada por: **Q-03**
  - Aceptación: el video sigue el mismo camino que la imagen, con los límites
    de formato, tamaño y duración acordados; sin veredicto no se sube.
  - Verificación: `./gradlew test` con doble de moderación de video (C-09).
  - Archivos: `data/moderation/VideoModerator.kt`,
    `test/.../data/moderation/VideoModeratorTest.kt`

> **Punto de verificación F3:** ningún archivo sin veredicto llega al servidor
> ni queda en caché.

---

## Fase F4 — Robustez

  - FUERA DE ALCANCE, no pendiente. La app no maneja video en ninguna pantalla. La spec 016 lo dice
    explicitamente: "Se envian texto y fotos. No hay audio, video, ubicacion ni archivos", y lo
    repite en su tabla de exclusiones. Esta spec nombra el video porque se escribio de forma
    generica ("texto, imagen o video"), pero esa capacidad nunca llego al producto.
    No se implementa: seria codigo para algo que la app no hace, y habria que mantenerlo. Si algun
    dia se agrega el video, `ImageModerator` ya tiene la forma a copiar y `ModerationGateway` ya
    declara `moderateVideo`, que hoy devuelve NotValidated — es decir, sin video no se sube nada,
    que es el comportamiento seguro.
- [ ] **T-008** — Fail-closed, timeout e idempotencia
  - Depende de: T-004, T-006 · Bloqueada por: **Q-04**
  - Aceptación: timeout, 5xx y respuesta ambigua se traducen a "no validado" y
    bloquean la publicación; cada solicitud lleva una clave de idempotencia
    derivada del contenido y del usuario; repetir no duplica la subida ni el
    cargo.
  - Verificación: `./gradlew test` con doble que falla, demora y repite
    (C-10).
  - Archivos: `data/moderation/ModerationRetryPolicy.kt`,
    `data/moderation/ModerationIdempotency.kt`,
    `test/.../data/moderation/ModerationRetryPolicyTest.kt`

- [x] **T-009** — Auditoría sin contenido _(27/11/2026)_
  - Depende de: T-003, 001/T-006
  - Aceptación: se registran tipo, resultado, duración e identificador opaco;
    no se registra texto, imagen, nombre de usuario ni puntaje; hay prueba que
    busca el contenido rechazado en la salida de registro.
  - Verificación: `./gradlew test` y búsqueda sobre el registro (C-11).
  - Archivos: `data/moderation/ModerationAuditLogger.kt`,
    `test/.../data/moderation/ModerationAuditLoggerTest.kt`
  - Hecho: `ModerationAuditLogger` escribe tipo, resultado, duracion y un identificador opaco (los
    primeros 12 caracteres del SHA-256 del contenido: sirve para correlacionar dos intentos del mismo
    texto, no se puede revertir). 5 pruebas; la principal busca el texto rechazado y cada una de sus
    palabras en la salida del registro y exige que no esten.
    Por que importa: un log con el contenido prohibido es una copia de ese contenido en un sitio
    menos protegido que la base de datos, y que a veces sale del telefono en un reporte de errores.
    Tampoco se registra el puntaje ni la categoria: con eso alguien podria calibrar un texto hasta
    pasar el filtro.

- [x] **T-010** — Verificación de llaves y configuración
  - Depende de: T-002
  - Aceptación: el APK y el repositorio no contienen llaves de Azure; la URL
    del endpoint sale de la configuración por entorno de 001; una prueba
    falla si aparece una llave en `BuildConfig` o en recursos.
  - Verificación: `./gradlew test` y `grep` de llaves (C-12).
  - Archivos: `test/.../security/NoCloudKeysTest.kt`,
    `build.gradle.kts`

> **Punto de verificación F4:** una caída del proveedor no publica nada y no
> hay secretos en la app.

---

## Fase F5 — Cierre

  - 27/11/2026: `NoCloudKeysTest`, 5 pruebas sobre los archivos reales: BuildConfig, recursos, codigo fuente y build.gradle.kts. Verifica que no hay llaves de Azure, que la app no llama directo a los servicios de nube (si lo hiciera necesitaria la llave) y que BASE_URL sale de la configuracion por entorno. Una llave dentro de un APK es publica: basta descomprimirlo.
- [ ] **T-011** — Matriz de pruebas de moderación
  - Depende de: T-005, T-007, T-008, T-009
  - Aceptación: una sola suite cubre texto en línea, texto diferido, imagen,
    video, rechazo, timeout y reintento; el doble cuenta las llamadas y
    demuestra que no hay atajos.
  - Verificación: `./gradlew test` con la matriz completa (C-01…C-13).
  - Archivos: `test/.../moderation/ModerationMatrixTest.kt`

- [ ] **T-012** — Cierre de la unidad
  - Depende de: T-010, T-011
  - Aceptación: las capas están limpias; no hay contenido en registros; el
    mensaje es único; `memoria.md` queda actualizada y la evidencia escrita.
  - Verificación: `./gradlew ktlintCheck detekt test`, skill `revisar-capas`
    y revisión de evidencia.
  - Archivos: `docs/evidencia-004.md`, `memoria.md`

> **Punto de verificación F5:** unidad cerrada con la matriz en verde y sin
> secretos.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Contrato | T-001 … T-003 | Modelos, cliente y veredicto |
| F2 Texto | T-004 … T-005 | En línea y diferido |
| F3 Archivos | T-006 … T-007 | Imagen y video |
| F4 Robustez | T-008 … T-010 | Fail-closed, auditoría y llaves |
| F5 Cierre | T-011 … T-012 | Matriz y evidencia |

**Tareas bloqueadas por preguntas abiertas:** T-002 (Q-01), T-007 (Q-03),
T-008 (Q-04), T-003 (Q-02, Q-08), T-009 (Q-09), T-005 (Q-10) y T-011 (Q-05,
Q-06, Q-07).

Esas preguntas deben resolverse con el responsable del backend y con el equipo
antes de ejecutar las tareas correspondientes.
