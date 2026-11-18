/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.featureflag.api

import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.core.meta.BuildType

/**
 * To enable or disable a FeatureFlags, change the `defaultValue` value.
 */
enum class FeatureFlags(
    override val key: String,
    override val title: String,
    override val description: String? = null,
    override val defaultValue: (BuildMeta) -> Boolean,
    override val isFinished: Boolean,
    override val isInLabs: Boolean = false,
) : Feature {
    ShowBlockedUsersDetails(
        key = "feature.showBlockedUsersDetails",
        title = "Данные заблокированных",
        description = "Показывать имя и аватар заблокированных пользователей в списке.",
        defaultValue = { false },
        isFinished = false,
    ),
    SyncOnPush(
        key = "feature.syncOnPush",
        title = "Синхронизация при уведомлении",
        description = "Обновлять переписки при получении push-уведомления.",
        defaultValue = { true },
        isFinished = false,
    ),
    OnlySignedDeviceIsolationMode(
        key = "feature.onlySignedDeviceIsolationMode",
        title = "Только проверенные устройства",
        description = "Передавать ключи сквозного шифрования только подтверждённым устройствам. После включения перезапустите приложение; непроверенные устройства не смогут читать новые сообщения.",
        defaultValue = { false },
        isFinished = false,
    ),
    PrintLogsToLogcat(
        key = "feature.print_logs_to_logcat",
        title = "Запись журнала в систему Android",
        description = "Дополнительно записывать события в системный журнал Android. После изменения перезапустите приложение. Внимание: эти записи могут быть доступны приложениям диагностики и замедлять работу. Не рекомендуется для повседневного использования.",
        defaultValue = { buildMeta -> buildMeta.buildType != BuildType.RELEASE },
        // False so it's displayed in the developer options screen
        isFinished = false,
    ),
    SelectableMediaQuality(
        key = "feature.selectable_media_quality",
        title = "Выбор качества фотографий и видео",
        description = "Разрешить выбирать качество каждого отправляемого медиафайла.",
        defaultValue = { false },
        // False so it's displayed in the developer options screen
        isFinished = false,
    ),
    Threads(
        key = "feature.thread_timeline",
        title = "Ветки обсуждений",
        description = "Показывать ответы в отдельных ветках сообщений. После изменения требуется перезапуск.",
        defaultValue = { false },
        isFinished = false,
        isInLabs = true,
    ),
    MultiAccount(
        key = "feature.multi_account",
        title = "Несколько учётных записей",
        description = "Разрешить вход в несколько аккаунтов одновременно. Экспериментальная и нестабильная функция.",
        defaultValue = { false },
        isFinished = false,
    ),
    QrCodeLogin(
        key = "feature.qr_code_login",
        title = "Вход по QR-коду",
        description = "Разрешить вход на дополнительных устройствах через QR-код.",
        defaultValue = { false },
        isFinished = false,
    ),
    AllowBlackTheme(
        key = "feature.allow_black_theme",
        title = "Чёрная тема",
        description = "Разрешить полностью чёрную тему для экономии заряда на OLED-экранах.",
        defaultValue = { false },
        isFinished = false,
    ),
    ValidateNetworkWhenSchedulingNotificationFetching(
        key = "feature.validate_network_when_scheduling_notification_fetching",
        title = "Проверка интернета для уведомлений",
        description = "Загружать новые уведомления только при наличии интернет-соединения.",
        defaultValue = { true },
        isFinished = false,
    ),
    JumpToUnread(
        key = "feature.jump_to_unread",
        title = "Переход к непрочитанным",
        description = "Добавить кнопку перехода к непрочитанным сообщениям и счётчик возле кнопки прокрутки вниз.",
        defaultValue = { false },
        isFinished = false,
    ),
    SlashCommand(
        key = "feature.slash_command",
        title = "Команды через косую черту",
        description = "Распознавать специальные команды, начинающиеся с символа /, в поле сообщения.",
        defaultValue = { false },
        isFinished = false,
    ),
    AutomaticBackPagination(
        key = "feature.automatic_back_pagination",
        title = "Загрузка истории переписки",
        description = "Автоматически загружать старые сообщения в фоне. После изменения требуется перезапуск.",
        defaultValue = { true },
        isFinished = false,
    ),
    UnreadIndicatorCount(
        key = "feature.unread_indicator_count",
        title = "Счётчик непрочитанных",
        description = "Показывать количество непрочитанных сообщений в списке чатов.",
        defaultValue = { true },
        isFinished = false,
    ),
    SendGalleryMessages(
        key = "feature.send_gallery_messages",
        title = "Несколько медиа в одном сообщении",
        description = "Разрешить отправлять несколько фотографий и видео одним сообщением.",
        defaultValue = { false },
        isFinished = false,
        isInLabs = true,
    ),
    Knock(
        key = "feature.knock",
        title = "Запрос на вступление",
        description = "Разрешить создавать комнаты, в которые можно запросить доступ.",
        defaultValue = { false },
        isFinished = false,
        isInLabs = true,
    ),
    MessageSearch(
        key = "feature.message_search",
        title = "Поиск по сообщениям",
        description = "Индексировать сообщения для локального поиска. Учитываются только сообщения, полученные после включения.",
        defaultValue = { true },
        isFinished = false,
    ),
    NewTimelineEventRenderer(
        key = "feature.new_timeline_event_renderer",
        title = "Новый режим отображения сообщений",
        description = "Отображать форматированный текст средствами Jetpack Compose вместо старого компонента.",
        defaultValue = { false },
        isFinished = false,
        isInLabs = false,
    ),
    NativeCall(
        key = "feature.native_call",
        title = "Новый движок звонков (тестовый)",
        description = "Использовать экспериментальный движок MatrixRTC вместо встроенной веб-версии звонков.",
        defaultValue = { false },
        isFinished = false,
    ),
    ReceiveNotificationsFromOtherSignedInAccounts(
        key = "feature.receive_notifications_from_other_signed_in_accounts",
        title = "Уведомления из других аккаунтов",
        description = "Получать уведомления из других аккаунтов, открытых на телефоне. Возможны дубликаты, использовать только для тестирования.",
        defaultValue = { false },
        isFinished = false,
    )
}

/**
 * Feature flag to show all activity indicators in the room list, using unread messages instead of notifications.
 *
 * This is separate from the [FeatureFlags] enum because it is not a feature flag that can be toggled in developer options,
 * but rather a setting that can be toggled in the app settings.
 */
object ShowAllActivityInRoomListFeature : Feature {
    override val key: String = "feature.show_all_activity_in_room_list"
    override val title: String = "Все события в списке чатов"
    override val description: String = "Показывать индикаторы активности по непрочитанным сообщениям, а не только по уведомлениям."
    override val defaultValue: (BuildMeta) -> Boolean = { true }
    override val isFinished: Boolean = false
    override val isInLabs: Boolean = false
}
