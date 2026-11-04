# Prompts para OpenCode — Auditoría y specs completas de PetMate

> Para la etapa en que el profesor ya autorizó crear **todas** las specs, no solo
> la arquitectura base.
>
> Se usan **en orden**. Cada uno es una etapa con puerta de aprobación: no pases
> al siguiente hasta revisar lo que salió del anterior.
>
> **Por qué por etapas:** pedir "haz todas las specs" de una sola vez produce
> documentos genéricos y repetidos. El valor está en que el mapa se apruebe
> antes de escribir, y en que cada spec se revise al salir.

**Antes de empezar:** ponlo en **Plan mode** (tecla `Tab`) para las etapas 1 y 2,
que son de solo lectura. Cámbialo a **Build mode** solo a partir de la etapa 3,
que ya escribe archivos.

---

## ETAPA 1 — Auditoría contra los lineamientos del profesor

```
Estás en el repositorio de la app móvil Android de PetMate (Kotlin + Jetpack
Compose), en la organización de GitHub petmateMMAE.

TU TRABAJO EN ESTA ETAPA: AUDITAR. No escribas ni modifiques ningún archivo
todavía. Solo lee y reporta.

PASO 1 — Lee el estado actual
  constitution.md
  AGENTS.md
  memoria.md
  .opencode/agents/   (5 archivos)
  .agents/skills/   (4 SKILL.md)
  specs/001-arquitectura-base/spec.md, plan.md, task.md

PASO 2 — Contrasta contra los lineamientos oficiales
Estos son los requerimientos del profesor (documento "Requerimientos para el
desarrollo del proyecto Móvil v2", cuatrimestre sep-dic 2026). Verifica, uno
por uno, si la arquitectura base los contempla, y marca cuáles NO están
cubiertos:

GENERALES
  1. La evaluación inicia descargando la app de Google Play.
  2. Se evalúa el proyecto con prompts para verificar lo implementado.
  3. El proyecto se compone de: Nube, Base de datos, Backend, Móvil y Landing.
  4. El móvil se desarrolla a imagen de la maqueta; se pueden AGREGAR funciones,
     nunca eliminar.
  5. Obligatorio Spec Driven Development y DevSecOps en backend y móvil.
  6. Backend y móvil desarrollados enteramente con agentes de IA.
  7. Pruebas unitarias, de seguridad e integración, más los demás procesos de
     DevSecOps.
  8. CI-CD para backend y móvil.
  9. Caché a nivel base de datos, backend y frontend.
  10. Logout a nivel backend y móvil.
  11. Matriz de numeración de versionamiento PREVIA al desarrollo, aplicada en
      cada despliegue.
  12. Se pueden usar MCP para eficientar el desarrollo.

NUBE (punto 13)
  - Todos los servicios configurados en la nube; la base de datos puede ser un
    servicio externo.
  - Load balancer o similar, y su uso desde el backend.
  - IaC con Terraform para crear y eliminar la infraestructura.
  - Salida del backend con SSL para HTTPS.

BASE DE DATOS (punto 14)
  - NoSQL; el número de bases depende del proyecto.

BACKEND (punto 15) — contexto, va en el repo hermano
  - Escalable, mínimo 2 instancias en la nube.
  - Solo AWS, Azure o Google Cloud, configurado a COSTO CERO.
  - Imágenes al CDN de la nube elegida.
  - Observabilidad: log de eventos y errores.
  - JWT con Redis distribuido.
  - Performance: caching, rate limit, middleware, paginación, indexing, JSON
    serializers, compresión de payload.
  - Creación y eliminación de infraestructura como un paso.
  - Versionamiento.

MÓVIL (punto 16) — esto es lo que más importa aquí
  - Despliegue desde GitHub a la tienda Google Play.
  - Moderación: antes de almacenar texto, imágenes y video, evaluarlos con la
    API de la nube; si es inapropiado, no se permite subir.
  - Notificaciones push para los procesos principales.
  - Aviso de privacidad y manejo de datos en la app, vinculado a la landing.
  - Eliminar cuenta.
  - Permisos del móvil.
  - Sesión siempre viva, excepto si el usuario hace logout.
  - SQLite local con sincronización al recuperar conexión.
  - Al menos una función de ML y una de IA generativa.

LANDING (punto 17) y GIT (punto 18) — contexto, otros repos.

PASO 3 — Reporta
  a) Tabla: requisito | ¿lo cubre la arquitectura base? sí/no/parcial | dónde
     (archivo y línea) | qué falta.
  b) Huecos de la arquitectura base, ordenados por gravedad. En especial
     revisa si están contemplados: despliegue a Google Play desde GitHub,
     despliegue en Azure, moderación de contenido, push, permisos, eliminar
     cuenta, aviso de privacidad, ML e IA generativa.
  c) Contradicciones o desalineaciones con constitution.md.
  d) Qué está bien hecho (sé específico).

REGLAS
  - No modifiques nada en esta etapa.
  - Cita archivo y línea en cada hallazgo.
  - Si algo ya está cubierto, dilo; no inventes huecos para parecer útil.
  - Ojo: que un tema aparezca en la sección "Fuera de alcance" de la spec 001
    NO es un hueco; significa que se atenderá en su propia spec. Distínguelo
    de lo que de plano no está contemplado en ningún lado.
```

