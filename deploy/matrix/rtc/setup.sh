#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
umask 077

if [[ -e livekit.yaml || -e secrets/livekit_secret ]]; then
  echo "Конфигурация звонков уже существует; секреты не заменяем." >&2
  exit 1
fi
command -v docker >/dev/null || { echo "Не установлен Docker" >&2; exit 1; }
command -v openssl >/dev/null || { echo "Не установлен OpenSSL" >&2; exit 1; }
docker compose version >/dev/null

mkdir -m 700 -p secrets
# A strong key unique to this VPS, never stored in GitHub.
openssl rand -hex 32 > secrets/livekit_secret
SECRET="$(cat secrets/livekit_secret)"
cat > livekit.yaml <<EOF
port: 7880
log_level: info
rtc:
  tcp_port: 7881
  port_range_start: 50000
  port_range_end: 50100
  use_external_ip: true
keys:
  lk-matrixrtc: "$SECRET"
room:
  auto_create: false
EOF

# The directory containing the key is mode 0700 on the host. Mounted
# files are readable by non-root users inside the isolated containers.
chmod 700 secrets
chmod 644 secrets/livekit_secret livekit.yaml
docker compose config --quiet
echo "Конфигурация звонков готова; контейнеры ещё не запущены."
echo "Адрес: wss://matrix.tyanet.ru/livekit/sfu"
echo "Следующий шаг: проверить порты, затем docker compose up -d."
