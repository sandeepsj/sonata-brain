"""Console output, report.md, and the requirements traceability check (REQ-003.AC1)."""
import re
import sys
from collections import defaultdict
from pathlib import Path

AC = re.compile(r"\*\*(REQ-\d{3}\.AC\d+)\*\* \[([a-z-]+)]")

_TTY = sys.stdout.isatty()
GREEN, RED, YELLOW, DIM, RESET = (("\033[32m", "\033[31m", "\033[33m", "\033[2m", "\033[0m") if _TTY
                                  else ("", "", "", "", ""))


def criteria_from_docs(requirements_dir):
    criteria = {}
    for f in sorted(Path(requirements_dir).glob("[0-9][0-9]-*.md")):
        for m in AC.finditer(f.read_text()):
            if m.group(1) in criteria:
                raise SystemExit(f"Duplicate acceptance criterion {m.group(1)} in {f.name}")
            criteria[m.group(1)] = m.group(2)
    return criteria


def print_result(case, error):
    if error is None:
        print(f"  {GREEN}✓{RESET} {case.id} {DIM}{case.title}{RESET}")
    else:
        print(f"  {RED}✗ {case.id}{RESET} {case.title}")
        print(f"      {error}")


def trace(all_cases, criteria):
    """Returns (unknown citations, uncovered criteria, coverage map)."""
    coverage = defaultdict(list)
    unknown = []
    for c in all_cases:
        for r in c.req:
            if r not in criteria:
                unknown.append(f"{c.id} ({c.file}) cites unknown {r}")
            coverage[r].append(c)
    uncovered = [ac for ac in criteria if ac not in coverage]
    return sorted(set(unknown)), uncovered, coverage


def write_report(path, results, all_cases, criteria):
    unknown, uncovered, coverage = trace(all_cases, criteria)
    status = {c.id: ("pass" if e is None else "FAIL") for c, e in results}
    lines = ["# Contract run", ""]
    by_suite = defaultdict(lambda: [0, 0])
    for c, e in results:
        by_suite[c.suite][0 if e is None else 1] += 1
    for suite, (ok, bad) in sorted(by_suite.items()):
        lines.append(f"- **{suite}**: {ok} passed, {bad} failed")
    lines += ["", "## Failures", ""]
    fails = [(c, e) for c, e in results if e is not None]
    lines += [f"- `{c.id}` {c.title} ({c.file})\n\n  ```\n  {e}\n  ```" for c, e in fails] or ["None."]
    lines += ["", "## Requirement coverage", "", "| Criterion | Kind | Cases |", "|---|---|---|"]
    for ac, kind in criteria.items():
        cells = [f"{c.id} ({status.get(c.id, 'not run')})" for c in coverage.get(ac, [])]
        lines.append(f"| {ac} | {kind} | {'<br>'.join(cells) or '**none**'} |")
    if unknown:
        lines += ["", "## Unknown citations", ""] + [f"- {u}" for u in unknown]
    Path(path).write_text("\n".join(lines) + "\n")
    return unknown, uncovered
