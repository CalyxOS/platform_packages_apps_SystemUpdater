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
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemProperties
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calyxos.systemupdater.update.models.PackageFile
import org.calyxos.systemupdater.update.models.UpdateConfig
import org.calyxos.systemupdater.update.models.UpdateStatus
import org.calyxos.systemupdater.util.CommonModule
import java.io.File
import java.net.URL
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class UpdateManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateEngine: UpdateEngine,
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : UpdateEngineCallback() {

    private val TAG = UpdateManagerImpl::class.java.simpleName
    private val UPDATE_STATUS = "UpdateStatus"

    private val otaServerURL = "https://release.calyxinstitute.org"

    private val payloadBinary = "payload.bin"
    private val payloadMetadata = "payload_metadata.bin"
    private val payloadProperties = "payload_properties.txt"

    private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)
    val updateStatus = _updateStatus.asStateFlow()

    private val _updateProgress = MutableStateFlow(0)
    val updateProgress = _updateProgress.asStateFlow()

    init {
        // restore last update status to properly reflect the status
        restoreLastUpdate()

        // handle status updates from update_engine
        updateEngine.bind(this)
        GlobalScope.launch {
            updateStatus.onEach {
                when (it) {
                    UpdateStatus.CHECKING_FOR_UPDATE -> {}
                    else -> {
                        sharedPreferences.edit(true) { putString(UPDATE_STATUS, it.name) }
                    }
                }
            }.collect()
            updateProgress.collect()
        }
    }

    suspend fun checkUpdates(): Boolean {
        _updateStatus.value = UpdateStatus.CHECKING_FOR_UPDATE
        return if (getUpdateConfig() != null) {
            Log.i(TAG, "New update available!")
            _updateStatus.value = UpdateStatus.UPDATE_AVAILABLE
            true
        } else {
            Log.i(TAG, "No new update available!")
            _updateStatus.value = UpdateStatus.IDLE
            false
        }
    }

    fun suspendUpdate() {
        updateEngine.suspend()
        _updateStatus.value = UpdateStatus.SUSPENDED
    }

    fun resumeUpdate() {
        restoreLastUpdate()
        updateEngine.resume()
    }

    suspend fun applyUpdate() {
        _updateStatus.value = UpdateStatus.PREPARING_TO_UPDATE

        val updateConfig = getUpdateConfig()
        if (updateConfig == null) {
            _updateStatus.value = UpdateStatus.FAILED_PREPARING_UPDATE
            return
        }

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
                    _updateStatus.value = UpdateStatus.DOWNLOADING
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

    /**
     * Returns an instance of [UpdateConfig] to fetch OTA from
     *
     * Fetches the config from remote server from the channel chosen by user
     * @return An instance of [UpdateConfig], null if remote update is N/A or old
     */
    private suspend fun getUpdateConfig(): UpdateConfig? {
        val channel = sharedPreferences.getString(CommonModule.channel, CommonModule.defaultChannel)
        val jsonFile = File("${context.filesDir.absolutePath}/${Build.DEVICE}.json")

        //
        val timePassed = Calendar.getInstance().time.time - jsonFile.lastModified()
        val updateConfig = if (timePassed > TimeUnit.HOURS.toMillis(3)) {
            Log.i(TAG, "Existing JSON config is old, fetching again!")
            val jsonConfig = fetchJsonConfig("$otaServerURL/$channel/${Build.DEVICE}")
            if (jsonConfig.isSuccess) {
                withContext(Dispatchers.IO) {
                    jsonFile.writeText(jsonConfig.getOrThrow())
                    return@withContext gson.fromJson(
                        jsonConfig.getOrThrow(),
                        UpdateConfig::class.java
                    )
                }
            } else {
                UpdateConfig()
            }
        } else {
            withContext(Dispatchers.IO) {
                Log.i(TAG, "Returning config from existing file")
                val jsonConfig = jsonFile.inputStream().bufferedReader().readText()
                return@withContext gson.fromJson(jsonConfig, UpdateConfig::class.java)
            }
        }

        return if (updateConfig.buildDateUTC > SystemProperties.get("ro.build.date.utc").toLong()) {
            updateConfig
        } else {
            null
        }
    }

    private suspend fun fetchJsonConfig(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpsURLConnection
                val jsonConfig = connection.inputStream.bufferedReader().use { it.readText() }
                if (jsonConfig.isNotBlank()) {
                    return@withContext Result.success(jsonConfig)
                }
                return@withContext Result.failure(Exception("Update config is empty!"))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch update config!", exception)
                return@withContext Result.failure(exception)
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
                    _updateStatus.value = UpdateStatus.FAILED_PREPARING_UPDATE
                    return@withContext Result.failure(Exception("Failed verifying metadata!"))
                }
                return@withContext Result.success(true)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download payload metadata! ", exception)
                _updateStatus.value = UpdateStatus.FAILED_PREPARING_UPDATE
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
                _updateStatus.value = UpdateStatus.FAILED_PREPARING_UPDATE
                return@withContext Result.failure(exception)
            }
        }
    }

    override fun onStatusUpdate(p0: Int, p1: Float) {
        when (val status = UpdateStatus.values()[p0]) {
            UpdateStatus.IDLE -> {
                when (_updateStatus.value) {
                    UpdateStatus.CHECKING_FOR_UPDATE, UpdateStatus.UPDATE_AVAILABLE -> {
                        // do nothing for these status as they are controlled from app side
                    }
                    else -> {
                        _updateStatus.value = status
                    }
                }
            }
            UpdateStatus.CHECKING_FOR_UPDATE, UpdateStatus.UPDATE_AVAILABLE -> {
                // do nothing for these status as they are controlled from app side
            }
            UpdateStatus.DOWNLOADING -> {
                // Ignore if update was suspended as engine will still say downloading
                if (_updateStatus.value != UpdateStatus.SUSPENDED) {
                    _updateStatus.value = status
                }
            }
            else -> { _updateStatus.value = status }
        }
        _updateProgress.value = (100 * p1).toInt()
    }

    override fun onPayloadApplicationComplete(p0: Int) {
        // This can emit any status present in system/update_engine/common/error_code.h
        // However, the status we care about are emitted in onStatusUpdate function.
        // Thus, simply log this and ignore.
        Log.d(TAG, "Payload completed with error code: $p0")
    }

    private fun restoreLastUpdate() {
        val status = sharedPreferences.getString(UPDATE_STATUS, UpdateStatus.IDLE.name)
        status?.let { _updateStatus.value = UpdateStatus.valueOf(it) }
    }
}
