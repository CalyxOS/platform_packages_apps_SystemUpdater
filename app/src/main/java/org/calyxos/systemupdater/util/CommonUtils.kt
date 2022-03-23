/*
 * Copyright (C) 2023 The Calyx Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.calyxos.systemupdater.util

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemProperties
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonUtils {

    const val calyxOSVersion = "calyxOSVersion"
    const val keyLastCheck = "last_check"

    /**
     * Provides current CalyxOS version
     */
    @Singleton
    @Provides
    @Named(calyxOSVersion)
    fun provideCalyxOSVersion(): String {
        return SystemProperties.get("ro.calyxos.version")
    }

    /**
     * Provides an instance of SharedPreferences
     */
    @Singleton
    @Provides
    fun provideSharedPrefInstance(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }
}
