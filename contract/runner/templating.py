"""{{ ... }} substitution in spec values. Vocabulary documented in contract/README.md ("Values and variables")."""
import base64
import calendar
import json
import itertools
import re
from datetime import date, datetime, timedelta

TOKEN = re.compile(r"\{\{\s*([^{}]+?)\s*\}\}")
OFFSET = re.compile(r"^([+-]\d+)([dwmy])$")
_uniq = itertools.count(1000)


class SpecError(Exception):
    """The spec itself is wrong (unknown variable, bad syntax), as opposed to the API failing it."""


def render(value, variables):
    if isinstance(value, dict):
        return {render(k, variables): render(v, variables) for k, v in value.items()}
    if isinstance(value, list):
        return [render(v, variables) for v in value]
    if not isinstance(value, str) or "{{" not in value:
        return value
    whole = TOKEN.fullmatch(value.strip())
    if whole:
        return evaluate(whole.group(1), variables)
    return TOKEN.sub(lambda m: _as_text(evaluate(m.group(1), variables)), value)


def _as_text(v):
    if isinstance(v, bool):
        return "true" if v else "false"
    return "" if v is None else str(v)


def evaluate(expr, variables):
    parts = expr.split()
    head, args = parts[0], parts[1:]
    if head == "today":
        return _shift(date.today(), args).isoformat()
    if head == "now":
        return _shift(datetime.now().replace(microsecond=0), args).isoformat()
    if head == "year":
        return date.today().year + (int(args[0]) if args else 0)
    if head == "uniq":
        return f"u{next(_uniq)}"
    if head == "jwtPayload":
        if len(args) != 1:
            raise SpecError("use {{jwtPayload <variable>}}")
        return _jwt_payload(lookup(variables, args[0]))
    if args:
        raise SpecError(f"unexpected text after variable in {{{{{expr}}}}}")
    return lookup(variables, head)


def _jwt_payload(token):
    """The decoded middle part of a JWT. The signature is not checked; the API does that."""
    parts = str(token).split(".")
    if len(parts) != 3:
        raise SpecError(f"not a JWT: {str(token)[:40]}")
    padded = parts[1] + "=" * (-len(parts[1]) % 4)
    return json.loads(base64.urlsafe_b64decode(padded))


def lookup(variables, dotted):
    current = variables
    for part in dotted.split("."):
        if isinstance(current, dict) and part in current:
            current = current[part]
        elif isinstance(current, list) and part.lstrip("-").isdigit() and -len(current) <= int(part) < len(current):
            current = current[int(part)]
        else:
            raise SpecError(f"unknown variable '{dotted}' (known: {', '.join(sorted(variables)) or 'none'})")
    return current


def _shift(value, args):
    for arg in args:
        m = OFFSET.match(arg)
        if not m:
            raise SpecError(f"bad date offset '{arg}' (use e.g. -3d, +2w, +1m, -1y)")
        n, unit = int(m.group(1)), m.group(2)
        if unit == "d":
            value = value + timedelta(days=n)
        elif unit == "w":
            value = value + timedelta(weeks=n)
        else:
            months = n * (12 if unit == "y" else 1)
            y, mo = divmod(value.month - 1 + months, 12)
            year, month = value.year + y, mo + 1
            day = min(value.day, calendar.monthrange(year, month)[1])
            value = value.replace(year=year, month=month, day=day)
    return value
