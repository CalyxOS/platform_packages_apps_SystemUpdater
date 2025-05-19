/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateConfig(
    val name: String = String(),
    val url: String = String(),
    @SerialName("changelog_url")
    val changelogUrl: String = String(),
    @SerialName("build_date_utc")
    val buildDateUTC: Long = 0L,
    @SerialName("ab_install_type")
    val abInstallType: ABInstallType = ABInstallType.NOT_AVAILABLE,
    @SerialName("ab_config")
    val abConfig: ABConfig = ABConfig(),
    var rawJson: String = String()
)
