# Segunda revisión — prompt para OpenCode

**Fecha:** 04/11/2026
**Para qué sirve:** OpenCode no ha trabajado en el proyecto desde que se
generaron las specs. Desde entonces se escribió código, se cambió de
organización en GitHub y se integró un mapa real. Este prompt le pide una
auditoría de todo lo que cambió, para decidir qué se corrige y qué no.

**Cómo usarlo:** abrir OpenCode en la carpeta del proyecto y pegar el bloque de
abajo completo. OpenCode **no debe modificar archivos** en esta revisión: solo
reporta. Las correcciones se autorizan una por una después de leer su informe.

> ⚠️ **El límite diario de mensajes es por modelo, no de la cuenta.**
> El 04/11/2026 el primer intento (DeepSeek V4.1 Flash Free) se agotó con un
> solo mensaje: disparó tres comandos de exploración seguidos y cada respuesta
> del modelo cuenta. Se resolvió escribiendo `/models` y cambiando de modelo —
> no hay que esperar a que el límite se reinicie.
>
> Dos medidas quedaron en este documento por eso:
>
> 1. El prompt le **prohíbe explorar la estructura**. El inventario ya está
>    dentro del prompt y verificado, así no gasta mensajes contando archivos.
> 2. La **versión por partes** de más abajo son cinco sesiones independientes,
>    una pregunta cada una. Si el límite se acaba en la parte 3, las partes 1 y
>    2 ya están entregadas y no se repiten.
>
> Al elegir modelo, preferir uno con ventana de contexto grande: la auditoría
> lee 61 archivos de spec más 57 de Kotlin.

---

## Prompt (pegar completo)

