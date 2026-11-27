# Spec 013 — Escáner de QR y ficha de la mascota

> Unidad de trabajo: `013-escaner-qr`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026
> Alineada con [`constitution.md`](../../constitution.md) y
> [`MAPA-CAPACIDADES.md`](../MAPA-CAPACIDADES.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. El escáner se abre desde el botón central del menú inferior cuando el
   usuario está en modo negocio, igual que "Mi QR" se abre en modo dueño.
2. Lo que se escanea es el token opaco que emite 009. El QR no contiene datos
   personales ni clínicos: la app pide el contenido al backend con ese token.
3. SUPUESTO CRÍTICO DE PRIVACIDAD: lo que se muestra DEPENDE DEL TIPO DE
   NEGOCIO que escanea. La veterinaria ve el historial completo autorizado;
   tienda y estética ven solo los datos básicos de la mascota más las
   alergias. Esto es una regla de privacidad, no una preferencia de interfaz.
4. Quien decide qué se muestra es el BACKEND, no la app. El backend recibe el
   token y la identidad del negocio y devuelve solo lo autorizado. La app no
   recibe datos que luego oculta.
5. Se respetan además las secciones que el dueño eligió compartir en 009 y la
   duración que fijó. Un token expirado no devuelve nada.
6. Las alergias siempre se muestran, aunque el dueño haya limitado el resto
   (regla de 008.3), y para cualquier tipo de negocio.
7. El escáner requiere permiso de cámara (003). Sin él, se ofrece escribir el
   código corto a mano.
8. Escanear requiere conexión: la validación del token y del permiso es del
   servidor. Sin red no se puede abrir una ficha nueva.
9. La ficha es de solo lectura. Registrar la consulta es de 014.
10. Un escaneo queda registrado para que el dueño pueda saber quién accedió,
    si esa función se confirma (es pregunta abierta en 009).
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Permitir que un **negocio afiliado lea el QR de una mascota** y vea su
información de salud, limitada a lo que el dueño autorizó y a lo que
corresponde al tipo de negocio.

**Por qué:** es el momento en que PetMate cumple su promesa principal. El
dueño llega a la veterinaria, muestra un QR, y el veterinario ve el historial
sin papeles ni llamadas. Sin esta unidad, el carnet digital y el compartir de
009 no sirven para nada en el mundo real.

**Para quién:** el personal de una veterinaria, tienda o estética afiliada,
atendiendo a una mascota frente a ellos.

**Cómo se ve el éxito:** el veterinario escanea, y en pocos segundos tiene en
pantalla las alergias y el historial de la mascota, sin haber tocado nada más.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Apertura del escáner desde el botón central en modo negocio |
| A-02 | Lectura de un QR con la cámara |
| A-03 | Entrada manual del código corto como alternativa |
| A-04 | Validación del token contra el backend |
| A-05 | Ficha de la mascota con el contenido que devuelve el backend |
| A-06 | Alergias siempre visibles y destacadas |
| A-07 | Mensajes claros para token expirado, inválido o revocado |
| A-08 | Manejo del permiso de cámara denegado, con alternativa manual |
| A-09 | Aviso explícito de que no hay conexión y no se puede escanear |
| A-10 | Navegación desde la ficha al registro de consulta (014) si el negocio es veterinaria |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Generar el QR | Spec 009 |
| Registrar consulta, receta y cobro | Spec 014 |
| Seguimiento del tratamiento | Spec 015 |
| Chat con el dueño | Spec 016 |
| Editar el carnet desde el negocio | Spec 014; el negocio no edita el carnet directamente |
| Panel y alertas | Spec 012 |
| Historial de quién escaneó, visible para el dueño | Spec 009, pregunta abierta |

---

## 4. Requisitos

### Funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El negocio debe poder escanear el QR de una mascota con la cámara | Maqueta |
| R-02 | El sistema debe permitir escribir el código a mano si la cámara no está disponible | Maqueta; accesibilidad |
| R-03 | El sistema debe mostrar solo la información que el backend autoriza para ese negocio | Privacidad; maqueta |
| R-04 | Una veterinaria debe poder ver el historial clínico autorizado | Maqueta |
| R-05 | Una tienda o estética debe ver solo datos básicos y alergias | Maqueta; privacidad |
| R-06 | Las alergias deben mostrarse siempre y de forma destacada | Spec 008 |
| R-07 | Un token expirado, inválido o revocado debe explicarse con un mensaje claro | RNF-16 |
| R-08 | El sistema debe respetar las secciones que el dueño eligió compartir | Spec 009 |
| R-09 | Desde la ficha, una veterinaria debe poder iniciar el registro de consulta | Maqueta |

### No funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-10 | La app no debe recibir del backend datos que no va a mostrar | RNF-01; privacidad |
| R-11 | El contenido clínico de una ficha no debe persistirse en el dispositivo del negocio | RNF-01, RNF-02 |
| R-12 | El token escaneado no debe aparecer en registros ni en la URL visible | RNF-01 |
| R-13 | La ficha debe aparecer en menos de 2 s desde el escaneo exitoso, con red normal | RNF-15 |
| R-14 | Ninguna operación de entrada/salida en el hilo principal | RNF-15 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | El botón central abre el escáner cuando el usuario está en modo negocio | Prueba de navegación |
| C-02 | Un QR válido abre la ficha de la mascota correspondiente | Prueba con doble de backend |
| C-03 | Escaneando como veterinaria se recibe y muestra el historial autorizado | Prueba con doble que responde como para veterinaria |
| C-04 | Escaneando como tienda o estética **no se recibe** historial clínico, solo básicos y alergias | Prueba que inspecciona la respuesta, no solo la pantalla |
| C-05 | Las alergias se muestran destacadas en los dos casos anteriores | Prueba de interfaz en ambos tipos |
| C-06 | Un token expirado muestra "el permiso expiró" y no abre ficha | Prueba con doble que responde expirado |
| C-07 | Un token inválido o alterado muestra error y no abre ficha | Prueba con token manipulado |
| C-08 | Un token revocado por regeneración del QR muestra error | Prueba con doble que responde revocado |
| C-09 | Sin permiso de cámara, se ofrece la entrada manual y la pantalla no se bloquea | Prueba con permiso denegado |
| C-10 | Un código escrito a mano válido abre la ficha igual que el escaneo | Prueba de entrada manual |
| C-11 | Sin conexión, se explica que no se puede escanear y no se queda cargando | Prueba con red ausente |
| C-12 | Tras cerrar la ficha, el contenido clínico no queda guardado en el dispositivo | Inspección del almacenamiento tras cerrar |
| C-13 | El token no aparece en los registros de la app | Búsqueda sobre la salida de registro de un escaneo completo |
| C-14 | Desde la ficha, una veterinaria puede navegar al registro de consulta | Prueba de navegación |
| C-15 | Una tienda o estética **no** ve la entrada al registro de consulta | Prueba de interfaz por tipo |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | La app recibe el historial completo y lo oculta en pantalla | **Crítico** | El filtrado es del backend (supuesto 4). C-04 inspecciona la respuesta recibida, no solo lo pintado |
| G-02 | El contenido clínico queda en caché en el dispositivo del negocio | Alto | R-11: la ficha no se persiste; se mantiene solo en memoria mientras está abierta |
| G-03 | Un token filtrado permite acceso indebido | Alto | El token es opaco, de un solo uso o con expiración corta; la validación es del servidor |
| G-04 | El contrato del backend de escaneo no está definido | Alto | Dobles de prueba; rutas y campos a Q-01 |
| G-05 | La librería de escaneo requiere servicios no disponibles en el dispositivo | Medio | Alternativa manual obligatoria (A-03); la decisión de librería va al plan |
| G-06 | El escaneo en mala luz o con QR dañado frustra al usuario | Medio | Guía visual, linterna y alternativa manual |
| G-07 | El negocio escanea y el permiso expira mientras ve la ficha | Bajo | La ficha es una fotografía del momento; al volver a entrar se revalida |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: red, errores, navegación, componentes.
- `002-identidad-y-sesion`: sesión del negocio.
- `003-permisos`: permiso de cámara y su degradación.
- `009-compartir-historial`: el token y sus reglas de duración y secciones.
- `012-panel-afiliado`: el botón central que abre el escáner.

**Externas:**

- Contrato del backend de escaneo y la regla de filtrado por tipo (Q-01, Q-02).

**Unidades posteriores que dependen de esta:**

- `014` (la consulta se inicia desde la ficha).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuál es la ruta, el cuerpo y la respuesta del canje del token por la ficha? | Miguel (backend) | Tareas de red |
| Q-02 | ¿El backend filtra por tipo de negocio, o devuelve todo y espera que la app filtre? Si es lo segundo, hay que cambiarlo | Miguel (backend) / profesor | Riesgo G-01 |
| Q-03 | ¿El token es de un solo uso o admite varios escaneos dentro de su duración? | Miguel (backend) | Alcance de A-04 |
| Q-04 | ¿Qué formato tiene el código corto para escribirlo a mano? ¿Cuántos caracteres? | Miguel (backend) | Tareas de entrada manual |
| Q-05 | ¿Un negocio con varios tipos (veterinaria y estética a la vez) ve el nivel más alto? | Equipo / profesor | Reglas de visualización |
| Q-06 | ¿Qué librería de escaneo se usa y funciona sin servicios de Google? | Equipo | Decisión del plan |
| Q-07 | ¿El escaneo queda registrado para el dueño? (hereda la pregunta abierta de 009) | Equipo | Alcance |

---

## 9. Puerta de aprobación

- [ ] Alcance y fuera de alcance revisados.
- [ ] **Supuesto 3 y 4 confirmados**: el filtrado por tipo de negocio lo hace
      el backend, no la app.
- [ ] Preguntas abiertas con dueño, en especial Q-02.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — POST /api/v1/scans/resolve, recibe { code } y devuelve la ficha. Los negocios atendidos salen de GET /api/v1/scans/get-attended.
- **Q-04 RESUELTA (27/11/2026)** — El codigo corto es el campo qrCode que devuelve pets/get-qr/{id}, el mismo que recibe scans/resolve en { code }.
