/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.util

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    // Last OTA Update Check Key
    const val PREF_LAST_CHECK = "last_check"

    // Flags to format date using DateUtils class
    const val DATE_UTILS_FLAGS = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or
        DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL

    /**
     * Provides an instance of SharedPreferences
     */
    @Singleton
    @Provides
    fun provideSharedPrefInstance(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    /**
     * Provides an instance of [Json]
     */
    @Singleton
    @Provides
    fun provideJsonInstance(): Json {
        return Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}
