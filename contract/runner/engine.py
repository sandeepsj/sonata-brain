"""Loads spec files and executes their steps. Step vocabulary documented in contract/README.md ("Steps")."""
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from pathlib import Path

import yaml

from .db import SqlError
from .matching import MISSING, Matcher, Mismatch, show
from .server import parse_duration
from .templating import SpecError, lookup, render

METHODS = ("GET", "POST", "PUT", "PATCH", "DELETE")
STEP_KEYS = {"auth", "query", "headers", "body", "rawBody", "expect", "save", "repeat", "eventually", "note"}


@dataclass
class Case:
    id: str
    title: str
    req: list
    suite: str
    steps: list
    background: list
    file: str
    area: str
    variables: dict = field(default_factory=dict)


class CaseFailed(Exception):
    def __init__(self, step_label, detail):
        super().__init__(f"{step_label}\n      {detail}")
        self.step_label = step_label
        self.detail = detail


def load_cases(spec_dir):
    cases, seen = [], {}
    for path in sorted(Path(spec_dir).rglob("*.yaml")):
        doc = yaml.safe_load(path.read_text()) or {}
        rel = str(path.relative_to(spec_dir))
        default_suite = doc.get("suite", "baseline")
        for raw in doc.get("cases", []):
            for key in ("id", "title", "steps"):
                if key not in raw:
                    raise SpecError(f"{rel}: a case is missing '{key}': {show(raw, 120)}")
            if raw["id"] in seen:
                raise SpecError(f"duplicate case id {raw['id']} in {rel} and {seen[raw['id']]}")
            seen[raw["id"]] = rel
            examples = raw.get("examples") or [None]
            for n, example in enumerate(examples, 1):
                suffix = f" [{n}]" if raw.get("examples") else ""
                cases.append(Case(
                    id=raw["id"] + suffix, title=raw["title"], req=list(raw.get("req", [])),
                    suite=raw.get("suite", default_suite), steps=raw["steps"],
                    background=doc.get("background", []), file=rel, area=doc.get("area", rel),
                    variables=dict(example or {})))
    return cases


