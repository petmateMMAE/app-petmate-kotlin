# Spec 005 — Sincronización offline

> Unidad de trabajo: `005-sincronizacion-offline`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0
> Depende de: [`001-arquitectura-base`](../001-arquitectura-base/spec.md) y
> [`002-identidad-y-sesion`](../002-identidad-y-sesion/spec.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:

1. La cola persistente y el observador de conectividad ya existen en 001/K-29
   y 001/K-28. Esta unidad define la política por entidad, la reconciliación y
   los conflictos; no reimplementa la cola.

2. Las entidades cubiertas son la mascota (007) y los registros del carnet:
   vacuna, desparasitación, peso, alergia y condición (008). La política queda
   declarada para esas entidades desde ahora, aunque 007 y 008 se construyan
   después. Una entidad nueva se agrega ampliando esta unidad con aprobación;
   la unidad que la consume no inventa su política.

3. Cada entidad declara cuatro cosas: si se puede crear sin conexión, si se
   puede editar, si se puede borrar y qué pasa en un conflicto.

4. Los ids de las creaciones offline los genera el cliente y el servidor los
   acepta para no duplicar. Si el backend exige id propio, la creación offline
   se deshabilita y se documenta.

5. Orden: FIFO por entidad y respeto de dependencias. Una vacuna no se envía
   antes que su mascota.

6. Reintento con backoff para fallos transitorios; los 4xx no se reintentan y
   se marcan como error permanente visible.

7. Deduplicación por clave de idempotencia: reintentar nunca duplica.

8. Conflicto por defecto, ya aprobado: en datos clínicos gana el servidor y se
   avisa al usuario; en datos de la mascota gana el cambio más reciente.

9. El borrado offline usa lápida: lo borrado no reaparece al sincronizar.

10. La UI muestra por elemento si está pendiente, sincronizando o con error, y
    permite reintentar a mano.

11. Sin conexión todo se lee de Room; la app nunca bloquea la lectura por estar
    offline.

12. Fuera de alcance: la moderación de contenido (004), la recepción de push
    (006) y la política de entidades que aún no existen (014, 016).

→ Corrígeme ahora o avanzo con esto.
```

---

## 1. Objetivo

Que el usuario **pueda crear, editar y borrar sus datos sin conexión, verlos
de inmediato y tener la certeza de que se sincronizarán al volver la red**, sin
pérdidas, sin duplicados y con una regla clara cuando el servidor y el
dispositivo tienen versiones distintas.

Esta unidad define la política transversal que 007 y 008 aplican. No es una
pantalla: es el contrato que hace que el carnet funcione en la veterinaria sin
señal.

**Para quién:** el dueño que registra datos sin internet y el equipo que
construye las entidades.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Política de sincronización declarada por entidad |
| A-02 | Cola persistente que sobrevive a la muerte del proceso |
| A-03 | Orden por dependencias entre entidades |
| A-04 | Claves de idempotencia para no duplicar |
| A-05 | Reintento con backoff y errores permanentes separados |
| A-06 | Reconciliación de ids y de versiones |
| A-07 | Resolución de conflictos según la entidad |
| A-08 | Borrado offline con lápida |
| A-09 | Estado de sincronización visible y reintento manual |
| A-10 | Integración con la moderación diferida de texto (004) |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Pantallas de mascotas y carnet | Specs 007 y 008 |
| Moderación de texto, imagen y video | Spec 004 |
| Sincronización de chat en tiempo real | Spec 016 |
| Sincronización de consultas y cobros | Spec 014 |
| Resolución de conflictos de negocio entre usuarios | Specs 012 y 014 |
| Sincronización en segundo plano con `WorkManager` | No se exige en los lineamientos; se evalúa en Q-09 |
| Edición colaborativa en vivo | No está en la maqueta |

**Ninguna regla clínica se escribe aquí; solo la política de sincronización.**

---

## 4. Requisitos

### Funcionales de sincronización

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | Toda escritura hecha sin conexión debe encolarse de forma persistente | Constitución P4; 001 R-29 |
| R-02 | La cola debe sobrevivir al cierre de la app y a la muerte del proceso | Constitución P4 |
| R-03 | Una entidad dependiente no debe enviarse antes que su entidad padre | Constitución P4 |
| R-04 | Reintentar una escritura no debe duplicarla en el servidor | RNF-15 |
| R-05 | Los errores transitorios deben reintentarse con backoff; los 4xx no | RNF-15, RNF-16 |
| R-06 | Un error permanente debe quedar visible y permitir corrección o descarte | RNF-16 |
| R-07 | En datos clínicos, un conflicto debe resolverse a favor del servidor y avisarse | Constitución P4 |
| R-08 | En datos de la mascota, un conflicto debe resolverse a favor del cambio más reciente | Constitución P4 |
| R-09 | Un borrado offline no debe reaparecer al sincronizar | Constitución P4 |
| R-10 | La UI debe mostrar el estado de sincronización por elemento y permitir reintentar | RNF-16 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-11 | La sincronización no debe ejecutarse en el hilo principal | RNF-15 |
| R-12 | La cola no debe contener credenciales ni contenido sin moderar que viole P13 | Constitución P6 y P13 |
| R-13 | La política por entidad debe vivir en un solo lugar y no repetirse en cada repositorio | Constitución P3 |
| R-14 | La lectura debe seguir viniendo de Room; la sincronización no debe ser requisito para mostrar datos | Constitución P4; 001 R-02 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Crear una entidad de prueba (con la forma de una mascota) sin red la deja visible y marcada como pendiente | Prueba con red simulada como ausente |
| C-02 | Al reconectar, la entidad de prueba se envía una sola vez y su id local se reconcilia con el del servidor | Prueba del ciclo completo con doble de API |
| C-03 | Reintentar la misma escritura diez veces produce un solo registro en el servidor | Prueba de idempotencia con contador |
| C-04 | Una vacuna no se envía antes que su mascota | Prueba del orden por dependencias |
| C-05 | Un 4xx no se reintenta, se marca como error permanente y se avisa | Prueba con respuesta 400 |
| C-06 | Un conflicto en una vacuna deja la versión del servidor y muestra el aviso | Prueba con dos versiones distintas |
| C-07 | Un conflicto en los datos de la mascota deja la versión más reciente | Prueba con reloj fijo y dos versiones |
| C-08 | Una mascota borrada sin red no reaparece al sincronizar | Prueba de lápida |
| C-09 | La cola pendiente sobrevive a la muerte del proceso | Prueba instrumentada de persistencia |
| C-10 | Sin red, la lista y el carnet se leen de Room y no bloquean | Prueba con red ausente |
| C-11 | La UI muestra pendiente, sincronizando y error, y permite reintentar | Prueba de interfaz del indicador |
| C-12 | Un texto pendiente de moderación no se envía antes de tener veredicto | Prueba de integración con 004 |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El servidor no acepta ids generados por el cliente | Alto | Q-01; si no los acepta, la creación offline se deshabilita y se documenta |
| G-02 | Un reintento duplica datos clínicos | Alto | Clave de idempotencia y prueba C-03 |
| G-03 | Una entidad hija se envía antes que la padre y falla | Alto | Grafo de dependencias y prueba C-04 |
| G-04 | Un conflicto clínico sobrescribe lo que escribió el veterinario | Alto | Gana el servidor y se avisa; prueba C-06 |
| G-05 | La cola crece sin límite y agota el almacenamiento | Medio | Límite y política de descarte visible; Q-04 |
| G-06 | El usuario no sabe que sus datos no se han sincronizado | Medio | Indicador por elemento y reintento manual; prueba C-11 |
| G-07 | El reloj del dispositivo está mal y "el más reciente" elige mal | Medio | Se usa la marca del servidor cuando existe; Q-07 |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: `001/K-29` (outbox), `001/K-28` (conectividad),
  `001/K-10` (Room), `001/K-17` (caché), `001/K-26` (reintentos), `001/K-04`
  y `001/K-05`.
- `002-identidad-y-sesion`: usuario autenticado y errores cerrados de red.

**Externas:**

- Contrato de escritura del backend para mascotas y carnet (Q-01).
- Soporte de ids de cliente y de claves de idempotencia (Q-01).
- Semántica de borrado del backend (Q-06).

**Unidades posteriores que dependen de esta:**

- `007` (mascotas) y `008` (carnet) aplican la política. `014` y `016` la
  extienden en sus propias specs.

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿El backend acepta ids generados por el cliente y claves de idempotencia? ¿Qué rutas y códigos usa para crear, editar y borrar? | Miguel (backend) | Todas las tareas de red |
| Q-02 | ¿El servidor devuelve `updatedAt` y versión de cada registro? | Miguel (backend) | Conflictos |
| Q-03 | ¿Las escrituras se envían de a una o en lote? ¿Hay límite de tamaño? | Miguel (backend) | Cola |
| Q-04 | ¿Cuál es el tamaño máximo de la cola y qué se hace si se llena? | Equipo | Cola |
| Q-05 | ¿Cómo se notifica un conflicto: código de estado, campo en la respuesta o consulta aparte? | Miguel (backend) | Resolución |
| Q-06 | ¿El borrado es lógico o físico? ¿Se puede recuperar? | Miguel (backend) | Lápidas |
| Q-07 | ¿Qué marca de tiempo es la autoridad: la del servidor o la del dispositivo? | Miguel (backend) | Conflictos |
| Q-08 | ¿Qué códigos de error son permanentes y cuáles transitorios? | Miguel (backend) | Reintentos |
| Q-09 | ¿La sincronización debe correr también en segundo plano con `WorkManager` o basta al abrir la app y al recuperar red? | Equipo | Alcance |
| Q-10 | ¿La cola debe cifrarse en disco? | Equipo / seguridad | Almacenamiento |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El alcance y el fuera de alcance están revisados.
- [ ] La tabla de política por entidad está aceptada.
- [ ] Los requisitos y los criterios de éxito son suficientes para planificar.
- [ ] Las preguntas abiertas están identificadas y se sabe a quién preguntar.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-02 RESUELTA (27/11/2026)** — El servidor devuelve updatedAt en cada recurso. NO devuelve un campo de version: la reconciliacion se hace por updatedAt.
- **Q-06 RESUELTA (27/11/2026)** — Existe DELETE /api/v1/pets/delete/{id}. El borrado se expone como operacion propia.
- **Q-08 RESUELTA (27/11/2026)** — No hay catalogo de codigos aparte: la envoltura lleva statusCode HTTP. 4xx son permanentes (no reintentar), 5xx y los fallos de red son transitorios.
