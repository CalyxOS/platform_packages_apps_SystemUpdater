package org.calyxos.systemupdater.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.calyxos.systemupdater.update.config.UpdateConfigRepository
import org.calyxos.systemupdater.update.manager.UpdateManagerRepository

class UpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateConfigRepository: UpdateConfigRepository,
    private val updateManagerRepository: UpdateManagerRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PERIODIC = "periodic"
    }

    private val TAG = UpdateWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        val updateConfig = updateConfigRepository.getLatestUpdateConfig()
        if (updateConfig != null &&
            updateConfigRepository.newUpdateAvailable(updateConfig.buildDateUTC)
        ) {
            Log.i(TAG, "New update available, applying!")
            updateManagerRepository.applyUpdate(updateConfig)
        } else {
            Log.i(TAG, "No new update available!")
        }
        return Result.success()
    }
}
