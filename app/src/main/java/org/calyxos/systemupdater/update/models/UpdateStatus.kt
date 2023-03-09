/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.models

/**
 * Possible status of the Update
 *
 * This enum holds a combination of both status supplied by update_engine and
 * custom ones required for this app. Status from update_engine are followed by custom.
 */
// Keep in sync with: system/update_engine/client_library/include/update_engine/update_status.h
enum class UpdateStatus {
    IDLE, // 0
    CHECKING_FOR_UPDATE, // 1
    UPDATE_AVAILABLE, // 2
    DOWNLOADING, // 3
    VERIFYING, // 4
    FINALIZING, // 5
    UPDATED_NEED_REBOOT, // 6
    REPORTING_ERROR_EVENT, // 7
    ATTEMPTING_ROLLBACK, // 8
    DISABLED, // 9
    NEED_PERMISSION_TO_UPDATE, // 10
    CLEANUP_PREVIOUS_UPDATE, // 11
    SUSPENDED, // custom: event when update is suspended
    PREPARING_TO_UPDATE, // custom: event sent during payload verification, fetching props
    FAILED_PREPARING_UPDATE // custom: event when payload verification or fetching props fails
}
