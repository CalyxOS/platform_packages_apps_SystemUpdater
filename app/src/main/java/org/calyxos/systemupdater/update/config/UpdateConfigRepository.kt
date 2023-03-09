/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.config

import org.calyxos.systemupdater.update.models.UpdateConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateConfigRepository @Inject constructor(
    private val updateConfigImpl: UpdateConfigImpl
) {

    suspend fun getLatestUpdateConfig(): UpdateConfig? {
        return updateConfigImpl.getUpdateConfig()
    }

    fun newUpdateAvailable(updateBuildDateUTC: Long): Boolean {
        return updateConfigImpl.newUpdateAvailable(updateBuildDateUTC)
    }
}
