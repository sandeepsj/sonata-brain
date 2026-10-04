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
    p.add_argument("--suite", default="baseline", choices=["baseline", "target", "quarantine", "all"])
    p.add_argument("--only", help="case id or id prefix")
    p.add_argument("--file", help="spec file name (substring)")
    p.add_argument("--no-build", action="store_true", help="don't rebuild the backend")
    p.add_argument("--no-server", action="store_true", help="use an already running server")
    p.add_argument("--base-url", help="override baseUrl from contract.yaml")
    p.add_argument("--strict-trace", action="store_true", help="fail if any acceptance criterion has no case")
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

    db = Database(cfg["database"])
    server = None
    if not args.no_server:
        server = Server(cfg["server"], base_url, HERE.parent, out / "server.log")
        if not args.no_build:
            server.build()
        db.recreate()
        server.start()

    engine = Engine(base_url, db, HERE)
    results = []
    started = time.time()
    try:
        current_area = None
        for case in selected:
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
    finally:
        if server:
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
