/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.systemupdater.work.UpdateWorker

/**
 * Broadcast receiver to schedule automatic system updates on first boot of a device
 */
@AndroidEntryPoint(BroadcastReceiver::class)
class BootCompletedReceiver : Hilt_BootCompletedReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context != null && intent?.isBootCompletedAction() == true) {
            UpdateWorker.scheduleAutomatedUpdates(context)
        }
    }

    fun Intent.isBootCompletedAction(): Boolean =
        action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED
}
