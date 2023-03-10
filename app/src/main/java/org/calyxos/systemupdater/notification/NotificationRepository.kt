package org.calyxos.systemupdater.notification

import org.calyxos.systemupdater.update.models.UpdateStatus
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val updateNotificationImpl: NotificationImpl
) {

    fun postNotification(updateStatus: UpdateStatus, progress: Int = -1) {
        return updateNotificationImpl.postNotification(updateStatus, progress)
    }

    fun removeNotification(updateStatus: UpdateStatus) {
        return updateNotificationImpl.removeNotification(updateStatus)
    }
}
