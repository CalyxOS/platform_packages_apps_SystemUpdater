/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.text.format.DateFormat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.service.SystemUpdaterService
import org.calyxos.systemupdater.update.manager.UpdateManagerRepository
import org.calyxos.systemupdater.util.CommonModule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class UpdateViewModel @Inject constructor(
    private val updateManager: UpdateManagerRepository,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val updateStatus = updateManager.updateStatus
    val updateProgress = updateManager.updateProgress

    private val _updateLastCheck = MutableStateFlow(getLastCheck())
    val updateLastCheck = _updateLastCheck.asStateFlow()

    fun checkUpdates() {
        Intent(context, SystemUpdaterService::class.java).also {
            it.action = SystemUpdaterService.CHECK_UPDATES
            context.startService(it)
        }
        _updateLastCheck.value = setLastCheck()
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

    private fun getLastCheck(): String {
        val lastCheck = sharedPreferences.getLong(CommonModule.lastCheck, 0)
        if (lastCheck != 0L) {
            val simpleDateFormat = SimpleDateFormat("MMMM dd, yyyy kk:mm", Locale.getDefault())
            return simpleDateFormat.format(lastCheck)
        }
        return context.getString(R.string.na)
    }

    private fun setLastCheck(): String {
        sharedPreferences.edit { putLong(CommonModule.lastCheck, Calendar.getInstance().time.time) }
        return getLastCheck()
    }
}
