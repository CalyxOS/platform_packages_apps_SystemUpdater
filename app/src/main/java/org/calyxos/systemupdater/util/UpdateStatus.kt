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

package org.calyxos.systemupdater.util

/**
 * Possible status of the Update
 *
 * This enum holds a combination of both status supplied by update_engine and
 * custom ones required for this app. Status from update_engine are followed by custom.
 */
// Keep in sync with: frameworks/base/core/java/android/os/UpdateEngine.java (UpdateStatusConstants)
enum class UpdateStatus {
    IDLE,
    CHECKING_FOR_UPDATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    VERIFYING,
    FINALIZING,
    UPDATED_NEED_REBOOT,
    REPORTING_ERROR_EVENT,
    ATTEMPTING_ROLLBACK,
    DISABLED,
    SUSPENDED
}
