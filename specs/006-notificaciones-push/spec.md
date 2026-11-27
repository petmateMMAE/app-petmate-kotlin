# Spec 006 — Notificaciones push

> Unidad de trabajo: `006-notificaciones-push`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0
> Depende de: [`001-arquitectura-base`](../001-arquitectura-base/spec.md) y
> [`002-identidad-y-sesion`](../002-identidad-y-sesion/spec.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:

1. La base técnica ya está en 001/K-31 y 001/T-039: token FCM, registro tras
   login, borrado en logout y recepción desacoplada. Esta unidad define el
   contenido, las categorías, la segmentación, los deep links y el modelo de
   preferencias; no reimplementa el token ni la recepción.

2. Categorías: recordatorio de salud (vacuna o desparasitación), seguimiento
   de tratamiento, mensaje de chat, novedades del negocio o de la app y
   seguridad de la cuenta. Cada una tiene su interruptor, salvo seguridad.

3. La UI de los interruptores es de 017 (ajustes). Aquí se define el modelo,
   los valores por defecto y el contrato; 017 solo pinta y guarda.

4. Las preferencias se guardan en el servidor para poder segmentar y se
   reflejan en el dispositivo. Si el backend no las soporta, se guardan solo
   localmente y se documenta.

5. La segmentación usa tópicos de FCM o audiencias del backend; la app solo se
   suscribe y se desuscribe. El mecanismo exacto va a preguntas abiertas.

6. El payload no lleva datos sensibles: ni nombre de la mascota, ni
   diagnóstico, ni texto del chat. Se muestra un texto genérico y el detalle se
   carga al abrir.

7. El permiso de notificaciones se pide al activar la primera categoría, nunca
   al arrancar ni al instalar, conforme a 003.

8. En primer plano se muestra un aviso dentro de la app; en segundo plano lo
   muestra el sistema. Tocar la notificación abre el destino.

9. Las notificaciones de seguridad de la cuenta no se pueden desactivar.

10. Fuera de alcance: el envío desde el backend (otro repositorio) y los
    textos finales de cada campaña.

→ Corrígeme ahora o avanza con esto.
```

---

## 1. Objetivo

Que la app **entregue notificaciones útiles para los procesos principales
—salud, seguimiento, chat, novedades y seguridad— con categorías que el
usuario controla, destinos que abren la pantalla correcta y sin filtrar datos
clínicos en la pantalla de bloqueo**.

Esta unidad convierte la base técnica de FCM que dejó 001 en una experiencia de
producto, y deja el modelo de preferencias que 017 solo pinta.

**Para quién:** el dueño que necesita saber que su mascota tiene una vacuna
próxima o que el veterinario le respondió.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Categorías de notificación con su interruptor |
| A-02 | Modelo de preferencias, valores por defecto y persistencia |
| A-03 | Contrato de payload y validación de campos |
| A-04 | Recepción en primer y segundo plano |
| A-05 | Deep links a mascota, carnet, chat, seguimiento y mapa |
| A-06 | Texto genérico con detalle bajo demanda |
| A-07 | Permiso de notificaciones al activar una categoría |
| A-08 | Sincronización de token y de preferencias con el servidor |
| A-09 | Notificaciones de seguridad no desactivables |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Pantalla de ajustes con los interruptores | Spec 017 |
| Envío de notificaciones desde el backend | Repositorio `backend-petmate-nosdd-aspnetcore` |
| Recordatorios clínicos y sus fechas | Spec 008 |
| Seguimiento de tratamiento | Spec 015 |
| Chat y sus mensajes | Spec 016 |
| Permiso del sistema y su justificación previa | Spec 003 |
| Notificaciones locales sin servidor | No están en los lineamientos |
| Campañas de marketing | No están en la maqueta |

**Ninguna regla clínica ni de negocio se escribe aquí.**

---

## 4. Requisitos

### Funcionales de push

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | La app debe recibir notificaciones con la app abierta y en segundo plano | 001 R-30; RNF-32 |
| R-02 | Cada categoría debe poder activarse o desactivarse, salvo seguridad | RNF-16 |
| R-03 | Las preferencias deben conservarse entre sesiones y reflejarse en el servidor | RNF-28 |
| R-04 | El payload no debe contener datos clínicos, nombres de mascotas ni texto de chat | RNF-01, RNF-32 |
| R-05 | Tocar una notificación debe abrir el destino correspondiente | RNF-16 |
| R-06 | Si el destino no existe o la unidad aún no está construida, debe abrir el inicio sin fallar | RNF-16 |
| R-07 | El permiso de notificaciones debe pedirse al activar la primera categoría, no antes | Constitución P10; 003 R-01 |
| R-08 | Sin permiso, la app debe seguir usable y avisar dentro de la interfaz | 003 R-08 |
| R-09 | Las notificaciones de seguridad de la cuenta no deben poder desactivarse | RNF-01 |
| R-10 | El token debe registrarse tras el login y borrarse en el logout, sobre la base de 001 y 002 | 001 R-30; 002 R-18 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-11 | La recepción no debe bloquear el hilo principal ni depender de que la app esté abierta | RNF-15, RNF-32 |
| R-12 | Un payload inválido o desconocido debe descartarse sin cerrar la app | RNF-16 |
| R-13 | Las notificaciones no deben duplicarse ni mostrar el mismo aviso dos veces | RNF-16 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Una notificación de cada categoría se muestra con la app abierta y en segundo plano | Prueba con mensajes simulados de FCM |
| C-02 | Desactivar una categoría impide que sus notificaciones se muestren | Prueba con la preferencia apagada |
| C-03 | Las preferencias sobreviven al cierre de la app y se envían al servidor | Prueba de persistencia y de contrato |
| C-04 | El payload de todas las categorías está libre de campos clínicos y de nombres | Prueba de esquema sobre los payloads de ejemplo |
| C-05 | Tocar una notificación abre el destino correcto en cada categoría | Prueba instrumentada de deep link |
| C-06 | Un destino desconocido abre el inicio sin cerrar la app | Prueba con un deep link inválido |
| C-07 | El permiso se pide solo al activar la primera categoría | Prueba de interfaz del flujo de activación |
| C-08 | Sin permiso, la app sigue usable y el aviso aparece dentro de la interfaz | Prueba con permiso denegado |
| C-09 | La categoría de seguridad no se puede desactivar | Prueba de interfaz y de modelo |
| C-10 | El token se registra tras el login y se borra en el logout | Prueba con doble del registro |
| C-11 | Un payload inválido se descarta y la app sigue funcionando | Prueba con mensaje malformado |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El contrato del payload no está definido y cada categoría inventa campos | Alto | Un solo contrato validado; Q-01 antes de la primera tarea de recepción |
| G-02 | Un push muestra datos clínicos en la pantalla de bloqueo | Alto | Payload genérico y detalle bajo demanda; prueba C-04 |
| G-03 | El usuario desactiva una categoría y el servidor sigue enviando | Medio | Preferencias sincronizadas y validación en el cliente; prueba C-02 |
| G-04 | El deep link abre una pantalla que aún no existe | Medio | Destino desconocido al inicio; prueba C-06 |
| G-05 | El permiso se pide al arrancar y el usuario lo niega para siempre | Medio | Pedido solo al activar; prueba C-07 |
| G-06 | Llegan notificaciones duplicadas por reintentos del backend | Bajo | Clave de colapso por evento; Q-08 |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: `001/K-31` (registro y recepción), `001/K-18`
  (navegación), `001/K-17` (caché de preferencias), `001/K-04` y `001/K-05`.
- `002-identidad-y-sesion`: sesión y transición de login/logout.
- `003-permisos`: permiso de notificaciones y su degradación.

**Externas:**

- Contrato del payload y de las preferencias con el backend (Q-01, Q-02).
- Mecanismo de segmentación: tópicos o audiencias (Q-03).

**Unidades posteriores que dependen de esta:**

- `007` y `008` (recordatorios de salud), `015` (seguimiento), `016` (chat) y
  `017` (interruptores).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuál es el contrato exacto del payload: claves, tipos, campo de destino y campo de categoría? | Miguel (backend) | Todas las tareas de recepción |
| Q-02 | ¿Las preferencias se guardan en el servidor? ¿Con qué endpoint y campos? | Miguel (backend) | Sincronización de preferencias |
| Q-03 | ¿La segmentación usa tópicos de FCM o audiencias calculadas en el backend? | Miguel (backend) | Suscripción |
| Q-04 | ¿Se admiten notificaciones con imagen o acciones? | Equipo / maqueta | Presentación |
| Q-05 | ¿Hay límite de frecuencia o horario de silencio? | Equipo | Envío |
| Q-06 | ¿Los textos se localizan en el dispositivo o llegan redactados del servidor? | Equipo | Contenido |
| Q-07 | ¿Cuál es el esquema de deep links (`petmate://...` o enlaces HTTPS)? | Equipo | Navegación |
| Q-08 | ¿Cómo se evita duplicar una notificación si el backend reintenta? | Miguel (backend) | Deduplicación |
| Q-09 | ¿Qué categoría exacta cubre "novedades" y quién la envía? | Equipo | Segmentación |
| Q-10 | ¿La recepción en segundo plano debe mostrar siempre una notificación o puede ser silenciosa? | Equipo | Comportamiento |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El alcance y el fuera de alcance están revisados.
- [ ] Las categorías y los valores por defecto están aceptados.
- [ ] Los requisitos y los criterios de éxito son suficientes para planificar.
- [ ] Las preguntas abiertas están identificadas y se sabe a quién preguntar.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-02 RESUELTA (27/11/2026)** — SI se guardan en el servidor: GET /api/v1/notification-settings/get y POST /api/v1/notification-settings/update. El dispositivo se registra con devices/register y devices/unregister.
