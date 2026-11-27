# Spec 016 — Chat de seguimiento

> Unidad de trabajo: `016-chat-seguimiento`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026
> Alineada con [`constitution.md`](../../constitution.md) y
> [`MAPA-CAPACIDADES.md`](../MAPA-CAPACIDADES.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. El chat pertenece a un seguimiento (015), no a una mascota ni a un
   usuario. Un seguimiento, una conversación.
2. Participan dos partes: el dueño de la mascota y el negocio que la atiende.
   No es un chat de grupo.
3. El chat se CIERRA al dar de alta (015). Cerrado significa de solo lectura:
   el historial se conserva pero ya no se puede escribir.
4. Se envían texto y fotos. No hay audio, video, ubicación ni archivos.
5. Las fotos pasan por la moderación de 004 antes de subirse.
6. El botón flotante con el contador de no leídos aparece donde la maqueta lo
   pide; el conteo alimenta también el panel de 012.
7. Los mensajes llegan por push (006). No se implementa conexión persistente
   ni websockets: se consulta al abrir y se refresca al recibir el aviso.
8. SUPUESTO CRÍTICO: el chat NO es un canal de consulta médica. Es para dar
   seguimiento a un tratamiento ya diagnosticado. Esto se advierte al usuario.
9. Enviar un mensaje sin conexión lo deja en la cola de 005 y se marca como
   pendiente; el historial se lee de Room.
10. Los mensajes se conservan mientras el seguimiento sea visible (015, Q-06).
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Dar al dueño y al negocio un **canal directo durante un tratamiento**, para
resolver dudas, mandar fotos de cómo va la herida y confirmar que el animal
está respondiendo, sin llamadas ni mensajes por fuera de la app.

**Por qué:** entre la consulta y la revisión pasan días en los que el dueño
tiene preguntas y la veterinaria necesita saber cómo evoluciona el animal. Hoy
eso ocurre por teléfono personal o no ocurre. Dentro de PetMate queda ligado
al seguimiento y al expediente.

**Para quién:** el dueño con una mascota en tratamiento, y la veterinaria que
la está tratando.

**Cómo se ve el éxito:** el dueño manda una foto de la herida un martes, la
veterinaria responde en el día, y ambos ven esa conversación junto al
seguimiento sin buscarla en otro lado.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Conversación ligada a un seguimiento |
| A-02 | Envío y recepción de mensajes de texto |
| A-03 | Envío de fotos, moderadas antes de subir |
| A-04 | Historial de la conversación, lo más reciente abajo |
| A-05 | Marcado de leído y contador de no leídos |
| A-06 | Botón flotante con el contador |
| A-07 | Aviso por notificación push de mensaje nuevo |
| A-08 | Cierre del chat al dar de alta, en modo solo lectura |
| A-09 | Lectura del historial sin conexión |
| A-10 | Envío sin conexión encolado y marcado como pendiente |
| A-11 | Advertencia de que el chat no sustituye una consulta |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Seguimiento y sus estados | Spec 015 |
| Consulta, receta y cobro | Spec 014 |
| Envío de las notificaciones | Spec 006 |
| Moderación en sí | Spec 004 |
| Audio, video, ubicación o archivos | No está en la maqueta |
| Llamadas o videollamadas | No está en la maqueta |
| Chat sin un seguimiento abierto | Fuera: el chat pertenece al seguimiento |
| Chat entre dueños, o con PetMate como soporte | No está en la maqueta |
| Borrar o editar un mensaje enviado | Pregunta abierta Q-05 |
| Cifrado de extremo a extremo | Pregunta abierta Q-06 |

---

## 4. Requisitos

### Funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El dueño y el negocio deben poder intercambiar mensajes de texto | Maqueta |
| R-02 | Los participantes deben poder enviar fotos | Maqueta |
| R-03 | Las fotos deben moderarse antes de almacenarse | Punto 16; spec 004 |
| R-04 | El sistema debe mostrar el número de mensajes sin leer | Maqueta |
| R-05 | El sistema debe avisar por notificación de un mensaje nuevo | Maqueta; punto 16 |
| R-06 | El chat debe cerrarse al dar de alta y quedar de solo lectura | Maqueta |
| R-07 | El historial debe poder leerse sin conexión | Punto 16 |
| R-08 | Un mensaje enviado sin conexión debe marcarse como pendiente y enviarse al recuperar la red | Punto 16; spec 005 |
| R-09 | El sistema debe advertir que el chat no sustituye una consulta | Seguridad del usuario |

### No funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-10 | Solo los dos participantes deben poder leer la conversación | RNF-01 |
| R-11 | El contenido de los mensajes no debe aparecer en el payload del push | RNF-01; spec 006 |
| R-12 | El historial debe desplazarse sin saltos con cientos de mensajes | RNF-16 |
| R-13 | Ninguna operación de entrada/salida en el hilo principal | RNF-15 |
| R-14 | El contenido del chat no debe quedar accesible tras cerrar sesión | RNF-01 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | El chat se abre desde el seguimiento y muestra su historial | Prueba de navegación con doble de backend |
| C-02 | Un mensaje de texto enviado aparece en la conversación de ambos | Prueba con dos sesiones simuladas |
| C-03 | Una foto aprobada por moderación se envía y se muestra | Prueba con doble de moderación aprobando |
| C-04 | Una foto rechazada **no se envía** y se explica al usuario | Prueba con doble de moderación rechazando |
| C-05 | Los mensajes se ordenan con el más reciente abajo | Prueba con varios mensajes de fechas distintas |
| C-06 | Abrir el chat marca como leídos los mensajes recibidos | Prueba del contador antes y después |
| C-07 | El contador de no leídos coincide con el que muestra el panel de 012 | Prueba comparativa |
| C-08 | Un mensaje nuevo genera una notificación sin exponer su contenido | Prueba del payload del push |
| C-09 | Al dar de alta, el chat queda de solo lectura y el historial se conserva | Prueba tras cambiar el estado a Dado de alta |
| C-10 | Sin conexión, el historial se lee de lo guardado | Prueba con red ausente |
| C-11 | Un mensaje enviado sin conexión se marca pendiente y se envía al volver la red | Prueba con red ausente y luego restaurada |
| C-12 | Un mensaje pendiente no se duplica al reintentarse | Prueba de reintento con clave de idempotencia |
| C-13 | Un tercero que no participa no puede leer la conversación | Prueba con sesión de otro usuario |
| C-14 | Tras cerrar sesión, el contenido del chat no es accesible | Prueba de cambio de sesión |
| C-15 | La advertencia de que el chat no sustituye una consulta es visible al abrirlo | Prueba de interfaz |
| C-16 | Una conversación con doscientos mensajes se desplaza sin saltos | Prueba de rendimiento con lista larga |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El chat se usa como consulta médica y alguien actúa sobre un consejo mal dado | **Alto** | R-09: advertencia visible al abrir; el chat se liga a un tratamiento ya diagnosticado |
| G-02 | El contenido del mensaje viaja en el payload del push y se ve en la pantalla de bloqueo | Alto | R-11 y la regla de 006: texto genérico, el detalle se carga al abrir |
| G-03 | Un usuario ajeno accede a la conversación | Alto | La autorización la valida el backend por participante; C-13 |
| G-04 | El contrato del backend no está definido | Alto | Dobles de prueba; rutas y campos a Q-01 |
| G-05 | Mensajes duplicados al reintentar un envío encolado | Medio | Clave de idempotencia por mensaje; C-12 |
| G-06 | Las fotos del chat llenan el almacenamiento del dispositivo | Medio | Caché de imágenes con límite; las fotos se sirven del CDN, no se guardan todas |
| G-07 | Sin conexión persistente, los mensajes tardan en aparecer | Medio | Refresco al abrir y al recibir el push; se documenta la expectativa en Q-03 |
| G-08 | El chat de un seguimiento cerrado sigue recibiendo mensajes | Medio | R-06: el backend rechaza escrituras en un chat cerrado; C-09 |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: red, Room, caché de imágenes, navegación.
- `002-identidad-y-sesion`: sesión y participante.
- `003-permisos`: cámara y fotos para adjuntar imágenes.
- `004-moderacion-contenido`: fotos y texto antes de enviarse.
- `005-sincronizacion-offline`: cola de mensajes pendientes.
- `006-notificaciones-push`: aviso de mensaje nuevo.
- `015-seguimiento-tratamiento`: el chat pertenece a un seguimiento y se
  cierra con él.

**Externas:**

- Contrato del backend de mensajería (Q-01).

**Unidades posteriores que dependen de esta:**

- `012` (consume el contador de no leídos).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuáles son las rutas, los cuerpos y las respuestas para listar y enviar mensajes? | Miguel (backend) | Tareas de red |
| Q-02 | ¿Cómo se paginan los mensajes? ¿Por fecha o por cursor? | Miguel (backend) | Tareas de historial |
| Q-03 | ¿Con qué frecuencia se refresca el chat si el push no llega? | Equipo | Riesgo G-07 |
| Q-04 | ¿Hay límite de tamaño o cantidad de fotos por mensaje? | Equipo | Tareas de adjuntos |
| Q-05 | ¿Se puede borrar o editar un mensaje enviado? | Equipo | Alcance |
| Q-06 | ¿Se requiere cifrado de extremo a extremo, o basta HTTPS y el control del backend? | Profesor / equipo | Arquitectura del chat |
| Q-07 | ¿El negocio puede iniciar el chat antes de que el dueño escriba? | Equipo | Flujo inicial |

---

## 9. Puerta de aprobación

- [ ] Alcance y fuera de alcance revisados.
- [ ] **Supuesto 8 confirmado**: el chat no es canal de consulta médica y se
      advierte al usuario.
- [ ] Preguntas abiertas con dueño, en especial Q-06.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — conversations (get-all, mark-read, reopen) y messages (get-all, send, send-photo, get-photo-url).
- **Q-02 RESUELTA (27/11/2026)** — Por CURSOR. MessageResponse trae un campo seq que sirve de cursor estable; no pagines por fecha.
