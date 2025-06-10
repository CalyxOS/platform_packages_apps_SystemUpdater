/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.calyxos.systemupdater.update.models.PropertyFile
import org.calyxos.systemupdater.update.models.UpdateConfig
import org.calyxos.systemupdater.update.models.UpdateStatus
import org.calyxos.systemupdater.util.PreferenceUtil
import java.io.File
import java.net.URL
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateEngine: UpdateEngine,
    private val json: Json,
    private val preferenceUtil: PreferenceUtil,
) : UpdateEngineCallback() {

    companion object {
        private const val URL_SERVER_OTA = "https://release.calyxinstitute.org"
        private const val PATH_METADATA = "/data/ota_package/payload_metadata.bin"
    }

    private val TAG = UpdateManager::class.java.simpleName

    private val rawUpdateConfig = File("${context.filesDir.absolutePath}/${Build.DEVICE}.json")

    @OptIn(ExperimentalSerializationApi::class)
    private val _updateConfig = MutableStateFlow(
        if (rawUpdateConfig.exists()) {
            json.decodeFromStream<UpdateConfig>(rawUpdateConfig.inputStream())
        } else {
            null
        }
    )
    val updateConfig = _updateConfig.asStateFlow()

    private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)
    val updateStatus = _updateStatus.asStateFlow()

    private val _updateProgress = MutableStateFlow(0)
    val updateProgress = _updateProgress.asStateFlow()

    init {
        // restore last update status to properly reflect the status
        _updateStatus.value = preferenceUtil.updateStatus

        // handle status updates from update_engine
        updateEngine.bind(this)
        updateStatus.onEach {
            when (it) {
                UpdateStatus.CHECKING_FOR_UPDATE -> {}
                else -> preferenceUtil.updateStatus = it
            }
        }.launchIn(GlobalScope)
        updateProgress.launchIn(GlobalScope)
    }

    suspend fun checkUpdates(): UpdateConfig? {
        val channel = preferenceUtil.currentChannel
        val url = "$URL_SERVER_OTA/$channel/${Build.DEVICE}"

        // Fetch the update config
        _updateStatus.value = UpdateStatus.CHECKING_FOR_UPDATE

        val config = fetchUpdateConfig(url)
        _updateConfig.value = when {
            !config?.requiredBuilds.isNullOrEmpty() -> {
                // TODO: Handle fetching required build's update config and emit that
                config
            }
            else -> config
        }

        return when (config) {
            null -> {
                _updateStatus.value = UpdateStatus.FAILED_CHECKING_UPDATE
                null
            }

            else -> {
                preferenceUtil.lastUpdateCheck = Calendar.getInstance().time.time

                val currentBuildDateUtc = SystemProperties.get("ro.build.date.utc").toLong()
                return if (config.buildDateUTC > currentBuildDateUtc) {
                    Log.i(TAG, "New update available!")
                    _updateStatus.value = UpdateStatus.UPDATE_AVAILABLE
                    config
                } else {
                    Log.i(
                        TAG, "Available update build date ${config.buildDateUTC} is older"
                            + " than current build date $currentBuildDateUtc; will not update"
                    )
                    _updateStatus.value = UpdateStatus.IDLE
                    null
                }
            }
        }
    }

    fun suspendUpdate() {
        updateEngine.suspend()
        _updateStatus.value = UpdateStatus.SUSPENDED
    }

    fun resumeUpdate() {
        _updateStatus.value = preferenceUtil.updateStatus
        updateEngine.resume()
    }

    suspend fun applyUpdate(config: UpdateConfig) {
        _updateStatus.value = UpdateStatus.PREPARING_TO_UPDATE

        try {
            val update = config.applicableUpdate
            val url = "$URL_SERVER_OTA/${update.filename}"
            val properties = fetchPayloadProperties(url, update.properties)!!

            // Verify payload metadata
            require(verifyPayloadMetadata(url, update.metadata))

            // Apply the payload in update_engine.
            Log.i(
                TAG, "Applying updateEngine payload: url=$url offset="
                    + "${update.payload.offset} size=${update.payload.size} headerKeyValuePairs="
                    + "$properties"
            )
            _updateStatus.value = UpdateStatus.DOWNLOADING

            updateEngine.applyPayload(
                url,
                update.payload.offset,
                update.payload.size,
                properties
            )
        } catch (exception: Exception) {
            _updateStatus.value = UpdateStatus.FAILED_PREPARING_UPDATE
            Log.e(TAG, "Failed applying updateEngine payload", exception)
        }
    }

    /**
     * Fetches [UpdateConfig] containing required properties and files to fetch OTA
     */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun fetchUpdateConfig(url: String): UpdateConfig? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpsURLConnection
                // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/If-None-Match
                preferenceUtil.eTag?.let { connection.setRequestProperty("If-None-Match", it) }

                connection.connect()

                if (connection.responseCode == HttpsURLConnection.HTTP_NOT_MODIFIED) {
                    Log.i(TAG, "No new config available, returning existing config!")
                    return@withContext json.decodeFromStream(rawUpdateConfig.inputStream())
                }

                // Save the new update config (and ETag header) before returning it
                val updateConfig = json.decodeFromStream<UpdateConfig>(connection.inputStream)
                preferenceUtil.eTag = connection.getHeaderField("ETag")
                return@withContext updateConfig.also {
                    rawUpdateConfig.writeText(json.encodeToString(updateConfig))
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch update config!", exception)
                return@withContext null
            }
        }
    }

    /**
     * Downloads and verifies payload metadata of a given OTA
     */
    private suspend fun verifyPayloadMetadata(url: String, file: PropertyFile): Boolean {
        val metadataFile = File(PATH_METADATA).apply {
            createNewFile()
            setReadable(true, false) // TODO: Find a better way to do this
        }

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpsURLConnection
                // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Range
                connection.setRequestProperty(
                    "Range",
                    "bytes=${file.offset}-${file.offset + file.size - 1}"
                )

                connection.inputStream.use { input ->
                    metadataFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                return@withContext updateEngine.verifyPayloadMetadata(metadataFile.absolutePath)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to verify payload metadata! ", exception)
                return@withContext false
            } finally {
                withContext(NonCancellable) {
                    metadataFile.delete()
                }
            }
        }
    }

    /**
     * Fetches payload properties of a given OTA
     */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun fetchPayloadProperties(url: String, file: PropertyFile): Array<String>? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpsURLConnection
                // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Range
                connection.setRequestProperty(
                    "Range",
                    "bytes=${file.offset}-${file.offset + file.size - 1}"
                )

                return@withContext json.decodeFromStream<Array<String>>(connection.inputStream)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch payload properties!", exception)
                return@withContext null
            }
        }
    }

    override fun onStatusUpdate(p0: Int, p1: Float) {
        when (val status = UpdateStatus.entries[p0]) {
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

            else -> {
                _updateStatus.value = status
            }
        }
        _updateProgress.value = (100 * p1).toInt()
    }

    override fun onPayloadApplicationComplete(p0: Int) {
        // This can emit any status present in system/update_engine/common/error_code.h
        // However, the status we care about are emitted in onStatusUpdate function.
        // Thus, simply log this and ignore.
        Log.i(TAG, "Payload completed with error code: $p0")
    }
}
