package org.calyxos.systemupdater.work

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.calyxos.systemupdater.update.manager.UpdateManagerRepository
import org.calyxos.systemupdater.util.CommonModule
import java.util.Calendar

class UpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateManagerRepository: UpdateManagerRepository,
    private val sharedPreferences: SharedPreferences
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "UpdateWork"
    }

    override suspend fun doWork(): Result {
        if (updateManagerRepository.checkUpdates()) updateManagerRepository.applyUpdate()

        // Bump update's last check time
        sharedPreferences.edit { putLong(CommonModule.lastCheck, Calendar.getInstance().time.time) }
        return Result.success()
    }
}
