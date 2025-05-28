/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.service.SystemUpdaterService
import org.calyxos.systemupdater.update.manager.UpdateManagerRepository
import org.calyxos.systemupdater.util.PreferenceUtil
import org.calyxos.systemupdater.work.UpdateWorker
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: UpdateManagerRepository,
    private val preferenceUtil: PreferenceUtil,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = UpdateViewModel::class.java.simpleName
    private val payloadBinary = "payload.bin"

    val updateStatus = updateManager.updateStatus
    val updateProgress = updateManager.updateProgress

    val updateLastCheck = preferenceUtil.lastUpdateCheckFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1)

    private val _updateSize = MutableStateFlow("")
    val updateSize = _updateSize.asStateFlow()

    fun checkAndApplyUpdates() {
        UpdateWorker.triggerUpdate(context)
    }

    fun getPayloadSize() {
        viewModelScope.launch {
            val updateConfig = updateManager.getUpdateConfig()
            val payloadFile =
                updateConfig?.abConfig?.propertyFiles?.find { it.filename == payloadBinary }
            payloadFile?.let {
                _updateSize.value = Formatter.formatFileSize(context, payloadFile.size)
            }
        }
    }

    fun loadChangelog(viewContext: Context) {
        viewModelScope.launch {
            val updateConfig = updateManager.getUpdateConfig()
            try {
                Intent(Intent.ACTION_VIEW, updateConfig!!.changelogUrl.toUri()).also {
                    viewContext.startActivity(it)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to load changelog!", exception)
                Toast.makeText(context, context.getString(R.string.na), Toast.LENGTH_SHORT).show()
            }
        }
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
