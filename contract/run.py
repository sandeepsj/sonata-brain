#!/usr/bin/env python3
"""Runs the API contract. See contract/README.md."""
import argparse
import sys
import time
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from runner.db import Database  # noqa: E402
from runner.engine import CaseFailed, Engine, load_cases  # noqa: E402
from runner.matching import Mismatch  # noqa: E402
from runner.report import YELLOW, RESET, criteria_from_docs, print_result, write_report  # noqa: E402
from runner.server import Server  # noqa: E402
from runner.templating import SpecError  # noqa: E402


def main():
    p = argparse.ArgumentParser(description="Run the Sonata API contract")
    p.add_argument("--suite", default="baseline", choices=["baseline", "target", "quarantine", "migration", "all"])
    p.add_argument("--only", help="case id or id prefix")
    p.add_argument("--file", help="spec file name (substring)")
    p.add_argument("--no-build", action="store_true", help="don't rebuild the backend")
    p.add_argument("--no-server", action="store_true", help="use an already running server")
    p.add_argument("--base-url", help="override baseUrl from contract.yaml")
    p.add_argument("--strict-trace", action="store_true", help="fail if any acceptance criterion has no case")
    p.add_argument("--trace-only", action="store_true", help="only check requirement coverage; run nothing")
    args = p.parse_args()

    cfg = yaml.safe_load((HERE / "contract.yaml").read_text())
    base_url = args.base_url or cfg["baseUrl"]
    out = HERE / ".out"
    out.mkdir(exist_ok=True)

    all_cases = load_cases(HERE / "specs")
    criteria = criteria_from_docs((HERE / cfg["requirements"]).resolve())
    selected = [c for c in all_cases
                if (args.suite == "all" or c.suite == args.suite)
                and (not args.only or c.id.startswith(args.only))
                and (not args.file or args.file in c.file)]
    print(f"{len(selected)} of {len(all_cases)} cases selected (suite={args.suite})")
    if args.trace_only:
        selected = []

    db = Database(cfg["database"])
    plain = [c for c in selected if not c.database]
    seeded = {}
    for c in selected:
        if c.database:
            seeded.setdefault(c.file, []).append(c)

    server = Server(cfg["server"], base_url, HERE.parent, out / "server.log")
    if not args.no_server and not args.no_build and selected:
        server.build()

    engine = Engine(base_url, db, HERE)
    results = []
    started = time.time()

    def run_group(cases):
        current_area = None
        for case in cases:
            if case.area != current_area:
                current_area = case.area
                print(f"\n{case.area}  ({case.file})")
            error = None
            try:
                engine.run_case(case)
            except CaseFailed as e:
                error = str(e)
            except (SpecError, Mismatch) as e:
                error = f"SPEC ERROR: {e}"
            print_result(case, error)
            results.append((case, error))

    # Cases on an empty database share one server.
    if plain:
        if not args.no_server:
            db.recreate()
            server.start()
        try:
            run_group(plain)
        finally:
            if not args.no_server:
                server.stop()

    # Each seeded file gets its own database and server start (migrations run at startup).
    for file, cases in seeded.items():
        spec = cases[0].database
        if args.no_server:
            print(f"\n{YELLOW}Skipping {file}: needs its own seeded database (don't use --no-server){RESET}")
            continue
        if "template" in spec and not db.exists(spec["template"]):
            print(f"\n{YELLOW}Skipping {file}: local database '{spec['template']}' not found "
                  f"(see scripts/db/reset-local-from-prod.sh){RESET}")
            continue
        db.recreate_from(spec)
        server.start()
        try:
            run_group(cases)
        finally:
            server.stop()

    unknown, uncovered = write_report(out / "report.md", results, all_cases, criteria)
    failed = sum(1 for _, e in results if e)
    print(f"\n{len(results) - failed} passed, {failed} failed in {time.time() - started:.1f}s"
          f"  (report: {out / 'report.md'})")
    if unknown:
        print(f"{YELLOW}Unknown requirement citations:{RESET}\n  " + "\n  ".join(unknown))
    if uncovered:
        print(f"{YELLOW}{len(uncovered)} acceptance criteria have no case yet{RESET} (listed in the report)")
    trace_failed = bool(unknown) or (args.strict_trace and bool(uncovered))
    sys.exit(1 if failed or trace_failed else 0)


if __name__ == "__main__":
    main()
