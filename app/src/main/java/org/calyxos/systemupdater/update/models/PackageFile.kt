/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

data class PackageFile(
    val filename: String = String(),
    val offset: Long = 0,
    val size: Long = 0
)
