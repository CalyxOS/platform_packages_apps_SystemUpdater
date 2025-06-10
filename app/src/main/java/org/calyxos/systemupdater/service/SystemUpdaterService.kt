/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.service

import android.app.NotificationManager
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.util.Log
import androidx.core.content.getSystemService
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.update.UpdateManager
import org.calyxos.systemupdater.update.models.UpdateStatus
import org.calyxos.systemupdater.util.NotificationUtil
import org.calyxos.systemupdater.util.PreferenceUtil
import org.calyxos.systemupdater.work.RebootWorker
import javax.inject.Inject

@AndroidEntryPoint(LifecycleService::class)
class SystemUpdaterService : Hilt_SystemUpdaterService() {

    companion object {
        const val CHECK_UPDATES = "CheckUpdates"
        const val APPLY_UPDATE = "ApplyUpdate"
        const val CHECK_AND_APPLY_UPDATES = "${CHECK_UPDATES}And$APPLY_UPDATE"

        private const val NOTIFICATION_ID_FGS = 1
    }

    private val TAG = SystemUpdaterService::class.java.simpleName

    private val connectivityManager: ConnectivityManager
        get() = this.getSystemService<ConnectivityManager>()!!

    private val notificationManager: NotificationManager
        get() = this.getSystemService<NotificationManager>()!!

    // Coroutine
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    private val shouldUpdateOnMobileData
        get() = preferenceUtil.shouldUpdateOnMobileDataFlow
            .stateIn(serviceScope, SharingStarted.WhileSubscribed(), false)

    private val isUsingMobileData: StateFlow<Boolean>
        get() = callbackFlow {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    trySend(networkCapabilities.hasTransport(TRANSPORT_CELLULAR))
                }
            }

            connectivityManager.activeNetwork?.let { activeNetwork ->
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                trySend(capabilities?.hasTransport(TRANSPORT_CELLULAR) ?: true)
            }

            connectivityManager.registerDefaultNetworkCallback(networkCallback)
            awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
        }.stateIn(serviceScope, SharingStarted.WhileSubscribed(), true)

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var preferenceUtil: PreferenceUtil

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            CHECK_UPDATES -> checkUpdates()
            APPLY_UPDATE -> applyUpdate()
            CHECK_AND_APPLY_UPDATES -> checkAndApplyUpdate()
            else -> Log.d(TAG, "Got Unknown Intent!")
        }

        combine(isUsingMobileData, shouldUpdateOnMobileData, updateManager.updateStatus) {
            usingMobileData, updateOnMobileData, status ->

            when {
                usingMobileData && !updateOnMobileData && status in UpdateStatus.UPDATING -> {
                    updateManager.suspendUpdate()
                }

                else -> updateManager.resumeUpdate()
            }
        }

        combine(updateManager.updateStatus, updateManager.updateProgress) { status, progress ->
            when (status) {
                UpdateStatus.UPDATE_AVAILABLE -> {
                    val notification = NotificationUtil.getNotification(
                        context = this,
                        status = status,
                        title = getString(R.string.update_available),
                        desc = getString(R.string.update_available_desc)
                    )
                    notificationManager.notify(status.ordinal, notification)
                }

                UpdateStatus.PREPARING_TO_UPDATE,
                UpdateStatus.DOWNLOADING,
                UpdateStatus.SUSPENDED,
                UpdateStatus.VERIFYING,
                UpdateStatus.FINALIZING,
                    -> {
                    val notification = NotificationUtil.getNotification(
                        context = this,
                        status = status,
                        title = getString(R.string.installing_update),
                        progress = progress
                    )
                    notificationManager.notify(NOTIFICATION_ID_FGS, notification)
                }

                UpdateStatus.FAILED_PREPARING_UPDATE,
                UpdateStatus.REPORTING_ERROR_EVENT,
                    -> {
                    val notification = NotificationUtil.getNotification(
                        context = this,
                        status = status,
                        title = getString(R.string.updated_failed),
                        desc = getString(R.string.updated_failed_desc)
                    )
                    notificationManager.notify(status.ordinal, notification)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }

                UpdateStatus.UPDATED_NEED_REBOOT -> {
                    if (preferenceUtil.shouldAutoReboot) {
                        RebootWorker.scheduleAutomaticReboot(this)
                    }
                    val notification = NotificationUtil.getNotification(
                        context = this,
                        status = status,
                        title = getString(R.string.update_done),
                        desc = getString(R.string.update_done_desc),
                    )
                    notificationManager.notify(status.ordinal, notification)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }

                else -> null
            }
        }.launchIn(serviceScope)

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private fun checkUpdates() {
        startForeground(
            NOTIFICATION_ID_FGS,
            NotificationUtil.getNotification(
                context = this,
                status = UpdateStatus.CHECKING_FOR_UPDATE,
                title = getString(R.string.checking_updates),
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
            NOTIFICATION_ID_FGS,
            NotificationUtil.getNotification(
                context = this,
                status = UpdateStatus.PREPARING_TO_UPDATE,
                title = getString(R.string.installing_update),
                progress = 0
            )
        )

        serviceScope.launch { updateManager.applyUpdate(updateManager.updateConfig.value!!) }
    }

    private fun checkAndApplyUpdate() {
        startForeground(
            NOTIFICATION_ID_FGS,
            NotificationUtil.getNotification(
                context = this,
                status = UpdateStatus.CHECKING_FOR_UPDATE,
                title = getString(R.string.checking_updates),
                progress = 0
            )
        )

        serviceScope.launch {
            updateManager.checkUpdates()?.let { config ->
                updateManager.applyUpdate(config)
            } ?: stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }
}
