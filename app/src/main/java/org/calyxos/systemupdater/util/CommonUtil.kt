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
        const val updateChannelProp = "sys.update.channel"
    }

    // OTA Update Channel Keys
    private val channel = "channel"
    private val stableChannel = "stable"
    private val betaChannel = "beta"

    // Keep channel order synced with string array used by preference
    val channels = listOf(stableChannel, betaChannel)

    fun getCurrentOTAChannel(): String {
        val savedChannel = sharedPreferences.getString(channel, stableChannel) ?: stableChannel
        val systemPropChannel = SystemProperties.get(updateChannelProp, savedChannel)
        // Ensure that the channel coming from property is a valid channel
        return if (systemPropChannel in channels) systemPropChannel else savedChannel
    }
}
