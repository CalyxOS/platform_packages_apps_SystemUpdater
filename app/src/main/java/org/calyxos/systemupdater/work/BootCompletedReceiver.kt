package org.calyxos.systemupdater.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy.EXPONENTIAL
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.NetworkType.UNMETERED
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.systemupdater.work.UpdateWorker.Companion.WORK_NAME
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
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

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
            val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(3, HOURS, 30, MINUTES)
                .setBackoffCriteria(EXPONENTIAL, DEFAULT_BACKOFF_DELAY_MILLIS, MILLISECONDS)
                .setInitialDelay(10, MINUTES)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, KEEP, workRequest)
        }
    }
}
