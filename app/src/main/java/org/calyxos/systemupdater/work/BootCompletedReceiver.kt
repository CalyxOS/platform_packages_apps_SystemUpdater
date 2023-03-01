package org.calyxos.systemupdater.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(BroadcastReceiver::class)
class BootCompletedReceiver : Hilt_BootCompletedReceiver() {

    private val TAG = BootCompletedReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context != null && intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Scheduling automatic system updates!")
        }
    }
}
