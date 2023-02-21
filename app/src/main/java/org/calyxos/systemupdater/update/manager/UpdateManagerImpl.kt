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

package org.calyxos.systemupdater.update.manager

import android.content.Context
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.calyxos.systemupdater.update.models.PackageFile
import org.calyxos.systemupdater.update.models.UpdateConfig
import org.calyxos.systemupdater.update.models.UpdateStatus
import org.calyxos.systemupdater.util.FileDownloader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateEngine: UpdateEngine
) : UpdateEngineCallback() {

    private val TAG = UpdateManagerImpl::class.java.simpleName

    private val payloadBinary = "payload.bin"
    private val payloadMetadata = "payload_metadata.bin"
    private val payloadProperties = "payload_properties.txt"

    val updateStatus = MutableStateFlow(UpdateStatus.IDLE)
    val updateProgress = MutableStateFlow(0)

    init {
        updateEngine.bind(this)
    }

    fun suspendUpdate() {
        updateEngine.suspend()
    }

    fun resumeUpdate() {
        updateEngine.resume()
    }

    suspend fun applyUpdate(updateConfig: UpdateConfig) {
        val metadataFile =
            updateConfig.abConfig.propertyFiles.find { it.filename == payloadMetadata }

        if (metadataFile != null && payloadMetadataVerified(updateConfig.url, metadataFile)) {
            val propertiesFile =
                updateConfig.abConfig.propertyFiles.find { it.filename == payloadProperties }
            val payloadFile =
                updateConfig.abConfig.propertyFiles.find { it.filename == payloadBinary }

            if (propertiesFile != null && payloadFile != null) {
                val properties = fetchPayloadProperties(updateConfig.url, propertiesFile)
                updateEngine.applyPayload(
                    updateConfig.url,
                    payloadFile.offset,
                    payloadFile.size,
                    properties.lines().toTypedArray()
                )
            } else {
                Log.d(TAG, "Failed to apply update!")
            }
        } else {
            Log.d(TAG, "Failed to verify payload metadata!")
        }
    }

    private suspend fun payloadMetadataVerified(url: String, packageFile: PackageFile): Boolean {
        var verified = false
        withContext(Dispatchers.IO) {
            val metadataPath = Paths.get(context.filesDir.absolutePath, payloadMetadata)
            val metadataFile =
                FileDownloader(url, packageFile.offset, packageFile.size, metadataPath.toFile())
            metadataFile.download()
            try {
                verified =
                    updateEngine.verifyPayloadMetadata(metadataPath.toAbsolutePath().toString())
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
        return verified
    }

    private suspend fun fetchPayloadProperties(url: String, packageFile: PackageFile): String {
        var properties = String()
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                // Request a specific range to avoid skipping through load of data
                // Also do a [-1] to the range end to avoid extra buffered data
                connection.setRequestProperty(
                    "Range",
                    "bytes=${packageFile.offset}-${packageFile.offset + packageFile.size - 2}"
                )
                properties = connection.inputStream.bufferedReader().use { it.readText() }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
        return properties
    }

    override fun onStatusUpdate(p0: Int, p1: Float) {
        updateStatus.value = UpdateStatus.values()[p0]
        updateProgress.value = (100 * p1).toInt()
    }

    override fun onPayloadApplicationComplete(p0: Int) {
        Log.d(TAG, "Payload completed with error code: $p0")
    }
}
