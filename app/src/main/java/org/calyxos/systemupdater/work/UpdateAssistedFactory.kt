package org.calyxos.systemupdater.work

import android.content.Context
import androidx.work.WorkerParameters
import dagger.assisted.AssistedFactory

@AssistedFactory
interface UpdateAssistedFactory {

    fun updateWorker(appContext: Context, workerParams: WorkerParameters): UpdateWorker
}