```
Eres el auditor de este proyecto. Trabajas sobre la carpeta actual, que es la
app movil Android de PetMate (Kotlin + Jetpack Compose), desarrollada con Spec
Driven Development.

REGLA 1: en esta sesion NO modificas ningun archivo. No escribes codigo, no
corriges specs, no haces commits. Solo lees y reportas. Si encuentras algo que
hay que arreglar, lo describes y esperas autorizacion. Si crees que una
correccion es urgente, lo dices en el informe, pero tampoco la aplicas.

REGLA 2: NO explores la estructura. No corras ls, dir, Get-ChildItem, tree,
find ni equivalentes para ver que archivos hay. El inventario ya lo tienes mas
abajo y es correcto. Tienes un limite de mensajes y cada comando que gastes
listando carpetas es un archivo que no vas a leer. Ve directo a abrir los
archivos que te nombro.

CONTEXTO DE POR QUE TE LLAMO

Tu participaste cuando se escribieron las especificaciones. Desde entonces pasaron
cuatro cosas que no has visto:

1. Se escribio codigo real: 57 archivos Kotlin. Hay registro, login, logout,
   pantalla de inicio, tema de marca, y un mapa de OpenStreetMap funcionando.
2. Se cambio de organizacion de GitHub: de PETMATE-APP-S a petmateMMAE, porque
   la primera tenia la estructura de repositorios mal armada.
3. Se agregaron dos dependencias: osmdroid (mapas) y el cambio a AGP 8.7.3 con
   Gradle 8.11.1, porque Hilt no era compatible con AGP 9.
4. Se integro a main una rama de diseno de pantallas.

Quiero que revises si todo eso es coherente con lo que las specs dicen, o si nos
desviamos sin darnos cuenta.

INVENTARIO (ya verificado, no lo vuelvas a contar)

  Unidades de spec:        20  (001-arquitectura-base ... 020-liberacion-play)
  Archivos de spec:        61  (20 x spec.md/plan.md/task.md + MAPA-CAPACIDADES.md)
  Tareas totales:         280
  Tareas marcadas [x]:      0
  Archivos Kotlin:         57  (54 en main, 3 de prueba)
  Pruebas unitarias:       24  (0 fallos)
  Subagentes:               5  (.opencode/agents/)
  Skills:                   4  (.agents/skills/)

Los tres archivos de prueba son:
  app/src/test/java/mx/petmate/app/data/repository/FakeAuthRepositoryTest.kt
  app/src/test/java/mx/petmate/app/domain/usecase/ValidateCredentialsUseCaseTest.kt
  app/src/test/java/mx/petmate/app/ui/auth/LoginViewModelTest.kt

Si tus cuentas no dan esto, dilo en el informe: puede ser que algo cambio.

QUE TIENES QUE LEER

Primero, los documentos de autoridad:
- constitution.md  (la ley del proyecto: 12 principios, stack obligatorio,
  limites de los agentes, umbrales de calidad)
- AGENTS.md        (reglas de operacion para cualquier agente de IA)
- memoria.md       (bitacora, equipo, decisiones, deudas conocidas)

Luego la estructura de especificaciones:
- specs/MAPA-CAPACIDADES.md  (las 20 unidades y sus dependencias)
- specs/001-arquitectura-base/ hasta specs/020-liberacion-play/
  (cada una con spec.md, plan.md y task.md: 61 archivos, 280 tareas)

Luego el codigo:
- app/src/main/java/mx/petmate/app/   (57 archivos .kt)
- app/build.gradle.kts y gradle/libs.versions.toml  (dependencias y versiones)
- app/src/main/AndroidManifest.xml    (permisos)

Y los agentes y skills ya definidos:
- .opencode/agents/   (arquitecto, dev-android, seguridad, spec-writer, tester)
- .agents/skills/     (crear-spec, nueva-pantalla, revisar-capas,
                       agregar-dependencia)

LAS SEIS PREGUNTAS QUE TIENES QUE RESPONDER

Responde cada una por separado, con la ruta y el numero de linea de cada cosa
que afirmes. Si no encuentras evidencia para afirmar algo, dilo: "no pude
verificarlo" es una respuesta valida y util. No inventes hallazgos para llenar
el informe.

1. CODIGO SIN TAREA EJECUTADA
   Hay 280 tareas en los 20 task.md y ninguna esta marcada como hecha ([x]).
   Pero hay 57 archivos Kotlin escritos.
   - ¿Que codigo existe que no corresponde a ninguna tarea de ningun task.md?
   - ¿Que tareas describen codigo que ya existe y por lo tanto deberian estar
     marcadas?
   - ¿Hay codigo que contradice lo que su spec pedia?
   Esto importa porque el profesor evalua la trazabilidad entre spec y codigo.

2. LA CONSTITUCION CONTRA EL CODIGO
   Revisa cada uno de los 12 principios de constitution.md contra el codigo real.
   En particular:
   - P3 (direccion de dependencias: ui -> domain <- data, domain en Kotlin puro):
     ¿algun archivo de domain importa algo de Android o de data?
   - P4 (offline-first)
   - P6 (nada de secretos ni llaves en archivos versionados): revisa tambien
     local.properties, el .gitignore y los BuildConfig.
   - Los umbrales de cobertura que fija la constitucion contra las pruebas que
     de verdad existen.
   Di cuales se cumplen, cuales no, y cuales no se pueden verificar todavia.

3. LAS DEPENDENCIAS NUEVAS
   La constitucion dice que agregar dependencias requiere preguntar primero.
   Se agregaron osmdroid 6.1.20 y se bajo AGP de 9 a 8.7.3.
   - ¿Estan declaradas y justificadas en la spec que les corresponde?
   - osmdroid esta archivado desde noviembre de 2024 (sin mantenimiento).
     ¿Esta ese riesgo escrito en alguna spec o en memoria.md? ¿Es aceptable
     para una app que se va a publicar en Google Play?
   - ¿El cambio de AGP y Gradle quedo documentado con su razon?

4. COHERENCIA ENTRE LAS SPECS
   Las unidades 002 a 010 las escribiste tu; las 011 a 020 las escribio otro
   agente. Busca donde no empatan:
   - Referencias cruzadas rotas (una spec que cita una tarea, pregunta o riesgo
     que no existe, o que apunta al numero equivocado).
   - Dependencias hacia adelante: una unidad que dependa de otra con numero
     mayor.
   - Contradicciones reales entre dos specs sobre el mismo tema (por ejemplo
     moderacion de contenido contra sincronizacion offline, o privacidad por
     tipo de negocio contra compartir historial).
   - Terminos que significan lo mismo pero se llaman distinto en unidades
     distintas.

5. EL CAMBIO DE ORGANIZACION
   Se paso de PETMATE-APP-S a petmateMMAE.
   - ¿Quedo alguna referencia a la organizacion vieja que deberia estar
     actualizada?
   - En memoria.md las menciones historicas de la bitacora conservan el nombre
     viejo a proposito, porque en esas fechas la organizacion nueva no existia.
     ¿Te parece correcta esa decision, o deberian cambiarse todas?
   - ¿Hay algo mas que se deba actualizar y se haya pasado por alto (CI, README,
     documentacion, URLs)?

6. LO QUE FALTA PARA LA ENTREGA
   La evaluacion es el 02/12/2026 y la app tiene que estar publicada en Google
   Play. La spec 020 dice que la prueba cerrada debe arrancar el 08/11/2026
   (12 probadores, 14 dias).
   - ¿Que falta que no este escrito en ninguna spec?
   - ¿Que riesgo real ves de no llegar?
   - ¿Que tres cosas harias primero si tu decidieras?

FORMATO DEL INFORME

Entrega un solo informe en texto, asi:

  RESUMEN
  Tres o cuatro frases: en que estado esta el proyecto segun lo que viste.

  HALLAZGOS
  Una tabla con: numero | que encontraste | archivo:linea | gravedad
  (bloqueante / alta / media / baja) | que propones

  Ordenada de mas grave a menos grave. Un hallazgo por fila. Si algo lo
  verificaste y esta bien, no lo pongas aqui: va en la seccion siguiente.

  LO QUE REVISE Y ESTA BIEN
  Lista corta de lo que comprobaste y no tiene problema. Sirve para saber que
  si revisaste.

  LO QUE NO PUDE VERIFICAR
  Que te falto y por que (no tenias acceso, hace falta correr algo, hace falta
  una decision humana).

  PREGUNTAS PARA EL EQUIPO
  Las decisiones que no te corresponden a ti. Numeradas, cada una con las
  opciones que ves y que recomendarias.

  SI YO DECIDIERA
  Tus tres primeras acciones, en orden, con una frase de por que cada una.

Cuando termines el informe, detente. No apliques nada. El equipo va a leerlo y
te va a decir que correcciones autoriza.
```

