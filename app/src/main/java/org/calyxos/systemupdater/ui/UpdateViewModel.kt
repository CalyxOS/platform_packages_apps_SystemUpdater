/*
 * Copyright (C) 2023 The Calyx Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.calyxos.systemupdater.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UpdateEngine
import android.text.format.DateFormat
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.UpdateManager
import org.calyxos.systemupdater.update.config.UpdateConfigRepository
import org.calyxos.systemupdater.update.models.UpdateConfig
import org.calyxos.systemupdater.update.models.UpdateStatus
import org.calyxos.systemupdater.util.CommonModule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class UpdateViewModel @Inject constructor(
    private val updateConfigRepository: UpdateConfigRepository,
    private val gson: Gson,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = UpdateViewModel::class.java.simpleName
    private val UPDATE_STATUS = "UpdateStatus"
    private val UPDATE_CONFIG = "UpdateConfig"

    private val updateManager = UpdateManager(UpdateEngine(), Handler(Looper.getMainLooper()))

    private val _updateConfig = MutableStateFlow(UpdateConfig())
    val updateConfig = _updateConfig.asStateFlow()

    private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)
    val updateStatus = _updateStatus.asStateFlow()

    private val _updateProgress = MutableStateFlow(0)
    val updateProgress = _updateProgress.asStateFlow()

    private val _updateLastCheck = MutableStateFlow(getLastCheck())
    val updateLastCheck = _updateLastCheck.asStateFlow()

    init {
        // restore last update status to properly reflect the status
        restoreLastUpdate()

        // set callbacks and bind the engine with app
        updateManager.setOnStateChangeCallback { handleEngineStatus(it) }
        updateManager.setOnProgressUpdateCallback { handleEngineProgress(it) }
        updateManager.bind()
    }

    fun checkUpdates() {
        _updateStatus.value = UpdateStatus.CHECKING_FOR_UPDATE
        viewModelScope.launch {
            val updateConfig = updateConfigRepository.getLatestUpdateConfig()
            if (updateConfig != null &&
                updateConfigRepository.newUpdateAvailable(updateConfig.buildDateUTC)
            ) {
                _updateConfig.value = updateConfig
                _updateStatus.value = UpdateStatus.UPDATE_AVAILABLE
            } else {
                Log.d(TAG, "No new update available!")
                _updateStatus.value = UpdateStatus.IDLE
            }
            _updateLastCheck.value = setLastCheck()
        }
    }

    fun saveLastUpdate() {
        _updateStatus.value.let {
            when (it) {
                UpdateStatus.IDLE, UpdateStatus.CHECKING_FOR_UPDATE -> {}
                else -> {
                    sharedPreferences.edit(true) {
                        putString(UPDATE_STATUS, it.name)
                        if (_updateConfig.value.name.isNotBlank()) {
                            putString(UPDATE_CONFIG, _updateConfig.value.rawJson)
                        }
                    }
                }
            }
        }
    }

    fun suspendUpdate() {
        _updateStatus.value = UpdateStatus.SUSPENDED
        updateManager.suspend()
    }

    fun resumeUpdate() {
        restoreLastUpdate()
        updateManager.resume()
    }

    fun applyUpdate() {
        viewModelScope.launch {
            if (_updateConfig.value.name.isNotBlank()) {
                _updateStatus.value = UpdateStatus.DOWNLOADING
                updateManager.applyUpdate(context, _updateConfig.value)
            } else {
                Log.d(TAG, "No new update to install!")
            }
        }
    }

    fun getASBDate(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val securityVersionDate = simpleDateFormat.parse(Build.VERSION.SECURITY_PATCH)
        val dateFormat = DateFormat.getLongDateFormat(context)
        return dateFormat.format(securityVersionDate!!)
    }

    private fun restoreLastUpdate() {
        val config = sharedPreferences.getString(UPDATE_CONFIG, String()) ?: String()
        if (config.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                _updateConfig.value = gson.fromJson(config, UpdateConfig::class.java)
                _updateConfig.value.rawJson = config
            }
        }

        val status = sharedPreferences.getString(UPDATE_STATUS, UpdateStatus.IDLE.name)
        status?.let { _updateStatus.value = UpdateStatus.valueOf(it) }
    }

    private fun handleEngineStatus(status: Int) {
        when (val upStatus = UpdateStatus.values()[status + 1]) {
            UpdateStatus.IDLE -> {
                // update_engine can only get idle either due to error or successful update
                when (_updateStatus.value) {
                    UpdateStatus.DOWNLOADING,
                    UpdateStatus.UPDATED_NEED_REBOOT -> { _updateStatus.value = upStatus }
                    else -> {
                        // do nothing
                    }
                }
            }
            UpdateStatus.CHECKING_FOR_UPDATE, UpdateStatus.UPDATE_AVAILABLE -> {
                // do nothing for these status as they are controlled from app side
            }
            else -> { _updateStatus.value = upStatus }
        }
    }

    private fun handleEngineProgress(progress: Double) {
        _updateProgress.value = (100 * progress).toInt()
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
