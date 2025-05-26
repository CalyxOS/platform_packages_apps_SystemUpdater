/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.extensions

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.calyxos.systemupdater.util.CommonModule.PREF_LAST_CHECK

/**
 * Flow that emits when the last update check date changes
 */
val SharedPreferences.lastUpdateCheck: Flow<Long>
    get() {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == PREF_LAST_CHECK) trySend(getLong(PREF_LAST_CHECK, -1))
            }

            trySend(getLong(PREF_LAST_CHECK, -1))

            registerOnSharedPreferenceChangeListener(listener)
            awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }
