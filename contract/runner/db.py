"""PostgreSQL access through the psql CLI (no Python driver needed)."""
import csv
import io
import os
import re
import subprocess

INT = re.compile(r"^-?\d+$")
DEC = re.compile(r"^-?\d+\.\d+$")


class SqlError(Exception):
    pass


class Database:
    def __init__(self, cfg):
        self.host = cfg["host"]
        self.port = str(cfg.get("port", 5432))
        self.name = cfg["name"]
        self.user = cfg["user"]
        self.env = {**os.environ, "PGPASSWORD": str(cfg.get("password", ""))}
        self.preserve = set(cfg.get("preserveTables", []))
        if self.host not in ("localhost", "127.0.0.1"):
            raise SystemExit(f"Refusing to use non-local database host '{self.host}'")

    def _psql(self, sql, database=None):
        cmd = ["psql", "-X", "-q", "-v", "ON_ERROR_STOP=1", "--csv",
               "-h", self.host, "-p", self.port, "-U", self.user, "-d", database or self.name, "-c", sql]
        result = subprocess.run(cmd, env=self.env, capture_output=True, text=True)
        if result.returncode != 0:
            raise SqlError(result.stderr.strip().removeprefix("ERROR:").strip())
        return result.stdout

    def query(self, sql):
        """Rows of the last statement as dicts, with numbers/booleans converted. NULL and '' both read as None."""
        out = self._psql(sql)
        if not out.strip():
            return []
        reader = csv.DictReader(io.StringIO(out))
        return [{k: _typed(v) for k, v in row.items()} for row in reader]

    def recreate(self):
        self._psql(f'DROP DATABASE IF EXISTS "{self.name}" WITH (FORCE)', database="postgres")
        self._psql(f'CREATE DATABASE "{self.name}"', database="postgres")

    def clear(self):
        tables = [r["tablename"] for r in self.query("SELECT tablename FROM pg_tables WHERE schemaname = 'public'")]
        doomed = [f'"{t}"' for t in tables if t not in self.preserve]
        if doomed:
            self._psql(f"TRUNCATE {', '.join(doomed)} RESTART IDENTITY CASCADE")


def _typed(v):
    if v is None or v == "":
        return None
    if v in ("t", "f"):
        return v == "t"
    if INT.match(v):
        return int(v)
    if DEC.match(v):
        return float(v)
    return v
