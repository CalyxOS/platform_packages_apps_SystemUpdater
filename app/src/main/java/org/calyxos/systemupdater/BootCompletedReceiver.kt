package org.calyxos.systemupdater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.systemupdater.work.WorkManagerRepository
import javax.inject.Inject

@AndroidEntryPoint(BroadcastReceiver::class)
class BootCompletedReceiver : Hilt_BootCompletedReceiver() {

    private val TAG = BootCompletedReceiver::class.java.simpleName

    @Inject
    lateinit var workManagerRepository: WorkManagerRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context != null && intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Scheduling automatic system updates!")

            workManagerRepository.scheduleAutoUpdates()
        }
    }
}
