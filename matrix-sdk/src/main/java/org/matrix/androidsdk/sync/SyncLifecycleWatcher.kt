package org.matrix.androidsdk.sync

interface SyncLifecycleWatcher {
    fun onSyncStarted()
    fun onSyncFinished()
}