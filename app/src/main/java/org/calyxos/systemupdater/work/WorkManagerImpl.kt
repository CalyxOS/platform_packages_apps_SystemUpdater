package org.calyxos.systemupdater.work

import androidx.work.BackoffPolicy.EXPONENTIAL
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType.CONNECTED
import androidx.work.NetworkType.UNMETERED
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Inject
import javax.inject.Singleton
import org.calyxos.systemupdater.work.workers.AutoUpdateWorker
import org.calyxos.systemupdater.work.workers.UpdatesCheckWorker

@Singleton
class WorkManagerImpl @Inject constructor(
    private val workManager: WorkManager
) {

    fun expeditedUpdatesCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<UpdatesCheckWorker>()
            .setBackoffCriteria(EXPONENTIAL, DEFAULT_BACKOFF_DELAY_MILLIS, MILLISECONDS)
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            UpdatesCheckWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleAutoUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()
        val workRequest = PeriodicWorkRequestBuilder<AutoUpdateWorker>(3, HOURS, 30, MINUTES)
            .setBackoffCriteria(EXPONENTIAL, DEFAULT_BACKOFF_DELAY_MILLIS, MILLISECONDS)
            .setInitialDelay(10, MINUTES)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            AutoUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
