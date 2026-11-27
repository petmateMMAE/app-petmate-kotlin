#!/usr/bin/env bash
# Comprueba que la corrida de pruebas no omitio clases en silencio.
#
# Por que existe (27/11/2026): tras una caida del demonio de Kotlin, Gradle puede dar BUILD SUCCESSFUL
# habiendo ejecutado SOLO una parte de las clases. Paso de verdad: la corrida dijo verde con 61 clases y 627
# pruebas cuando habia 89 y 879. Las 28 que faltaban no tenian .class generado y la tarea simplemente no las
# encontro, sin avisar. `--rerun` no lo arregla.
#
# Un verde que omite un tercio de las pruebas es peor que un rojo: da permiso para seguir.
set -euo pipefail

RESULTS_DIR="${1:-app/build/test-results/testDebugUnitTest}"
SOURCE_DIR="${2:-app/src/test}"

if [ ! -d "$RESULTS_DIR" ]; then
    echo "No hay resultados en $RESULTS_DIR. Corre las pruebas primero."
    exit 1
fi

# Clases de prueba que existen en el codigo.
expected=$(grep -rl "@Test" "$SOURCE_DIR" --include=*.kt 2>/dev/null | wc -l | tr -d ' ')
# Clases que dejaron resultado.
actual=$(ls "$RESULTS_DIR"/TEST-*.xml 2>/dev/null | wc -l | tr -d ' ')

echo "clases con @Test en el codigo: $expected"
echo "clases con resultado:          $actual"

if [ "$expected" -ne "$actual" ]; then
    echo
    echo "LA CORRIDA OMITIO $((expected - actual)) CLASES EN SILENCIO."
    echo "Normalmente pasa despues de que muere el demonio de Kotlin: queda un build a medias y la tarea"
    echo "no encuentra las clases sin compilar. No le creas al BUILD SUCCESSFUL."
    echo
    echo "Que hacer: ./gradlew --stop && ./gradlew :app:testDebugUnitTest --rerun-tasks"
    echo "y vuelve a correr esta comprobacion."
    exit 1
fi

echo "La corrida esta completa."
