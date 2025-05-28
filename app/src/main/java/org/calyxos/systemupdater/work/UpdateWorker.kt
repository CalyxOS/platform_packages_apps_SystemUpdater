/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.work

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.calyxos.systemupdater.service.SystemUpdaterService
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

/**
 * Worker to trigger a foreground service to check and update the system
 * @see SystemUpdaterService
 */
class UpdateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "UpdateWorker"
        private const val EXPEDITED_UPDATE_WORKER = "EXPEDITED_UPDATE_WORKER"
        private const val PERIODIC_UPDATE_WORKER = "UPDATE_WORKER"

        fun triggerUpdate(context: Context) {
            Log.i(TAG, "Checking system updates!")
            val work = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(EXPEDITED_UPDATE_WORKER, ExistingWorkPolicy.KEEP, work)
        }

        fun scheduleAutomatedUpdates(context: Context) {
            Log.i(TAG, "Scheduling automatic system updates!")
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_UPDATE_WORKER,
                    ExistingPeriodicWorkPolicy.KEEP,
                    buildUpdateWork()
                )
        }

        fun updateAutomatedCheck(
            context: Context,
            requiresBatteryNotLow: Boolean = true
        ) {
            Log.i(TAG, "Updating periodic app updates!")
            WorkManager.getInstance(context)
                .updateWork(
                    buildUpdateWork(requiresBatteryNotLow)
                )
        }

        private fun buildUpdateWork(
            requiresBatteryNotLow: Boolean = true
        ): PeriodicWorkRequest {
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
        Intent(context, SystemUpdaterService::class.java).also {
            it.action = SystemUpdaterService.CHECK_AND_APPLY_UPDATES
            context.startService(it)
        }
        return Result.success()
    }
}
