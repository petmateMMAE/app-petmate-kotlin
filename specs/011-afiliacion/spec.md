# Spec 011 — Afiliación de negocios

> Unidad de trabajo: `011-afiliacion`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026
> Alineada con [`constitution.md`](../../constitution.md) y
> [`MAPA-CAPACIDADES.md`](../MAPA-CAPACIDADES.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. La afiliación se inicia desde el perfil del usuario ya autenticado. No es
   un registro aparte: la misma cuenta gana el modo negocio.
2. Un usuario afiliado NO pierde "Mis mascotas". Sigue siendo dueño y además
   es negocio; la app le permite alternar entre los dos modos.
3. Un negocio puede ser de varios tipos a la vez (veterinaria, tienda,
   estética). No es excluyente.
4. El flujo es de 3 pasos, como la maqueta: datos y tipos → servicios con
   precio (mínimo uno) → pin en el mapa.
5. El progreso se guarda entre pasos: si el usuario sale, puede retomar donde
   se quedó sin volver a capturar.
6. El pin se coloca sobre el mapa de 010, reutilizando su componente. No se
   construye un segundo mapa.
7. La afiliación requiere conexión: crea una entidad del lado del servidor y
   no tiene sentido encolarla sin validar. Los borradores sí se guardan
   localmente.
8. Los textos y las fotos del negocio pasan por la moderación de 004.
9. Si el backend exige validar la cédula profesional de una veterinaria, ese
   trámite NO se implementa aquí: la spec solo captura el dato.
10. Al terminar, el usuario entra al panel del afiliado (012).
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Permitir que un usuario convierta su cuenta en un **negocio afiliado**
(veterinaria, tienda o estética) para aparecer en el mapa de PetMate, ofrecer
servicios con precio y atender mascotas.

**Por qué:** el mapa de negocios es la pantalla de inicio del producto y el
corazón de su propuesta. Sin un camino para que los negocios se den de alta,
el mapa queda vacío y la app pierde la mitad de su valor. Además es la puerta
de entrada al lado del producto que genera los datos clínicos (consultas,
recetas, seguimiento).

**Para quién:** un dueño de veterinaria, tienda de mascotas o estética que ya
usa PetMate como dueño y quiere que su negocio sea visible y pueda atender.

**Cómo se ve el éxito:** alguien completa los tres pasos en una sola sesión,
sin ayuda, y su negocio aparece en el mapa con sus servicios y su ubicación
correcta.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Entrada a la afiliación desde el perfil del usuario |
| A-02 | Paso 1: datos del negocio (nombre, descripción, teléfono, horario) |
| A-03 | Paso 1: selección de uno o varios tipos de negocio |
| A-04 | Paso 2: alta de servicios con nombre y precio, mínimo uno |
| A-05 | Paso 2: edición y eliminación de servicios antes de confirmar |
| A-06 | Paso 3: colocación del pin en el mapa y confirmación de la dirección |
| A-07 | Guardado del borrador entre pasos y reanudación |
| A-08 | Validación por paso, sin dejar avanzar con datos incompletos |
| A-09 | Envío de la afiliación y entrada al modo negocio |
| A-10 | Alternancia entre modo dueño y modo negocio sin perder "Mis mascotas" |
| A-11 | Foto de portada del negocio, moderada antes de subir |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Panel del negocio, alertas y mascotas atendidas | Spec 012 |
| Escanear QR y ver la ficha de una mascota | Spec 013 |
| Consultas, recetas y cobro | Spec 014 |
| Edición del negocio después de afiliado | Pregunta abierta Q-08 |
| Verificación de cédula profesional ante un tercero | Fuera del producto; solo se captura el dato |
| Facturación, pagos del negocio a PetMate o comisiones | No está en la maqueta ni en los lineamientos |
| Varios locales o sucursales del mismo negocio | Pregunta abierta Q-07 |
| Empleados o varias cuentas por negocio | No está en la maqueta |
| El mapa en sí y sus marcadores | Spec 010 |

---

## 4. Requisitos

### Funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El usuario autenticado debe poder iniciar la afiliación desde su perfil | Maqueta; punto 4 |
| R-02 | El usuario debe poder elegir uno o varios tipos de negocio | Maqueta |
| R-03 | El sistema debe exigir al menos un servicio con precio antes de continuar | Maqueta |
| R-04 | El usuario debe poder colocar y ajustar el pin de su ubicación en el mapa | Maqueta |
| R-05 | El sistema debe conservar el avance si el usuario sale a la mitad | Usabilidad |
| R-06 | El sistema debe validar cada paso antes de permitir avanzar | Usabilidad |
| R-07 | Al completar la afiliación, el usuario debe entrar al modo negocio | Maqueta |
| R-08 | El usuario afiliado debe conservar el acceso a "Mis mascotas" | Maqueta |
| R-09 | El usuario debe poder alternar entre modo dueño y modo negocio | Maqueta |
| R-10 | El texto y las imágenes del negocio deben moderarse antes de almacenarse | Punto 16; spec 004 |

### No funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-11 | El flujo debe funcionar en pantallas de teléfono sin desbordes | RNF-16 |
| R-12 | Ninguna operación de entrada/salida debe correr en el hilo principal | RNF-15 |
| R-13 | El borrador no debe contener datos de otro usuario tras un cambio de sesión | RNF-01 |
| R-14 | Los errores del servidor deben mostrarse comprensiblemente sin perder lo capturado | RNF-16 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | El perfil de un usuario no afiliado muestra la entrada a afiliarse; el de uno afiliado, no | Prueba de interfaz con ambos estados |
| C-02 | El paso 1 no deja avanzar sin nombre y sin al menos un tipo seleccionado | Prueba de validación |
| C-03 | Se pueden seleccionar dos o tres tipos a la vez y los tres quedan registrados | Prueba con selección múltiple |
| C-04 | El paso 2 no deja avanzar con cero servicios | Prueba de validación |
| C-05 | Un servicio agregado se puede editar y eliminar antes de confirmar | Prueba de interfaz |
| C-06 | Un precio inválido (vacío, negativo o no numérico) se rechaza con mensaje | Prueba con los tres casos |
| C-07 | El paso 3 no deja confirmar sin un pin colocado | Prueba de validación |
| C-08 | Si el usuario sale en el paso 2 y vuelve, encuentra lo que ya capturó | Prueba de persistencia del borrador |
| C-09 | Al completar los tres pasos, el usuario queda en modo negocio y en el panel | Prueba del flujo completo con doble de backend |
| C-10 | Un usuario afiliado sigue viendo "Mis mascotas" y puede alternar de modo | Prueba de navegación entre modos |
| C-11 | Un texto rechazado por moderación no se envía y se explica al usuario | Prueba con el doble de moderación rechazando |
| C-12 | Un error del servidor al enviar conserva lo capturado y permite reintentar | Prueba con doble de backend fallando |
| C-13 | Al cambiar de sesión, el borrador del usuario anterior no es visible | Prueba de cambio de sesión |
| C-14 | Sin conexión, el envío se explica como no disponible y el borrador se conserva | Prueba con red simulada ausente |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El contrato del backend de negocios no está definido | Alto | Se prueba con dobles; rutas y campos a Q-01, no se inventan |
| G-02 | El flujo de 3 pasos es largo y el usuario lo abandona | Medio | Borrador persistente (A-07) e indicador de progreso visible |
| G-03 | El pin queda mal colocado y el negocio aparece en otro lado | Medio | Confirmación explícita de la dirección y posibilidad de ajustar antes de enviar |
| G-04 | Un negocio falso o malicioso se da de alta | Medio | Moderación de textos e imágenes (004); la validación de identidad del negocio es del backend y va a Q-05 |
| G-05 | Los precios se capturan en formatos distintos y luego no se pueden sumar | Medio | Un solo formato numérico decidido aquí y reutilizado por 014 (cobro) |
| G-06 | Alternar entre modos confunde al usuario sobre en cuál está | Medio | Indicador permanente del modo activo; prueba C-10 |
| G-07 | El borrador local guarda datos del negocio en claro | Bajo | No se guardan datos sensibles en el borrador; solo lo que el usuario capturó |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: red, Room, navegación, componentes, errores.
- `002-identidad-y-sesion`: el usuario debe estar autenticado.
- `003-permisos`: ubicación para centrar el mapa en el paso 3.
- `004-moderacion-contenido`: textos y foto de portada.
- `010-mapa-e-inicio`: componente de mapa para colocar el pin.

**Externas:**

- Contrato del backend para crear y consultar negocios (Q-01).
- Catálogo de tipos de negocio y de servicios sugeridos, si existe (Q-02).

**Unidades posteriores que dependen de esta:**

- `012` (panel del afiliado), `013` (ficha según tipo de negocio), `014`
  (servicios y precios en el cobro) y `010` (los negocios que pinta el mapa).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuáles son las rutas, los cuerpos y las respuestas para crear y consultar un negocio? | Miguel (backend) | Tareas de red |
| Q-02 | ¿Hay un catálogo cerrado de servicios por tipo de negocio o el usuario escribe el nombre libremente? | Equipo | Tareas del paso 2 |
| Q-03 | ¿La moneda es siempre MXN y el precio lleva decimales? ¿Se captura con o sin IVA? | Equipo | Tareas del paso 2 y spec 014 |
| Q-04 | ¿El horario se captura por día de la semana o como texto libre? | Equipo / maqueta | Tareas del paso 1 |
| Q-05 | ¿El backend valida la identidad del negocio (cédula, RFC) antes de publicarlo en el mapa, o se publica de inmediato? | Miguel / profesor | Tareas de envío |
| Q-06 | ¿La dirección se escribe a mano, se deriva del pin con geocodificación inversa, o ambas? | Equipo | Tareas del paso 3 |
| Q-07 | ¿Un negocio puede tener varias sucursales o es un pin por cuenta? | Equipo | Alcance del paso 3 |
| Q-08 | ¿Dónde se edita el negocio después de afiliado: en esta unidad, en 012 o en 017? | Equipo | Alcance |

> Las preguntas abiertas **no se responden inventando**.

---

## 9. Puerta de aprobación

- [ ] Alcance y fuera de alcance revisados.
- [ ] Supuestos confirmados o corregidos.
- [ ] Preguntas abiertas con dueño.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — 9 rutas bajo /api/v1/businesses: create, get, get-nearby, update, delete, upload-photo, get-photo-url, delete-photo y servicios. Detalle en backend-petmate-sdd-aspnetcore/openapi/openapi.json (76 rutas, 132 esquemas). El backend esta TERMINADO y desplegado; el contrato completo esta ahi, no hay que preguntarle a nadie.
