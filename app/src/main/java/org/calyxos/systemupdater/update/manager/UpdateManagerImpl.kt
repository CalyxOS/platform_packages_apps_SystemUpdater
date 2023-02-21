/*
 * Copyright (C) 2018 The Android Open Source Project
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.calyxos.systemupdater.update.models.PackageFile
import org.calyxos.systemupdater.update.models.UpdateConfig
import org.calyxos.systemupdater.update.models.UpdateStatus
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

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
        // StateFlow doesn't emit same value twice, so send a different status first
        // otherwise it won't broadcast failure-related status when update fails again
        updateStatus.value = UpdateStatus.PREPARING_TO_UPDATE

        val metadataFile =
            updateConfig.abConfig.propertyFiles.find { it.filename == payloadMetadata }

        if (metadataFile != null && payloadMetadataVerified(
                updateConfig.url,
                metadataFile
            ).getOrDefault(false)
        ) {
            val propertiesFile =
                updateConfig.abConfig.propertyFiles.find { it.filename == payloadProperties }
            val payloadFile =
                updateConfig.abConfig.propertyFiles.find { it.filename == payloadBinary }

            if (propertiesFile != null && payloadFile != null) {
                val properties = fetchPayloadProperties(updateConfig.url, propertiesFile)
                if (properties.isSuccess) {
                    updateStatus.value = UpdateStatus.DOWNLOADING
                    updateEngine.applyPayload(
                        updateConfig.url,
                        payloadFile.offset,
                        payloadFile.size,
                        properties.getOrDefault(emptyArray())
                    )
                }
            }
        }
    }

    private suspend fun payloadMetadataVerified(
        url: String,
        packageFile: PackageFile
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val metadataFile = File("${context.filesDir.absolutePath}/${packageFile.filename}")
            try {
                metadataFile.createNewFile()
                val connection = URL(url).openConnection() as HttpsURLConnection
                // Request a specific range to avoid skipping through load of data
                // Also do a [-1] to the range end to adjust the file size
                connection.setRequestProperty(
                    "Range",
                    "bytes=${packageFile.offset}-${packageFile.offset + packageFile.size - 1}"
                )
                connection.inputStream.use { input ->
                    metadataFile.outputStream().use { input.copyTo(it) }
                }
                if (!updateEngine.verifyPayloadMetadata(metadataFile.absolutePath)) {
                    updateStatus.value = UpdateStatus.FAILED_PREPARING_UPDATE
                    return@withContext Result.failure(Exception("Failed verifying metadata!"))
                }
                return@withContext Result.success(true)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download payload metadata! ", exception)
                updateStatus.value = UpdateStatus.FAILED_PREPARING_UPDATE
                return@withContext Result.failure(exception)
            } finally {
                withContext(NonCancellable) {
                    metadataFile.delete()
                }
            }
        }
    }

    private suspend fun fetchPayloadProperties(
        url: String,
        packageFile: PackageFile
    ): Result<Array<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpsURLConnection
                // Request a specific range to avoid skipping through load of data
                // Also do a [-1] to the range end to adjust the file size
                connection.setRequestProperty(
                    "Range",
                    "bytes=${packageFile.offset}-${packageFile.offset + packageFile.size - 1}"
                )
                val properties = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext Result.success(properties.trim().lines().toTypedArray())
            } catch (exception: Exception) {
                Log.e(TAG, "Failed fetching payload properties!", exception)
                updateStatus.value = UpdateStatus.FAILED_PREPARING_UPDATE
                return@withContext Result.failure(exception)
            }
        }
    }

    override fun onStatusUpdate(p0: Int, p1: Float) {
        updateStatus.value = UpdateStatus.values()[p0]
        updateProgress.value = (100 * p1).toInt()
    }

    override fun onPayloadApplicationComplete(p0: Int) {
        // This can emit any status present in system/update_engine/common/error_code.h
        // However, the status we care about are emitted in onStatusUpdate function.
        // Thus, simply log this and ignore.
        Log.d(TAG, "Payload completed with error code: $p0")
    }
}
