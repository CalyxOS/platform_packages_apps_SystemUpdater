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

package org.calyxos.systemupdater.network

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.calyxos.systemupdater.network.models.UpdateConfig

class OTARepository @Inject constructor(
    private val otaUtils: OTAUtils
) {
    private val TAG = OTARepository::class.java.simpleName

    val latestUpdate: Flow<UpdateConfig> = flow {
        while (true) {
            val latestUpdateConfig = otaUtils.getUpdateConfig()
            if (otaUtils.newUpdateAvailable(latestUpdateConfig.version)) {
                emit(latestUpdateConfig)
            } else {
                Log.d(TAG, "No new update is available!")
            }
        }
    }

}
