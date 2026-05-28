#!/usr/bin/env python3
"""Build coverage-comment.md from backend JaCoCo and frontend Vitest artifacts."""

from __future__ import annotations

import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

BACKEND_JACOCO = Path("artifacts/backend/backend/target/site/jacoco/jacoco.xml")
FRONTEND_SUMMARY = Path("artifacts/frontend/frontend/homepage/coverage/coverage-summary.json")
OUTPUT = Path("coverage-comment.md")


def jacoco_pct(path: Path, counter_type: str) -> str:
    root = ET.parse(path).getroot()
    for counter in root.findall("counter"):
        if counter.attrib.get("type") != counter_type:
            continue
        covered = int(counter.attrib["covered"])
        missed = int(counter.attrib["missed"])
        total = covered + missed
        return f"{(covered / total * 100):.2f}" if total else "0.00"
    return "0.00"


def vitest_pct(path: Path, key: str) -> str:
    with path.open(encoding="utf-8") as handle:
        total = json.load(handle)["total"]
    return f"{total[key]['pct']:.2f}"


def main() -> int:
    missing = [str(p) for p in (BACKEND_JACOCO, FRONTEND_SUMMARY) if not p.is_file()]
    if missing:
        print(f"Missing coverage artifacts: {', '.join(missing)}", file=sys.stderr)
        return 1

    backend_line = jacoco_pct(BACKEND_JACOCO, "LINE")
    backend_branch = jacoco_pct(BACKEND_JACOCO, "BRANCH")
    frontend_line = vitest_pct(FRONTEND_SUMMARY, "lines")
    frontend_branch = vitest_pct(FRONTEND_SUMMARY, "branches")

    OUTPUT.write_text(
        "\n".join(
            [
                "## Coverage summary",
                "",
                "| Area | Line % | Branch % |",
                "| --- | ---: | ---: |",
                f"| Backend (JaCoCo) | {backend_line}% | {backend_branch}% |",
                f"| Frontend (Vitest) | {frontend_line}% | {frontend_branch}% |",
                "",
            ]
        ),
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
