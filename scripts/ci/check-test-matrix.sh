#!/usr/bin/env bash
# Cada clase de prueba unitaria debe tener su job en la matriz `feature-tests` del pipeline.
# Equivale a la prueba de arquitectura del backend que exige [Trait("Endpoint", ...)] en cada prueba de contrato:
# si alguien agrega una prueba y no la registra en la matriz, el pipeline falla aqui.
set -euo pipefail

workflow=".github/workflows/pipeline.yml"
missing=0
count=0

while IFS= read -r file; do
  count=$((count + 1))
  class="$(basename "$file" .kt)"
  if ! grep -q "\.${class}'" "$workflow"; then
    echo "::error file=${file}::La prueba ${class} no esta en la matriz feature-tests de ${workflow}."
    missing=1
  fi
done < <(find app/src/test -name '*Test.kt' | sort)

if [ "$count" -eq 0 ]; then
  echo "::error::No se encontraron pruebas unitarias en app/src/test."
  exit 1
fi

{
  echo "### Matriz de pruebas"
  if [ "$missing" -eq 0 ]; then
    echo "Las ${count} clases de prueba tienen su job en la matriz."
  else
    echo "**Hay clases de prueba sin job en la matriz.** Agregalas a \`feature-tests\`."
  fi
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"

exit "$missing"
