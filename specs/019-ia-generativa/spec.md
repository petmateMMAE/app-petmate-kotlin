# Spec 019 — IA generativa: explicar la receta y resumir el historial

> Unidad de trabajo: `019-ia-generativa`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026
> Alineada con [`constitution.md`](../../constitution.md) y
> [`MAPA-CAPACIDADES.md`](../MAPA-CAPACIDADES.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. Esta unidad cumple el requisito del punto 16: "al menos una función de IA
   generativa" dentro del proyecto móvil.
2. Son dos funciones sobre contenido que YA EXISTE:
   a) "Explícamelo fácil": traduce la receta del veterinario a lenguaje
      cotidiano.
   b) "Resumen del historial": condensa el carnet en un párrafo legible.
3. SUPUESTO CRÍTICO: la IA NO da consejo veterinario, no diagnostica, no
   sugiere tratamientos y no modifica dosis. Solo EXPLICA lo que un
   veterinario ya escribió.
4. SUPUESTO CRÍTICO: la llamada al proveedor de IA la hace el BACKEND, no la
   app. La llave nunca vive en el dispositivo (P6), igual que en 004.
5. El texto generado se marca siempre como generado por IA y se acompaña de
   la advertencia de consultar al veterinario ante dudas.
6. El texto original del veterinario SIEMPRE está disponible y visible. La
   explicación es un añadido, nunca un reemplazo.
7. La función requiere conexión. Sin red, se muestra el texto original.
8. El resultado se cachea para no regenerar lo mismo y gastar cuota.
9. El proveedor concreto no está decidido. La spec define el comportamiento,
   no la tecnología.
10. Si la generación falla, la pantalla sigue siendo útil con el contenido
    original.
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Ayudar al dueño a **entender la información clínica de su mascota**,
traduciendo la receta y el historial médico a un lenguaje que cualquiera
comprenda, sin sustituir al veterinario.

**Por qué:** los lineamientos exigen al menos una función de IA generativa.
Entre las opciones, ésta resuelve un problema real: una receta que dice
"amoxicilina 250 mg c/12 h VO x 7 d" no le dice nada a la mayoría de los
dueños, y la mala comprensión de una indicación es una causa frecuente de que
un tratamiento no funcione.

**Para quién:** el dueño de la mascota que acaba de recibir una receta o que
revisa el historial de su animal.

**Cómo se ve el éxito:** el dueño toca "explícamelo fácil" y entiende que debe
dar media pastilla cada doce horas durante una semana, con comida.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Explicación en lenguaje sencillo de una receta |
| A-02 | Resumen del historial médico de una mascota |
| A-03 | Marca visible de que el texto fue generado por IA |
| A-04 | Advertencia de consultar al veterinario ante dudas |
| A-05 | Acceso permanente al texto original del veterinario |
| A-06 | Caché del resultado para no regenerarlo |
| A-07 | Degradación limpia si la generación falla o no hay red |
| A-08 | Indicador mientras se genera |
| A-09 | Posibilidad de regenerar manualmente |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Registrar la receta y la consulta | Spec 014 |
| El carnet y el historial | Spec 008 |
| **Diagnosticar, sugerir tratamientos o cambiar dosis** | **Fuera del producto** (supuesto 3) |
| Responder preguntas abiertas de salud | **Fuera del producto**: sería consulta médica |
| Chat con la IA | No está en la maqueta |
| Traducir a otros idiomas | Pregunta abierta Q-06 |
| Generar contenido para el negocio | No está en la maqueta |
| ML de raza por foto | Spec 018 |

---

## 4. Requisitos

### Funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El sistema debe explicar una receta en lenguaje sencillo | **Punto 16 (obligatorio)** |
| R-02 | El sistema debe resumir el historial médico de una mascota | Punto 16 |
| R-03 | Todo texto generado debe marcarse como generado por IA | Transparencia |
| R-04 | Todo texto generado debe acompañarse de la advertencia de consultar al veterinario | **Seguridad del animal** |
| R-05 | El texto original del veterinario debe permanecer accesible | **Supuesto 6** |
| R-06 | El usuario debe poder pedir que se regenere | Usabilidad |

