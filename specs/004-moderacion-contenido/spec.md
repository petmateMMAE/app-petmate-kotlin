# Spec 004 — Moderación de contenido

> Unidad de trabajo: `004-moderacion-contenido`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0
> Depende de: [`001-arquitectura-base`](../001-arquitectura-base/spec.md) y
> [`002-identidad-y-sesion`](../002-identidad-y-sesion/spec.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:

1. El punto de intercepción ya existe en 001/K-30. Esta unidad define su
   política, su contrato y su comportamiento; no crea un segundo punto.

2. El proveedor es la moderación de contenido de la nube de Azure, como fija
   la constitución. Categorías, umbrales, idioma y política exacta van a
   preguntas abiertas: la spec no los inventa.

3. La app NO llama a Azure directamente ni guarda su llave. Llama a un
   endpoint del backend y el backend llama a Azure. La llave nunca vive en el
   dispositivo (P6). Si alguien propone llamada directa, la respuesta es un
   token efímero emitido por el backend, nunca una llave estática.

4. El TEXTO que escribe el usuario (nombre de mascota, notas) sí se guarda sin
   conexión y se modera al sincronizar, antes de enviarlo al servidor. Si se
   rechaza en ese momento, el registro queda marcado como "rechazado" y el
   usuario lo corrige. Esto respeta offline-first.

5. Las IMÁGENES y los VIDEOS no se suben sin moderar. La entidad se guarda sin
   el archivo y el archivo se sube después, cuando haya red y la moderación lo
   apruebe.

6. La diferencia del punto 4 y el 5 es deliberada: el texto sin publicar no
   expone a nadie; una imagen sin moderar que se sube sí. El riesgo está en la
   publicación, no en el guardado local.

7. Esto no rompe P13: el lineamiento exige moderar antes de almacenar en el
   servidor, y eso se cumple en ambos casos.

8. Si el servicio de moderación falla, no responde o tarda demasiado, la
   operación se rechaza (fail-closed). No se aprueba por defecto.

9. La app solo aplica el veredicto del servidor: aprobado o rechazado. No
   calcula puntajes ni decide umbrales en el dispositivo.

10. "Revisión humana" del proveedor se trata como rechazo temporal con un
    mensaje de "no se pudo validar, intenta más tarde".

11. Fuera de alcance: las pantallas que suben contenido (007, 016) y el
    backend que habla con Azure (otro repositorio).

→ Corrígeme ahora o avanzo con esto.
```

---

## 1. Objetivo

Que **todo contenido aportado por el usuario —texto, imagen o video— pase por
un único punto de moderación antes de publicarse en el servidor**, que lo
rechazado no se almacene ni se envíe, y que el usuario reciba un mensaje claro
sin filtrar detalles internos ni datos sensibles.

Esta unidad convierte el punto de intercepción vacío que dejó 001 en una
política real, con un contrato único que 007 y 016 consumen sin volver a
decidir.

**Para quién:** el dueño que registra una mascota o escribe en el chat, y el
afiliado que sube contenido en su ficha.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Punto único de moderación para texto, imagen y video |
| A-02 | Contrato de solicitud y veredicto, con estados explícitos |
| A-03 | Moderación de texto en línea antes de enviarlo |
| A-04 | Moderación de texto diferida al sincronizar, con marca de rechazo |
| A-05 | Moderación de imagen antes de subirla |
| A-06 | Moderación de video antes de subirlo |
| A-07 | Rechazo con mensaje claro y sin almacenamiento del contenido |
| A-08 | Comportamiento fail-closed ante fallo o demora del servicio |
| A-09 | Registro de auditoría sin contenido ni datos personales |
| A-10 | Reintento controlado y estados visibles para la UI |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Pantalla de alta y edición de mascota con foto | Spec 007 |
| Fotos y texto del chat | Spec 016 |
| Ficha y servicios del negocio | Spec 011 |
| Panel de revisión humana del proveedor | Fuera de la app; consola de Azure |
| Implementación del backend que llama a Azure | Repositorio `backend-petmate-nosdd-aspnetcore` |
| Texto legal sobre moderación | Spec 017 |
| Moderación de contenido que ya existe en el servidor | Proceso del backend, no de la app |

**Ninguna regla de negocio de mascotas, mapa o afiliación se escribe aquí.**

---

## 4. Requisitos

### Funcionales de moderación

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | Todo texto, imagen o video aportado por el usuario debe pasar por el punto único antes de publicarse | Constitución P13; 001 R-27 |
| R-02 | El texto debe moderarse en línea si hay red y diferirse al sincronizar si no la hay | Constitución P4 y P13 |
| R-03 | Un texto rechazado al sincronizar no debe enviarse y debe quedar marcado para que el usuario lo corrija | Constitución P13 |
| R-04 | Una imagen o un video no deben subirse sin veredicto de aprobación | Constitución P13 |
| R-05 | Una entidad con imagen o video rechazado debe poder guardarse sin el archivo, conservando el resto de sus datos | Constitución P4 y P13 |
| R-06 | El contenido rechazado no debe persistirse en el servidor ni quedar en la caché local | Constitución P13 |
| R-07 | El mensaje de rechazo debe ser claro y no revelar categorías internas, puntajes ni el proveedor | RNF-01, RNF-16 |
| R-08 | Ante fallo, timeout o indisponibilidad del servicio, la operación debe rechazarse, no aprobarse | Constitución P13 |
| R-09 | La app no debe contener llaves del proveedor ni llamar a Azure directamente | Constitución P6 |
| R-10 | La app solo debe aplicar el veredicto del servidor; no calcula umbrales propios | Constitución P13 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-11 | El contenido moderado no debe aparecer en registros, mensajes de error ni reportes de fallo | RNF-01, RNF-32 |
| R-12 | La moderación no debe bloquear el hilo principal; la UI muestra estado de espera | RNF-15 |
| R-13 | Un reintento de moderación no debe duplicar la publicación ni generar cargos dobles | RNF-15 |
| R-14 | El punto de moderación debe ser único y verificable por arquitectura, no por convención | Constitución P13 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Toda escritura de contenido de usuario pasa por el gateway; ninguna lo evita | Prueba con doble que cuenta llamadas y revisión de dependencias |
| C-02 | Un texto aprobado en línea se guarda y se envía una sola vez | Prueba con doble de moderación aprobando |
| C-03 | Un texto rechazado en línea no se guarda ni se envía | Prueba con doble que rechaza |
| C-04 | Sin red, el texto se guarda localmente y queda pendiente de moderación | Prueba con red simulada como ausente |
| C-05 | Al sincronizar, un texto rechazado queda marcado como rechazado, no se envía y el usuario puede corregirlo | Prueba del flujo diferido |
| C-06 | Una imagen aprobada se sube una sola vez | Prueba con doble de moderación y de subida |
| C-07 | Una imagen rechazada no se sube y no queda en la caché local | Prueba con doble que rechaza e inspección de la caché |
| C-08 | Sin red, la entidad se guarda sin la imagen y el archivo no se encola | Prueba con red ausente |
| C-09 | Un video se comporta igual que una imagen: sin veredicto no se sube | Prueba con doble de moderación de video |
| C-10 | Si el servicio falla o tarda más del tiempo acordado, la operación se rechaza | Prueba con doble que falla y con demora simulada |
| C-11 | Ni el contenido rechazado ni los puntajes aparecen en los registros | Búsqueda sobre la salida de registro de una sesión completa |
| C-12 | La app no contiene ninguna llave de Azure y la petición sale hacia el backend | `grep` de llaves y revisión del cliente |
| C-13 | El mensaje visible es el mismo para todas las categorías de rechazo | Prueba con dos categorías distintas y comparación de mensajes |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El contrato del endpoint de moderación no está definido | Alto | Todo se prueba con dobles; rutas y campos van a Q-01 y no se inventan |
| G-02 | El servicio de moderación se cae y bloquea toda publicación | Alto | Fail-closed con mensaje de reintento; Q-04 define el tiempo de espera |
| G-03 | Un texto guardado sin conexión se publica sin moderar por un error de orden | Alto | La sincronización exige veredicto antes de enviar; prueba C-05 |
| G-04 | Una imagen rechazada queda en la caché y se filtra después | Medio | Se borra el archivo temporal al rechazar; prueba C-07 |
| G-05 | El mensaje de rechazo revela la categoría o el proveedor | Medio | Mensaje único y genérico; prueba C-13 |
| G-06 | Los umbrales cambian y el contenido ya aprobado deja de ser válido | Medio | La política vive en el servidor; la app no guarda umbrales |
| G-07 | Los reintentos generan cargos dobles en el proveedor | Medio | Clave de idempotencia por contenido; prueba C-06 |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: `001/K-30` (punto de intercepción), `001/K-11`
  (cliente HTTP), `001/K-14` (errores), `001/K-04` y `001/K-05` (resultado y
  texto), `001/K-06` (logger) y `001/K-21` (pruebas).
- `002-identidad-y-sesion`: sesión válida para llamar al backend y errores
  cerrados de red.

**Externas:**

- Contrato del endpoint de moderación del backend (Q-01).
- Categorías, umbrales e idioma que aplica Azure (Q-02).
- Formatos, tamaños y duración máxima de archivos (Q-03).

**Unidades posteriores que dependen de esta:**

- `007` (foto de mascota), `011` (ficha del negocio), `016` (chat) y cualquier
  unidad que suba contenido.

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuáles son la ruta, el cuerpo, la respuesta y los códigos del endpoint de moderación del backend? | Miguel (backend) | Todas las tareas de red |
| Q-02 | ¿Qué categorías, umbrales e idioma aplica Azure? ¿El veredicto llega como aprobado/rechazado o con puntajes? | Miguel (backend) / Adrián (nube) | Regla de decisión |
| Q-03 | ¿Qué formatos, tamaños y duración máxima se aceptan para imagen y video? | Equipo / backend | Moderación de archivos |
| Q-04 | ¿Cuál es el tiempo máximo de espera y cuántos reintentos se permiten antes de rechazar? | Equipo | Fail-closed |
| Q-05 | ¿En qué idioma se escribe el contenido y el proveedor lo detecta solo? | Equipo | Umbrales |
| Q-06 | ¿Se admite "revisión humana"? ¿Quién la atiende y en qué plazo? | Equipo / backend | Flujo de rechazo temporal |
| Q-07 | ¿Cuál es la cuota y el costo por llamada del proveedor? | Adrián (nube) | Reintentos y caché |
| Q-08 | ¿El mensaje de rechazo lo redacta el equipo o viene del backend? | Equipo | Textos |
| Q-09 | ¿Cuánto tiempo se conserva el registro de auditoría de moderación? | Equipo / privacidad | Registro |
| Q-10 | ¿La misma política aplica a nombres de mascota, notas, chat y datos de negocio, o hay reglas por tipo? | Equipo | Alcance del gateway |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El alcance y el fuera de alcance están revisados.
- [ ] La decisión de texto diferido y archivos bloqueados está aceptada.
- [ ] Los requisitos y los criterios de éxito son suficientes para planificar.
- [ ] Las preguntas abiertas están identificadas y se sabe a quién preguntar.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — NO existe endpoint de moderacion en el backend. Por eso NoOpModerationGateway devolviendo NotValidated es lo correcto: no hay contra que programar.
