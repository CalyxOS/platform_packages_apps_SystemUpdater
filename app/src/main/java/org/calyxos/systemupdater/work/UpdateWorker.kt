package org.calyxos.systemupdater.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PERIODIC = "periodic"
    }

    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }
}
