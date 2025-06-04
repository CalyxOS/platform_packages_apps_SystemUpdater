/*
 * SPDX-FileCopyrightText: 2023-2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.systemupdater.work.UpdateWorker

/**
 * Broadcast receiver to schedule automatic system updates on first boot of a device
 */
@AndroidEntryPoint(BroadcastReceiver::class)
class BootCompletedReceiver : Hilt_BootCompletedReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val bootCompletedActions = listOf(ACTION_BOOT_COMPLETED, ACTION_LOCKED_BOOT_COMPLETED)
        if (context != null && intent?.action in bootCompletedActions) {
            UpdateWorker.scheduleAutomatedUpdates(context)
        }
    }
}
