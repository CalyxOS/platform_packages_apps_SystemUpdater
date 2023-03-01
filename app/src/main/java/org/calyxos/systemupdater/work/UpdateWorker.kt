package org.calyxos.systemupdater.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.calyxos.systemupdater.update.manager.UpdateManagerRepository

class UpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateManagerRepository: UpdateManagerRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PERIODIC = "periodic"
    }

    override suspend fun doWork(): Result {
        if (updateManagerRepository.checkUpdates()) updateManagerRepository.applyUpdate()
        return Result.success()
    }
}
