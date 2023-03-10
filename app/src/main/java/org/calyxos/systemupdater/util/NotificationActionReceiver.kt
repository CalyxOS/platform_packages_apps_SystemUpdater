package org.calyxos.systemupdater.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class NotificationActionReceiver : BroadcastReceiver() {

    private val TAG = NotificationActionReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            when (intent?.action) {
                NotificationAction.REBOOT.name -> {
                    val pm = context.getSystemService(PowerManager::class.java)
                    pm.reboot(null)
                }
                else -> Log.i(TAG, "Got Unhandled Intent: ${intent?.action}")
            }
        }
    }
}
