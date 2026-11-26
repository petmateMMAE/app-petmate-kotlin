# Tareas 016 — Chat de seguimiento

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **2 / 13 tareas** (2 verificadas, al 26/11/2026)

---

## Fase F1 — Dominio

- [x] **T-001** — Modelos del mensaje y la conversación
  - Depende de: —
  - Aceptación: `Message` (autor, contenido, fecha, estado), `MessageContent`
    como tipo cerrado (texto o imagen), `MessageStatus` (pendiente, enviado,
    entregado, fallido) y `Conversation` (seguimiento, participantes, no
    leídos) en `domain/model`, en Kotlin puro.
  - Verificación: `./gradlew test`; `grep` sobre `domain/` sin framework.
  - Archivos: `domain/model/Message.kt`, `domain/model/MessageContent.kt`,
    `domain/model/Conversation.kt`

- [x] **T-002** — Contrato del repositorio de chat
  - Depende de: T-001
  - Aceptación: `ChatRepository` declara observar la conversación paginada,
    enviar mensaje, marcar como leídos y consultar el contador; devuelve
    `Flow` y `AppResult`.
  - Verificación: `./gradlew test` con doble del contrato.
  - Archivos: `domain/repository/ChatRepository.kt`

> **Verificación F1:** modelos y contrato listos.

---

## Fase F2 — Datos

- [ ] **T-003** — DTOs, servicio remoto y mappers
  - Depende de: T-002 · Bloqueada por: **Q-01, Q-02**
  - Aceptación: DTOs de mensaje y conversación con su serialización;
    `ChatApiService` declara listar paginado y enviar; mappers en ambos
    sentidos; el orden lo fija la fecha del servidor.
  - Verificación: `./gradlew test` con `MockWebServer` y prueba de ordenación.
  - Archivos: `data/remote/dto/MessageDto.kt`,
    `data/remote/ChatApiService.kt`, `data/mapper/MessageMapper.kt`,
    `test/.../data/mapper/MessageMapperTest.kt`

- [ ] **T-004** — Historial local paginado
  - Depende de: T-001 · Bloqueada por: **Q-02** (estrategia de paginación)
  - Aceptación: `MessageEntity` y su DAO guardan el historial por conversación
    y lo exponen paginado como `Flow`; **la entidad guarda la referencia de la
    imagen, no la imagen** (D-05); índice por conversación y fecha.
  - Verificación: `./gradlew connectedAndroidTest` con base en memoria y
    doscientos mensajes sembrados.
  - Archivos: `data/local/entity/MessageEntity.kt`,
    `data/local/dao/MessageDao.kt`,
    `androidTest/.../data/local/MessageDaoTest.kt`
  - Nota: cambiar el esquema de Room requiere autorización.

> **Verificación F2:** historial paginado; sin imágenes embebidas en la base.

---

## Fase F3 — Repositorio y casos de uso

- [ ] **T-005** — `ChatRepositoryImpl` con lectura offline
  - Depende de: T-003, T-004
  - Aceptación: la UI observa Room y la red lo refresca; sin conexión el
    historial se lee de lo guardado; un error de red no borra lo local.
  - Verificación: `./gradlew test` con red presente y ausente (C-10).
  - Archivos: `data/repository/ChatRepositoryImpl.kt`, `di/ChatModule.kt`,
    `test/.../data/repository/ChatRepositoryImplTest.kt`

- [ ] **T-006** — Envío con moderación y cola
  - Depende de: T-005 · Bloqueada por: **Q-04** (límite de fotos)
  - Aceptación: `SendMessageUseCase` **modera antes de encolar** (D-03); si el
    contenido se rechaza, no se encola y se explica; sin conexión el mensaje
    entra a la cola de 005 con clave de idempotencia y se marca pendiente; al
    volver la red se envía sin duplicarse.
  - Verificación: `./gradlew test` con: moderación aprobando, moderación
    rechazando, sin red y reintento (C-04, C-11, C-12).
  - Archivos: `domain/usecase/SendMessageUseCase.kt`,
    `test/.../domain/usecase/SendMessageUseCaseTest.kt`

- [ ] **T-007** — Leídos y contador
  - Depende de: T-005
  - Aceptación: `MarkAsReadUseCase` marca leídos al abrir la conversación y
    actualiza el contador; **el contador tiene un solo origen**, consumido
    también por el panel de 012 (RT-04).
  - Verificación: `./gradlew test` comprobando el contador antes y después
    (C-06) y una prueba comparativa con el valor que lee 012 (C-07).
  - Archivos: `domain/usecase/MarkAsReadUseCase.kt`,
    `domain/usecase/ObserveConversationUseCase.kt`, `test/.../domain/usecase/`

> **Verificación F3:** envío con y sin red, sin duplicados; contador único.

---

## Fase F4 — Conversación

