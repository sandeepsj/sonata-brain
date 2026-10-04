"""Generic expected-vs-actual matching. Semantics documented in contract/README.md ("Expectations").

Nothing here knows about any particular endpoint or case.
"""
import json
import re

MISSING = object()  # a field that is not present at all (different from JSON null)


class Mismatch(Exception):
    def __init__(self, path, message):
        super().__init__(f"{path or '(root)'}: {message}")
        self.path = path
        self.message = message


def show(value, limit=300):
    if value is MISSING:
        return "<missing>"
    text = json.dumps(value, ensure_ascii=False, default=str)
    return text if len(text) <= limit else text[:limit] + "…"


def is_operator(expected):
    return isinstance(expected, dict) and expected and all(str(k).startswith("$") for k in expected)


def _has_operator(expected):
    return isinstance(expected, dict) and any(str(k).startswith("$") for k in expected)


def _is_number(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def _equal(expected, actual):
    if _is_number(expected) and _is_number(actual):
        return float(expected) == float(actual)
    if isinstance(expected, dict) and isinstance(actual, dict):
        return expected.keys() == actual.keys() and all(_equal(expected[k], actual[k]) for k in expected)
    if isinstance(expected, list) and isinstance(actual, list):
        return len(expected) == len(actual) and all(_equal(e, a) for e, a in zip(expected, actual))
    return type(expected) is type(actual) and expected == actual


TYPES = {
    "string": lambda v: isinstance(v, str),
    "number": _is_number,
    "integer": lambda v: isinstance(v, int) and not isinstance(v, bool)
                         or isinstance(v, float) and v.is_integer(),
    "boolean": lambda v: isinstance(v, bool),
    "object": lambda v: isinstance(v, dict),
    "array": lambda v: isinstance(v, list),
    "null": lambda v: v is None,
}


class Matcher:
    def __init__(self, shapes):
        self.shapes = shapes or {}

    def check(self, expected, actual, path=""):
        """Raise Mismatch if actual doesn't satisfy expected."""
        if _has_operator(expected):
            self._operators(expected, actual, path)
            return
        if actual is MISSING:
            raise Mismatch(path, f"expected {show(expected)} but the field is missing")
        if isinstance(expected, dict):
            if not isinstance(actual, dict):
                raise Mismatch(path, f"expected an object but got {show(actual)}")
            for key, exp in expected.items():
                self.check(exp, actual.get(key, MISSING), f"{path}.{key}" if path else key)
            return
        if isinstance(expected, list):
            if not isinstance(actual, list):
                raise Mismatch(path, f"expected a list of {len(expected)} but got {show(actual)}")
            if len(actual) != len(expected):
                raise Mismatch(path, f"expected {len(expected)} items but got {len(actual)}: {show(actual)}")
            for i, (exp, act) in enumerate(zip(expected, actual)):
                self.check(exp, act, f"{path}[{i}]")
            return
        if not _equal(expected, actual):
            raise Mismatch(path, f"expected {show(expected)} but got {show(actual)}")

    def matches(self, expected, actual):
        try:
            self.check(expected, actual)
            return True
        except Mismatch:
            return False

    # ---- operators ------------------------------------------------------------------------------------------

    def _operators(self, spec, actual, path):
        plain = {k: v for k, v in spec.items() if not str(k).startswith("$")}
        ops = {k: v for k, v in spec.items() if str(k).startswith("$")}

        if ops.get("$absent") is True:
            if actual is not MISSING:
                raise Mismatch(path, f"expected the field to be absent but it is {show(actual)}")
            return
        if "$present" in ops:
            if (actual is MISSING) == bool(ops["$present"]):
                raise Mismatch(path, "expected the field to be present" if ops["$present"]
                               else f"expected the field to be absent but it is {show(actual)}")
            ops.pop("$present")
            if actual is MISSING:
                return
        if actual is MISSING:
            raise Mismatch(path, f"expected a value matching {show(spec)} but the field is missing")

        for op, arg in ops.items():
            handler = getattr(self, "_op_" + op[1:], None)
            if handler is None:
                raise Mismatch(path, f"unknown operator {op} in the spec")
            handler(arg, actual, path)
        if plain:
            self.check(plain, actual, path)

    def _op_shape(self, name, actual, path):
        if name not in self.shapes:
            raise Mismatch(path, f"unknown shape '{name}' (see shapes.yaml)")
        try:
            self.check(self.shapes[name], actual, path)
        except Mismatch as m:
            raise Mismatch(m.path, f"{m.message} (shape '{name}')")

    def _op_eq(self, arg, actual, path):
        if not _equal(arg, actual):
            raise Mismatch(path, f"expected exactly {show(arg)} but got {show(actual)}")

    def _op_ne(self, arg, actual, path):
        if _equal(arg, actual):
            raise Mismatch(path, f"expected anything but {show(arg)}")

    def _op_type(self, arg, actual, path):
        if arg not in TYPES:
            raise Mismatch(path, f"unknown $type '{arg}'")
        if not TYPES[arg](actual):
            raise Mismatch(path, f"expected type {arg} but got {show(actual)}")

    def _op_oneOf(self, arg, actual, path):
        if not any(self.matches(option, actual) for option in arg):
            raise Mismatch(path, f"expected one of {show(arg)} but got {show(actual)}")

    def _compare(self, arg, actual, path, name, ok):
        comparable = (_is_number(arg) and _is_number(actual)) or (isinstance(arg, str) and isinstance(actual, str))
        if not comparable or not ok(actual, arg):
            raise Mismatch(path, f"expected {name} {show(arg)} but got {show(actual)}")

    def _op_gt(self, arg, actual, path):
        self._compare(arg, actual, path, ">", lambda a, b: a > b)

    def _op_gte(self, arg, actual, path):
        self._compare(arg, actual, path, ">=", lambda a, b: a >= b)

    def _op_lt(self, arg, actual, path):
        self._compare(arg, actual, path, "<", lambda a, b: a < b)

    def _op_lte(self, arg, actual, path):
        self._compare(arg, actual, path, "<=", lambda a, b: a <= b)

    def _op_regex(self, arg, actual, path):
        if not isinstance(actual, str) or not re.search(arg, actual):
            raise Mismatch(path, f"expected text matching /{arg}/ but got {show(actual)}")

    def _op_contains(self, arg, actual, path):
        if not isinstance(actual, str) or str(arg).lower() not in actual.lower():
            raise Mismatch(path, f"expected text containing {show(arg)} but got {show(actual)}")

    def _op_notBlank(self, arg, actual, path):
        blank = not isinstance(actual, str) or not actual.strip()
        if blank == bool(arg):
            raise Mismatch(path, f"expected {'non-blank' if arg else 'blank'} text but got {show(actual)}")

    def _op_len(self, arg, actual, path):
        if not isinstance(actual, (list, str, dict)):
            raise Mismatch(path, f"expected something with a length but got {show(actual)}")
        try:
            self.check(arg, len(actual))
        except Mismatch as m:
            raise Mismatch(path, f"length: {m.message}; value: {show(actual)}")

    def _need_list(self, actual, path):
        if not isinstance(actual, list):
            raise Mismatch(path, f"expected a list but got {show(actual)}")

    def _op_has(self, arg, actual, path):
        self._need_list(actual, path)
        for item in arg:
            if not any(self.matches(item, a) for a in actual):
                raise Mismatch(path, f"expected the list to include {show(item)}; list is {show(actual)}")

    def _op_unordered(self, arg, actual, path):
        self._need_list(actual, path)
        if len(arg) != len(actual):
            raise Mismatch(path, f"expected {len(arg)} items (any order) but got {len(actual)}: {show(actual)}")
        if not self._assign(list(arg), list(actual)):
            raise Mismatch(path, f"expected (any order) {show(arg)} but got {show(actual)}")

    def _assign(self, expected, actual):
        if not expected:
            return True
        head, rest = expected[0], expected[1:]
        for i, candidate in enumerate(actual):
            if self.matches(head, candidate) and self._assign(rest, actual[:i] + actual[i + 1:]):
                return True
        return False

    def _op_every(self, arg, actual, path):
        self._need_list(actual, path)
        for i, item in enumerate(actual):
            self.check(arg, item, f"{path}[{i}]")

    def _op_none(self, arg, actual, path):
        self._need_list(actual, path)
        for i, item in enumerate(actual):
            if self.matches(arg, item):
                raise Mismatch(f"{path}[{i}]", f"expected no item matching {show(arg)} but found {show(item)}")

    def _op_only(self, arg, actual, path):
        if not isinstance(actual, dict):
            raise Mismatch(path, f"expected an object but got {show(actual)}")
        extra = sorted(set(actual) - set(arg))
        if extra:
            raise Mismatch(path, f"unexpected fields {extra}")
        self.check(arg, actual, path)

    def _op_not(self, arg, actual, path):
        if self.matches(arg, actual):
            raise Mismatch(path, f"expected NOT to match {show(arg)} but got {show(actual)}")