---

## Versión por partes (recomendada con modelos gratuitos)

Cinco sesiones independientes. Cada una es un mensaje y entrega su propia tabla
de hallazgos. Si el límite se acaba en la parte 3, las partes 1 y 2 ya están
guardadas y no se repiten.

**Antes de pegar cualquiera:** si el modelo dice que se acabó el límite, escribir
`/models` y elegir otro de los gratuitos. El límite es por modelo, no de la
cuenta.

### Parte 1 — Código sin tarea

```
Auditoria de la app movil Android de PetMate (Kotlin + Compose, Spec Driven
Development). NO modifiques archivos y NO listes carpetas: ve directo a leer.

Dato verificado: hay 280 tareas en los 20 specs/0NN-*/task.md, ninguna marcada
como hecha ([x]), pero existen 57 archivos .kt (54 en app/src/main, 3 de prueba).

Lee los task.md de specs/001-arquitectura-base, specs/002-identidad-y-sesion y
specs/010-mapa-e-inicio, y el codigo en app/src/main/java/mx/petmate/app/.

Dime, con ruta y linea:
1. Que codigo existe que no corresponde a ninguna tarea.
2. Que tareas describen codigo que ya existe y deberian estar marcadas.
3. Que codigo contradice lo que su spec pedia.

Entrega una tabla: hallazgo | archivo:linea | gravedad | que propones.
Luego detente.
```

### Parte 2 — La constitución contra el código

```
Auditoria de la app movil Android de PetMate. NO modifiques archivos y NO listes
carpetas.

Lee constitution.md completo y luego el codigo en
app/src/main/java/mx/petmate/app/ (54 archivos), app/build.gradle.kts y
.gitignore.

Revisa cada uno de los 12 principios contra el codigo real. Pon atencion en:
- P3: direccion de dependencias (ui -> domain <- data; domain en Kotlin puro).
  ¿Algun archivo de domain/ importa android.* o algo de data/?
- P6: nada de secretos ni llaves en archivos versionados. Revisa tambien
  local.properties, .gitignore y los buildConfigField.
- Los umbrales de cobertura que fija la constitucion contra las 24 pruebas que
  existen.

Entrega tres listas: principios que SI se cumplen, los que NO (con
archivo:linea), y los que no se pueden verificar todavia y por que.
Luego detente.
```

### Parte 3 — Dependencias nuevas

```
Auditoria de la app movil Android de PetMate. NO modifiques archivos y NO listes
carpetas.

Lee gradle/libs.versions.toml, app/build.gradle.kts,
app/src/main/java/mx/petmate/app/ui/map/OsmMapConfig.kt,
specs/010-mapa-e-inicio/spec.md y la seccion de limites de agentes de
constitution.md.

Contexto: se agrego osmdroid 6.1.20 para el mapa y se bajo AGP de 9 a 8.7.3
porque Hilt no era compatible con AGP 9. La constitucion dice que agregar
dependencias requiere preguntar primero.

Contesta:
1. ¿Estan declaradas y justificadas en la spec que les corresponde?
2. osmdroid esta archivado desde noviembre de 2024 (sin mantenimiento). ¿Ese
   riesgo esta escrito? ¿Es aceptable para una app que se publica en Google Play?
3. ¿El cambio de AGP y Gradle quedo documentado con su razon?
4. La politica de teselas de OpenStreetMap exige User-Agent propio, atribucion
   visible y cache. ¿El codigo las cumple?

Entrega tabla: hallazgo | archivo:linea | gravedad | que propones. Luego detente.
```

### Parte 4 — Coherencia entre specs

