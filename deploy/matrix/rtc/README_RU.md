# «Только мы»: аудио- и видеозвонки (MatrixRTC / LiveKit)

**Статус: конфигурация готова к установке, но на VPS не применена.** Для двух тестовых аккаунтов (Jony и Natasha). По состоянию на ноябрь 2026 — Synapse 1.163.0, LiveKit 1.13.10, lk-jwt-service 0.9.0. Источники: [официальная инструкция Element Call](https://github.com/element-hq/element-call/blob/main/docs/self_hosting.md), [справочник LiveKit](https://docs.livekit.io/transport/self-hosting/deployment/).

Существующие Docker-контейнеры Synapse/PostgreSQL и Nginx не заменяем. Используем **тот же домен** `matrix.tyanet.ru`, без создания дополнительного DNS и SSL-сертификата.

## Архитектура
- HTTPS `https://matrix.tyanet.ru/livekit/jwt/` → Nginx → `127.0.0.1:18080` → сервис выдачи токенов.
- WebSocket `wss://matrix.tyanet.ru/livekit/sfu/` → Nginx → `127.0.0.1:17880` → LiveKit.
- Медиа-потоки между участниками и LiveKit → публичные `TCP 7881` или `UDP 50000–50100`. Это отдельные сетевые порты: WebSocket на 443 **не заменяет** их.
- MatrixRTC transports endpoint предоставляется существующим Synapse на `https://matrix.tyanet.ru/_matrix/client/unstable/org.matrix.msc4143/rtc/transports`.
- Открытая регистрация на Synapse остаётся отключённой; только локальный Matrix-сервер `matrix.tyanet.ru` может инициировать вызовы на SFU; у LiveKit `room.auto_create: false`.

## Перед установкой
1. Сделать снимок VPS через панель провайдера и резервную копию PostgreSQL и `synapse-data` **за пределами VPS**, включая ключ подписи. Не отправлять никому эти ключи и содержимое `homeserver.yaml`.
2. Проверить свободную RAM и то, что `17880`, `18080`, `7881/TCP` и `50000–50100/UDP` не используются другими службами. Для протокола UDP LISTEN в `ss` может не показываться до запуска.
3. Убедиться, что у FirstVDS и у системного файрвола разрешён входящий `7881/TCP` и `50000–50100/UDP` **только для звонков**. Не отключать полностью файрвол. Учитывать, что Docker может обходить правила UFW для опубликованных портов.
4. Обязательно проверить, что домен `matrix.tyanet.ru` отдаёт `matrix-https-ready` и `/_matrix/client/versions` отвечает 200.

Безопасная проверка, не меняющая сервер:

```bash
free -h; df -h /
ss -lntup | grep -E ':(17880|18080|7881|50000|50100)\\b' || true
ufw status 2>/dev/null || true
```

## 1. Получить файлы на VPS
Выполнять под root, в отдельной папке `/opt/tolkomy-calls`. Брать **только** код из своей ветки GitHub. Не скачивать весь репозиторий Android (он большой). Пример:

```bash
install -d -m 700 /opt/tolkomy-calls
cd /opt/tolkomy-calls
BASE="https://raw.githubusercontent.com/jkl09901/element-x-android/feat/tolkomy-mobile-v1/deploy/matrix/rtc"
for f in compose.yaml setup.sh enable_synapse_rtc.py enable_nginx_rtc.py nginx-rtc-locations.conf.example; do
  curl -fLsS "$BASE/$f" -o "$f" || exit 1
done
bash -n setup.sh
python3 -m py_compile enable_synapse_rtc.py enable_nginx_rtc.py
bash setup.sh
docker compose config --quiet
```

Секрет генерируется **только на VPS** и хранится в каталоге `secrets/`, который не доступен другим обычным пользователям сервера. **Не запускать `setup.sh` повторно.**

## 2. Запустить LiveKit и сервис авторизации

```bash
cd /opt/tolkomy-calls
docker compose up -d
docker compose ps
curl -i --max-time 10 http://127.0.0.1:18080/healthz
curl -i --max-time 10 http://127.0.0.1:17880/
```

Если у одного контейнера состояние unhealthy или он перезапускается, проверить `docker compose logs --tail=50 livekit jwt` (не выводить секреты!). До успешного запуска **не** менять конфигурацию Synapse.

## 3. Подключить существующий HTTPS-сайт

```bash
cd /opt/tolkomy-calls
python3 enable_nginx_rtc.py
curl -i --max-time 10 https://matrix.tyanet.ru/livekit/jwt/healthz
curl -i --max-time 10 https://matrix.tyanet.ru/livekit/sfu/
curl -i --max-time 10 https://matrix.tyanet.ru/.well-known/matrix/client
```

Скрипт создаёт резервную копию **только** файла `/etc/nginx/sites-available/matrix.tyanet.ru.conf`, добавляет `include` только в его HTTPS-блок, проверяет `nginx -t` и выполняет `systemctl reload nginx`. Если проверка не проходит, автоматически возвращает прежнюю конфигурацию. Сайты `dev/test/msg` не трогает.

## 4. Включить поиск звонков в Synapse

```bash
cd /opt/tolkomy-calls
python3 enable_synapse_rtc.py
cd /opt/tolkomy-matrix
docker compose restart synapse
docker compose ps
curl -fsS https://matrix.tyanet.ru/_matrix/client/versions
```

Скрипт изменяет **существующий** `homeserver.yaml` с сохранением владельца файла и резервной копией. В нём добавляются `msc4143_enabled`, `msc4222_enabled`, задержанные события, настройки лимитов и `matrix_rtc.transports`. Подключается ресурс `openid`, который нужен авторизатору. **Скрипт откажется** работать, если эти настройки уже существуют: в таком случае конфигурацию нужно сверять вручную.

Проверка API транспорта требует токена зарегистрированного пользователя. Его нельзя присылать в чат и нельзя вставлять в историю shell. Без токена возможен ответ 401 — он не доказывает, что настройка не сработала.

## 5. Тест с двух телефонов

1. Оба аккаунта должны быть в приложении «Только мы» на `matrix.tyanet.ru`.
2. Jony начинает аудиозвонок Наташе, затем видеозвонок. Проверяем звук, микрофон, камеру, отключение/включение микрофона, переключение камеры, завершение вызова.
3. Повторяем тест, когда один телефон подключён через Wi-Fi, другой — через мобильный интернет.
4. Если висит «Ожидание медиа», проверить `7881/TCP`, `UDP 50000–50100`, ICE-кандидаты LiveKit, затем сетевые ограничения операторов.
5. Вызовы в фоне и входящий звонок при заблокированном экране дополнительно зависят от push-уведомлений. В нашей F-Droid-сборке UnifiedPush пока не настроен — это **следующий отдельный этап**.

**Важное ограничение:** этот шаг — тестовая конфигурация `lk-jwt-service` в режиме standalone, который объявлен устаревающим. Перед масштабированием коммерческого приложения нужно перевести авторизацию в режим Synapse application service, настроить вебхуки и продумать TURN/TLS для сетей, блокирующих UDP.

## Разбор неполадок
- `MISSING_MATRIX_RTC_TRANSPORT`: Synapse не отдаёт `matrix_rtc.transports` либо отключён `msc4143_enabled`.
- `OPEN_ID_ERROR`: проверить ресурс `openid` и возможность доступа сервиса авторизации к `/_matrix/federation/v1/openid/userinfo`.
- `Ждём медиа`: проверить брандмауэр и ICE-порты; WebSocket `443` сам по себе недостаточен.
- `jwt` не стартует: проверить чтение `secrets/livekit_secret` из контейнера и URL.
- Не приходят входящие звонки на заблокированный экран: настроить UnifiedPush/собственный push-провайдер, оптимизацию батареи и разрешения Android.

**Не удалять существующие аккаунты или Synapse-данные ради установки звонков.**
