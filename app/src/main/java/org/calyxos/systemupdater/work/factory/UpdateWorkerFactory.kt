package org.calyxos.systemupdater.work.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject

class UpdateWorkerFactory @Inject constructor(
    private val updateAssistedFactory: UpdateAssistedFactory
) : WorkerFactory() {

    private val autoUpdateWorker = "AutoUpdateWorker"
    private val updatesCheckWorker = "UpdatesCheckWorker"

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            autoUpdateWorker -> updateAssistedFactory.autoUpdateWorker(appContext, workerParameters)
            updatesCheckWorker -> updateAssistedFactory.updatesCheckWorker(
                appContext,
                workerParameters
            )
            else -> null
        }
    }
}
