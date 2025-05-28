/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.work

import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.calyxos.systemupdater.service.SystemUpdaterService
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES


class UpdateWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val UPDATE_WORKER = "UPDATE_WORKER"

        fun buildUpdateWork(requiresBatteryNotLow: Boolean): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(requiresBatteryNotLow)
                .build()

            return PeriodicWorkRequestBuilder<UpdateWorker>(
                repeatInterval = 3,
                repeatIntervalTimeUnit = HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = MINUTES
            ).setConstraints(constraints).build()
        }
    }

    override suspend fun doWork(): Result {
        Intent(appContext, SystemUpdaterService::class.java).also {
            it.action = SystemUpdaterService.CHECK_AND_APPLY_UPDATES
            appContext.startService(it)
        }
        return Result.success()
    }
}
