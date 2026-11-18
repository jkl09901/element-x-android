#!/usr/bin/env python3
"""Attach MatrixRTC locations ONLY to matrix.tyanet.ru's existing HTTPS vhost."""
from pathlib import Path
from datetime import datetime, timezone
import shutil
import subprocess

vhost = Path("/etc/nginx/sites-available/matrix.tyanet.ru.conf")
snippet = Path("/etc/nginx/snippets/tolkomy-rtc.conf")
example = Path(__file__).resolve().parent / "nginx-rtc-locations.conf.example"

old = vhost.read_text(encoding="utf-8")
needle = "    listen 443 ssl;\n    server_name matrix.tyanet.ru;"
include = "    include /etc/nginx/snippets/tolkomy-rtc.conf;"
if include in old or snippet.exists():
    raise SystemExit("Остановлено: MatrixRTC уже подключён. Ничего не перезаписано.")
if old.count(needle) != 1:
    raise SystemExit("Остановлено: HTTPS-блок отличается от ожидаемого.")
if not example.is_file():
    raise SystemExit("Не найден файл с настройками RTC.")

backup = vhost.with_name(vhost.name + ".pre-rtc-" + datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S") + ".bak")
shutil.copy2(vhost, backup)
snippet.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(example, snippet)
new = old.replace(needle, needle + "\n\n" + include, 1)
try:
    vhost.write_text(new, encoding="utf-8")
    subprocess.run(["nginx", "-t"], check=True)
    subprocess.run(["systemctl", "reload", "nginx"], check=True)
except BaseException:
    shutil.copy2(backup, vhost)
    snippet.unlink(missing_ok=True)
    subprocess.run(["nginx", "-t"], check=False)
    subprocess.run(["systemctl", "reload", "nginx"], check=False)
    raise

print("MatrixRTC подключён к HTTPS-сайту matrix.tyanet.ru.")
print("Резервная копия: ", backup)
