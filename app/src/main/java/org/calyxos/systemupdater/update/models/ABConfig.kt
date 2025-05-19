/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ABConfig(
    @SerialName("verify_payload_metadata")
    val verifyPayloadMetadata: Boolean = false,
    @SerialName("property_files")
    val propertyFiles: List<PackageFile> = emptyList(),
    val authorization: String = String(),
)
