# Spec 007 — Mis mascotas

> Unidad de trabajo: `007-mis-mascotas`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0
> Depende de: [`002-identidad-y-sesion`](../002-identidad-y-sesion/spec.md),
> [`003-permisos`](../003-permisos/spec.md),
> [`004-moderacion-contenido`](../004-moderacion-contenido/spec.md) y
> [`005-sincronizacion-offline`](../005-sincronizacion-offline/spec.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:

1. La mascota se describe con nombre, especie, raza, sexo, fecha de nacimiento,
   color, foto y esterilizado. El peso y todo lo clínico son de 008.

2. La lista muestra una tarjeta por mascota con foto o iniciales, nombre, edad
   calculada, especie y raza. Hay estado vacío con llamada a crear la primera.

3. La edad se calcula en `domain` con el reloj inyectable de 001, no con la
   fecha del dispositivo leída a mano.

4. La foto se elige de galería o cámara (003), se modera (004) antes de subir y
   se cachea con Coil. Es la primera unidad que carga imágenes: aquí se agrega
   `coil-compose`, con autorización previa.

5. Sin conexión se puede crear y editar la mascota según 005. La foto requiere
   red para moderarse y subirse, así que la mascota se guarda sin foto y la
   imagen se sube después.

6. El catálogo de razas y los campos obligatorios van a preguntas abiertas; no
   se inventa una lista de razas.

7. Borrar una mascota exige confirmación explícita y es un borrado lógico. Qué
   pasa con su carnet y con los QR vigentes va a preguntas abiertas.

8. El límite de mascotas por cuenta va a preguntas abiertas.

9. La tarjeta puede mostrar la próxima vacuna, pero ese cálculo es de 008; aquí
   solo se reserva el espacio.

10. Esta unidad retira la pantalla de verificación desechable de 001, como
    estaba previsto en su plan.

11. Fuera de alcance: carnet (008), QR (009), mapa (010), ML de raza (018) y
    chat (016).

→ Corrígeme ahora o avanzo con esto.
```

---

## 1. Objetivo

Que el dueño **registre a sus mascotas, las edite, les ponga foto y las vea en
una lista clara**, con o sin conexión, y que esa lista sea la puerta de entrada
al carnet de salud.

Esta unidad es la primera feature real de PetMate: la que convierte una cuenta
en un usuario con mascotas y la que estrena la subida de imágenes con permisos
y moderación.

**Para quién:** el dueño de mascotas, desde el inicio de la app ya
autenticado.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Alta de mascota con sus datos básicos |
| A-02 | Edición de los datos de la mascota |
| A-03 | Foto de perfil con permiso, moderación y caché |
| A-04 | Lista de mascotas con tarjeta y estado vacío |
| A-05 | Edad calculada de forma consistente |
| A-06 | Creación y edición sin conexión, con sincronización posterior |
| A-07 | Borrado con confirmación y borrado lógico |
| A-08 | Aislamiento por usuario: cada quien ve solo sus mascotas |
| A-09 | Retiro de la pantalla de verificación de 001 |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Vacunas, peso, alergias, condiciones e historial | Spec 008 |
| Compartir el historial por QR | Spec 009 |
| Mapa y negocios | Spec 010 |
| Identificación de raza por foto | Spec 018 |
| Fotos y texto del chat | Spec 016 |
| Edición del perfil del dueño | Spec 017 |
| Mascotas compartidas entre varias cuentas | No está en la maqueta |
| Recordatorios de vacunas | Specs 006 y 008 |

**Ninguna regla clínica se escribe aquí.**

---

## 4. Requisitos

### Funcionales de mascotas

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | El usuario debe poder crear una mascota con nombre, especie, raza, sexo, fecha de nacimiento, color y esterilizado | Maqueta; lineamientos móvil |
| R-02 | El usuario debe poder editar los datos de una mascota existente | Maqueta |
| R-03 | El usuario debe poder elegir o cambiar la foto de la mascota | Maqueta |
| R-04 | La foto debe pasar por el permiso correspondiente y por la moderación antes de subirse | 003 R-07; 004 R-04 |
| R-05 | La lista debe mostrar una tarjeta por mascota con foto o iniciales, nombre, edad, especie y raza | Maqueta |
| R-06 | Sin mascotas, la lista debe mostrar un estado vacío con la acción de crear | RNF-16 |
| R-07 | La edad debe calcularse de forma consistente con la fecha de nacimiento y el reloj del sistema | RNF-16 |
| R-08 | Crear y editar deben funcionar sin conexión, según la política de 005 | Constitución P4; 005 R-01 |
| R-09 | Borrar una mascota debe pedir confirmación y no debe eliminar datos de forma irreversible sin aviso | RNF-16 |
| R-10 | Cada usuario debe ver únicamente sus propias mascotas | RNF-01 |
| R-11 | La tarjeta debe reservar el espacio de la próxima vacuna, que calcula 008 | 008 R-05 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-12 | La lista debe leerse de Room y actualizarse sola al cambiar los datos | Constitución P4; 001 R-02 |
| R-13 | La subida de la foto no debe bloquear el guardado de la mascota | RNF-15 |
| R-14 | Los textos deben vivir en `strings.xml` y las imágenes usar caché con expiración | RNF-16; 001 R-08 |
| R-15 | La pantalla de verificación de 001 debe quedar fuera del grafo de producto | 001 A-15 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Crear una mascota con datos válidos la muestra en la lista al instante | Prueba de interfaz con doble de repositorio |
| C-02 | Editar un dato se refleja en la tarjeta sin recargar la pantalla | Prueba de interfaz y de `Flow` |
| C-03 | Sin mascotas, la lista muestra el estado vacío y la acción de crear | Prueba de interfaz |
| C-04 | La edad mostrada coincide con la calculada con un reloj fijo | Prueba de dominio con reloj fijo |
| C-05 | Sin red, la mascota se crea y queda pendiente de sincronizar | Prueba con red simulada como ausente |
| C-06 | La foto se modera antes de subirse; una foto rechazada no se adjunta | Prueba con doble de moderación |
| C-07 | Sin permiso de fotos, la tarjeta usa iniciales y permite reintentar | Prueba con permiso denegado |
| C-08 | Borrar pide confirmación y, al confirmar, la mascota desaparece de la lista | Prueba de interfaz |
| C-09 | Un usuario no ve las mascotas de otro | Prueba con dos sesiones y doble de API |
| C-10 | La lista se lee de Room y funciona con el modo avión | Prueba instrumentada |
| C-11 | La pantalla de verificación de 001 ya no está en el grafo de producto | Revisión de `AppNavHost` |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El catálogo de razas no existe y se inventa | Medio | Q-02; mientras no exista, raza como texto libre acotado |
| G-02 | Una foto sin moderar se sube por un atajo | Alto | Todo pasa por 004; prueba C-06 |
| G-03 | Borrar una mascota deja su carnet huérfano | Alto | **Q-04 RESUELTA 27/11/2026: no aplica.** El borrado es físico y en cascada; el carnet se borra con la mascota y el QR muere con ella |
| G-04 | La edad se calcula distinto en dos pantallas | Medio | Un solo caso de uso con reloj inyectable; prueba C-04 |
| G-05 | Un usuario ve mascotas ajenas por un filtro mal puesto | Alto | Filtro por sesión en el repositorio y prueba C-09 |
| G-06 | La subida de la foto bloquea el guardado | Medio | Se guarda primero la mascota y la foto se sube después |
| G-07 | La pantalla de verificación se queda en producción | Bajo | T-010 la retira y C-11 lo comprueba |

---

## 7. Dependencias

**De esta unidad:**

- `002-identidad-y-sesion`: usuario autenticado y sesión.
- `003-permisos`: permiso de cámara y fotos, con su degradación.
- `004-moderacion-contenido`: moderación de la imagen antes de subir.
- `005-sincronizacion-offline`: política de creación y edición sin conexión.
- `001`: Room, red, caché, navegación, componentes y pruebas.

**Externas:**

- Contrato del backend de mascotas: rutas, campos, códigos y límites (Q-01).
- Catálogo de especies y razas (Q-02).
- Límite de mascotas y semántica del borrado (Q-03, Q-04).

**Unidades posteriores que dependen de esta:**

- `008` (carnet), `009` (compartir), `010` (mapa y demo), `014` (consulta),
  `016` (chat) y `018` (raza por foto).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuáles son las rutas, los campos, los códigos y los límites del backend de mascotas? | Miguel (backend) | Todas las tareas de red |
| Q-02 | ¿Existe un catálogo de especies y razas o la raza es texto libre? ¿Quién lo mantiene? | Equipo / backend | Formulario |
| Q-03 | ¿Cuántas mascotas puede tener una cuenta? | Equipo | Alta |
| Q-04 | Al borrar una mascota, ¿se borra su carnet, se archiva o se conserva? ¿Los QR vigentes se invalidan? | Equipo / backend | Borrado |
| Q-05 | ¿Qué formatos, tamaño máximo y proporción se aceptan para la foto? | Equipo / backend | Foto |
| Q-06 | ¿El campo "esterilizado" existe en la maqueta y es obligatorio? | Equipo / maqueta | Formulario |
| Q-07 | ¿Se puede compartir una mascota con otra cuenta (familia)? | Equipo | Aislamiento |
| Q-08 | ¿Qué campos son obligatorios y cuáles opcionales? | Equipo / maqueta | Validación |
| Q-09 | ¿La lista se ordena por nombre, por fecha de creación o por última actividad? | Equipo / maqueta | Lista |
| Q-10 | ¿La foto se guarda también en local para verla sin conexión? | Equipo | Caché |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El alcance y el fuera de alcance están revisados.
- [ ] La dependencia de Coil está autorizada.
- [ ] Los requisitos y los criterios de éxito son suficientes para planificar.
- [ ] Las preguntas abiertas están identificadas y se sabe a quién preguntar.
- [ ] **Aprobada para pasar a `plan.md`.**


## Preguntas resueltas contra el backend ya terminado

Estas estaban marcadas como "esperando a Miguel". El backend se termino y se desplego, asi que
se respondieron leyendo su codigo y su contrato. Las tareas que solo dependian de ellas YA NO
ESTAN BLOQUEADAS.

- **Q-01 RESUELTA (27/11/2026)** — 10 rutas bajo /api/v1/pets: create, get-all, get-by-id/{id}, update/{id}, delete/{id}, get-qr/{id}, regenerate-qr/{id}, upload-photo/{id}, get-photo-url/{id}, delete-photo/{id}. PetResponse: id, name, species, breed, birthDate, sex, photoUrl, createdAt, updatedAt. Detalle en backend-petmate-sdd-aspnetcore/openapi/openapi.json (76 rutas, 132 esquemas). El backend esta TERMINADO y desplegado; el contrato completo esta ahi, no hay que preguntarle a nadie.

- **Q-02 RESUELTA (27/11/2026)** — No hay catálogo de razas y nadie lo mantiene. La **especie** sí es lista cerrada de exactamente dos valores, `"Perro"` y `"Gato"` (`Pet.SpeciesDog` / `Pet.SpeciesCat` en `PetMate.Domain/Pets/Pet.cs`; `Pet.IsValidSpecies` rechaza cualquier otro). La **raza** es texto libre opcional de hasta 60 caracteres (`Pet.MaxBreedLength`, `IsValidBreed(required: false)`). Leído del código del backend terminado, no supuesto. Esto confirma la mitigación que ya traía el riesgo G-01.
- **Q-04 RESUELTA (27/11/2026)** — El borrado **no es lógico: es físico y en cascada** (`DeleteMyPetHandler`, pm-104). Borra primero los hijos —recordatorios, vacunas, desparasitaciones, pesos, alergias, consultas, fotos de chat, mensajes, conversaciones, seguimientos, permisos de acceso y escaneos— y al final la mascota, **que se lleva su QR**. Cada paso es idempotente. Consecuencias: el carnet **no queda huérfano** (desaparece con la mascota) y los QR vigentes **mueren con ella**, no se invalidan por separado. Esto **corrige el riesgo G-03**, que suponía borrado lógico con alcance decidido por el servidor.
- **Q-06 RESUELTA (27/11/2026)** — El campo «esterilizado» **no existe en el contrato**: no está en `PetDetails`, ni en `PetRequest`, ni en `PetResponse`, ni en `PetValidation`; `grep -rni "steril|esteril|neuter|castr"` sobre todo `src/` del backend no devuelve ni una coincidencia en código. No hay dónde guardarlo, así que el formulario **no debe ofrecerlo**. Si la maqueta lo pinta, es un desacuerdo maqueta-contrato que hay que llevar al equipo; no es algo que la interfaz pueda persistir.
- **Q-08 RESUELTA (27/11/2026)** — De `PetValidation.Check` y las constantes de `Pet`: **obligatorios** nombre (1 a 50 caracteres) y especie (`Perro`|`Gato`); **obligatorio con valor por omisión** sexo (`Male`|`Female`|`Unknown`, así que `Unknown` siempre satisface al servidor); **opcionales** raza (≤60), fecha de nacimiento (ni futura ni de hace más de 50 años) y foto (URL HTTPS, ≤2048). `PetRequest` marca los seis como `required` en el JSON, pero cuatro aceptan `null`: lo que manda es la validación de arriba, no la lista `required` del esquema.
- **Q-09 RESUELTA (27/11/2026)** — El orden lo fija el servidor: `MongoPetRepository.ListByOwner` ordena `SortByDescending(CreatedAtUtc).ThenByDescending(Id)`, es decir **la más recién creada primero**, y pagina con `skip`/`take`. El cliente **no debe reordenar** (serían dos criterios que pueden no coincidir, igual que la lección de 010/Q-06). Verificado: no hay ningún `sortedBy` sobre la lista de mascotas en `ui/pets/` ni en `PetRepositoryImpl`.
