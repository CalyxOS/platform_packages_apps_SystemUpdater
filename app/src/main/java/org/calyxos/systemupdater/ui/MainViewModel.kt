/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.calyxos.systemupdater.service.SystemUpdaterService
import org.calyxos.systemupdater.update.UpdateManager
import org.calyxos.systemupdater.util.PreferenceUtil
import org.calyxos.systemupdater.work.UpdateWorker
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val preferenceUtil: PreferenceUtil,
    private val updateManager: UpdateManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val updateConfig get() = updateManager.updateConfig
    val updateStatus get() = updateManager.updateStatus
    val updateProgress get() = updateManager.updateProgress

    val updateLastCheck
        get() = preferenceUtil.lastUpdateCheckFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, -1)

    fun checkAndApplyUpdates() {
        UpdateWorker.triggerUpdate(context)
    }

    fun suspendUpdate() {
        updateManager.suspendUpdate()
    }

    fun resumeUpdate() {
        updateManager.resumeUpdate()
    }

    fun applyUpdate() {
        Intent(context, SystemUpdaterService::class.java).also {
            it.action = SystemUpdaterService.APPLY_UPDATE
            context.startService(it)
        }
    }

    fun getASBDate(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val securityVersionDate = simpleDateFormat.parse(Build.VERSION.SECURITY_PATCH)
        val dateFormat = DateFormat.getLongDateFormat(context)
        return dateFormat.format(securityVersionDate!!)
    }
}
