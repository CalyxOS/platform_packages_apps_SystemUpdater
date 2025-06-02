/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class UpdateConfig(
    val name: String? = null,
    @SerialName("calyxos_version")
    val calyxOSVersion: String? = null,
    @SerialName("android_version")
    val androidVersion: String? = null,
    @SerialName("build_date_utc")
    val buildDateUTC: Long,
    @SerialName("build_id")
    val buildId: String? = null,
    @SerialName("build_number")
    val buildNumber: String? = null,
    @SerialName("changelog_url")
    val changelogUrl: String? = null,
    @SerialName("security_patch_level")
    val securityPatchLevel: String? = null,
    @SerialName("required_builds")
    val requiredBuilds: List<String> = emptyList(),
    val zips: List<ABConfig>
) {

    /**
     * Finds and returns closest update that can be installed, can be incremental or full
     */
    val applicableUpdate: ABConfig
        get() {
            // TODO: Handle required builds
            val buildNumber = Build.VERSION.INCREMENTAL
            return zips.find { it.type == UpdateType.INCREMENTAL && it.from == buildNumber }
                ?: zips.find { it.type == UpdateType.FULL }!!
        }
}
