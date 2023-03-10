package org.calyxos.systemupdater.update.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.ui.MainActivity
import org.calyxos.systemupdater.update.models.UpdateStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNotificationImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TAG = UpdateNotificationImpl::class.java.simpleName

    private lateinit var updateStatus: UpdateStatus

    private val updatesGroupID = "updates"
    private val lowPriorityUpdatesChannelID = "high-updates"
    private val highPriorityUpdatesChannelID = "low-updates"

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    // TODO: Add notification actions
    fun postNotification(updateStatus: UpdateStatus) {
        this.updateStatus = updateStatus

        // Create required update notification channels
        createNotificationChannel()

        when (updateStatus) {
            UpdateStatus.CHECKING_FOR_UPDATE -> {
                postNotification(
                    id = updateStatus.ordinal,
                    title = R.string.checking_updates,
                    progress = 0
                )
            }
            UpdateStatus.UPDATE_AVAILABLE -> {
                postNotification(
                    id = updateStatus.ordinal,
                    title = R.string.update_available,
                    desc = R.string.update_available_desc
                )
            }
            else -> {
                Log.d(TAG, "No notification for: ${updateStatus.name}")
            }
        }
    }

    fun removeNotification(updateStatus: UpdateStatus) {
        notificationManager.cancel(updateStatus.ordinal)
    }

    private fun postNotification(id: Int, title: Int, desc: Int = -1, progress: Int = -1) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Channel based on priority
        val channelID = when (updateStatus) {
            UpdateStatus.UPDATED_NEED_REBOOT,
            UpdateStatus.REPORTING_ERROR_EVENT,
            UpdateStatus.FAILED_PREPARING_UPDATE,
            UpdateStatus.UPDATE_AVAILABLE -> highPriorityUpdatesChannelID
            else -> lowPriorityUpdatesChannelID
        }

        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_update)
            .setContentTitle(context.getString(title))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        // Description can be optional, for e.g. in case of ongoing notification
        if (desc != -1) {
            notification.setContentText(context.getString(desc))
        }

        // If progress is 0, switch to indeterminate
        if (progress != -1) {
            notification
                .setOngoing(true)
                .setProgress(100, progress, progress == 0)
        }

        // Cancel existing notifications as this function might be called again
        with(notificationManager) {
            cancelAll()
            notify(id, notification.build())
        }
    }

    private fun createNotificationChannel() {
        val updateGroup = NotificationChannelGroup(
            updatesGroupID,
            context.getString(R.string.update_group_title)
        )

        val lowPriorityNotificationChannel = NotificationChannel(
            lowPriorityUpdatesChannelID,
            context.getString(R.string.low_priority_update_channel_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.low_priority_update_channel_desc)
            group = updatesGroupID
        }

        val highPriorityNotificationChannel = NotificationChannel(
            highPriorityUpdatesChannelID,
            context.getString(R.string.high_priority_update_channel_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.high_priority_update_channel_desc)
            group = updatesGroupID
        }

        // Create notification group and related channels
        with(notificationManager) {
            createNotificationChannelGroup(updateGroup)
            createNotificationChannels(
                listOf(
                    lowPriorityNotificationChannel,
                    highPriorityNotificationChannel
                )
            )
        }
    }
}
