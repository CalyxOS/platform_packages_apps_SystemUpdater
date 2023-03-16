package org.calyxos.systemupdater.work.factory

import android.content.Context
import androidx.work.WorkerParameters
import dagger.assisted.AssistedFactory
import org.calyxos.systemupdater.work.workers.AutoUpdateWorker
import org.calyxos.systemupdater.work.workers.UpdatesCheckWorker

@AssistedFactory
interface UpdateAssistedFactory {

    fun autoUpdateWorker(appContext: Context, workerParams: WorkerParameters): AutoUpdateWorker

    fun updatesCheckWorker(appContext: Context, workerParams: WorkerParameters): UpdatesCheckWorker
}
