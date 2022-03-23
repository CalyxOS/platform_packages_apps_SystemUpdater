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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.calyxos.systemupdater.network.OTARepository
import org.calyxos.systemupdater.network.models.UpdateConfig
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val otaRepository: OTARepository,
) : ViewModel() {

    private val TAG = UpdateViewModel::class.java.simpleName

    val updateConfigFlow: Flow<UpdateConfig>
        get() = _updateConfigFlow
    private val _updateConfigFlow = MutableStateFlow(UpdateConfig())

    fun checkUpdates() {
        viewModelScope.launch {
            val updateConfig = otaRepository.getLatestUpdateConfig()
            if (otaRepository.newUpdateAvailable(updateConfig.version)) {
                _updateConfigFlow.value = updateConfig
            } else {
                Log.d(TAG, "No new update available!")
            }
        }
    }
}
