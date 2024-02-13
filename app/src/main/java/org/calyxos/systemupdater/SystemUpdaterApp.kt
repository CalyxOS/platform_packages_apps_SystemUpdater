/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.calyxos.systemupdater.work.factory.UpdateWorkerFactory
import javax.inject.Inject

@HiltAndroidApp(Application::class)
class SystemUpdaterApp : Hilt_SystemUpdaterApp(), Configuration.Provider {

    @Inject
    lateinit var updateWorkerFactory: UpdateWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(updateWorkerFactory)
            .build()
}
