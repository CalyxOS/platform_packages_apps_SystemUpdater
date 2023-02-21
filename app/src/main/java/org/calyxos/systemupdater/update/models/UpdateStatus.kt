/*
 * Copyright (C) 2023 The Calyx Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    SUSPENDED
}
