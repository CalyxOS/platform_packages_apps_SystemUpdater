package org.calyxos.systemupdater.update.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.update.models.UpdateStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNotificationImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TAG = UpdateNotificationImpl::class.java.simpleName
    private val updatesChannelID = "updates"

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun postUpdateNotification(updateStatus: UpdateStatus) {
        createUpdateNotificationChannel()
        when (updateStatus) {
            UpdateStatus.CHECKING_FOR_UPDATE -> {}
            else -> {
                Log.d(TAG, "No update button action for: ${updateStatus.name}")
            }
        }
    }

    private fun createUpdateNotificationChannel() {
        val name = context.getString(R.string.update_channel_title)
        val descriptionText = context.getString(R.string.update_channel_desc)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(updatesChannelID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }
}
