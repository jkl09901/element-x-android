/*
 * Russian-only UI for the independent "Только мы" fork.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.x

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

internal object RussianLocale {
    private val russian: Locale = Locale.forLanguageTag("ru")

    fun wrap(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(russian)
        configuration.setLayoutDirection(russian)
        return context.createConfigurationContext(configuration)
    }
}
