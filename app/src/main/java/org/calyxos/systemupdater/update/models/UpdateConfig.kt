/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

import com.google.gson.annotations.SerializedName

data class UpdateConfig(
    val name: String = String(),
    val url: String = String(),
    @SerializedName("changelog_url")
    val changelogUrl: String = String(),
    @SerializedName("build_date_utc")
    val buildDateUTC: Long = 0L,
    @SerializedName("ab_install_type")
    val abInstallType: ABInstallType = ABInstallType.NOT_AVAILABLE,
    @SerializedName("ab_config")
    val abConfig: ABConfig = ABConfig(),
    var rawJson: String = String()
)