class Engine:
    def __init__(self, base_url, db, contract_dir):
        self.base_url = base_url.rstrip("/")
        self.db = db
        self.fixtures_dir = Path(contract_dir) / "fixtures"
        shapes_file = Path(contract_dir) / "shapes.yaml"
        self.matcher = Matcher(yaml.safe_load(shapes_file.read_text()) if shapes_file.exists() else {})

    # ---- cases -----------------------------------------------------------------------------------------------

    def run_case(self, case):
        self.db.clear()
        variables = dict(case.variables)
        for i, step in enumerate(case.background, 1):
            self.run_step(step, variables, f"background step {i}")
        for i, step in enumerate(case.steps, 1):
            self.run_step(step, variables, f"step {i}")

    def run_step(self, step, variables, where):
        if not isinstance(step, dict):
            raise SpecError(f"{where}: a step must be a mapping, got {show(step, 80)}")
        if "use" in step:
            return self._use(step, variables, where)
        count = render(step.get("repeat", 1), variables)
        for i in range(1, int(count) + 1):
            if "repeat" in step:
                variables["i"] = i
            label = where + (f" (repeat {i}/{count})" if "repeat" in step else "")
            if "eventually" in step:
                self._eventually(step, variables, label)
            else:
                self._once(step, variables, label)

    def _use(self, step, variables, where):
        path = self.fixtures_dir / f"{step['use']}.yaml"
        if not path.exists():
            raise SpecError(f"{where}: fixture '{step['use']}' not found in {self.fixtures_dir}")
        fixture = yaml.safe_load(path.read_text()) or {}
        params = {**(fixture.get("params") or {}), **render(step.get("with") or {}, variables)}
        previous = {k: variables.get(k, MISSING) for k in params}
        variables.update(params)
        for i, sub in enumerate(fixture.get("steps", []), 1):
            self.run_step(sub, variables, f"{where} › {step['use']} step {i}")
        for k, v in previous.items():  # fixture parameters don't leak; saved values do
            if v is MISSING:
                variables.pop(k, None)
            else:
                variables[k] = v

    def _eventually(self, step, variables, label):
        deadline = time.time() + parse_duration(step["eventually"])
        while True:
            try:
                return self._once(step, variables, label)
            except CaseFailed:
                if time.time() >= deadline:
                    raise
                time.sleep(0.3)

    def _once(self, step, variables, label):
        if "sql" in step:
            return self._sql(step, variables, label)
        methods = [m for m in METHODS if m in step]
        if len(methods) != 1:
            raise SpecError(f"{label}: a step needs exactly one of {METHODS}, 'sql' or 'use'")
        unknown = set(step) - STEP_KEYS - {methods[0]}
        if unknown:
            raise SpecError(f"{label}: unknown step keys {sorted(unknown)}")
        method = methods[0]
        path = render(step[method], variables)
        label = f"{label}: {method} {path}"
        response = self._http(method, path, step, variables)
        self._expect_http(step.get("expect"), response, variables, label)
        self._save(step.get("save"), {"status": response["status"], "body": response["body"],
                                      "text": response["text"]}, variables, label)

    # ---- HTTP ------------------------------------------------------------------------------------------------

    def _http(self, method, path, step, variables):
        url = self.base_url + path
        query = render(step.get("query") or {}, variables)
        if query:
            url += ("&" if "?" in url else "?") + urllib.parse.urlencode(
                {k: _query_value(v) for k, v in query.items() if v is not None})
        headers = {"Accept": "application/json, text/plain, */*"}
        if step.get("auth"):
            headers["Authorization"] = f"Bearer {lookup(variables, step['auth'])}"
        headers.update({k: str(v) for k, v in render(step.get("headers") or {}, variables).items()})
        data = None
        if "body" in step:
            data = json.dumps(render(step["body"], variables)).encode()
            headers["Content-Type"] = "application/json"
        elif "rawBody" in step:
            data = str(render(step["rawBody"], variables)).encode()
            headers.setdefault("Content-Type", "application/json")
        request = urllib.request.Request(url, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=60) as r:
                status, text = r.status, r.read().decode()
        except urllib.error.HTTPError as e:
            status, text = e.code, e.read().decode()
        try:
            body = json.loads(text) if text.strip() else MISSING
        except json.JSONDecodeError:
            body = text
        return {"status": status, "text": text, "body": body}

    def _expect_http(self, expect, response, variables, label):
        if not expect:
            return
        expect = render(expect, variables)
        unknown = set(expect) - {"status", "body", "text"}
        if unknown:
            raise SpecError(f"{label}: unknown expect keys {sorted(unknown)}")
        try:
            if "status" in expect:
                self.matcher.check(expect["status"], response["status"], "status")
            if "body" in expect:
                self.matcher.check(expect["body"], response["body"], "body")
            if "text" in expect:
                self.matcher.check(expect["text"], response["text"], "text")
        except Mismatch as m:
            raise CaseFailed(label, f"{m}\n      response {response['status']}: {show(response['text'], 600)}")

    # ---- SQL -------------------------------------------------------------------------------------------------

    def _sql(self, step, variables, label):
        sql = render(step["sql"], variables)
        label = f"{label}: SQL {' '.join(sql.split())[:100]}"
        expect = render(step.get("expect") or {}, variables)
        try:
            rows, error = self.db.query(sql), MISSING
        except SqlError as e:
            rows, error = MISSING, str(e)
        try:
            if "error" in expect:
                if error is MISSING:
                    raise Mismatch("error", f"expected the statement to fail but it succeeded: {show(rows)}")
                self.matcher.check(expect["error"], error, "error")
            elif error is not MISSING:
                raise Mismatch("sql", f"statement failed: {error}")
            if "rows" in expect:
                self.matcher.check(expect["rows"], rows, "rows")
        except Mismatch as m:
            raise CaseFailed(label, str(m))
        self._save(step.get("save"), {"rows": rows if rows is not MISSING else []}, variables, label)

    # ---- save ------------------------------------------------------------------------------------------------

    def _save(self, save, source, variables, label):
        for name, dotted in (save or {}).items():
            try:
                variables[name] = lookup(source, dotted)
            except SpecError:
                raise CaseFailed(label, f"save {name}: '{dotted}' not found in response: {show(source, 400)}")


def _query_value(v):
    if isinstance(v, bool):
        return "true" if v else "false"
    return v
