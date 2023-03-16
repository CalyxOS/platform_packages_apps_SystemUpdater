package org.calyxos.systemupdater.work

import javax.inject.Inject

class WorkManagerRepository @Inject constructor(
    private val workManagerImpl: WorkManagerImpl
) {

    fun expeditedUpdatesCheck() {
        workManagerImpl.expeditedUpdatesCheck()
    }

    fun scheduleAutoUpdates() {
        workManagerImpl.scheduleAutoUpdates()
    }
}
