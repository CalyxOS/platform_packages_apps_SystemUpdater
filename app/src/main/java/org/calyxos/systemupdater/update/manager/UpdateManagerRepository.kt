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

package org.calyxos.systemupdater.update.manager

import kotlinx.coroutines.flow.asStateFlow
import org.calyxos.systemupdater.update.models.UpdateConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManagerRepository @Inject constructor(
    private val updateManagerImpl: UpdateManagerImpl
) {

    val updateStatus = updateManagerImpl.updateStatus.asStateFlow()
    val updateProgress = updateManagerImpl.updateProgress.asStateFlow()

    fun suspendUpdate() {
        updateManagerImpl.suspendUpdate()
    }

    fun resumeUpdate() {
        updateManagerImpl.resumeUpdate()
    }

    suspend fun applyUpdate(updateConfig: UpdateConfig) {
        updateManagerImpl.applyUpdate(updateConfig)
    }
}
