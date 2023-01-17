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

package org.calyxos.systemupdater.network.models

import com.google.gson.annotations.SerializedName

data class UpdateConfig(
    val name: String = String(),
    val url: String = String(),
    val version: String = String(),
    @SerializedName("ab_install_type")
    val abInstallType: ABInstallType = ABInstallType.NOT_AVAILABLE,
    @SerializedName("ab_config")
    val abConfig: ABConfig = ABConfig(),
    val rawJson: String = String()
)
