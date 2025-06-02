/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class UpdateConfig(
    @SerialName("name")
    val name: String = String(),
    @SerialName("calyxos_version")
    val calyxOSVersion: String = String(),
    @SerialName("android_version")
    val androidVersion: String = String(),
    @SerialName("build_date_utc")
    val buildDateUTC: Long = 0L,
    @SerialName("build_id")
    val buildId: String = String(),
    @SerialName("build_number")
    val buildNumber: String = String(),
    @SerialName("changelog_url")
    val changelogUrl: String = String(),
    @SerialName("security_patch_level")
    val securityPatchLevel: String = String(),
    @SerialName("required_builds")
    val requiredBuilds: List<String> = emptyList(),
    @SerialName("zips")
    val zips: List<ABConfig> = emptyList()
)
