/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.systemupdater.work.UpdateWorker.Companion.UPDATE_WORKER
import org.calyxos.systemupdater.work.UpdateWorker.Companion.buildUpdateWork
import javax.inject.Inject

@AndroidEntryPoint(BroadcastReceiver::class)
class BootCompletedReceiver : Hilt_BootCompletedReceiver() {

    private val TAG = BootCompletedReceiver::class.java.simpleName

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context != null && intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Scheduling automatic system updates!")

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UPDATE_WORKER,
                    KEEP,
                    buildUpdateWork(requiresBatteryNotLow = true)
                )
        }
    }
}
