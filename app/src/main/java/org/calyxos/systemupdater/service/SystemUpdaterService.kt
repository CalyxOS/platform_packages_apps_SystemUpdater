/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.service

import android.app.Notification
import android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.ui.MainActivity
import org.calyxos.systemupdater.update.manager.UpdateManagerRepository
import org.calyxos.systemupdater.update.models.UpdateStatus
import org.calyxos.systemupdater.util.NotificationAction
import org.calyxos.systemupdater.util.NotificationActionReceiver
import javax.inject.Inject

@AndroidEntryPoint(LifecycleService::class)
class SystemUpdaterService : Hilt_SystemUpdaterService() {

    companion object {
        const val CHECK_UPDATES = "CheckUpdates"
        const val APPLY_UPDATE = "ApplyUpdate"
        const val CHECK_AND_APPLY_UPDATES = "${CHECK_UPDATES}And$APPLY_UPDATE"

        var IS_SERVICE_RUNNING = false
    }

    private val TAG = SystemUpdaterService::class.java.simpleName

    // Notification
    private val serviceID = 1
    private val updatesGroupID = "updates"
    private val lowPriorityUpdatesChannelID = "lowPriorityUpdates"
    private val highPriorityUpdatesChannelID = "highPriorityUpdates"

    private val connectivityManager: ConnectivityManager
        get() = this.getSystemService<ConnectivityManager>()!!

    private val notificationManager: NotificationManager
        get() = this.getSystemService<NotificationManager>()!!

    // Coroutine
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    // Network status
    private enum class NetworkStatus {
        METERED,
        NOT_METERED
    }

