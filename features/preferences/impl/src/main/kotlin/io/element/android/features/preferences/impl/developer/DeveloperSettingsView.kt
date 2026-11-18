/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.developer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.preferences.impl.R
import io.element.android.features.preferences.impl.developer.appsettings.AppDeveloperSettingsView
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.ProgressDialog
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.components.preferences.PreferenceCategory
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.DeviceId
import io.element.android.libraries.ui.strings.CommonStrings
import io.mhssn.colorpicker.ColorPickerDialog
import io.mhssn.colorpicker.ColorPickerType

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DeveloperSettingsView(
    state: DeveloperSettingsState,
    onPushHistoryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    extraOptions: @Composable () -> Unit = {},
) {
    if (state.showLoader) {
        ProgressDialog()
    }
    if (state.markAllRoomsAsReadAction.isConfirming()) {
        ConfirmationDialog(
            title = "Отметить все переписки прочитанными?",
            content = "",
            submitText = stringResource(CommonStrings.action_yes),
            onSubmitClick = { state.eventSink(DeveloperSettingsEvent.MarkAllRoomsAsRead(needsConfirmation = false)) },
            onDismiss = { state.eventSink(DeveloperSettingsEvent.DismissMarkAllRoomsAsReadConfirmation) },
        )
    }
    (state.pushRulesAction as? AsyncAction.Failure)?.let { failure ->
        ErrorDialog(
            content = failure.error.message ?: stringResource(CommonStrings.error_unknown),
            onSubmit = { state.eventSink(DeveloperSettingsEvent.DismissPushRulesError) },
        )
    }
    BackHandler(
        enabled = !state.showLoader,
        onBack = onBackClick,
    )
    PreferencePage(
        modifier = modifier,
        onBackClick = {
            if (!state.showLoader) {
                onBackClick()
            }
        },
        title = stringResource(id = CommonStrings.common_developer_options)
    ) {
        // Note: this is OK to hardcode strings in this debug screen.
        AppDeveloperSettingsView(
            state = state.appDeveloperSettingsState,
        )
        SessionCategory(
            deviceId = state.deviceId,
            onCopyToClipboard = { state.eventSink(DeveloperSettingsEvent.CopyToClipboard(it)) },
        )
        NotificationCategory(
            onPushRulesClick = { state.eventSink(DeveloperSettingsEvent.OpenPushRules) },
            onPushHistoryClick = onPushHistoryClick,
        )
        MarkAllRoomsAsReadCategory(state)

        if (state.isEnterpriseBuild) {
            PreferenceCategory(title = "Оформление") {
                ListItem(
                    content = {
                        Text("Изменить фирменный цвет")
                    },
                    onClick = {
                        state.eventSink(DeveloperSettingsEvent.SetShowColorPicker(true))
                    }
                )
                ListItem(
                    content = {
                        Text("Сбросить фирменный цвет")
                    },
                    onClick = {
                        state.eventSink(DeveloperSettingsEvent.ChangeBrandColor(null))
                    }
                )
            }
        }

        extraOptions()

        val cache = state.cacheSize
        PreferenceCategory(title = "Данные и кэш") {
            ListItem(
                content = { Text("Размеры баз данных") },
                supportingContent = {
                    if (state.databaseSizes.isLoading()) {
                        Text("Подсчёт...")
                    } else {
                        val dbSizes = state.databaseSizes.dataOrNull()
                        if (dbSizes != null && dbSizes.isNotEmpty()) {
                            Column {
                                for ((dbName, size) in dbSizes) {
                                    Text("$dbName: $size")
                                }
                            }
                        } else {
                            Text("Неизвестно")
                        }
                    }
                }
            )
            ListItem(
                content = {
                    Text("Оптимизировать хранилища")
                },
                onClick = {
                    state.eventSink(DeveloperSettingsEvent.VacuumStores)
                }
            )
            ListItem(
                content = {
                    Text("Очистить кэш")
                },
                trailingContent = if (state.cacheSize.isLoading() || state.clearCacheAction.isLoading()) {
                    ListItemContent.Custom {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    ListItemContent.Text(cache.dataOrNull().orEmpty())
                },
                onClick = {
                    if (state.clearCacheAction.isLoading().not()) {
                        state.eventSink(DeveloperSettingsEvent.ClearCache)
                    }
                }
            )
        }
    }
    ColorPickerDialog(
        show = state.showColorPicker,
        type = ColorPickerType.Classic(
            showAlphaBar = false,
        ),
        onDismissRequest = {
            state.eventSink(DeveloperSettingsEvent.SetShowColorPicker(false))
        },
        onPickedColor = {
            state.eventSink(DeveloperSettingsEvent.ChangeBrandColor(it))
        },
    )
}

@Composable
private fun SessionCategory(
    deviceId: DeviceId,
    onCopyToClipboard: (String) -> Unit,
) {
    PreferenceCategory(title = "Текущий сеанс") {
        ListItem(
            content = { Text("Идентификатор устройства") },
            supportingContent = { Text(text = deviceId.value) },
            onClick = { onCopyToClipboard(deviceId.value) }
        )
    }
}

@Composable
private fun MarkAllRoomsAsReadCategory(state: DeveloperSettingsState) {
    PreferenceCategory(title = "Список переписок") {
        ListItem(
            content = {
                Text("Отметить все переписки прочитанными")
            },
            supportingContent = {
                Text(
                    text = """
                        Отправит подтверждение прочтения во все ваши переписки.
                        Операция выполняется в фоне и может занять время.
                        Для завершения не закрывайте приложение.
                        """.trimIndent(),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            },
            enabled = !state.showLoader,
            onClick = {
                state.eventSink(DeveloperSettingsEvent.MarkAllRoomsAsRead(needsConfirmation = true))
            },
        )
    }
}

@Composable
private fun NotificationCategory(
    onPushRulesClick: () -> Unit,
    onPushHistoryClick: () -> Unit,
) {
    PreferenceCategory(title = stringResource(id = R.string.screen_notification_settings_title)) {
        ListItem(
            content = {
                Text("Правила уведомлений")
            },
            onClick = onPushRulesClick,
        )
        ListItem(
            content = {
                Text(stringResource(R.string.troubleshoot_notifications_entry_point_push_history_title))
            },
            onClick = onPushHistoryClick,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun DeveloperSettingsViewPreview(
    @PreviewParameter(DeveloperSettingsStatePreviewParam::class) state: DeveloperSettingsState
) = ElementPreview {
    DeveloperSettingsView(
        state = state,
        onPushHistoryClick = {},
        onBackClick = {},
    )
}
