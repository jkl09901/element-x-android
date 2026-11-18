#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
umask 077

if [[ -e livekit.yaml || -e secrets/livekit_secret || -e /opt/tolkomy-matrix/synapse-data/rtc-appservice.yaml ]]; then
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
webhook:
  api_key: lk-matrixrtc
  urls:
    - https://matrix.tyanet.ru/livekit/jwt/sfu_webhook
EOF

# The directory containing the key is mode 0700 on the host. Mounted
# files are readable by non-root users inside the isolated containers.
chmod 700 secrets
chmod 644 secrets/livekit_secret livekit.yaml

# A single application service registration is mounted into both containers.
# It gives the authorization service permission to verify room memberships.
AS_TOKEN="$(openssl rand -hex 32)"
HS_TOKEN="$(openssl rand -hex 32)"
AS_FILE=/opt/tolkomy-matrix/synapse-data/rtc-appservice.yaml
test -d /opt/tolkomy-matrix/synapse-data || {
  echo "Synapse media/config folder not found" >&2
  exit 1
}
cat > "$AS_FILE" <<EOF
id: "tolkomy-livekit-jwt"
as_token: "$AS_TOKEN"
hs_token: "$HS_TOKEN"
sender_localpart: "_tolkomy_lk_jwt_service"
namespaces:
  users:
    - exclusive: false
      regex: ".*"
url: null
io.element.msc4502.scopes:
  - "urn:matrix:client:io.element.msc4502:rooms:is_joined"
io.element.msc4512.proxy_prefix: "rtc/livekit"
io.element.msc4512.proxy_url: "https://matrix.tyanet.ru/livekit/jwt"
EOF
chown 991:991 "$AS_FILE"
chmod 644 "$AS_FILE"
docker compose config --quiet
echo "Конфигурация звонков готова; контейнеры ещё не запущены."
echo "Адрес: wss://matrix.tyanet.ru/livekit/sfu"
echo "Следующий шаг: проверить порты, затем docker compose up -d."
