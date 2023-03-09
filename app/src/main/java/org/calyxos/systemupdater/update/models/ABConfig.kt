/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

import com.google.gson.annotations.SerializedName

data class ABConfig(
    @SerializedName("verify_payload_metadata")
    val verifyPayloadMetadata: Boolean = false,
    @SerializedName("property_files")
    val propertyFiles: List<PackageFile> = emptyList(),
    val authorization: String = String(),
)