**→ Revisa el reporte antes de seguir.**

---

## ETAPA 2 — Mapa de capacidades

```
Con la auditoría de la etapa anterior, ahora propón el MAPA DE CAPACIDADES del
proyecto móvil: la lista de todas las unidades de trabajo (specs) que hacen
falta para cubrir los lineamientos y la maqueta aprobada.

NO escribas ninguna spec todavía. Solo el mapa. Es un documento corto y
revisable.

CONTEXTO DEL PRODUCTO
PetMate es una app Android con dos roles: Usuario (dueño de mascota) y Afiliado
(negocio: veterinaria, tienda o estética). Funciones de la maqueta aprobada:

  - Bienvenida: iniciar sesión / crear cuenta / probar demo.
  - Verificación con código de 6 dígitos por correo o SMS.
  - Inicio en un MAPA de negocios afiliados, con marcadores por tipo, filtros,
    carrusel y tarjeta "lo más próximo".
  - Menú inferior con botón central redondo (Mi QR para dueño / Escanear para
    negocio).
  - Mis mascotas: tarjetas con foto.
  - Carnet: vacunas, desparasitación, peso, alergias y condiciones, historial
    médico en línea de tiempo con filtros.
  - Compartir historial: elegir negocio, secciones y duración (30 min, 1 día,
    7 días); las alergias siempre van; QR regenerable.
  - Panel del afiliado: portada, alertas de seguimiento, mensajes sin leer.
  - Afiliación en 3 pasos: datos y tipos → servicios con precio → pin en mapa.
  - Escáner de QR y ficha según tipo de negocio (veterinaria ve todo;
    tienda y estética solo lo básico más alergias).
  - Consulta de veterinaria: motivo, diagnóstico, peso, receta, cobro en vivo
    desglosado (servicios, vacunas, medicinas, IVA, forma de pago), y
    seguimiento (en tratamiento / mejorando / dado de alta).
  - Chat de seguimiento tipo WhatsApp con fotos.
  - Mascotas atendidas con filtro por día.
  - Ajustes de notificaciones, privacidad con enlace a la landing, eliminar
    cuenta.

Más lo que exigen los lineamientos: SQLite offline con sincronización, ML,
IA generativa, moderación de contenido, push, permisos, sesión siempre viva,
despliegue a Google Play desde GitHub.

FORMATO DEL MAPA

| id | Responsabilidad | Depende de | Requisito del profe que cubre |
|---|---|---|---|
| 001-arquitectura-base | (ya existe) | — | puntos 5, 7, 8, 9, 11 |
| 002-... | ... | 001 | ... |

Y debajo: el orden de construcción.

REGLAS DEL MAPA
  - Ids en kebab-case con número: 002-nombre-corto. Una vez puesto, no se
    renombra.
  - Las dependencias van en una sola dirección, sin ciclos. Si dos unidades se
    necesitan mutuamente, son una sola.
  - Cada unidad debe ser verificable por separado: si no se puede probar sola,
    está mal cortada.
  - Toda función de la maqueta y todo requisito de los lineamientos debe caer
    en alguna unidad. Al final, lista explícitamente qué requisito cubre cada
    una y verifica que no quede ninguno fuera.
  - Las unidades transversales (moderación, push, permisos, sincronización
    offline, observabilidad) decide si son unidades propias o parte de otras,
    y justifica la decisión.
  - No incluyas backend, landing ni infraestructura de nube: son otros repos.
    Sí incluye lo que la app móvil necesita hacer para consumirlos.

No escribas specs. Solo el mapa, la justificación de los cortes y el orden.
```

**→ Revisa y corrige el mapa antes de seguir. Este es el paso que más importa:
si el mapa está mal, todas las specs salen mal.**

---

## ETAPA 3 — Rectificar la arquitectura base

```
Ahora sí puedes modificar archivos. Cambia a Build mode.

Con los huecos que encontraste en la etapa 1, corrige la unidad
specs/001-arquitectura-base/ para que la arquitectura base contemple lo que
hace falta. En particular, verifica que estén cubiertos:

  - Despliegue a GOOGLE PLAY desde GitHub Actions: generación del AAB firmado,
    versionCode tomado de la matriz de versionamiento, firma con secretos de
    GitHub, y la subida a la tienda (prueba cerrada antes de producción).

  - Que el backend se despliega en AZURE y la app lo consume por HTTPS: URL
    base por entorno, certificados, y que el cliente HTTP esté preparado para
    el balanceador de Azure Container Apps.

  - El punto de extensión para MODERACIÓN de contenido: la arquitectura no
    implementa la moderación (eso va en su spec), pero sí debe dejar claro
    dónde se intercepta la subida de texto, imagen y video antes de almacenar.

  - La base para NOTIFICACIONES PUSH (FCM): qué deja listo la arquitectura y
    qué queda para la spec de push.

  - La base para PERMISOS del móvil: el mecanismo para pedirlos en el momento
    de uso con explicación previa, y que la app funcione si se niegan.

  - La base para SINCRONIZACIÓN offline: cómo se encola lo que se crea sin
    conexión y cómo se reconcilia al volver la red.

REGLAS
  - Modifica specs/001-arquitectura-base/spec.md, plan.md y task.md, y si hace
    falta constitution.md. Mantén el formato y la numeración que ya traen.
  - Si agregas tareas nuevas, continúa la numeración (la última es T-032) y
    actualiza el conteo total y los resúmenes.
  - Las tareas nuevas van SIN marcar, igual que las demás.
  - Toda adición debe ser coherente con constitution.md. Si algo choca con la
    constitución, repórtalo en vez de romperla.
  - No implementes código. Solo documentos.
  - Al terminar, resume qué cambiaste y por qué.
```