- [ ] **T-008** — ViewModel y pantalla del chat
  - Depende de: T-006, T-007
  - Aceptación: `ChatViewModel` expone un `StateFlow` con los mensajes
    paginados, el estado de envío y si se puede escribir; `ChatScreen` lista
    los mensajes con **el más reciente abajo** y mantiene la posición al
    cargar más hacia arriba.
  - Verificación: `./gradlew test` con Turbine y
    `./gradlew connectedAndroidTest` con mensajes de fechas distintas (C-02,
    C-05).
  - Archivos: `ui/chat/ChatViewModel.kt`, `ui/chat/ChatUiState.kt`,
    `ui/chat/ChatScreen.kt`, `test/.../ui/chat/ChatViewModelTest.kt`

- [ ] **T-009** — Burbuja con estado del mensaje
  - Depende de: T-008
  - Aceptación: `MessageBubble` distingue mensajes propios y ajenos y muestra
    el estado (pendiente, enviado, fallido) con un indicador comprensible; un
    mensaje fallido ofrece reintentar.
  - Verificación: `./gradlew connectedAndroidTest` con los cuatro estados.
  - Archivos: `ui/chat/MessageBubble.kt`,
    `androidTest/.../ui/chat/MessageBubbleTest.kt`

- [ ] **T-010** — Rendimiento con historial largo
  - Depende de: T-009
  - Aceptación: una conversación con doscientos mensajes se desplaza sin
    saltos perceptibles; las imágenes se cargan perezosamente.
  - Verificación: `./gradlew connectedAndroidTest` con lista larga y medición
    de cuadros perdidos (C-16).
  - Archivos: `ui/chat/ChatScreen.kt`,
    `androidTest/.../ui/chat/ChatPerformanceTest.kt`

> **Verificación F4:** conversación ordenada, con estados visibles y fluida.

---

## Fase F5 — Fotos

- [ ] **T-011** — Adjuntar fotos moderadas
  - Depende de: T-010 · Bloqueada por: **Q-04**
  - Aceptación: se puede adjuntar una foto desde cámara o galería pidiendo el
    permiso según 003; la foto pasa por moderación antes de subirse; una foto
    rechazada no se envía y se explica sin perder el resto del mensaje; las
    fotos se sirven del CDN y la caché respeta su límite.
  - Verificación: `./gradlew connectedAndroidTest` con moderación aprobando y
    rechazando (C-03, C-04).
  - Archivos: `ui/chat/AttachPhotoAction.kt`,
    `androidTest/.../ui/chat/AttachPhotoTest.kt`

> **Verificación F5:** solo se envían fotos aprobadas.

---

## Fase F6 — Integración y cierre

- [ ] **T-012** — Botón flotante, advertencia y push
  - Depende de: T-011
  - Aceptación: `UnreadBadge` muestra el contador donde lo pide la maqueta;
    `ChatDisclaimer` advierte que el chat no sustituye una consulta y es
    visible al abrir, contraíble pero no descartable (D-07); un mensaje nuevo
    genera una notificación **sin exponer su contenido** en el payload.
  - Verificación: `./gradlew connectedAndroidTest` (C-15) y prueba del payload
    del push (C-08).
  - Archivos: `ui/chat/UnreadBadge.kt`, `ui/chat/ChatDisclaimer.kt`,
    `androidTest/.../ui/chat/`

- [ ] **T-013** — Cierre al dar de alta y cierre de la unidad
  - Depende de: T-012
  - Aceptación: al pasar el seguimiento a Dado de alta, el chat queda de solo
    lectura y conserva el historial; el backend también rechaza escrituras
    (D-06); un usuario que no participa no puede leer la conversación; tras
    cerrar sesión el contenido no es accesible; C-01 a C-16 comprobados;
    `revisar-capas` sin violaciones; revisión del agente `seguridad` sin
    hallazgos críticos ni altos; `memoria.md` actualizada.
  - Verificación: `./gradlew ktlintCheck detekt test`, prueba tras dar de alta
    (C-09), prueba con sesión de tercero (C-13), prueba de cambio de sesión
    (C-14) y reporte del agente `seguridad`.
  - Archivos: `memoria.md`

> **Verificación F6:** chat cerrado correctamente; sin fugas entre usuarios.

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Dominio | T-001, T-002 | Modelos y contrato |
| F2 Datos | T-003, T-004 | Transporte e historial paginado |
| F3 Repositorio | T-005 … T-007 | Envío, cola, leídos y contador |
| F4 Conversación | T-008 … T-010 | Historial, estados y rendimiento |
| F5 Fotos | T-011 | Adjuntos moderados |
| F6 Integración | T-012, T-013 | Contador, advertencia, push y cierre |

**Bloqueadas:** T-003 (Q-01, Q-02), T-004 (Q-02), T-006 (Q-04), T-011 (Q-04).

**Nota:** Q-06 (cifrado de extremo a extremo) no bloquea ninguna tarea pero sí
cambia la arquitectura si el profesor lo exige. Conviene resolverla antes de
empezar la unidad, no durante.
