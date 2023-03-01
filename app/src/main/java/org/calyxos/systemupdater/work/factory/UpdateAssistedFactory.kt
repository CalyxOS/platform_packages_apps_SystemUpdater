/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.work.factory

import android.content.Context
import androidx.work.WorkerParameters
import dagger.assisted.AssistedFactory
import org.calyxos.systemupdater.work.UpdateWorker

@AssistedFactory
interface UpdateAssistedFactory {

    fun updateWorker(appContext: Context, workerParams: WorkerParameters): UpdateWorker
}
