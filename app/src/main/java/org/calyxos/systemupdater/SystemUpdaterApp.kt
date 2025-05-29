/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp(Application::class)
class SystemUpdaterApp : Hilt_SystemUpdaterApp() {
    // WorkManager crashes when it knows you are trying to use it in Direct Boot; however,
    // it should work. Below is a workaround. See https://issuetracker.google.com/issues/112773820
    override fun isDeviceProtectedStorage() = false
}
