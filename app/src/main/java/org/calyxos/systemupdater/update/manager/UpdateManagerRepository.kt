/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.manager

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManagerRepository @Inject constructor(
    private val updateManagerImpl: UpdateManagerImpl
) {

    val updateStatus = updateManagerImpl.updateStatus
    val updateProgress = updateManagerImpl.updateProgress

    suspend fun checkUpdates(): Boolean {
        return updateManagerImpl.checkUpdates()
    }

    fun suspendUpdate() {
        updateManagerImpl.suspendUpdate()
    }

    fun resumeUpdate() {
        updateManagerImpl.resumeUpdate()
    }

    suspend fun applyUpdate() {
        updateManagerImpl.applyUpdate()
    }
}
