/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.tracing

enum class TraceLogPack(val key: String) {
    EVENT_CACHE("event_cache") {
        override val title: String = "Кэш событий"
    },
    SEND_QUEUE("send_queue") {
        override val title: String = "Очередь отправки"
    },
    TIMELINE("timeline") {
        override val title: String = "История сообщений"
    },
    NOTIFICATION_CLIENT("notification_client") {
        override val title: String = "Получение уведомлений"
    },
    SYNC_PROFILING("sync_profiling") {
        override val title: String = "Диагностика синхронизации"
    },
    LATEST_EVENTS("latest_events") {
        override val title = "Последние события"
    };

    abstract val title: String
}