**→ Revisa los cambios con `git diff` antes de seguir.**

---

## ETAPA 4 — Crear las specs, UNA POR UNA

> No le pidas todas de golpe. Repite este prompt cambiando el id, en el orden
> de dependencias del mapa.

```
Crea la unidad de trabajo <PON AQUÍ EL ID, ej: 002-autenticacion> siguiendo la
skill crear-spec de este repositorio (.agents/skills/crear-spec/SKILL.md).

PASO 1 — Antes de escribir, enumera tus supuestos así:

    SUPUESTOS QUE ESTOY HACIENDO:
    1. ...
    2. ...
    → Corrígeme ahora o avanzo con esto.

Y espera. No escribas la spec hasta que te responda.

PASO 2 — Cuando te apruebe los supuestos, escribe los tres archivos en
specs/<id>/:

  spec.md  — QUÉ y POR QUÉ. Objetivo, alcance, fuera de alcance, requisitos
             trazables, criterios de éxito VERIFICABLES (no "debe ser rápido"
             sino "< 2.5 s en 4G"), riesgos, dependencias, preguntas abiertas.
             Sin nombres de clases ni librerías.

  plan.md  — CÓMO. Componentes, dependencias entre ellos, orden de
             construcción, decisiones técnicas con su justificación y las
             alternativas descartadas, riesgos con mitigación, puntos de
             verificación.

  task.md  — Tareas ordenadas por dependencia. Cada una con: criterio de
             aceptación, comando de verificación, archivos que toca. Máximo
             ~5 archivos por tarea. TODAS sin marcar.

REGLAS
  - Coherente con constitution.md. Si choca, gana la constitución.
  - Respeta las capas: ui → domain ← data. El domain es Kotlin puro.
  - Las tareas NO se ejecutan. Solo se redactan.
  - Lo que no sepas va a "Preguntas abiertas". NO lo inventes. En particular,
    si el contrato del endpoint no está definido, dilo en vez de suponerlo.
  - Si esta unidad depende de otra que aún no existe, decláralo en
    Dependencias.
  - Al terminar, actualiza memoria.md: agrega la unidad a la lista y una línea
    en la bitácora.
```

**→ Revisa cada spec al salir. Luego repite con la siguiente.**

---

## ETAPA 5 — Verificación final

```
Verifica la coherencia del conjunto completo. No modifiques nada; solo reporta.

  1. ¿Toda unidad del mapa de capacidades tiene sus tres archivos?
  2. ¿Las dependencias entre unidades forman un orden válido, sin ciclos?
  3. ¿Cada función de la maqueta aprobada cae en alguna unidad? Lista las que
     no.
  4. ¿Cada requisito de los lineamientos del profesor (los 19 puntos) está
     cubierto por alguna unidad? Lista los que no.
  5. ¿Alguna spec contradice constitution.md? Cita archivo y línea.
  6. ¿Hay requisitos duplicados en dos unidades distintas?
  7. ¿Cuántas tareas hay en total y cuántas están marcadas como hechas?
     (deben ser 0 marcadas)
  8. ¿Hay preguntas abiertas sin dueño asignado?

Reporta en tabla, con evidencia de archivo y línea. Al final, un veredicto:
¿está el proyecto listo para empezar a ejecutar tareas? Sí / No / Qué falta.
```

---

## Notas de uso

**Modos de OpenCode.** Etapas 1, 2 y 5 en **Plan mode** (`Tab`), que es de solo
lectura. Etapas 3 y 4 en **Build mode**, que escribe.

**Guarda la conversación.** Al terminar, `/export` genera un Markdown de la
sesión. Sirve como evidencia de que el desarrollo se hizo con agentes de IA,
que es el punto 6 de los lineamientos.

**Si el modelo gratis se atora.** Son modelos chicos; la etapa 4 es la más
pesada. Si una spec sale pobre, pídele que la rehaga señalando qué le faltó,
o cambia de modelo con `/models`.

**Revisa siempre antes de aceptar.** Un modelo gratis tiende a escribir
criterios de éxito vagos ("debe funcionar bien") y a inventar endpoints que
nadie definió. Esos dos son los errores a cazar en cada revisión.

**Commits.** Cuando una etapa quede bien, haz commit. No dejes todo para el
final, porque si una etapa sale mal es más fácil volver atrás.
