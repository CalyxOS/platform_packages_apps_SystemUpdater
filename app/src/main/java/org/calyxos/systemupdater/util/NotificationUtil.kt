/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.util

import android.app.Notification
import android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.graphics.drawable.IconCompat
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.receiver.RebootReceiver
import org.calyxos.systemupdater.ui.MainActivity
import org.calyxos.systemupdater.update.models.UpdateStatus

object NotificationUtil {

    private const val CHANNEL_ID_UPDATES_PRIORITY_LOW = "CHANNEL_ID_UPDATES_PRIORITY_LOW"
    private const val CHANNEL_ID_UPDATES_PRIORITY_HIGH = "CHANNEL_ID_UPDATES_PRIORITY_HIGH"

    fun getNotificationChannels(context: Context): List<NotificationChannel> {
        val lowPriorityNotificationChannel = NotificationChannel(
            CHANNEL_ID_UPDATES_PRIORITY_LOW,
            context.getString(R.string.default_priority_update_channel_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.default_priority_update_channel_desc)
        }

        val highPriorityNotificationChannel = NotificationChannel(
            CHANNEL_ID_UPDATES_PRIORITY_HIGH,
            context.getString(R.string.high_priority_update_channel_title),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.high_priority_update_channel_desc)
        }

        return listOf(lowPriorityNotificationChannel, highPriorityNotificationChannel)
    }

    fun getNotification(
        context: Context,
        status: UpdateStatus,
        title: String,
        desc: String? = null,
        progress: Int? = null
    ): Notification {
        val channelID = when (status) {
            UpdateStatus.UPDATED_NEED_REBOOT,
            UpdateStatus.REPORTING_ERROR_EVENT,
            UpdateStatus.FAILED_PREPARING_UPDATE,
            UpdateStatus.UPDATE_AVAILABLE,
                -> CHANNEL_ID_UPDATES_PRIORITY_HIGH

            else -> CHANNEL_ID_UPDATES_PRIORITY_LOW
        }

        val contentIntent = PendingIntentCompat.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            0,
            false
        )

        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_update)
            .setContentTitle(title)
            .setContentText(desc)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)

        if (progress != null) {
            notification
                .setOngoing(true)
                .setProgress(100, progress, progress == 0)
        } else {
            notification.setAutoCancel(true)
        }

        // Add action button for reboot notification
        if (status == UpdateStatus.UPDATED_NEED_REBOOT) {
            val action = NotificationCompat.Action.Builder(
                IconCompat.createWithResource(context, R.drawable.ic_restart),
                context.getString(R.string.reboot),
                PendingIntentCompat.getBroadcast(
                    context,
                    0,
                    Intent(context, RebootReceiver::class.java),
                    0,
                    false
                )
            ).build()
            notification.addAction(action)
        }

        return notification.build()
    }
}
