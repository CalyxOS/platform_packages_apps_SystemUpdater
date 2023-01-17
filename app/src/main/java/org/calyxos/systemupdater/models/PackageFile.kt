package org.calyxos.systemupdater.models

data class PackageFile(
    val filename: String = String(),
    val offset: Long = 0,
    val size: Long = 0
)
