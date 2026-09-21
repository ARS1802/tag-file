#!/usr/bin/env python3
"""Compila e testa o schema em dois MySQL descartáveis; nunca usa a porta 3333."""
import argparse
import os
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile
import time

ROOT = Path(__file__).resolve().parents[1]
MYSQL = Path(os.environ.get("TAG_FILE_MYSQL", ROOT / "database/runtime/mysql")).resolve()
ENV = dict(os.environ, LD_LIBRARY_PATH=str(MYSQL / "lib") + ":" + os.environ.get("LD_LIBRARY_PATH", ""))


def run():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", default=os.environ.get("TAG_FILE_JDK") or
                        str(Path(shutil.which("javac") or "javac").resolve().parents[1]))
    parser.add_argument("--probe", action="store_true", help="Verifica somente o schema original")
    args = parser.parse_args()
    jdk = Path(args.jdk)
    with tempfile.TemporaryDirectory(prefix="tag-file-schema-") as work:
        work = Path(work)
        classes = work / "classes"
        classes.mkdir()
        subprocess.run([str(jdk / "bin/javac"), "--release", "22", "-encoding", "UTF-8",
                        "-cp", str(ROOT / "lib/*"), "-d", str(classes),
                        *map(str, sorted((ROOT / "src").rglob("*.java"))),
                        str(ROOT / "tests/SchemaContractCheck.java")], check=True)
        failures = []
        for mode in (0, 1):
            fixture = work / f"mode-{mode}"
            fixture.mkdir()
            with socket.socket() as reservation:
                reservation.bind(("127.0.0.1", 0))
                port = reservation.getsockname()[1]
            if port == 3333:
                raise RuntimeError("O teste recusa a porta da aplicação")
            common = [str(MYSQL / "bin/mysqld"), "--no-defaults", f"--basedir={MYSQL}",
                      f"--datadir={fixture / 'data'}", f"--lower-case-table-names={mode}"]
            server = None
            try:
                with (fixture / "initialize.log").open("w") as output:
                    subprocess.run([*common, "--initialize-insecure"], env=ENV,
                                   stdout=output, stderr=subprocess.STDOUT, check=True, timeout=90)
                with (fixture / "server.log").open("w") as output:
                    server = subprocess.Popen([*common, f"--port={port}", "--bind-address=127.0.0.1",
                                               f"--socket={fixture / 'mysql.sock'}",
                                               f"--pid-file={fixture / 'mysql.pid'}", "--mysqlx=0",
                                               "--skip-log-bin", "--innodb-buffer-pool-size=64M"],
                                              env=ENV, stdout=output, stderr=subprocess.STDOUT)
                deadline = time.monotonic() + 60
                while True:
                    if server.poll() is not None:
                        raise RuntimeError("MySQL encerrou antes de aceitar conexões")
                    ping = subprocess.run([str(MYSQL / "bin/mysqladmin"), "--no-defaults",
                                           "--protocol=TCP", "--host=127.0.0.1", f"--port={port}",
                                           "--user=root", "--connect-timeout=1", "ping"],
                                          env=ENV, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                    if ping.returncode == 0:
                        break
                    if time.monotonic() >= deadline:
                        raise TimeoutError("MySQL temporário não ficou pronto")
                    time.sleep(0.2)
                print(f"\nMySQL isolado: lower_case_table_names={mode}", flush=True)
                subprocess.run([str(jdk / "bin/java"), "-Djava.awt.headless=true", "-cp",
                                os.pathsep.join([str(classes), str(ROOT / "lib/*")]),
                                "SchemaContractCheck", str(ROOT), str(fixture), str(port), str(mode),
                                *(["--probe"] if args.probe else [])], check=True, timeout=90)
            except Exception as error:
                failures.append(f"modo {mode}: {error}")
                for name in ("initialize.log", "server.log"):
                    file = fixture / name
                    if file.exists():
                        print(f"{name}:\n{file.read_text()[-6000:]}", flush=True)
            finally:
                if server is not None and server.poll() is None:
                    server.terminate()
                    try:
                        server.wait(timeout=30)
                    except subprocess.TimeoutExpired:
                        server.kill()
                        server.wait()
        if failures:
            raise RuntimeError("; ".join(failures))


if __name__ == "__main__":
    run()
