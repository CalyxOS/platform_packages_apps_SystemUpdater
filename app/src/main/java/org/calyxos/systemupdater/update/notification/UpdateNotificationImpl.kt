package org.calyxos.systemupdater.update.notification

import android.app.NotificationChannel
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
    private val updatesChannelID = "updates"

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun postUpdateNotification(updateStatus: UpdateStatus) {
        createNotificationChannel()
        when (updateStatus) {
            UpdateStatus.UPDATE_AVAILABLE -> {
                val installIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
                postNotification(
                    updateStatus.ordinal,
                    R.string.update_available,
                    R.string.update_available_desc,
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_install_mobile,
                        context.getString(R.string.install),
                        installIntent
                    ).build()
                )
            }
            else -> {
                Log.d(TAG, "No notification for: ${updateStatus.name}")
            }
        }
    }

    private fun postNotification(id: Int, title: Int, desc: Int, act: NotificationCompat.Action) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, updatesChannelID)
            .setSmallIcon(R.drawable.ic_update)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(desc))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(act)
            .build()

        // Cancel existing notifications as this function might be called again
        with(notificationManager) {
            cancelAll()
            notify(id, notification)
        }
    }

    private fun createNotificationChannel() {
        val name = context.getString(R.string.update_channel_title)
        val descriptionText = context.getString(R.string.update_channel_desc)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(updatesChannelID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }
}
