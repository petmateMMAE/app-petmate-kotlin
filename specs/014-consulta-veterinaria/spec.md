# Spec 014 — Consulta veterinaria y cobro

> Unidad de trabajo: `014-consulta-veterinaria`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026
> Alineada con [`constitution.md`](../../constitution.md) y
> [`MAPA-CAPACIDADES.md`](../MAPA-CAPACIDADES.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. La consulta la registra SOLO una veterinaria, nunca una tienda ni una
   estética. Se inicia desde la ficha de 013, tras escanear el QR.
2. La consulta tiene dos partes que se llenan en la misma pantalla: la parte
   clínica (motivo, diagnóstico, peso, receta) y el cobro desglosado.
3. SUPUESTO CRÍTICO: el cobro es un REGISTRO de lo cobrado, no un cobro
   electrónico. PetMate no procesa pagos ni mueve dinero; solo anota qué se
   cobró y cómo se pagó. No hay pasarela de pago.
4. El cobro se arma en vivo: el veterinario va agregando renglones y el total
   se actualiza al momento, con IVA.
5. Los renglones salen de los servicios que el negocio dio de alta en 011,
   más vacunas y medicinas. Se puede agregar un servicio nuevo con su costo
   durante la consulta, y ese servicio queda disponible para el negocio.
6. El peso capturado en la consulta se registra también en el carnet de 008:
   es el mismo dato, no dos.
7. La receta es texto estructurado (medicamento, dosis, frecuencia, duración).
   La explicación "fácil" de la receta es de 019, no de aquí.
8. Al guardar la consulta se puede abrir un seguimiento (015). La consulta se
   puede guardar sin seguimiento.
9. La consulta requiere conexión para guardarse: escribe en el historial
   clínico de una mascota que no es del negocio. No se encola sin validar.
10. El dinero se representa con el tipo Money de 011, en enteros. Nunca con
    decimales de punto flotante.
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Permitir que una veterinaria **registre lo que hizo en una consulta** —motivo,
diagnóstico, peso, receta— y **anote el cobro desglosado**, de modo que el
dueño reciba ambas cosas en el carnet de su mascota.

**Por qué:** es el momento en que PetMate deja de ser un carnet que el dueño
llena a mano y pasa a tener información clínica de origen profesional. Sin
esta unidad, el historial médico de 008 queda siempre vacío y el seguimiento
de 015 no tiene de dónde nacer.

**Para quién:** el veterinario que acaba de atender a una mascota y tiene al
dueño enfrente esperando la cuenta.

**Cómo se ve el éxito:** el veterinario termina de atender, llena la consulta
y el cobro en una sola pantalla sin pelearse con la app, y el dueño ve el
diagnóstico y la receta en su carnet antes de salir del consultorio.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Inicio de la consulta desde la ficha escaneada (solo veterinaria) |
| A-02 | Captura de motivo y diagnóstico |
| A-03 | Captura del peso, que se registra también en el carnet |
| A-04 | Receta estructurada: medicamento, dosis, frecuencia y duración |
| A-05 | Varios medicamentos en una misma receta |
| A-06 | Cobro desglosado por renglones: servicios, vacunas y medicinas |
| A-07 | Cálculo del subtotal, IVA y total en vivo |
| A-08 | Alta de un servicio nuevo con su costo durante la consulta |
| A-09 | Registro de la forma de pago |
| A-10 | Guardado de la consulta completa |
| A-11 | Apertura opcional de un seguimiento al guardar |
| A-12 | Borrador local mientras la consulta está en curso |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Seguimiento del tratamiento y sus estados | Spec 015 |
| Chat con el dueño | Spec 016 |
| Explicar la receta en lenguaje sencillo | Spec 019 |
| Escanear el QR y ver la ficha | Spec 013 |
| Procesar pagos con tarjeta o terminal | **No está en el producto** (supuesto 3) |
| Facturación fiscal o CFDI | No está en la maqueta |
| Inventario de medicinas o control de existencias | No está en la maqueta |
| Editar o cancelar una consulta ya guardada | Pregunta abierta Q-07 |
| Historial de consultas del negocio | Spec 012 (mascotas atendidas) |

---

## 4. Requisitos

### Funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | Solo una veterinaria debe poder registrar una consulta | Maqueta; privacidad |
| R-02 | El sistema debe permitir capturar motivo y diagnóstico | Maqueta |
| R-03 | El peso capturado debe quedar registrado en el carnet de la mascota | Maqueta; spec 008 |
| R-04 | El sistema debe permitir una receta con varios medicamentos | Maqueta |
| R-05 | El cobro debe desglosarse por renglones con su concepto y precio | Maqueta |
| R-06 | El total debe actualizarse al agregar o quitar un renglón | Maqueta |
| R-07 | El sistema debe calcular el IVA sobre el subtotal | Maqueta |
| R-08 | El veterinario debe poder agregar un servicio nuevo con su costo | Maqueta |
| R-09 | El sistema debe registrar la forma de pago | Maqueta |
| R-10 | Al guardar, el sistema debe ofrecer abrir un seguimiento | Maqueta |
| R-11 | El avance debe conservarse si la app se cierra durante la consulta | Usabilidad |

### No funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-12 | Los montos no deben perder precisión en ninguna operación | Corrección |
| R-13 | La consulta no debe guardarse si el permiso de la mascota expiró | Privacidad; spec 009 |
| R-14 | El borrador no debe conservar datos clínicos tras guardarse o descartarse | RNF-01, RNF-02 |
| R-15 | Ninguna operación de entrada/salida en el hilo principal | RNF-15 |
| R-16 | El total debe recalcularse en menos de 100 ms al cambiar un renglón | RNF-15 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Una veterinaria ve la entrada a registrar consulta; tienda y estética no | Prueba de interfaz por tipo de negocio |
| C-02 | El motivo y el diagnóstico se capturan y se guardan | Prueba del flujo con doble de backend |
| C-03 | El peso capturado aparece después en el carnet de la mascota | Prueba de integración con el repositorio de 008 |
| C-04 | Una receta con tres medicamentos se guarda con los tres completos | Prueba con varios renglones de receta |
| C-05 | Agregar un renglón actualiza el subtotal, el IVA y el total | Prueba de cálculo |
| C-06 | Quitar un renglón recalcula correctamente | Prueba de cálculo |
| C-07 | La suma de 10.10 + 20.20 + 0.70 da exactamente 31.00, sin error de redondeo | Prueba unitaria de `Money` |
| C-08 | El IVA se calcula sobre el subtotal con la tasa configurada y se redondea de forma consistente | Prueba con montos que producen fracciones |
| C-09 | Un servicio nuevo creado en la consulta queda disponible para el negocio | Prueba con doble de backend |
| C-10 | No se puede guardar sin forma de pago | Prueba de validación |
| C-11 | No se puede guardar con el cobro en cero y sin renglones | Prueba de validación |
| C-12 | Si la app se cierra a media consulta, al volver se recupera lo capturado | Prueba de persistencia del borrador |
| C-13 | Con el permiso de la mascota expirado, el guardado se rechaza con mensaje claro | Prueba con doble que responde permiso expirado |
| C-14 | Sin conexión, el guardado se explica como no disponible y no se pierde lo capturado | Prueba con red ausente |
| C-15 | Tras guardar o descartar, el borrador no conserva datos clínicos | Inspección del almacenamiento |
| C-16 | Al guardar se ofrece abrir seguimiento, y la consulta se guarda igual si se declina | Prueba de ambos caminos |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | Error de redondeo en el cobro: el total no cuadra con los renglones | **Alto** | `Money` en enteros (011/D-01); C-07 y C-08 con montos que producen fracciones |
| G-02 | El contrato del backend de consultas no está definido | Alto | Dobles de prueba; rutas y campos a Q-01 |
| G-03 | El borrador guarda diagnóstico y receta de una mascota ajena en el dispositivo del negocio | **Alto** | R-14: el borrador se borra al guardar o descartar; C-15 lo verifica |
| G-04 | La consulta se guarda después de que expiró el permiso del dueño | Alto | R-13: el backend valida el permiso al guardar, no la app; C-13 |
| G-05 | La pantalla es demasiado larga y el veterinario se pierde | Medio | Secciones plegables y guardado de borrador continuo |
| G-06 | La tasa de IVA cambia o difiere por entidad | Medio | La tasa es configurable, no una constante en el código (Q-03) |
| G-07 | Dos consultas simultáneas de la misma mascota en negocios distintos | Bajo | El backend decide; se documenta en Q-06 |
| G-08 | El peso se duplica en el carnet por registrarse dos veces | Medio | Un solo origen: la consulta escribe el peso una vez; C-03 |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: red, errores, navegación, componentes, reloj.
- `002-identidad-y-sesion`: sesión del negocio.
- `008-carnet-salud`: el peso y el historial médico donde aterriza la consulta.
- `011-afiliacion`: los servicios con precio y el tipo `Money`.
- `012-panel-afiliado`: la consulta aparece en mascotas atendidas.
- `013-escaner-qr`: la consulta se inicia desde la ficha escaneada.

**Externas:**

- Contrato del backend de consultas y de servicios (Q-01, Q-02).
- Tasa de IVA aplicable (Q-03).

**Unidades posteriores que dependen de esta:**

- `015` (el seguimiento nace de una consulta), `019` (explica la receta),
  `012` (lista las atendidas).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuáles son las rutas, los cuerpos y las respuestas para crear una consulta? | Miguel (backend) | Tareas de red |
| Q-02 | ¿Crear un servicio nuevo durante la consulta usa el mismo endpoint que 011 o uno distinto? | Miguel (backend) | Tarea del servicio nuevo |
| Q-03 | ¿Cuál es la tasa de IVA y se aplica a todos los renglones o solo a algunos? ¿Los precios de 011 incluyen IVA o no? | Equipo / profesor | Cálculo del total |
| Q-04 | ¿Qué formas de pago se registran? (efectivo, tarjeta, transferencia, otra) | Equipo / maqueta | Tarea de forma de pago |
| Q-05 | ¿La receta tiene un catálogo de medicamentos o se escribe libremente? | Equipo | Tareas de receta |
| Q-06 | ¿Puede haber dos consultas abiertas de la misma mascota al mismo tiempo? | Miguel (backend) | Riesgo G-07 |
| Q-07 | ¿Se puede editar o cancelar una consulta ya guardada? ¿Con qué límite de tiempo? | Equipo / profesor | Alcance |
| Q-08 | ¿El dueño recibe una notificación al guardarse la consulta? | Equipo | Integración con 006 |

---

## 9. Puerta de aprobación

- [ ] Alcance y fuera de alcance revisados.
- [ ] **Supuesto 3 confirmado**: PetMate registra el cobro, no procesa pagos.
- [ ] Preguntas abiertas con dueño, en especial Q-03 (IVA).
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — 4 rutas bajo /api/v1/consultations: create, get-all, get-by-id/{id}, add-medication. Detalle en backend-petmate-sdd-aspnetcore/openapi/openapi.json (76 rutas, 132 esquemas). El backend esta TERMINADO y desplegado; el contrato completo esta ahi, no hay que preguntarle a nadie.
