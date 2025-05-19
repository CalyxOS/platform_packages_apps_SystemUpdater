/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.util

import android.content.SharedPreferences
import android.os.SystemProperties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonUtil @Inject constructor(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val UPDATE_CHANNEL_PROP = "sys.updater.channel"

        // OTA Update Channel Keys
        private const val CHANNEL = "channel"
        private const val CHANNEL_STABLE = "stable"
        private const val CHANNEL_BETA = "beta"
    }

    // Keep channel order synced with string array used by preference
    val channels = listOf(CHANNEL_STABLE, CHANNEL_BETA)

    fun currentOTAChannel(): String {
        val savedChannel = sharedPreferences.getString(CHANNEL, CHANNEL_STABLE) ?: CHANNEL_STABLE
        return SystemProperties.get(UPDATE_CHANNEL_PROP, savedChannel)
    }
}
