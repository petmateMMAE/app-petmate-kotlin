# Spec 015 — Seguimiento del tratamiento

> Unidad de trabajo: `015-seguimiento-tratamiento`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026
> Alineada con [`constitution.md`](../../constitution.md) y
> [`MAPA-CAPACIDADES.md`](../MAPA-CAPACIDADES.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. Un seguimiento nace de una consulta (014) y pertenece a esa consulta. No
   existen seguimientos sueltos.
2. Los estados son tres, como la maqueta: En tratamiento, Mejorando y Dado de
   alta. Son un avance en ese orden, aunque se permite retroceder de
   Mejorando a En tratamiento si el animal recae.
3. Dado de alta es un estado FINAL: cierra el seguimiento y cierra el chat de
   016.
4. Cada cambio de estado genera una alerta para el dueño y queda registrado
   con su fecha, formando una bitácora.
5. El seguimiento tiene una fecha de próxima revisión que alimenta las alertas
   del panel de 012 (rojo si está vencida, amarillo si está próxima).
6. Quien cambia el estado es la veterinaria, no el dueño. El dueño lo consulta
   y puede escribir en el chat (016).
7. El dueño ve el seguimiento de su mascota desde el carnet (008).
8. Las notas de cada cambio son texto libre y pasan por la moderación de 004.
9. Cambiar de estado requiere conexión: notifica al dueño y debe quedar
   registrado del lado del servidor.
10. Los umbrales de color (≤3 días rojo, ≤14 amarillo) son los mismos de 008,
    010 y 012. No se redefinen aquí.
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Permitir que la veterinaria **dé seguimiento a un tratamiento** después de la
consulta: registrar cómo va el animal, cuándo toca revisarlo y cuándo se le da
de alta, avisando al dueño en cada cambio.

**Por qué:** una consulta sin seguimiento es un evento aislado. El valor para
el dueño está en saber si su mascota va mejorando y cuándo le toca volver, y
el valor para la veterinaria está en que el dueño no desaparezca a media
curación. Es también lo que llena las alertas del panel de 012.

**Para quién:** la veterinaria que trata a una mascota durante varios días, y
el dueño que quiere saber cómo va.

**Cómo se ve el éxito:** el dueño recibe un aviso cuando su mascota pasa a
"Mejorando" y sabe sin preguntar que la próxima revisión es el viernes.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Apertura de un seguimiento desde una consulta guardada |
| A-02 | Estados En tratamiento, Mejorando y Dado de alta |
| A-03 | Cambio de estado con nota opcional |
| A-04 | Bitácora de cambios con fecha y autor |
| A-05 | Fecha de próxima revisión, editable |
| A-06 | Cálculo de atrasada o próxima según la fecha de revisión |
| A-07 | Alerta al dueño en cada cambio de estado |
| A-08 | Vista del seguimiento para la veterinaria |
| A-09 | Vista del seguimiento para el dueño, desde el carnet |
| A-10 | Cierre del seguimiento al dar de alta |
| A-11 | Lectura del seguimiento sin conexión |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Registrar la consulta y el cobro | Spec 014 |
| Chat entre dueño y negocio | Spec 016 |
| Alertas en el panel del negocio | Spec 012 (las consume) |
| Envío de las notificaciones | Spec 006 (las dispara) |
| Explicar el tratamiento en lenguaje sencillo | Spec 019 |
| Que el dueño cambie el estado | Fuera: lo hace la veterinaria (supuesto 6) |
| Citas o agenda del negocio | No está en la maqueta |
| Reabrir un seguimiento dado de alta | Pregunta abierta Q-05 |

---

## 4. Requisitos

### Funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | La veterinaria debe poder abrir un seguimiento desde una consulta | Maqueta |
| R-02 | El seguimiento debe tener los estados En tratamiento, Mejorando y Dado de alta | Maqueta |
| R-03 | Cada cambio de estado debe registrarse con fecha y quedar en la bitácora | Maqueta |
| R-04 | El sistema debe permitir fijar y cambiar la fecha de próxima revisión | Maqueta |
| R-05 | Cada cambio de estado debe generar una alerta para el dueño | Maqueta |
| R-06 | Dar de alta debe cerrar el seguimiento | Maqueta |
| R-07 | El dueño debe poder ver el seguimiento de su mascota | Maqueta |
| R-08 | Solo la veterinaria que atiende debe poder cambiar el estado | Privacidad |
| R-09 | Las notas deben moderarse antes de almacenarse | Punto 16; spec 004 |

### No funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-10 | El seguimiento debe poder leerse sin conexión | Punto 16 |
| R-11 | El cálculo de atrasada o próxima debe usar el reloj inyectado | Corrección |
| R-12 | Los umbrales de color deben ser los mismos de 008, 010 y 012 | Consistencia |
| R-13 | Ninguna operación de entrada/salida en el hilo principal | RNF-15 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Desde una consulta guardada se puede abrir un seguimiento | Prueba del flujo con doble de backend |
| C-02 | Un seguimiento nuevo arranca en "En tratamiento" | Prueba de estado inicial |
| C-03 | Se puede pasar de En tratamiento a Mejorando y a Dado de alta | Prueba de las transiciones válidas |
| C-04 | Se puede retroceder de Mejorando a En tratamiento | Prueba de recaída |
| C-05 | **No** se puede cambiar el estado de un seguimiento dado de alta | Prueba de transición inválida |
| C-06 | Cada cambio queda en la bitácora con su fecha y autor | Prueba con tres cambios consecutivos |
| C-07 | Cada cambio de estado dispara una alerta para el dueño | Prueba con doble del disparador de notificaciones |
| C-08 | Una revisión vencida se marca como atrasada en rojo | Prueba con fecha pasada y reloj inyectado |
| C-09 | Una revisión dentro de 3 días se marca en rojo y dentro de 14 en amarillo | Prueba de fronteras |
| C-10 | El dueño ve el seguimiento desde el carnet de su mascota | Prueba de navegación desde 008 |
| C-11 | El dueño **no** puede cambiar el estado | Prueba de interfaz con sesión de dueño |
| C-12 | Dar de alta cierra el seguimiento y lo marca como finalizado | Prueba del estado final |
| C-13 | Una nota rechazada por moderación no se guarda y se explica | Prueba con doble de moderación rechazando |
| C-14 | Sin conexión, el seguimiento se lee de lo guardado y el cambio de estado se explica como no disponible | Prueba con red ausente |
| C-15 | Los umbrales usados coinciden con los de 012 | Prueba que compara el resultado de ambos cálculos |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | Transiciones de estado inválidas dejan el seguimiento inconsistente | Alto | Máquina de estados explícita en el dominio; C-03, C-04, C-05 |
| G-02 | El contrato del backend no está definido | Alto | Dobles de prueba; rutas y campos a Q-01 |
| G-03 | Los umbrales se redefinen y difieren de 012 | Medio | Constante compartida; C-15 compara ambos cálculos |
| G-04 | El dueño puede cambiar el estado por un hueco en la interfaz | Alto | La autorización la valida el backend, no solo la pantalla; C-11 |
| G-05 | Un cambio de estado se guarda pero la alerta no se envía | Medio | El disparo de la alerta es del backend al registrar el cambio, no de la app |
| G-06 | El seguimiento acumula datos clínicos en el dispositivo del negocio | Medio | Se guarda estado, fechas y notas; el contenido clínico detallado sigue en 014 |
| G-07 | Varias veterinarias atienden a la misma mascota y compiten por el seguimiento | Bajo | El seguimiento pertenece a la consulta y a su negocio; se documenta en Q-04 |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: red, Room, reloj, navegación, componentes.
- `002-identidad-y-sesion`: sesión y rol del usuario.
- `004-moderacion-contenido`: notas de los cambios.
- `006-notificaciones-push`: alertas al dueño.
- `008-carnet-salud`: el dueño accede desde ahí.
- `014-consulta-veterinaria`: el seguimiento nace de una consulta.

**Externas:**

- Contrato del backend de seguimientos (Q-01).

**Unidades posteriores que dependen de esta:**

- `012` (alimenta las alertas del panel), `016` (el chat se cierra al dar de
  alta), `019` (puede explicar el tratamiento).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuáles son las rutas, los cuerpos y las respuestas para crear un seguimiento y cambiar su estado? | Miguel (backend) | Tareas de red |
| Q-02 | ¿La alerta al dueño la dispara el backend al registrar el cambio, o la app pide enviarla? | Miguel (backend) | Riesgo G-05 |
| Q-03 | ¿La fecha de próxima revisión es obligatoria al abrir el seguimiento o puede quedar vacía? | Equipo | Validación |
| Q-04 | ¿Una mascota puede tener dos seguimientos abiertos en negocios distintos? | Equipo / backend | Riesgo G-07 |
| Q-05 | ¿Se puede reabrir un seguimiento dado de alta, o se abre uno nuevo? | Equipo | Alcance de C-05 |
| Q-06 | ¿Cuánto tiempo se conserva un seguimiento cerrado visible para el dueño? | Equipo | Retención |

---

## 9. Puerta de aprobación

- [ ] Alcance y fuera de alcance revisados.
- [ ] Supuestos confirmados, en especial que el dueño no cambia el estado.
- [ ] Preguntas abiertas con dueño.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — 5 rutas bajo /api/v1/followups: create, get-all, get-by-consultation/{id}, add-note y el cambio de estado. Detalle en backend-petmate-sdd-aspnetcore/openapi/openapi.json (76 rutas, 132 esquemas). El backend esta TERMINADO y desplegado; el contrato completo esta ahi, no hay que preguntarle a nadie.
