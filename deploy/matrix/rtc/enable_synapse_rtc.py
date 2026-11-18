#!/usr/bin/env python3
"""Enable the MatrixRTC transport for our EXISTING Synapse without replacing secrets.

Run only after LiveKit + lk-jwt-service are healthy.
This script is intentionally specific to the already-deployed TolkoMy Synapse config.
"""
from pathlib import Path
from datetime import datetime, timezone
import os
import re
import shutil
import stat
import tempfile

config = Path("/opt/tolkomy-matrix/synapse-data/homeserver.yaml")
original = config.read_text(encoding="utf-8")
if not re.search(r"^server_name:\s*['\"]?matrix\.tyanet\.ru['\"]?\s*$", original, re.M):
    raise SystemExit("Остановлено: не найден ожидаемый сервер matrix.tyanet.ru")
for key in ("experimental_features", "matrix_rtc", "max_event_delay_duration", "rc_message", "rc_delayed_event_mgmt", "app_service_config_files"):
    if re.search(rf"^{key}:", original, re.M):
        raise SystemExit(f"Остановлено: раздел {key} уже присутствует; ручная проверка обязательна.")

if original.count("names: [client]") != 1:
    raise SystemExit("Остановлено: список ресурсов отличается от ожидаемого.")

registration = Path("/opt/tolkomy-matrix/synapse-data/rtc-appservice.yaml")
if not registration.is_file():
    raise SystemExit("Остановлено: сначала создайте секреты и регистрацию RTC командой setup.sh")

new_config = original.replace("names: [client]", "names: [client, openid]", 1).rstrip() + """

# MatrixRTC: calls between TolkoMy clients.
experimental_features:
  msc4143_enabled: true
  msc4222_enabled: true
  msc4502_enabled: true
  msc4512_enabled: true

max_event_delay_duration: 24h

rc_message:
  per_second: 0.5
  burst_count: 30

rc_delayed_event_mgmt:
  per_second: 1
  burst_count: 20

matrix_rtc:
  transports:
    - type: livekit
      url: wss://matrix.tyanet.ru/livekit/sfu
      livekit_service_url: https://matrix.tyanet.ru/livekit/jwt

app_service_config_files:
  - /data/rtc-appservice.yaml
"""

backup = config.with_name(config.name + ".pre-rtc-" + datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S") + ".bak")
shutil.copy2(config, backup)
os.chmod(backup, 0o600)
st = config.stat()
fd, filename = tempfile.mkstemp(prefix=".homeserver-rtc-", dir=config.parent)
try:
    os.fchmod(fd, stat.S_IMODE(st.st_mode))
    os.fchown(fd, st.st_uid, st.st_gid)
    with os.fdopen(fd, "w", encoding="utf-8") as file:
        file.write(new_config)
    os.replace(filename, config)
except BaseException:
    if os.path.exists(filename):
        os.unlink(filename)
    raise

print("Настройки MatrixRTC добавлены.")
print("Резервная копия: ", backup)
print("Перезапустите только Synapse: cd /opt/tolkomy-matrix && docker compose restart synapse")
