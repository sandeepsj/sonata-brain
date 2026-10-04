"""Builds, starts and stops the backend under test as described in contract.yaml."""
import os
import subprocess
import time
import urllib.error
import urllib.request


def parse_duration(text):
    text = str(text).strip()
    if text.endswith("ms"):
        return float(text[:-2]) / 1000
    if text.endswith("s"):
        return float(text[:-1])
    if text.endswith("m"):
        return float(text[:-1]) * 60
    return float(text)


class Server:
    def __init__(self, cfg, base_url, repo_root, log_path):
        self.cfg = cfg
        self.base_url = base_url
        self.root = repo_root
        self.log_path = log_path
        self.proc = None

    def build(self):
        if self.cfg.get("build"):
            print(f"Building backend: {self.cfg['build']}", flush=True)
            subprocess.run(self.cfg["build"], shell=True, cwd=self.root, check=True)

    def start(self):
        env = {**os.environ, **{k: str(v) for k, v in (self.cfg.get("env") or {}).items()}}
        self.log = open(self.log_path, "w")
        print(f"Starting backend: {self.cfg['start']}", flush=True)
        self.proc = subprocess.Popen(self.cfg["start"], shell=True, cwd=self.root, env=env,
                                     stdout=self.log, stderr=subprocess.STDOUT, start_new_session=True)
        deadline = time.time() + parse_duration(self.cfg.get("readyTimeout", "120s"))
        url = self.base_url + self.cfg.get("ready", "/")
        while time.time() < deadline:
            if self.proc.poll() is not None:
                raise SystemExit(f"Backend exited during startup (code {self.proc.returncode}); see {self.log_path}")
            try:
                with urllib.request.urlopen(url, timeout=2) as r:
                    if r.status == 200:
                        print(f"Backend ready at {self.base_url}", flush=True)
                        return
            except (urllib.error.URLError, ConnectionError, TimeoutError):
                pass
            time.sleep(0.5)
        self.stop()
        raise SystemExit(f"Backend not ready after {self.cfg.get('readyTimeout', '120s')}; see {self.log_path}")

    def stop(self):
        if self.proc and self.proc.poll() is None:
            os.killpg(self.proc.pid, 15)
            try:
                self.proc.wait(timeout=20)
            except subprocess.TimeoutExpired:
                os.killpg(self.proc.pid, 9)
        if getattr(self, "log", None):
            self.log.close()
