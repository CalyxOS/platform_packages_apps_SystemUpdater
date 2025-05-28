/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.work

import android.content.Context
import android.os.PowerManager
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Worker to automatically reboot the system once it has finished updating
 * @see UpdateWorker
 */
class RebootWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val REBOOT_WORKER = "REBOOT_WORKER"

        fun scheduleAutomaticReboot(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<RebootWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    REBOOT_WORKER,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
        }
    }

    private val powerManager: PowerManager
        get() = context.getSystemService<PowerManager>()!!

    override suspend fun doWork(): Result {
        powerManager.reboot(null)
        return Result.success()
    }
}
