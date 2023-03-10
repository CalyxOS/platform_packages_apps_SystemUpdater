package org.calyxos.systemupdater.notification

import org.calyxos.systemupdater.update.models.UpdateStatus
import javax.inject.Inject

class UpdateNotificationRepository @Inject constructor(
    private val updateNotificationImpl: UpdateNotificationImpl
) {

    fun postNotification(updateStatus: UpdateStatus) {
        return updateNotificationImpl.postNotification(updateStatus)
    }

    fun removeNotification(updateStatus: UpdateStatus) {
        return updateNotificationImpl.removeNotification(updateStatus)
    }
}
