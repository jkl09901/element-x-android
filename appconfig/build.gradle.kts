import config.BuildTimeConfig
import extension.buildConfigFieldStr

/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.appconfig"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Private messenger: never submit diagnostics, device identifiers,
        // crash logs or screenshots to Element's Rageshake endpoint.
        buildConfigFieldStr("URL_POLICY", BuildTimeConfig.URL_POLICY ?: "")
        buildConfigFieldStr("BUG_REPORT_URL", BuildTimeConfig.BUG_REPORT_URL ?: "")
        buildConfigFieldStr("BUG_REPORT_APP_NAME", BuildTimeConfig.BUG_REPORT_APP_NAME ?: "")

    }
}

dependencies {
    implementation(libs.androidx.annotationjvm)
    implementation(libs.androidx.corektx)
    implementation(projects.libraries.matrix.api)
}
