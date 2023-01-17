package org.calyxos.systemupdater.models

import com.google.gson.annotations.SerializedName

data class UpdateConfig(
    val name: String = String(),
    val url: String = String(),
    @SerializedName("ab_install_type")
    val abInstallType: ABInstallType = ABInstallType.NOT_AVAILABLE,
    @SerializedName("ab_config")
    val abConfig: ABConfig = ABConfig(),
    val rawJson: String = String()
)