```
Auditoria de las especificaciones de PetMate (app movil Android). NO modifiques
archivos y NO listes carpetas.

Hay 20 unidades en specs/001-* a specs/020-*. Las 002 a 010 las escribio un
agente y las 011 a 020 otro, asi que busca donde no empatan.

Lee specs/MAPA-CAPACIDADES.md primero, y luego los spec.md de 004, 005, 009,
013 y 017 (son los que tocan los mismos temas).

Busca:
1. Referencias cruzadas rotas: una spec que cite una tarea, pregunta (Q-) o
   riesgo (G-) que no existe o apunta al numero equivocado.
2. Dependencias hacia adelante: una unidad que dependa de otra de numero mayor.
3. Contradicciones reales entre dos specs sobre el mismo tema. En particular:
   moderacion de contenido (004) contra sincronizacion offline (005), y
   privacidad por tipo de negocio (013/017) contra compartir historial (009).
4. Terminos que significan lo mismo pero se llaman distinto segun la unidad.

Entrega tabla: hallazgo | archivo:linea | gravedad | que propones. Luego detente.
```

### Parte 5 — Entrega y organización

```
Auditoria de la app movil Android de PetMate. NO modifiques archivos y NO listes
carpetas.

Lee specs/020-liberacion-play/spec.md y plan.md, memoria.md y constitution.md.

Contexto: la evaluacion es el 02/12/2026 y la app tiene que estar publicada en
Google Play. La prueba cerrada necesita 12 probadores durante 14 dias. Tambien
se cambio de organizacion de GitHub (de PETMATE-APP-S a petmateMMAE) porque la
primera tenia la estructura mal armada.

Contesta:
1. Segun el calendario de la spec 020, ¿se llega al 02/12/2026? ¿Que fecha es
   la mas critica?
2. ¿Que falta para publicar que no este escrito en ninguna spec?
3. ¿Quedo alguna referencia a la organizacion vieja que deberia actualizarse?
   Nota: en la bitacora de memoria.md las menciones historicas conservan el
   nombre viejo a proposito, porque en esas fechas la organizacion nueva no
   existia. ¿Te parece correcto o deberian cambiarse todas?
4. Si tu decidieras, ¿cuales serian las tres primeras acciones?

Entrega tabla de hallazgos y tus tres acciones en orden. Luego detente.
```

---

## Versión corta (si la sesión se corta o va muy lenta)

```
Audita esta carpeta: app movil Android de PetMate, hecha con Spec Driven
Development. NO modifiques ningun archivo: solo lee y reporta.

Lee constitution.md, AGENTS.md, memoria.md, specs/MAPA-CAPACIDADES.md, los 20
specs/0NN-*/ (spec.md, plan.md, task.md) y el codigo en
app/src/main/java/mx/petmate/app/.

Contesta con rutas y numeros de linea:

1. Hay 280 tareas y ninguna marcada como hecha, pero hay 57 archivos .kt.
   ¿Que codigo no corresponde a ninguna tarea? ¿Que tareas ya estan hechas en
   realidad?
2. ¿El codigo viola alguno de los 12 principios de constitution.md? Revisa
   sobre todo P3 (direccion de dependencias) y P6 (nada de secretos).
3. Se agrego osmdroid 6.1.20 (archivado desde 2024) y se bajo AGP 9 a 8.7.3.
   ¿Esta justificado y documentado?
4. ¿Hay contradicciones o referencias rotas entre las specs 002-010 y 011-020?
5. ¿Que falta para publicar en Google Play antes del 02/12/2026?

Entrega: resumen, tabla de hallazgos (que | archivo:linea | gravedad | que
propones), lo que revisaste y esta bien, lo que no pudiste verificar, y
preguntas para el equipo. Luego detente sin aplicar nada.
```

---

## Qué hacer con su respuesta

1. **Leer el informe completo** antes de autorizar nada.
2. **Separar** lo que es un hallazgo real de lo que es opinión de estilo.
3. **Autorizar las correcciones una por una**, no en bloque: así queda registro
   de qué se decidió y por qué.
4. Si contradice algo que ya se decidió a propósito (como las menciones
   históricas de la organización en la bitácora), **contestarle con la razón**
   en vez de cambiarlo. Esa conversación también es evidencia de desarrollo con
   agentes.

### Cifras verificadas el 04/11/2026 (para contrastar lo que reporte)

| Qué | Cuánto |
|---|---|
| Unidades de spec | 20 |
| Archivos de spec | 61 (20 × 3 + el mapa de capacidades) |
| Tareas totales | 280 |
| Tareas marcadas como hechas | 0 |
| Archivos Kotlin | 57 (54 en `main`, 3 de prueba) |
| Pruebas unitarias | 24, 0 fallos |
| Subagentes definidos | 5 |
| Skills definidas | 4 |