    private val networkStatus: StateFlow<NetworkStatus?>
        get() = callbackFlow {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val isNotMetered = networkCapabilities.hasCapability(NET_CAPABILITY_NOT_METERED)
                    trySend(if (isNotMetered) NetworkStatus.NOT_METERED else NetworkStatus.METERED)
                }
            }

            connectivityManager.registerDefaultNetworkCallback(networkCallback)
            awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
        }.stateIn(serviceScope, SharingStarted.WhileSubscribed(), null)

    @Inject
    lateinit var updateManager: UpdateManagerRepository

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            CHECK_UPDATES -> checkUpdates()
            APPLY_UPDATE -> applyUpdate()
            CHECK_AND_APPLY_UPDATES -> checkAndApplyUpdate()
            else -> Log.d(TAG, "Got Unknown Intent!")
        }

        combine(
            updateManager.updateStatus,
            updateManager.updateProgress,
            networkStatus.filterNotNull()
        ) { updateStatus, progress, networkStatus ->
            when (updateStatus) {
                UpdateStatus.UPDATE_AVAILABLE -> {
                    val notification = getNotification(
                        updateStatus = updateStatus,
                        title = R.string.update_available,
                        desc = R.string.update_available_desc
                    )
                    notificationManager.notify(updateStatus.ordinal, notification)
                }
                UpdateStatus.SUSPENDED -> {
                    when (networkStatus) {
                        NetworkStatus.METERED -> notificationManager.cancel(serviceID)
                        NetworkStatus.NOT_METERED -> updateManager.resumeUpdate()
                    }
                }
                UpdateStatus.PREPARING_TO_UPDATE,
                UpdateStatus.DOWNLOADING,
                UpdateStatus.VERIFYING,
                UpdateStatus.FINALIZING -> {
                    when (networkStatus) {
                        NetworkStatus.METERED -> updateManager.suspendUpdate()
                        NetworkStatus.NOT_METERED -> {
                            val notification = getNotification(
                                updateStatus = updateStatus,
                                title = R.string.installing_update,
                                progress = progress
                            )
                            notificationManager.notify(serviceID, notification)
                        }
                    }
                }
                UpdateStatus.FAILED_PREPARING_UPDATE,
                UpdateStatus.REPORTING_ERROR_EVENT -> {
                    val notification = getNotification(
                        updateStatus = updateStatus,
                        title = R.string.updated_failed,
                        desc = R.string.updated_failed_desc
                    )
                    notificationManager.notify(updateStatus.ordinal, notification)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                UpdateStatus.UPDATED_NEED_REBOOT -> {
                    val actionIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                        putExtra(NotificationAction.REBOOT.name, NotificationAction.REBOOT.name)
                    }
                    val notification = getNotification(
                        updateStatus = updateStatus,
                        title = R.string.update_done,
                        desc = R.string.update_done_desc,
                        action = NotificationCompat.Action.Builder(
                            IconCompat.createWithResource(this, R.drawable.ic_restart),
                            this.getString(R.string.reboot),
                            PendingIntent.getBroadcast(
                                this,
                                0,
                                actionIntent,
                                PendingIntent.FLAG_IMMUTABLE
                            )
                        ).build()
                    )
                    notificationManager.notify(updateStatus.ordinal, notification)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                else -> null
            }
        }.launchIn(serviceScope)

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        job.cancel()
        IS_SERVICE_RUNNING = false
        super.onDestroy()
    }

    private fun checkUpdates() {
        startForeground(
            serviceID,
            getNotification(
                updateStatus = UpdateStatus.CHECKING_FOR_UPDATE,
                title = R.string.checking_updates,
                progress = 0
            )
        )

        serviceScope.launch {
            updateManager.checkUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun applyUpdate() {
        startForeground(
            serviceID,
            getNotification(
                updateStatus = UpdateStatus.PREPARING_TO_UPDATE,
                title = R.string.installing_update,
                progress = 0
            )
        )

        serviceScope.launch { updateManager.applyUpdate() }
    }

    private fun checkAndApplyUpdate() {
        startForeground(
            serviceID,
            getNotification(
                updateStatus = UpdateStatus.CHECKING_FOR_UPDATE,
                title = R.string.checking_updates,
                progress = 0
            )
        )

        serviceScope.launch {
            if (updateManager.checkUpdates()) {
                updateManager.applyUpdate()
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
    }

    private fun getNotification(
        updateStatus: UpdateStatus,
        title: Int,
        desc: Int? = null,
        progress: Int? = null,
        action: NotificationCompat.Action? = null
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
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

        val notification = NotificationCompat.Builder(this, channelID)
            .setSmallIcon(R.drawable.ic_update)
            .setContentTitle(this.getString(title))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add action button
        if (action != null) {
            notification.addAction(action)
        }

        // Description can be optional, for e.g. in case of ongoing notification
        if (desc != null) {
            notification.setContentText(this.getString(desc))
        } else {
            if (updateStatus != UpdateStatus.CHECKING_FOR_UPDATE) {
                notification.setContentText(
                    updateStatus.name.lowercase().replace("_", " ")
                        .replaceFirstChar { it.uppercase() })
            }
        }

        // If progress is 0, switch to indeterminate
        if (progress != null) {
            notification
                .setOngoing(true)
                .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
                .setProgress(100, progress, progress == 0)
        } else {
            // Allow non-ongoing notifications to be removed on user click
            notification.setAutoCancel(true)
        }

        return notification.build()
    }

    private fun createNotificationChannel() {
        val updateGroup = NotificationChannelGroup(
            updatesGroupID,
            this.getString(R.string.update_group_title)
        )

        val lowPriorityNotificationChannel = NotificationChannel(
            lowPriorityUpdatesChannelID,
            this.getString(R.string.default_priority_update_channel_title),
            NotificationManager.IMPORTANCE_LOW
        ).also {
            it.description = this.getString(R.string.default_priority_update_channel_desc)
            it.group = updatesGroupID
        }

        val highPriorityNotificationChannel = NotificationChannel(
            highPriorityUpdatesChannelID,
            this.getString(R.string.high_priority_update_channel_title),
            NotificationManager.IMPORTANCE_HIGH
        ).also {
            it.description = this.getString(R.string.high_priority_update_channel_desc)
            it.group = updatesGroupID
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
