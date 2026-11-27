# Spec 012 — Panel del afiliado

> Unidad de trabajo: `012-panel-afiliado`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026
> Alineada con [`constitution.md`](../../constitution.md) y
> [`MAPA-CAPACIDADES.md`](../MAPA-CAPACIDADES.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. El panel es la pantalla de inicio cuando el usuario está en modo negocio,
   igual que el mapa lo es en modo dueño.
2. Muestra tres bloques: portada del negocio, alertas de seguimiento y
   mascotas atendidas.
3. Las alertas de seguimiento usan el mismo código de color que la tarjeta
   "lo más próximo" de 010: rojo para atrasadas, amarillo para próximas.
4. "Personas que vieron tu perfil" NO va aquí: va en el Perfil, según la
   maqueta.
5. El seguimiento como proceso lo define 015; 012 solo lista y enlaza lo que
   015 produce. Mientras 015 no exista, la sección se muestra vacía con su
   estado.
6. Las mascotas atendidas se filtran por día: Hoy, Ayer, Semana y calendario.
7. Los datos del panel se leen de Room y se refrescan por red según la caché
   de 001. El panel debe abrir sin conexión con lo último conocido.
8. El contador de mensajes sin leer lo alimenta 016; mientras no exista, se
   muestra en cero.
9. El panel no crea consultas ni edita seguimientos: solo navega hacia ellos.
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Dar al negocio afiliado una **pantalla de control** desde la que vea de un
vistazo qué requiere su atención hoy: tratamientos atrasados, revisiones
próximas, mensajes sin leer y las mascotas que ha atendido.

**Por qué:** un negocio que atiende varias mascotas al día necesita saber a
quién le toca seguimiento sin revisar una por una. Sin este panel, el
seguimiento que construye 015 queda invisible y deja de usarse.

**Para quién:** el dueño o el personal de una veterinaria, tienda o estética
afiliada.

**Cómo se ve el éxito:** al abrir la app en modo negocio, la persona sabe en
menos de cinco segundos si hay algo atrasado y puede llegar a ello con un
toque.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Portada del negocio con nombre, tipos, foto y datos de contacto |
| A-02 | Resumen de alertas: atrasadas, próximas y mensajes sin leer |
| A-03 | Lista de alertas de seguimiento con código de color |
| A-04 | Navegación desde una alerta al seguimiento correspondiente |
| A-05 | Lista de mascotas atendidas |
| A-06 | Filtro por día: Hoy, Ayer, Semana y selección de fecha |
| A-07 | Navegación desde una mascota atendida a su consulta |
| A-08 | Estado vacío propio para cada bloque |
| A-09 | Apertura sin conexión con los últimos datos conocidos |
| A-10 | Acceso al escáner de QR desde el botón central |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Crear o editar el seguimiento | Spec 015 |
| Registrar la consulta y el cobro | Spec 014 |
| Escanear el QR y ver la ficha | Spec 013 |
| El chat en sí | Spec 016 |
| "Personas que vieron tu perfil" | Perfil, no el panel (maqueta) |
| Editar los datos del negocio | Spec 011, pregunta Q-08 |
| Estadísticas, ingresos o reportes | No está en la maqueta |
| Agenda o citas | No está en la maqueta |

---

## 4. Requisitos

### Funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El panel debe ser el destino inicial en modo negocio | Maqueta |
| R-02 | El panel debe mostrar la portada del negocio con sus tipos | Maqueta |
| R-03 | El panel debe listar los seguimientos atrasados en rojo | Maqueta |
| R-04 | El panel debe listar los seguimientos próximos en amarillo | Maqueta |
| R-05 | El panel debe mostrar el número de mensajes sin leer | Maqueta |
| R-06 | El panel debe listar las mascotas atendidas con filtro por día | Maqueta |
| R-07 | Tocar una alerta debe llevar al seguimiento correspondiente | Maqueta |
| R-08 | El panel debe abrir sin conexión con los últimos datos guardados | Punto 16 |
| R-09 | Cada bloque vacío debe explicar qué falta, no quedar en blanco | RNF-16 |

### No funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-10 | El panel debe pintar su primer contenido en menos de 1 s con datos locales | RNF-15 |
| R-11 | Ninguna operación de entrada/salida en el hilo principal | RNF-15 |
| R-12 | Las listas largas deben desplazarse sin saltos perceptibles | RNF-16 |
| R-13 | Los datos de un negocio no deben verse tras cambiar de cuenta | RNF-01 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | En modo negocio, el destino inicial es el panel y no el mapa | Prueba de navegación |
| C-02 | La portada muestra nombre, tipos y foto del negocio afiliado | Prueba de interfaz con doble de datos |
| C-03 | Un seguimiento con revisión vencida aparece en rojo | Prueba con fecha pasada y reloj inyectado |
| C-04 | Un seguimiento con revisión dentro de los próximos días aparece en amarillo | Prueba con fecha futura cercana |
| C-05 | Un seguimiento lejano no aparece destacado | Prueba con fecha futura lejana |
| C-06 | El resumen cuenta correctamente atrasadas, próximas y sin leer | Prueba con conjunto conocido |
| C-07 | Tocar una alerta navega al seguimiento con el identificador correcto | Prueba de navegación con argumento |
| C-08 | El filtro Hoy muestra solo lo atendido hoy según el reloj inyectado | Prueba con datos de varios días |
| C-09 | Los filtros Ayer, Semana y fecha concreta devuelven el conjunto esperado | Prueba por cada filtro |
| C-10 | Con cero datos, cada bloque muestra su estado vacío explicativo | Prueba con repositorio vacío |
| C-11 | Sin conexión, el panel abre con los últimos datos y avisa que pueden estar desactualizados | Prueba con red simulada ausente |
| C-12 | El panel pinta contenido local antes de 1 s con servidor lento | Prueba instrumentada con servidor lento |
| C-13 | Tras cambiar de cuenta, no se ven datos del negocio anterior | Prueba de cambio de sesión |
| C-14 | El botón central abre el escáner en modo negocio | Prueba de navegación |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El contrato del backend del panel no está definido | Alto | Dobles de prueba; rutas y campos a Q-01 |
| G-02 | 015 y 016 no existen todavía y el panel queda a medias | Alto | A-08: cada bloque tiene estado vacío propio y la unidad se cierra sin ellos |
| G-03 | El cálculo de atrasado y próximo usa la fecha del dispositivo | Medio | Reloj inyectado de 001; pruebas C-03 a C-05 con fechas controladas |
| G-04 | Listas grandes de mascotas atendidas hacen lento el panel | Medio | Paginación y carga por páginas; prueba C-12 |
| G-05 | El panel muestra datos clínicos de mascotas que ya no autorizan al negocio | Alto | El acceso se valida contra el permiso vigente del backend, no contra la caché local |
| G-06 | Los umbrales de color quedan distintos a los de 010 | Bajo | Se reutiliza el mismo cálculo de 008/010, no se reimplementa |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: Room, red, caché, navegación, componentes, reloj.
- `002-identidad-y-sesion`: sesión del usuario.
- `011-afiliacion`: el negocio debe existir y el usuario estar en modo negocio.

**Parciales (el panel funciona sin ellas, con estado vacío):**

- `015-seguimiento-tratamiento`: alimenta las alertas.
- `016-chat-seguimiento`: alimenta el contador de mensajes sin leer.
- `014-consulta-veterinaria`: alimenta las mascotas atendidas.

**Externas:**

- Contrato del backend para el resumen del panel (Q-01).

**Unidades posteriores que dependen de esta:**

- `013` (el escáner se abre desde aquí), `015` (navega desde las alertas).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Hay un endpoint de resumen del panel o la app arma el resumen con varias llamadas? | Miguel (backend) | Tareas de red |
| Q-02 | ¿"Próxima" es ≤ 14 días como en 010, o el negocio puede configurar el umbral? | Equipo | Cálculo de alertas |
| Q-03 | ¿Cuántas mascotas atendidas se cargan por página? | Equipo | Paginación |
| Q-04 | ¿El panel es igual para los tres tipos de negocio, o tienda y estética ven menos? | Equipo / maqueta | Alcance de la portada |
| Q-05 | ¿Las alertas se calculan en el backend o en la app a partir de los seguimientos? | Miguel (backend) | Tareas de alertas |
| Q-06 | ¿Qué pasa con las mascotas atendidas cuando el permiso de compartir expira? ¿Se ocultan o se ven sin detalle clínico? | Equipo / profesor | Riesgo G-05 |

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

- **Q-01 RESUELTA (27/11/2026)** — NO existe endpoint de resumen del panel. La app arma el resumen con varias llamadas. Esto es una respuesta, no un pendiente.
