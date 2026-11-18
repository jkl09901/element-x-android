#!/usr/bin/env bash
set -euo pipefail

# Prepared only; run on the user's own VPS after reviewing DNS, Nginx and ports.
cd "$(dirname "$0")"
DOMAIN="${1:-matrix.tyanet.ru}"
if [[ ! "$DOMAIN" =~ ^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$ ]]; then
  echo 'Ошибка: допустимо только DNS-имя из латинских букв, цифр, точек и дефисов.' >&2
  exit 1
fi

for cmd in docker openssl; do
  command -v "$cmd" >/dev/null || { echo "Не найдена команда $cmd" >&2; exit 1; }
done
docker compose version >/dev/null

if [[ -e .env || -e synapse-data/homeserver.yaml ]]; then
  echo 'Конфигурация уже существует. Не перезаписываем её и ключи шифрования.' >&2
  exit 1
fi

umask 077
mkdir -p synapse-data

echo 'Создаём ключ подписи Matrix (локально на сервере)...'
docker run --rm --user 0:0 \
  -v "$PWD/synapse-data:/data" \
  -e "SYNAPSE_SERVER_NAME=$DOMAIN" \
  -e SYNAPSE_REPORT_STATS=no \
  matrixdotorg/synapse:v1.163.0 generate

DB_PASSWORD="$(openssl rand -hex 32)"
REGISTRATION_SECRET="$(openssl rand -hex 32)"
MACAROON_SECRET="$(openssl rand -hex 32)"
FORM_SECRET="$(openssl rand -hex 32)"
printf 'SYNAPSE_DB_PASSWORD=%s\n' "$DB_PASSWORD" > .env
chmod 600 .env

cat > synapse-data/homeserver.yaml <<EOF
server_name: "$DOMAIN"
public_baseurl: "https://$DOMAIN/"
pid_file: /data/homeserver.pid
report_stats: false
listeners:
  - port: 8008
    type: http
    tls: false
    x_forwarded: true
    bind_addresses: ['0.0.0.0']
    resources:
      - names: [client]
        compress: false
database:
  name: psycopg2
  args:
    user: synapse
    password: "$DB_PASSWORD"
    database: synapse
    host: database
    port: 5432
    cp_min: 1
    cp_max: 5
enable_registration: false
allow_guest_access: false
registration_shared_secret: "$REGISTRATION_SECRET"
macaroon_secret_key: "$MACAROON_SECRET"
form_secret: "$FORM_SECRET"
signing_key_path: "/data/$DOMAIN.signing.key"
media_store_path: /data/media_store
max_upload_size: 100M
url_preview_enabled: false
allow_public_rooms_without_auth: false
allow_public_rooms_over_federation: false
federation_domain_whitelist: []
EOF

chmod 600 synapse-data/homeserver.yaml
chown -R 991:991 synapse-data

echo 'Файлы готовы. Ничего не запущено и действующие сайты не изменены.'
echo "Будущий адрес: https://$DOMAIN/"
echo 'Проверьте README_RU.md, DNS и HTTPS до команды docker compose up -d.'
