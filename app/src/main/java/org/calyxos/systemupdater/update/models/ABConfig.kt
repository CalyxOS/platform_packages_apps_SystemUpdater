/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class ABConfig(
    val type: UpdateType = UpdateType.NOT_AVAILABLE,
    val from: String? = null,
    val filename: String,
    @SerialName("property_files")
    val propertyFiles: List<PropertyFile>,
) {

    companion object {
        private const val FILENAME_BINARY_PAYLOAD = "payload.bin"
        private const val FILENAME_BINARY_METADATA = "payload_metadata.bin"
        private const val FILENAME_TXT_PROPERTIES = "payload_properties.txt"
    }

    val payload: PropertyFile
        get() = propertyFiles.find { it.filename == FILENAME_BINARY_PAYLOAD }!!

    val metadata: PropertyFile
        get() = propertyFiles.find { it.filename == FILENAME_BINARY_METADATA }!!

    val properties: PropertyFile
        get() = propertyFiles.find { it.filename == FILENAME_TXT_PROPERTIES }!!
}
