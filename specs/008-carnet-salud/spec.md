# Spec 008 — Carnet de salud

> Unidad de trabajo: `008-carnet-salud`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0
> Depende de: [`007-mis-mascotas`](../007-mis-mascotas/spec.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:

1. El carnet tiene seis secciones: vacunas, desparasitación, peso, alergias,
   condiciones e historial médico. Todo se muestra en una línea de tiempo con
   filtros por tipo y por rango de fechas.

2. El carnet se lee siempre de Room y debe verse completo sin conexión,
   incluida la foto ya cacheada. Es el caso real de estar en la veterinaria sin
   señal. Ninguna sección puede requerir red para mostrarse.

3. Las alergias son información crítica: se muestran siempre, con distintivo
   propio, y no se pueden ocultar al compartir en 009. Esta regla se escribe
   aquí y 009 la consume.

4. Quién registra: el dueño registra vacunas, desparasitación y peso a mano; el
   veterinario registra la consulta desde 014. El carnet muestra ambos y
   distingue el origen.

5. Los registros de origen veterinario no se editan ni se borran desde el
   carnet; solo se ocultan de la vista. Es una decisión, no una pregunta.

6. Vacuna: nombre, fecha, dosis, próxima dosis, lugar o veterinario y
   comprobante opcional (foto moderada por 004).

7. Desparasitación: tipo (interna o externa), producto, fecha y próxima.
   Peso: valor en kilogramos y fecha, en lista y con evolución simple.

8. Condición: nombre, desde cuándo, notas y si es crónica.

9. Los recordatorios de próxima dosis los envía 006; aquí solo se guardan y se
   muestran las fechas.

10. La escritura sin conexión sigue la política de 005; la lectura está siempre
    disponible.

11. La sección de historial médico existe aunque 014 aún no esté construida: se
    muestra vacía con su estado.

12. Fuera de alcance: QR y compartir (009), recetas y cobro (014), seguimiento
    (015) e IA que explique la receta (019).

→ Corrígeme ahora o avanzo con esto.
```

---

## 1. Objetivo

Que el dueño **tenga el historial de salud de su mascota completo, ordenado y
consultable en el teléfono, incluso sin señal**, con las alergias siempre
visibles y con la certeza de que lo que escribió un veterinario no se puede
alterar desde el carnet.

Esta unidad es el corazón del producto: lo que se comparte por QR, lo que el
veterinario escanea y lo que alimenta los recordatorios.

**Para quién:** el dueño en la veterinaria y el veterinario que revisa el
historial.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Sección de vacunas con dosis, fecha y próxima dosis |
| A-02 | Sección de desparasitación interna y externa |
| A-03 | Registro de peso con evolución en el tiempo |
| A-04 | Sección de alergias marcada como crítica |
| A-05 | Sección de condiciones, crónicas o temporales |
| A-06 | Historial médico con las consultas de 014 |
| A-07 | Línea de tiempo unificada con filtros por tipo y fecha |
| A-08 | Lectura completa sin conexión desde Room |
| A-09 | Distinción de origen: dueño o veterinario |
| A-10 | Registros veterinarios no editables desde el carnet |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Compartir el carnet por QR | Spec 009 |
| Consulta, diagnóstico, receta y cobro | Spec 014 |
| Seguimiento del tratamiento | Spec 015 |
| Recordatorios y notificaciones de dosis | Spec 006 |
| Explicación de la receta con IA | Spec 019 |
| Edición de datos de la mascota | Spec 007 |
| Historial de varios dueños para una mascota | No está en la maqueta |
| Exportación a PDF | No está en los lineamientos |

**Ninguna regla de negocio del negocio afiliado se escribe aquí.**

---

## 4. Requisitos

### Funcionales del carnet

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El carnet debe mostrar vacunas, desparasitación, peso, alergias, condiciones e historial médico | Maqueta; lineamientos móvil |
| R-02 | El dueño debe poder registrar vacunas, desparasitación y peso | Maqueta |
| R-03 | Cada vacuna debe guardar nombre, fecha, dosis, próxima dosis y lugar | Maqueta |
| R-04 | La desparasitación debe distinguir interna y externa, con producto y fecha | Maqueta |
| R-05 | El peso debe registrarse con fecha y mostrarse en orden cronológico | Maqueta |
| R-06 | Las alergias deben mostrarse siempre, con distintivo de crítica, y no deben poder ocultarse | Constitución P10; 009 R-04 |
| R-07 | Las condiciones deben indicar si son crónicas y desde cuándo | Maqueta |
| R-08 | El historial médico debe listar las consultas registradas por el veterinario | 014 |
| R-09 | La línea de tiempo debe poder filtrarse por tipo y por rango de fechas | Maqueta |
| R-10 | Todo el carnet debe verse sin conexión, después de una primera sincronización | Constitución P4; 001 R-01 |
| R-11 | Los registros de origen veterinario no deben poder editarse ni borrarse desde el carnet | RNF-01 |
| R-12 | El carnet debe distinguir visualmente el origen de cada registro | RNF-16 |
| R-13 | La próxima dosis debe guardarse como fecha y quedar disponible para 006 | 006 R-01 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-14 | La lectura debe venir de Room; la red solo actualiza la base local | Constitución P4; 001 R-02 |
| R-15 | La escritura sin conexión debe seguir la política de 005 | 005 R-01 |
| R-16 | Las fotos de comprobante deben pasar por la moderación de 004 antes de subirse | 004 R-04 |
| R-17 | Los textos deben vivir en `strings.xml` y las fechas mostrarse en el formato local | RNF-16 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Con el modo avión activo, las seis secciones muestran sus datos guardados | Prueba instrumentada con red ausente |
| C-02 | Abrir el carnet sin red no dispara ninguna petición de red | Prueba con doble de API que cuenta llamadas (debe ser cero) |
| C-03 | Una vacuna registrada aparece en la línea de tiempo y en su sección | Prueba de interfaz y de `Flow` |
| C-04 | La próxima dosis se guarda y se recupera con la fecha correcta | Prueba de dominio con reloj fijo |
| C-05 | Una alergia se muestra siempre y lleva el distintivo de crítica | Prueba de interfaz |
| C-06 | El caso de uso de alergias críticas devuelve todas las alergias activas sin depender de filtros ni de secciones | Prueba de dominio |
| C-07 | Un registro de origen veterinario no ofrece editar ni borrar | Prueba de interfaz |
| C-08 | Un registro del dueño sí se puede editar y borrar con confirmación | Prueba de interfaz |
| C-09 | Los filtros por tipo y por fecha reducen la lista a lo esperado | Prueba de interfaz con datos fijos |
| C-10 | El peso se muestra en orden cronológico y con su evolución | Prueba de interfaz |
| C-11 | Un registro creado sin red queda pendiente y se sincroniza según 005 | Prueba con red ausente |
| C-12 | Una foto de comprobante rechazada no se adjunta al registro | Prueba con doble de moderación |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El carnet depende de la red para alguna sección y falla en la veterinaria | Alto | Lectura solo de Room; prueba C-01 y C-02 |
| G-02 | Una alergia se pierde al compartir porque el usuario limitó secciones | Alto | Regla R-06 y caso de uso de alergias críticas; 009 la consume |
| G-03 | El dueño edita o borra lo que escribió el veterinario | Alto | R-11 y prueba C-07 |
| G-04 | Los registros de distintas fuentes se duplican al sincronizar | Medio | Política de 005 e idempotencia |
| G-05 | Las fechas se muestran en formato incorrecto o con zona horaria errada | Medio | Formato local y prueba con fechas fijas |
| G-06 | El historial médico queda vacío y parece un error | Bajo | Estado vacío explicativo hasta que 014 exista |
| G-07 | El comprobante sin moderar se sube | Alto | 004 y prueba C-12 |

---

## 7. Dependencias

**De esta unidad:**

- `007-mis-mascotas`: la mascota existe y se puede seleccionar.
- `005`: política de escritura sin conexión.
- `004`: moderación del comprobante.
- `001`: Room, red, caché, navegación y componentes.

**Externas:**

- Contrato del backend del carnet: rutas, campos y códigos (Q-01).
- Catálogo de vacunas y productos de desparasitación (Q-02).
- Unidades y formato del peso (Q-03).

**Unidades posteriores que dependen de esta:**

- `009` (compartir), `010` ("lo más próximo"), `014` (consulta), `015`
  (seguimiento) y `019` (IA sobre el historial).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuáles son las rutas, los campos y los códigos del backend del carnet? | Miguel (backend) | Todas las tareas de red |
| Q-02 | ¿Existe un catálogo de vacunas y de productos de desparasitación o son texto libre? | Equipo / backend | Formularios |
| Q-03 | ¿El peso se guarda en kilogramos o se admite otra unidad? ¿Se muestra gráfica o solo lista? | Equipo / maqueta | Peso |
| Q-04 | ¿El dueño puede borrar un registro que él mismo creó? ¿Se conserva el histórico? | Equipo | Edición |
| Q-05 | ¿Qué tipos de condición existen (crónica, temporal, alergia) y se pueden combinar? | Equipo / maqueta | Condiciones |
| Q-06 | ¿La dosis de la vacuna es texto libre o un número con unidad? | Equipo / backend | Vacunas |
| Q-07 | ¿El comprobante admite imagen, PDF o ambos? | Equipo | Adjuntos |
| Q-08 | ¿La "próxima dosis" se calcula desde un intervalo o la escribe el usuario? | Equipo / backend | Recordatorios |
| Q-09 | ¿El historial médico muestra la consulta completa o solo el resumen? | Equipo / 014 | Historial |
| Q-10 | ¿Se puede registrar una vacuna sin veterinario o lugar? | Equipo / maqueta | Vacunas |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El alcance y el fuera de alcance están revisados.
- [ ] La regla de alergias siempre compartidas está aceptada.
- [ ] Los requisitos y los criterios de éxito son suficientes para planificar.
- [ ] Las preguntas abiertas están identificadas y se sabe a quién preguntar.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — vaccines (create, create-by-business, get-all, delete/{id}), dewormings (los mismos 4), weights (create, get-all) y conditions. Detalle en backend-petmate-sdd-aspnetcore/openapi/openapi.json (76 rutas, 132 esquemas). El backend esta TERMINADO y desplegado; el contrato completo esta ahi, no hay que preguntarle a nadie.
