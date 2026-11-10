"""Resumen de pruebas JUnit (Gradle) para $GITHUB_STEP_SUMMARY.

Uso: python3 scripts/ci/test-summary.py "<titulo>" <carpeta de resultados XML>
Equivale a scripts/ci-test-summary.ps1 del backend: tabla con aprobadas, fallidas y omitidas,
y la lista de las pruebas que fallaron.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    title = sys.argv[1] if len(sys.argv) > 1 else "Pruebas"
    results_dir = sys.argv[2] if len(sys.argv) > 2 else "app/build/test-results"
    total = failed = skipped = 0
    failures = []
    for path in glob.glob(os.path.join(results_dir, "**", "*.xml"), recursive=True):
        try:
            suite = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for case in suite.iter("testcase"):
            total += 1
            if case.find("failure") is not None or case.find("error") is not None:
                failed += 1
                failures.append(f"{case.get('classname', '')}.{case.get('name', '')}")
            elif case.find("skipped") is not None:
                skipped += 1
    passed = total - failed - skipped

    lines = [
        f"### {title}",
        "",
        "| Total | Aprobadas | Fallidas | Omitidas |",
        "|---:|---:|---:|---:|",
        f"| {total} | {passed} | {failed} | {skipped} |",
    ]
    if total == 0:
        lines += ["", "> No se encontraron resultados de pruebas."]
    if failures:
        lines += ["", "**Fallaron:**", ""] + [f"- `{name}`" for name in failures]

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    text = "\n".join(lines) + "\n"
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(text)
    print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