### No funcionales

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-07 | La llamada al proveedor debe hacerla el backend, no la app | RNF-01; constitución P6 |
| R-08 | Ninguna llave de proveedor debe existir en el dispositivo | **Constitución P6** |
| R-09 | El resultado debe cachearse para no regenerar lo mismo | Costo; RNF-28 |
| R-10 | Si la generación falla, la pantalla debe seguir siendo útil | Robustez |
| R-11 | La generación no debe bloquear el hilo principal | RNF-15 |
| R-12 | La generación debe responder en menos de 10 s o avisar | RNF-15 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Con una receta registrada, la explicación aparece en lenguaje cotidiano | Prueba con doble de backend y receta conocida |
| C-02 | El texto original del veterinario sigue visible junto a la explicación | Prueba de interfaz |
| C-03 | La explicación está marcada como generada por IA | Prueba de interfaz |
| C-04 | La advertencia de consultar al veterinario es visible | Prueba de interfaz |
| C-05 | El resumen del historial condensa el carnet sin inventar datos | Prueba con historial conocido y revisión del contenido |
| C-06 | Pedir la misma explicación dos veces usa la caché y no regenera | Prueba con conteo de llamadas |
| C-07 | Regenerar manualmente sí llama de nuevo | Prueba del botón de regenerar |
| C-08 | **No existe ninguna llave de proveedor en el dispositivo ni en el código** | Revisión del agente `seguridad` y búsqueda en el repositorio |
| C-09 | **La app no llama al proveedor de IA directamente** | Inspección del tráfico: solo hay llamadas al backend |
| C-10 | Si la generación falla, se muestra el texto original con un aviso | Prueba con doble que falla |
| C-11 | Sin conexión, se muestra el texto original sin intentar generar | Prueba con red ausente |
| C-12 | Mientras genera, hay un indicador y la pantalla responde | Prueba de hilo principal |
| C-13 | Si la generación tarda más de 10 s, se avisa y se puede cancelar | Prueba con servidor lento |
| C-14 | La interfaz **no ofrece** preguntar dudas de salud a la IA | Revisión de la interfaz |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | La IA inventa o altera una indicación y alguien medica mal a su animal | **Crítico** | R-05: el original siempre visible; R-04: advertencia; alcance limitado a explicar, nunca a recomendar; revisión del texto en C-05 |
| G-02 | El usuario toma la explicación como consejo veterinario | **Alto** | R-03 y R-04; C-14: la interfaz no ofrece hacer preguntas abiertas |
| G-03 | La llave del proveedor acaba en el dispositivo | **Alto** | R-07 y R-08; C-08 y C-09 lo verifican |
| G-04 | El costo por generación se dispara | Medio | R-09: caché; el límite de uso lo controla el backend (Q-03) |
| G-05 | Datos clínicos de la mascota viajan a un tercero sin que el usuario lo sepa | **Alto** | Se declara en el aviso de privacidad (017); el backend decide qué envía (Q-04) |
| G-06 | El proveedor no está disponible y la función queda rota | Medio | R-10 y C-10: degradación al texto original |
| G-07 | La generación es lenta y frustra | Medio | R-12 y C-13: aviso y cancelación |
| G-08 | El resumen omite una alergia importante | **Alto** | Las alergias se muestran siempre por 008, fuera del resumen generado |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base`: red, errores, caché, componentes.
- `002-identidad-y-sesion`: sesión del usuario.
- `008-carnet-salud`: el historial que se resume.
- `014-consulta-veterinaria`: la receta que se explica.
- `017-ajustes-privacidad`: el aviso de privacidad debe declarar este uso.

**Externas:**

- Endpoint del backend que llama al proveedor (Q-01).
- Decisión del proveedor y su costo (Q-02).

**Unidades posteriores que dependen de esta:**

- Ninguna. Es una función opcional sobre 008 y 014.

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuál es la ruta del backend para pedir una explicación o un resumen? | Miguel (backend) | Tareas de red |
| Q-02 | ¿Qué proveedor de IA se usa y cuál es su costo por llamada? | Equipo / profesor | Decisión del backend |
| Q-03 | ¿Hay límite de generaciones por usuario o por día? | Equipo / backend | Riesgo G-04 |
| Q-04 | ¿Qué datos exactos se envían al proveedor? ¿Van anonimizados? | Miguel / profesor | Riesgo G-05 y aviso de privacidad |
| Q-05 | ¿El aviso de privacidad de la landing ya declara el uso de IA? | Equipo / landing | Cumplimiento |
| Q-06 | ¿Se requiere la explicación en otros idiomas? | Equipo | Alcance |
| Q-07 | ¿El resumen del historial se genera bajo demanda o se precalcula? | Equipo | Diseño de la caché |

---

## 9. Puerta de aprobación

- [ ] Alcance y fuera de alcance revisados.
- [ ] **Supuesto 3 confirmado**: la IA explica, nunca aconseja ni diagnostica.
- [ ] **Supuesto 4 confirmado**: la llamada la hace el backend.
- [ ] Preguntas abiertas con dueño, en especial Q-04 (qué datos viajan).
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — NO existe endpoint de IA generativa en el backend.
