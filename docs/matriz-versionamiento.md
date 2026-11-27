# Matriz de versionamiento — app PetMate

> Spec 001, T-001. Resuelve la deuda D-04 de `memoria.md`.
> Escrita el 27/11/2026. **Pendiente de revisión por Adrián**, que es lo que pide la verificación
> de la tarea.

Este documento dice tres cosas y nada más: cómo se calcula cada número de versión, qué entregas
hay, y quién mueve los números. Si alguien tiene que adivinar una de esas tres al preparar una
entrega, este archivo falló.

## Alcance: este repositorio

La matriz cubre **la app Android** (`mx.petmate.app`). El backend y el panel web llevan la suya.

Esto es una respuesta provisional a **Q-07** (¿un documento compartido para los tres repositorios o
uno por repositorio?), que sigue abierta con el profesor. Se escribió por repositorio porque es lo
que se puede sostener hoy: los tres se liberan por separado, el backend ya va en 0.5.0 mientras la
app va en 0.2.0, y forzarles un número común obligaría a publicar versiones vacías de uno para
acompañar al otro.

**Si el equipo decide que sea compartida**, lo que cambia es poco: este archivo se queda como la
sección de la app dentro del documento común, y las reglas de `versionCode` de abajo no se tocan,
porque son específicas de Google Play y no aplican al backend ni al panel.

## `versionName`: versionado semántico

Formato `MAYOR.MENOR.PARCHE`, sin sufijos. El pipeline lo valida con una expresión regular y
**reprueba** si no cumple (job `6 · APK y AAB de release`), así que un `0.3.0-rc1` no pasa.

| Parte | Cuándo sube | Ejemplo en este proyecto |
|---|---|---|
| MAYOR | Cambio que rompe al usuario o al contrato del backend que la app necesita. Mientras la app no esté publicada en producción se queda en `0`. | `1.0.0` será la primera versión en producción en Play |
| MENOR | Funcionalidad nueva visible para el usuario: una spec que cierra. | `0.2.0` al cerrar el carnet de salud |
| PARCHE | Correcciones sin funcionalidad nueva. | `0.2.1` arreglaría un fallo de `0.2.0` |

Por qué no se usan sufijos como `-rc1` o `-beta`: Play ya distingue los canales (interno, cerrado,
abierto, producción) y el mismo artefacto sube por los cuatro. Meter el canal en el nombre de la
versión obliga a reconstruir el AAB para promoverlo, y entonces lo que se promueve no es lo que se
probó.

## `versionCode`: entero que solo sube

Regla: **un número entero que se incrementa de uno en uno en cada artefacto que se sube a Play.**
No se calcula a partir del `versionName`.

Por qué así y no `MAYOR*10000 + MENOR*100 + PARCHE`, que es la fórmula de manual: esa fórmula se
rompe en cuanto hay que subir dos artefactos del mismo `versionName` (un AAB rechazado por la ficha,
una firma mal puesta, un `google-services.json` que faltaba). Google Play **no acepta dos veces el
mismo `versionCode` ni uno menor que el ya subido**, nunca, y eso no se puede deshacer. Un contador
que solo sube no tiene ese problema.

La consecuencia a aceptar: el `versionCode` y el `versionName` no se corresponden, así que la
correspondencia vive en la tabla de abajo. Hay que mantenerla.

## Entregas

| `versionCode` | `versionName` | Estado | Qué trae |
|---|---|---|---|
| 1 | 0.1.0 | Entregada | Primera versión armada del proyecto |
| 2 | 0.1.1 | Entregada | Correcciones sobre 0.1.0 |
| 3 | **0.2.0** | **Actual en el repositorio** | Carnet de salud; backend real en Azure (ver `memoria.md`) |
| 4 | 0.3.0 | Planeada | Siguiente corte: lo que cierre de 005, 014 y 015 |
| — | 1.0.0 | Planeada | Primera a producción en Play. Depende de 020 (firma, ficha, política de privacidad) |

Los `versionCode` 1 y 2 están en la tabla por continuidad; el repositorio arranca su historia en 3.

**La columna "Planeada" es una intención, no un compromiso.** Si una entrega se mueve, se corrige
aquí en el mismo momento; una tabla que miente es peor que no tenerla.

## Quién la actualiza y cuándo

**Adrián**, y solo él, porque es quien hace los commits y quien lanza las entregas.

Cuándo: **en el mismo commit que prepara la entrega**, nunca antes. Subir el número al empezar a
trabajar en una versión deja el repositorio diciendo que es `0.3.0` durante semanas en las que
cualquier artefacto que salga de ahí está mintiendo sobre lo que trae.

Qué toca, los dos juntos y en un solo lugar:

```kotlin
// app/build.gradle.kts
versionCode = 4
versionName = "0.3.0"
```

Y la fila correspondiente de la tabla de arriba, en el mismo commit.

## Lo que ya está automatizado y lo que no

Automático hoy, en `.github/workflows/pipeline.yml`:

- Lee `versionCode` y `versionName` de `app/build.gradle.kts`; no hay un segundo lugar donde
  escribirlos.
- **Reprueba si el `versionName` no es versionado semántico.**
- Nombra el artefacto `petmate-app-<versionName>` y calcula huellas SHA-256.

**No** automático, a propósito:

- Nadie incrementa el `versionCode` por ti. Un paso de CI que lo subiera solo haría que dos ramas
  en paralelo generaran el mismo número y el conflicto apareciera en Play, no en el repositorio.
- Que el `versionCode` no se repita entre entregas. La tabla de arriba es la única memoria de eso,
  y es la razón por la que la tabla tiene que estar al día.

## Referencias

- `app/build.gradle.kts` — dónde viven los dos números.
- `.github/workflows/pipeline.yml`, job `6 · APK y AAB de release` — la validación.
- Spec 020 — la liberación a Play (T-040, T-041).
- Spec 001, T-031 — depende de este documento: su aceptación dice que los números salen de aquí.
