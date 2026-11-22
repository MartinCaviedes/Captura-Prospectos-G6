"""Resume JaCoCo por líneas de fuente y comprueba el umbral sin redondear.

Android: dominio + ViewModels + toda la capa data escrita a mano. El informe
completo permanece disponible, incluidos UI y código generado por Room/KSP.
No suma porcentajes ni suma contadores de clases Kotlin que comparten líneas.
"""
import argparse
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def counts(node, kind="LINE"):
    counter = node.find(f"counter[@type='{kind}']")
    if counter is None:
        return (0, 0)
    covered, missed = int(counter.attrib["covered"]), int(counter.attrib["missed"])
    return covered, covered + missed


def metric(covered, total):
    return dict(covered=covered, total=total,
                percent=round(100 * covered / total, 2) if total else None)


def summarize(android_report):
    results = {}
    for module in ("api-gateway", "users-service", "prospects-service"):
        path = ROOT / "backend" / module / "target/site/jacoco/jacoco.xml"
        report = ET.parse(path).getroot()
        results[module] = metric(*counts(report))

    report = ET.parse(android_report).getroot()
    results["android_report_all_including_generated"] = metric(*counts(report))
    groups = {name: [0, 0] for name in ("domain", "viewmodels", "data", "rendering", "generated")}
    files = []
    for package in report.findall("package"):
        for source in package.findall("sourcefile"):
            name = source.attrib["name"]
            relative = Path(package.attrib["name"]) / name
            handwritten = (ROOT / "android/app/src/main/java" / relative).is_file()
            if not handwritten:
                group = "generated"
            elif "/domain/" in str(relative).replace("\\", "/"):
                group = "domain"
            elif name.endswith("ViewModel.kt"):
                group = "viewmodels"
            elif "/data/" in str(relative).replace("\\", "/"):
                group = "data"
            else:
                group = "rendering"
            covered, total = counts(source)
            groups[group][0] += covered
            groups[group][1] += total
            files.append(dict(file=relative.as_posix(), group=group, **metric(covered, total)))
    for name, values in groups.items():
        results["android_" + name] = metric(*values)
    selected = [groups[name] for name in ("domain", "viewmodels", "data")]
    results["android_logic"] = metric(sum(c for c, _ in selected), sum(t for _, t in selected))
    return dict(reports=results, android_files=files)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--android-report", type=Path, default=ROOT / "android/app/build/reports/jacoco/jacocoDemoDebugCombinedReport/jacocoDemoDebugCombinedReport.xml")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--minimum", type=int, default=80)
    args = parser.parse_args()
    if not 0 <= args.minimum <= 100:
        parser.error("minimum debe estar entre 0 y 100")
    try:
        result = summarize(args.android_report)
    except (OSError, ET.ParseError) as error:
        parser.exit(2, f"Informe ausente o inválido: {error}\n")
    required = ("api-gateway", "users-service", "prospects-service", "android_logic")
    failed = [name for name in required if not result["reports"][name]["total"] or
              result["reports"][name]["covered"] * 100 < args.minimum * result["reports"][name]["total"]]
    result.update(minimum=args.minimum, failed=failed)
    for name, value in result["reports"].items():
        print(f"{name}: {value['covered']}/{value['total']} ({value['percent']} %)")
    if args.output:
        args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if failed:
        print("Umbral pendiente: " + ", ".join(failed), file=sys.stderr)
        return 1
    print("Umbral aprobado para cada servicio y lógica Android.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
