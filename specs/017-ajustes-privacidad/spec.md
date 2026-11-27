# Spec 017 — Ajustes, privacidad y eliminar cuenta

> Unidad de trabajo: `017-ajustes-privacidad`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026
> Alineada con [`constitution.md`](../../constitution.md) y
> [`MAPA-CAPACIDADES.md`](../MAPA-CAPACIDADES.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. Esta unidad reúne tres cosas que los lineamientos exigen por separado pero
   que viven en la misma pantalla: ajustes de notificaciones, aviso de
   privacidad y eliminar cuenta.
2. Los interruptores de notificaciones usan el modelo que definió 006. Aquí
   solo se pintan y se guardan; el modelo no se redefine.
3. SUPUESTO CRÍTICO: eliminar cuenta es DEFINITIVO. El borrado en cascada lo
   hace el backend, no la app (no hay llaves foráneas en NoSQL).
4. Eliminar cuenta exige confirmación explícita y reautenticación: no basta
   con tocar un botón.
5. El aviso de privacidad vive en la landing page; la app lo enlaza y no lo
   duplica. Se abre en el navegador o en una vista integrada.
6. El perfil muestra "personas que vieron tu perfil" si el usuario es
   afiliado, según la maqueta. El dato lo da el backend.
7. Cerrar sesión ya existe en 002; aquí solo se expone el acceso.
8. Los ajustes se leen sin conexión; cambiarlos requiere red porque se
   guardan del lado del servidor para poder segmentar (006).
9. Eliminar cuenta requiere conexión. No se encola jamás.
10. Tras eliminar la cuenta, la app vuelve a la bienvenida sin rastro de la
    sesión anterior.
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Dar al usuario **control sobre sus datos y sus avisos**: decidir qué
notificaciones recibe, consultar cómo se tratan sus datos, cerrar sesión y
eliminar su cuenta de forma definitiva.

**Por qué:** los lineamientos lo exigen de forma explícita —aviso de
privacidad vinculado a la landing, eliminar cuenta y ajustes de
notificaciones— y Google Play lo revisa antes de publicar: una app que maneja
datos personales sin un camino claro para borrarlos es rechazada.

**Para quién:** cualquier usuario, dueño o afiliado, que quiera revisar o
retirar su información.

**Cómo se ve el éxito:** el usuario encuentra sin buscar cómo apagar un tipo
de aviso, y quien quiere irse puede borrar su cuenta sin escribir un correo a
nadie.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Pantalla de ajustes accesible desde el perfil |
| A-02 | Interruptores por categoría de notificación, según el modelo de 006 |
| A-03 | Indicación de que las notificaciones de seguridad no se desactivan |
| A-04 | Enlace al aviso de privacidad en la landing |
| A-05 | Enlace a los términos y al manejo de datos |
| A-06 | Acceso a cerrar sesión |
| A-07 | Eliminación de cuenta con confirmación y reautenticación |
| A-08 | Explicación clara de qué se borra al eliminar la cuenta |
| A-09 | Perfil con los datos del usuario |
| A-10 | "Personas que vieron tu perfil", solo para afiliados |
| A-11 | Lectura de los ajustes sin conexión |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Modelo y categorías de notificación | Spec 006 (aquí solo se pintan) |
| Envío de notificaciones | Spec 006 |
| Cerrar sesión en sí | Spec 002 (aquí solo el acceso) |
| El texto legal del aviso de privacidad | Landing page, otro repositorio |
| Editar los datos del negocio | Spec 011, pregunta Q-08 |
| Exportar los datos del usuario | Pregunta abierta Q-05 |
| Cambiar contraseña | Pregunta abierta Q-06 |
| Preferencias de apariencia o idioma | No está en la maqueta |

---

## 4. Requisitos

### Funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El usuario debe poder activar y desactivar cada categoría de notificación | Punto 16; maqueta |
| R-02 | Las notificaciones de seguridad de la cuenta no deben poder desactivarse | Spec 006 |
| R-03 | La app debe enlazar el aviso de privacidad alojado en la landing | **Punto 16 (obligatorio)** |
| R-04 | La app debe ofrecer eliminar la cuenta | **Punto 16 (obligatorio)** |
| R-05 | Eliminar la cuenta debe exigir confirmación explícita y reautenticación | Seguridad |
| R-06 | El sistema debe explicar qué datos se borran antes de confirmar | Transparencia; Google Play |
| R-07 | Tras eliminar la cuenta, la app debe volver a la bienvenida sin sesión | Seguridad |
| R-08 | El perfil debe mostrar "personas que vieron tu perfil" solo a afiliados | Maqueta |
| R-09 | El usuario debe poder cerrar sesión desde los ajustes | Punto 10 |

### No funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-10 | Los ajustes deben leerse sin conexión | Punto 16 |
| R-11 | Tras eliminar la cuenta no debe quedar ningún dato del usuario en el dispositivo | RNF-01, RNF-02 |
| R-12 | El enlace al aviso de privacidad debe funcionar sin sesión iniciada | Google Play |
| R-13 | Ninguna operación de entrada/salida en el hilo principal | RNF-15 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Los ajustes se abren desde el perfil y muestran las categorías de 006 | Prueba de navegación e interfaz |
| C-02 | Apagar una categoría la guarda y se refleja al volver a entrar | Prueba con doble de backend |
| C-03 | La categoría de seguridad aparece pero no se puede desactivar | Prueba de interfaz |
| C-04 | El enlace al aviso de privacidad abre la página de la landing | Prueba de navegación externa |
| C-05 | El aviso de privacidad es accesible sin haber iniciado sesión | Prueba desde la bienvenida |
| C-06 | Eliminar cuenta muestra qué se borra antes de pedir confirmación | Prueba de interfaz |
| C-07 | Eliminar cuenta exige reautenticación antes de ejecutarse | Prueba del flujo completo |
| C-08 | Una reautenticación fallida cancela la eliminación | Prueba con credencial incorrecta |
| C-09 | Tras eliminar, la app vuelve a la bienvenida y no queda sesión | Prueba del estado posterior |
| C-10 | Tras eliminar, no queda ningún dato del usuario en el dispositivo | Inspección del almacenamiento |
| C-11 | Sin conexión, los ajustes se leen y el cambio se explica como no disponible | Prueba con red ausente |
| C-12 | Sin conexión, eliminar cuenta se explica como no disponible y no se encola | Prueba con red ausente |
| C-13 | "Personas que vieron tu perfil" aparece para un afiliado y no para un dueño | Prueba con ambos perfiles |
| C-14 | Cerrar sesión desde ajustes invoca el logout de 002 | Prueba del flujo |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | Un usuario elimina su cuenta por accidente y pierde el historial de sus mascotas | **Alto** | R-05 y R-06: explicación previa, confirmación explícita y reautenticación; C-06, C-07 |
| G-02 | La eliminación borra la cuenta pero deja datos clínicos huérfanos en el backend | Alto | El borrado en cascada es del backend; se documenta el alcance en Q-01 |
| G-03 | Google Play rechaza la app por no ofrecer eliminar cuenta de forma accesible | **Alto** | A-07 y C-06; la ruta debe ser visible, no escondida |
| G-04 | El aviso de privacidad no existe todavía en la landing | Medio | Se enlaza una URL configurable; la landing es de otro repositorio (Q-02) |
| G-05 | Los ajustes se guardan solo localmente y no segmentan | Medio | Se guardan en el servidor según 006; si el backend no lo soporta, se documenta |
| G-06 | Datos residuales tras eliminar la cuenta | Alto | R-11 y C-10: se limpia Room, preferencias y credenciales cifradas |
| G-07 | Un usuario afiliado elimina su cuenta y su negocio queda en el mapa | Medio | El backend debe retirar el negocio; se documenta en Q-01 |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: red, Room, navegación, componentes.
- `002-identidad-y-sesion`: sesión, logout y reautenticación.
- `003-permisos`: permiso de notificaciones al activar la primera categoría.
- `006-notificaciones-push`: modelo y categorías de notificación.

**Externas:**

- URL del aviso de privacidad en la landing (Q-02).
- Contrato del backend para ajustes y para eliminar cuenta (Q-01).

**Unidades posteriores que dependen de esta:**

- `020-liberacion-play`: Google Play exige la ruta de eliminar cuenta y el
  enlace al aviso de privacidad para aprobar la publicación.

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuál es la ruta de eliminar cuenta y qué borra exactamente en cascada? ¿Qué pasa con el negocio de un afiliado? | Miguel (backend) | Tareas de eliminación |
| Q-02 | ¿Cuál es la URL del aviso de privacidad y de los términos en la landing? | Equipo / landing | Tareas de enlaces |
| Q-03 | ¿La eliminación es inmediata o hay un periodo de gracia para arrepentirse? | Equipo / profesor | Flujo de eliminación |
| Q-04 | ¿Los ajustes de notificación se guardan en el servidor o solo localmente? (hereda 006) | Miguel (backend) | Tareas de ajustes |
| Q-05 | ¿Se ofrece exportar los datos del usuario antes de borrarlos? | Equipo / profesor | Alcance |
| Q-06 | ¿Cambiar contraseña va aquí o en 002? | Equipo | Alcance |
| Q-07 | ¿De dónde sale "personas que vieron tu perfil" y qué cuenta como visita? | Miguel (backend) | Tarea del perfil |

---

## 9. Puerta de aprobación

- [ ] Alcance y fuera de alcance revisados.
- [ ] **Supuesto 3 confirmado**: eliminar cuenta es definitivo y el borrado en
      cascada lo hace el backend.
- [ ] Preguntas abiertas con dueño, en especial Q-01 y Q-02.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — POST /api/v1/account-deletion/request y POST /api/v1/account-deletion/confirm (dos pasos, con confirmacion).
- **Q-04 RESUELTA (27/11/2026)** — En el servidor: notification-settings/get y /update. Igual que 006/Q-02.
