/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.util

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemProperties
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.calyxos.systemupdater.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        const val PREF_CHANNEL = "PREF_CHANNEL"
        const val PREF_NOTIFICATION = "PREF_NOTIFICATION"

        const val UPDATE_STATUS = "UpdateStatus"
        const val E_TAG = "ETag"
        const val PREF_LAST_CHECK = "PREF_LAST_CHECK"

        private const val PROP_CHANNEL = "sys.updater.channel"
    }

    /**
     * Last successful update check time
     * @see lastUpdateCheckFlow
     */
    var lastUpdateCheck: Long
        get() = sharedPreferences.getLong(PREF_LAST_CHECK, -1)
        set(value) = sharedPreferences.edit { putLong(PREF_LAST_CHECK, value) }

    /**
     * Flow emitting last successful update check time
     * @see lastUpdateCheck
     */
    val lastUpdateCheckFlow: Flow<Long>
        get() {
            return callbackFlow {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                    if (changedKey == PREF_LAST_CHECK) trySend(lastUpdateCheck)
                }

                trySend(lastUpdateCheck)

                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                awaitClose {
                    sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }
        }

    /**
     * Currently preferred release channel
     */
    val currentChannel: String
        get() {
            val defaultChannel = context.getString(R.string.channel_default)
            val currentChannelApp = sharedPreferences.getString(PREF_CHANNEL, defaultChannel)
                ?: defaultChannel
            // Use preferred channel set by system or user (via ADB), fallback to app otherwise
            return SystemProperties.get(PROP_CHANNEL, currentChannelApp)
        }
}
