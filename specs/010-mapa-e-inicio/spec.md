# Spec 010 — Mapa e inicio

> Unidad de trabajo: `010-mapa-e-inicio`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0
> Depende de: [`002-identidad-y-sesion`](../002-identidad-y-sesion/spec.md),
> [`003-permisos`](../003-permisos/spec.md),
> [`007-mis-mascotas`](../007-mis-mascotas/spec.md) y
> [`008-carnet-salud`](../008-carnet-salud/spec.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:

1. El mapa es la pantalla de INICIO de la app para el rol de dueño, una vez
   autenticado. No hay un menú de inicio separado: al abrir la app se ve el
   mapa con los negocios afiliados.

2. Los tipos de negocio son veterinaria, tienda y estética, más los que el
   directorio del backend declare. Cada tipo tiene su icono y su color, y se
   puede filtrar por tipo. La lista exacta de tipos va a preguntas abiertas.

3. El mapa muestra marcadores del directorio de negocios del backend. 010 los
   PINTA; no los crea ni los edita. El alta de negocios es 011 y el panel es
   012. El pin que el afiliado coloca en 011 se verá aquí cuando exista.

4. Al tocar un marcador se abre una tarjeta resumida del negocio y un carrusel
   horizontal con los negocios visibles. El carrusel y el marcador seleccionado
   están sincronizados: mover el carrusel cambia la selección y viceversa.

5. "Lo más próximo" NO es distancia: es el PRÓXIMO EVENTO DE SALUD de las
   mascotas del usuario, tomado del carnet de 008 (vacuna o desparasitación).
   Se pinta rojo si faltan 3 días o menos y amarillo si faltan 14 días o menos,
   contados desde hoy hasta la próxima dosis. Es una decisión cerrada, no una
   pregunta.

6. La tarjeta "lo más próximo" necesita 008, que va antes en el mapa (ola 4
   frente a ola 5). Si 010 se construyera antes, la tarjeta no tendría de dónde
   leer; por eso la dependencia es explícita y bloqueante.

7. El demo del profesor se apoya en 002 (sesión), 007 (mascotas) y 008
   (carnet): se entra, se ve el mapa, se abre una mascota y se muestra su
   historial. El demo NO escribe en el backend ni crea datos de prueba en
   producción: usa datos locales de demostración.

8. Sin permiso de ubicación el mapa abre igual, con un centro por defecto y la
   lista de negocios; la ubicación solo se pide al abrir el mapa o al pulsar
   "cerca de mí", conforme a 003.

9. El mapa y el directorio se cachean para verse sin conexión; sin red se
   muestra la última versión cacheada con un aviso de que puede estar
   desactualizada. Nunca se queda en blanco.

10. **RESUELTO el 04/11/2026**: el proveedor es **osmdroid con teselas de
    OpenStreetMap**. Se eligió porque no requiere llave de API, registro ni
    tarjeta de crédito, y cumple el requisito de costo cero del profesor.
    Google Maps quedó descartado porque exige habilitar facturación, aunque
    la carga del mapa en Android sea ilimitada. El supuesto original decía:
    el proveedor de mapas (Google Maps, MapLibre u otro) NO está decidido. Va
    a Q-01 y bloquea la dependencia del SDK y las tareas que lo tocan; no
    bloquea la spec ni el resto del plan.

11. La búsqueda por texto de negocios y el detalle completo del negocio son de
    013. Aquí solo hay filtros por tipo, marcadores, carrusel y tarjeta
    resumida.

12. Fuera de alcance: alta de negocios (011), panel del afiliado (012), ficha
    del negocio y escaneo (013), consulta (014) y chat (016).

→ Corrígeme ahora o avanzo con esto.
```

---

## 1. Objetivo

Que al abrir PetMate el dueño **vea de inmediato las veterinarias, tiendas y
estéticas afiliadas en un mapa, pueda filtrarlas por tipo, descubra qué
negocio tiene cerca y sepa a qué mascota se le viene encima la próxima
vacuna**, sin que un permiso negado o la falta de red le impidan usar la app.

Esta unidad es la cara del producto y la pantalla que sostiene el demo: el
mapa como inicio, el carrusel como exploración y la tarjeta de salud como
recordatorio útil.

**Para quién:** el dueño que abre la app y el profesor que ve el demo.

---

## 2. Alcance

| ID | Capacidad |
|---|---|
| A-01 | Mapa como pantalla de inicio tras autenticarse |
| A-02 | Marcadores de negocios por tipo, con icono y color |
| A-03 | Filtros por tipo de negocio |
| A-04 | Tarjeta resumida del negocio al tocar un marcador |
| A-05 | Carrusel horizontal sincronizado con el mapa |
| A-06 | Tarjeta "lo más próximo" con el próximo evento de salud de 008 |
| A-07 | Ubicación con permiso, centro por defecto y "cerca de mí" |
| A-08 | Mapa y directorio cacheados para verse sin conexión |
| A-09 | Modo demo con datos locales, sin escribir en el backend |
| A-10 | Estado vacío y de error del directorio |

---

## 3. Fuera de alcance

| Qué | Dónde se atenderá |
|---|---|
| Alta de negocio y colocación del pin | Spec 011 |
| Panel del afiliado y sus alertas | Spec 012 |
| Ficha completa del negocio y escaneo de QR | Spec 013 |
| Consulta, receta y cobro | Spec 014 |
| Seguimiento y chat | Specs 015 y 016 |
| Cálculo clínico de la próxima dosis | Spec 008 |
| Recordatorios y notificaciones de esa dosis | Spec 006 |
| Búsqueda por texto y ordenamiento avanzado | Pregunta abierta; no entra por defecto |
| Navegación giro a giro hasta el negocio | No está en la maqueta |
| Mapa en modo negocio (rol afiliado) | Spec 012 |

**Ninguna regla clínica se escribe aquí: se consume la de 008.**

---

## 4. Requisitos

### Funcionales del mapa y del inicio

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | Tras autenticarse, la pantalla de inicio debe ser el mapa de negocios | Maqueta; lineamientos móvil |
| R-02 | Cada negocio del directorio debe mostrarse como marcador con icono y color según su tipo | Maqueta |
| R-03 | El usuario debe poder filtrar los marcadores por tipo de negocio | Maqueta |
| R-04 | Al tocar un marcador debe abrirse una tarjeta resumida del negocio | Maqueta |
| R-05 | Debe existir un carrusel horizontal de negocios sincronizado con la selección del mapa | Maqueta |
| R-06 | La tarjeta "lo más próximo" debe mostrar el próximo evento de salud de las mascotas del usuario | Maqueta; 008 R-13 |
| R-07 | La tarjeta debe pintarse roja con 3 días o menos y amarilla con 14 días o menos hasta la próxima dosis | Decisión aprobada; maqueta |
| R-08 | Sin permiso de ubicación el mapa debe abrir con centro por defecto, lista y opción "cerca de mí" | 003 R-05 |
| R-09 | El mapa y el directorio deben verse sin conexión desde la caché, con aviso de posible desactualización | Constitución P4; 001 R-08 |
| R-10 | El directorio vacío o con error debe mostrar un estado explicativo y permitir reintentar | RNF-16 |
| R-11 | El modo demo debe funcionar con datos locales y no escribir en el backend | Decisión aprobada |
| R-12 | Tocar la tarjeta "lo más próximo" debe llevar al carnet de la mascota correspondiente | Maqueta; 008 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-13 | La app no debe contener la llave del proveedor de mapas en el código ni en recursos versionados | Constitución P6 |
| R-14 | El proveedor de mapas debe quedar detrás de una abstracción para poder cambiarlo sin tocar la UI | Constitución P3 |
| R-15 | El cálculo de días hasta la próxima dosis debe vivir en `domain` con reloj inyectable | RNF-16 |
| R-16 | La carga del mapa no debe bloquear el hilo principal; el directorio se carga en segundo plano | RNF-15 |
| R-17 | Los textos deben vivir en `strings.xml` y los iconos de tipo deben ser recursos del proyecto | RNF-16 |
| R-18 | Ningún permiso debe pedirse al arrancar; la ubicación solo al abrir el mapa o pulsar "cerca de mí" | Constitución P10; 003 R-01 |

---

## 5. Criterios de éxito

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Tras el login, la primera pantalla es el mapa | Prueba de navegación |
| C-02 | Cada negocio del directorio aparece como marcador con el icono de su tipo | Prueba de interfaz con doble de directorio |
| C-03 | Filtrar por un tipo deja solo los marcadores de ese tipo | Prueba de interfaz con datos fijos |
| C-04 | Tocar un marcador abre su tarjeta resumida | Prueba de interfaz |
| C-05 | Mover el carrusel cambia el marcador seleccionado y viceversa | Prueba de interfaz |
| C-06 | La tarjeta "lo más próximo" muestra la próxima dosis de la mascota con menos días | Prueba de dominio con reloj fijo |
| C-07 | Con 3 días o menos la tarjeta es roja; con 14 o menos, amarilla; con más, neutra | Prueba de dominio con tres fechas fijas |
| C-08 | Sin permiso de ubicación el mapa abre con centro por defecto y sigue mostrando la lista | Prueba con permiso denegado |
| C-09 | Al abrir la app no se pide ningún permiso | Prueba de interfaz del arranque |
| C-10 | Sin red, el mapa muestra el directorio cacheado con aviso de desactualización | Prueba con red ausente y caché previa |
| C-11 | Sin caché y sin red, se muestra un estado de error con reintento, no una pantalla en blanco | Prueba con red ausente y caché vacía |
| C-12 | El modo demo muestra mapa, mascota y carnet con datos locales y no hace ninguna petición de escritura | Prueba con doble de API que cuenta escrituras (debe ser cero) |
| C-13 | Tocar "lo más próximo" abre el carnet de la mascota correcta | Prueba de navegación |
| C-14 | No hay ninguna llave de mapas en el repositorio ni en `BuildConfig` | `grep` de llaves y revisión del APK |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | ~~El proveedor de mapas no está decidido~~ **Resuelto**: osmdroid + OSM | — | Decidido el 04/11/2026 |
| G-08 | La librería osmdroid está archivada desde nov 2024 (última versión 6.1.20) y no recibe actualizaciones | Medio | Funciona hoy y es estable; si un Android futuro la rompe, se migra a MapLibre. La abstracción de R-14 lo permite |
| G-09 | OpenStreetMap bloquea el tráfico si no se cumple su política de uso | Alto | User-Agent propio y estable, atribución visible, caché de 7 días y nada de descargas masivas. Todo implementado en `OsmMapConfig` |
| G-10 | OpenStreetMap no garantiza disponibilidad (no hay SLA) | Medio | Es aceptable para una app escolar; si el servicio falla, el mapa muestra su estado de error |
| G-02 | La llave del proveedor se filtra en el repositorio | Alto | R-13 y prueba C-14; la llave va por configuración de entorno |
| G-03 | El mapa se queda en blanco sin red | Alto | Caché del directorio y estado de error con reintento (C-10, C-11) |
| G-04 | "Lo más próximo" se confunde con distancia y se implementa mal | Alto | R-06 y R-07 lo fijan como fecha; C-06 y C-07 lo prueban |
| G-05 | El demo depende de datos de producción y falla frente al profesor | Alto | Modo demo con datos locales y sin escrituras (C-12) |
| G-06 | El carrusel y el mapa se desincronizan | Medio | Una sola fuente de selección; prueba C-05 |
| G-07 | Se pide ubicación al arrancar y el usuario la niega para siempre | Medio | R-18 y prueba C-09 |
| G-08 | El directorio crece y el mapa se vuelve lento | Medio | Filtros en cliente y límite de marcadores; Q-05 |
| G-09 | La próxima dosis se calcula con el reloj del dispositivo y muestra un color equivocado | Medio | Reloj inyectable en `domain`; C-07 |

---

## 7. Dependencias

**De esta unidad:**

- `002-identidad-y-sesion`: sesión válida y pantalla posterior al login.
- `003-permisos`: permiso de ubicación, justificación previa y degradación.
- `007-mis-mascotas`: mascotas del usuario para la tarjeta de salud.
- `008-carnet-salud`: próxima dosis y carnet al que navega la tarjeta.
- `001`: red, caché, Room, navegación, componentes y pruebas.

**Externas:**

- Proveedor de mapas y forma de gestionar su llave (Q-01).
- Contrato del directorio de negocios del backend (Q-02).
- Lista de tipos de negocio y sus iconos (Q-03).
- Límite de negocios devueltos y política de caché (Q-05).

**Unidades posteriores que dependen de esta:**

- `011` (el pin del negocio se coloca sobre este mapa), `012` (vista del
  afiliado) y `013` (desde la ficha se puede volver al mapa).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| ~~Q-01~~ | ~~¿Qué proveedor de mapas se usa y cómo se gestiona su llave?~~ **RESUELTA 04/11/2026**: osmdroid + OpenStreetMap. No hay llave que gestionar. | — | — |
| Q-02 | ¿Cuál es el endpoint y el contrato del directorio de negocios: campos, tipos, coordenadas y paginación? | Miguel (backend) | Todas las tareas de datos |
| Q-03 | ¿Qué tipos de negocio existen y qué icono y color tiene cada uno? | Equipo / maqueta | Marcadores y filtros |
| Q-04 | ¿Qué se muestra en la tarjeta resumida del negocio (nombre, tipo, dirección, horario, distancia)? | Equipo / maqueta | Tarjeta |
| Q-05 | ¿Cuántos negocios devuelve el directorio y con qué radio? ¿Se pagina o se filtra en el servidor? | Miguel (backend) | Rendimiento y filtros |
| Q-06 | ¿La distancia se calcula en el cliente o la devuelve el backend? ¿En qué unidad se muestra? | Equipo / backend | Orden y tarjeta |
| Q-07 | ¿El demo usa un usuario y datos fijos, o datos locales sembrados en el dispositivo? ¿Quién los mantiene? | Equipo / profesor | Modo demo |
| Q-08 | ¿El centro por defecto sin ubicación es una ciudad fija o el último centro visto? | Equipo / maqueta | Degradación |
| Q-09 | ¿"Cerca de mí" ordena por distancia o solo recentra el mapa? | Equipo / maqueta | Ubicación |
| Q-10 | ¿La tarjeta "lo más próximo" muestra una sola mascota o rota entre varias? | Equipo / maqueta | Tarjeta de salud |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El alcance y el fuera de alcance están revisados.
- [ ] "Lo más próximo" como fecha de salud, con rojo ≤ 3 y amarillo ≤ 14, está aceptado.
- [ ] La dependencia de 008 para la tarjeta de salud está aceptada.
- [ ] Los requisitos y los criterios de éxito son suficientes para planificar.
- [ ] Las preguntas abiertas están identificadas y se sabe a quién preguntar.
- [ ] **Aprobada para pasar a `plan.md`.**
