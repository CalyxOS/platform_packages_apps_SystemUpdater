/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.util

import android.content.SharedPreferences
import android.os.SystemProperties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonUtil @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        const val updateChannelProp = "sys.updater.channel"
    }

    // OTA Update Channel Keys
    private val channel = "channel"
    private val stableChannel = "stable"
    private val betaChannel = "beta"

    // Keep channel order synced with string array used by preference
    val channels = listOf(stableChannel, betaChannel)

    fun currentOTAChannel(): String {
        val savedChannel = sharedPreferences.getString(channel, stableChannel) ?: stableChannel
        return SystemProperties.get(updateChannelProp, savedChannel)
    }
}
