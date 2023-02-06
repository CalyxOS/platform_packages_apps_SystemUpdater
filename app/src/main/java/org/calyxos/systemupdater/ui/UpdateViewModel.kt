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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.UpdateManager
import org.calyxos.systemupdater.network.OTARepository
import org.calyxos.systemupdater.network.models.UpdateConfig
import org.calyxos.systemupdater.util.CommonUtils
import org.calyxos.systemupdater.util.UpdateStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class UpdateViewModel @Inject constructor(
    private val otaRepository: OTARepository,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = UpdateViewModel::class.java.simpleName

    val updateManager = UpdateManager(UpdateEngine(), Handler(Looper.getMainLooper()))

    val updateConfigFlow: Flow<UpdateConfig>
        get() = _updateConfigFlow
    private val _updateConfigFlow = MutableStateFlow(UpdateConfig())

    val updateStatus: Flow<UpdateStatus>
        get() = _updateStatus
    private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)

    val updateLastCheck: Flow<String>
        get() = _updateLastCheck
    private val _updateLastCheck = MutableStateFlow(getLastCheck())

    fun checkUpdates() {
        _updateStatus.value = UpdateStatus.CHECKING_FOR_UPDATE
        viewModelScope.launch {
            val updateConfig = otaRepository.getLatestUpdateConfig()
            if (otaRepository.newUpdateAvailable(updateConfig.version)) {
                _updateConfigFlow.value = updateConfig
                _updateStatus.value = UpdateStatus.UPDATE_AVAILABLE
            } else {
                Log.d(TAG, "No new update available!")
                _updateStatus.value = UpdateStatus.IDLE
            }
            _updateLastCheck.value = setLastCheck()
        }
    }

    fun getASBDate(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val securityVersionDate = simpleDateFormat.parse(Build.VERSION.SECURITY_PATCH)
        val dateFormat = DateFormat.getLongDateFormat(context)
        return dateFormat.format(securityVersionDate!!)
    }

    fun applyUpdate() {
        viewModelScope.launch {
            updateConfigFlow.collect {
                if (it.name.isNotBlank()) {
                    updateManager.applyUpdate(context, it)
                } else {
                    Log.d(TAG, "No new update to install!")
                }
            }
        }
    }

    private fun getLastCheck(): String {
        val notAvailable = context.getString(R.string.na)
        return sharedPreferences.getString(CommonUtils.keyLastCheck, notAvailable) ?: notAvailable
    }

    private fun setLastCheck(): String {
        val simpleDateFormat = SimpleDateFormat("MMMM dd, yyyy kk:mm", Locale.getDefault())
        val date: String = simpleDateFormat.format(Calendar.getInstance().time)
        sharedPreferences.edit { putString(CommonUtils.keyLastCheck, date) }
        return date
    }
}
