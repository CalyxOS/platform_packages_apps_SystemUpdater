package org.calyxos.systemupdater.update.notification

import org.calyxos.systemupdater.update.models.UpdateStatus
import javax.inject.Inject

class UpdateNotificationRepository @Inject constructor(
    private val updateNotificationImpl: UpdateNotificationImpl
) {

    fun postUpdateNotification(updateStatus: UpdateStatus) {
        return updateNotificationImpl.postUpdateNotification(updateStatus)
    }
}
