# Spec 009 — Compartir historial

> Unidad de trabajo: `009-compartir-historial`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0
> Depende de: [`008-carnet-salud`](../008-carnet-salud/spec.md) y
> [`002-identidad-y-sesion`](../002-identidad-y-sesion/spec.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:

1. El flujo es: elegir mascota → elegir negocio o "cualquier veterinaria" →
   elegir secciones → elegir duración (30 minutos, 1 día o 7 días) → generar
   QR.

2. "Elegir negocio" usa un selector mínimo de solo lectura alimentado por el
   directorio del backend, sin mapa y sin alta de negocios. El directorio es un
   recurso del backend, no algo que produzcan 010 ni 011. También existe la
   opción "cualquier veterinaria", que comparte sin elegir un negocio
   específico.

3. Las secciones compartibles son vacunas, desparasitación, peso, condiciones e
   historial. Las alergias siempre van incluidas y no se pueden deseleccionar,
   conforme a la regla de 008.

4. El QR es regenerable: se puede emitir uno nuevo en cualquier momento y el
   anterior deja de servir o caduca con el suyo.

5. El QR contiene un token opaco, nunca datos personales ni clínicos. Quien lo
   escanea (013) pide el contenido al backend con ese token.

6. La duración se cumple de verdad: al expirar, el QR no muestra nada y la app
   ofrece regenerar. La cuenta regresiva se muestra al usuario.

7. Sin conexión no se puede emitir un QR nuevo. Un QR ya emitido y vigente se
   puede seguir mostrando desde caché hasta su expiración; queda en preguntas
   abiertas si esto se conserva.

8. Compartir por enlace además de QR: la maqueta pide QR; el enlace va a
   preguntas abiertas y no se implementa por defecto.

9. El dueño puede ver qué negocios accedieron y cuándo: va a preguntas abiertas;
   si no está en la maqueta, no entra.

10. Fuera de alcance: escanear (013), la ficha del negocio (013) y el contenido
    clínico en sí (008).

→ Corrígeme ahora o avanzo con esto.
```

---

## 1. Objetivo

Que el dueño **comparta el historial de su mascota con una veterinaria, una
tienda o una estética durante un tiempo limitado y con las secciones que
elija**, mediante un QR que se puede regenerar, sin exponer datos personales y
sin poder ocultar las alergias.

Esta unidad es la que convierte el carnet en algo útil fuera del teléfono del
dueño: el negocio escanea y ve solo lo autorizado, durante el tiempo acordado.

**Para quién:** el dueño que llega a la veterinaria y el negocio que necesita
el historial.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Selección de la mascota a compartir |
| A-02 | Selector de negocio de solo lectura y opción "cualquier veterinaria" |
| A-03 | Selección de secciones compartibles |
| A-04 | Alergias siempre incluidas y no deseleccionables |
| A-05 | Duración de 30 minutos, 1 día o 7 días |
| A-06 | Generación de un QR con token opaco |
| A-07 | Regeneración e invalidación del QR anterior |
| A-08 | Expiración real y cuenta regresiva |
| A-09 | Visualización de un QR vigente sin conexión |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Escanear el QR y ver la ficha del negocio | Spec 013 |
| Alta de negocios y servicios | Spec 011 |
| Panel del afiliado | Spec 012 |
| Contenido clínico y su edición | Spec 008 |
| Compartir por enlace o correo | Pregunta abierta; no entra por defecto |
| Compartir con otro usuario de la app | No está en la maqueta |
| Exportar a PDF | No está en los lineamientos |

**Ninguna regla clínica se escribe aquí; se consume la de 008.**

---

## 4. Requisitos

### Funcionales de compartir

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El dueño debe poder elegir la mascota cuyo historial comparte | Maqueta |
| R-02 | El dueño debe poder elegir un negocio del directorio o "cualquier veterinaria" | Maqueta |
| R-03 | El dueño debe poder elegir qué secciones comparte | Maqueta |
| R-04 | Las alergias deben incluirse siempre y no deben poder deseleccionarse | 008 R-06 |
| R-05 | La duración debe ser una de tres opciones: 30 minutos, 1 día o 7 días | Maqueta |
| R-06 | El QR debe contener un token opaco, sin datos personales ni clínicos | RNF-01 |
| R-07 | El QR debe poder regenerarse y el anterior debe dejar de servir | Maqueta |
| R-08 | Al cumplirse la duración, el QR no debe mostrar ningún dato | RNF-01 |
| R-09 | La app debe mostrar el tiempo restante del QR vigente | RNF-16 |
| R-10 | Sin conexión no se debe poder emitir un QR nuevo; uno vigente se puede mostrar desde caché | Constitución P4 |
| R-11 | El negocio debe ver solo las secciones autorizadas y las alergias | RNF-01 |
| R-12 | La emisión debe quedar registrada con mascota, negocio, secciones y expiración | RNF-32 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-13 | El token no debe ser adivinable ni reutilizable después de expirar | RNF-01 |
| R-14 | El QR no debe poder leerse sin conexión al backend para resolver el contenido | RNF-01 |
| R-15 | La emisión y la consulta del QR deben ir por HTTPS | RNF-29 |
| R-16 | Los textos deben vivir en `strings.xml` y la cuenta regresiva respetar el reloj del servidor | RNF-16 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | El flujo permite elegir mascota, negocio, secciones y duración antes de generar | Prueba de interfaz |
| C-02 | El selector de negocios muestra el directorio y la opción "cualquier veterinaria" | Prueba de interfaz con doble de directorio |
| C-03 | Las alergias aparecen marcadas y no se pueden desmarcar | Prueba de interfaz |
| C-04 | El QR generado no contiene datos personales ni clínicos legibles | Decodificación del QR en prueba |
| C-05 | Regenerar invalida el QR anterior | Prueba con dos emisiones y consulta del primero |
| C-06 | Al expirar la duración, el QR no devuelve ningún dato | Prueba con reloj fijo y servidor local |
| C-07 | La cuenta regresiva coincide con la expiración del servidor | Prueba con reloj fijo |
| C-08 | Sin red no se emite un QR nuevo y se explica por qué | Prueba con red ausente |
| C-09 | Un QR vigente se puede mostrar desde caché sin red | Prueba con red ausente y caché previa |
| C-10 | El negocio recibe solo las secciones autorizadas más las alergias | Prueba del contrato de consulta |
| C-11 | La emisión queda registrada con sus datos | Prueba del registro de auditoría |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El QR expone datos clínicos porque el token los contiene | Alto | Token opaco y contenido resuelto en el servidor; prueba C-04 |
| G-02 | Un QR viejo sigue sirviendo después de regenerar | Alto | Invalidación en el servidor y prueba C-05 |
| G-03 | El usuario cree que ocultó una alergia y la compartió igual | Medio | La alergia no se puede desmarcar y se explica por qué; prueba C-03 |
| G-04 | La duración se calcula con el reloj del dispositivo y expira antes o después | Medio | Se usa la expiración del servidor; prueba C-07 |
| G-05 | Sin red el usuario no puede compartir y no entiende por qué | Medio | Mensaje claro y opción de mostrar un QR vigente desde caché |
| G-06 | El directorio de negocios no está disponible | Medio | Selector con estado vacío y opción "cualquier veterinaria"; Q-07 |
| G-07 | Un negocio recibe más secciones de las autorizadas | Alto | El servidor filtra por la selección; prueba C-10 |

---

## 7. Dependencias

**De esta unidad:**

- `008-carnet-salud`: secciones, alergias críticas y contenido a compartir.
- `002-identidad-y-sesion`: dueño autenticado.
- `001`: red, caché, navegación y componentes.

**Externas:**

- Contrato del backend para emitir y resolver el QR (Q-01).
- Directorio de negocios de solo lectura (Q-07).
- Semántica de expiración y de invalidación (Q-03, Q-05).

**Unidades posteriores que dependen de esta:**

- `010` (mapa y demo pueden enlazar al compartir), `013` (escáner) y `014`
  (consulta que llega por el QR).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuál es el contrato para emitir y resolver el QR: rutas, campos, códigos y formato del token? | Miguel (backend) | Todas las tareas de red |
| Q-02 | ¿El QR codifica una URL, un token o ambos? ¿Qué esquema usa? | Equipo / backend | Generación |
| Q-03 | ¿La expiración la calcula el servidor y la devuelve en la respuesta? | Miguel (backend) | Cuenta regresiva |
| Q-04 | ¿Se puede revocar un QR antes de que expire? | Equipo / backend | Regeneración |
| Q-05 | Al regenerar, ¿el anterior se invalida de inmediato o caduca solo? | Equipo / backend | Regeneración |
| Q-06 | ¿Cuántos QR vigentes puede tener una mascota a la vez? | Equipo | Emisión |
| Q-07 | ¿Cuál es el endpoint y los campos del directorio de negocios para el selector? | Miguel (backend) | Selector |
| Q-08 | ¿"Cualquier veterinaria" significa que cualquier negocio verificado puede escanear, o solo veterinarias? | Equipo / maqueta | Selector |
| Q-09 | ¿Se registra y se muestra al dueño quién accedió y cuándo? | Equipo / privacidad | Auditoría |
| Q-10 | ¿Se admite compartir por enlace además de QR? | Equipo / maqueta | Alcance |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El alcance y el fuera de alcance están revisados.
- [ ] La regla de alergias siempre incluidas está aceptada.
- [ ] Los requisitos y los criterios de éxito son suficientes para planificar.
- [ ] Las preguntas abiertas están identificadas y se sabe a quién preguntar.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — GET /api/v1/pets/get-qr/{id} devuelve { petId, qrToken, qrCode }. POST /api/v1/pets/regenerate-qr/{id} lo renueva. El escaner canjea con POST /api/v1/scans/resolve, que recibe { code }.
- **Q-07 RESUELTA (27/11/2026)** — El directorio es /api/v1/businesses (9 rutas). Para el selector sirve businesses/get-nearby, el mismo del mapa.
