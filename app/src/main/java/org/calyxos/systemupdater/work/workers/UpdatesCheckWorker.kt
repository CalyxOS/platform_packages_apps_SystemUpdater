package org.calyxos.systemupdater.work.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.calyxos.systemupdater.notification.NotificationRepository
import org.calyxos.systemupdater.update.manager.UpdateManagerRepository
import org.calyxos.systemupdater.update.models.UpdateStatus

class UpdatesCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateManagerRepository: UpdateManagerRepository,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "periodic"
    }

    override suspend fun doWork(): Result {
        notificationRepository.postNotification(UpdateStatus.UPDATE_AVAILABLE)
        if (updateManagerRepository.checkUpdates()) {
            notificationRepository.postNotification(UpdateStatus.UPDATE_AVAILABLE)
        } else {
            notificationRepository.removeNotification(UpdateStatus.UPDATE_AVAILABLE)
        }
        return Result.success()
    }
}
