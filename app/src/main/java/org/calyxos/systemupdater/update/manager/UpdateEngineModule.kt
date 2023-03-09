/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.manager

import android.os.UpdateEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdateEngineModule {

    /**
     * Provides an instance of UpdateEngine
     */
    @Singleton
    @Provides
    fun providesUpdateEngineInstance(): UpdateEngine {
        return UpdateEngine()
    }
}
