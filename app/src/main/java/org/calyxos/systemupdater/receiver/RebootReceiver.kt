/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(BroadcastReceiver::class)
class RebootReceiver : Hilt_RebootReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        context?.getSystemService<PowerManager>()?.reboot(null)
    }
}
