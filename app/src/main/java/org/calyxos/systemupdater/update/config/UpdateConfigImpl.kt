/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.update.config

import android.content.SharedPreferences
import android.os.Build
import android.os.SystemProperties
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.calyxos.systemupdater.update.models.UpdateConfig
import org.calyxos.systemupdater.util.CommonModule
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class UpdateConfigImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) {

    private val TAG = UpdateConfigImpl::class.java.simpleName
    private val otaServerURL = "https://release.calyxinstitute.org"

    /**
     * Returns an instance of [UpdateConfig] to fetch OTA from
     *
     * Fetches the config from remote server from the channel chosen by user
     * @return An instance of [UpdateConfig], placeholder if remote update is N/A
     */
    suspend fun getUpdateConfig(): UpdateConfig? {
        val channel = sharedPreferences.getString(CommonModule.channel, CommonModule.defaultChannel)
        val jsonConfig = fetchJsonConfig("$otaServerURL/$channel/${Build.DEVICE}")
        return if (jsonConfig.isSuccess) {
            val updateConfig = gson.fromJson(jsonConfig.getOrThrow(), UpdateConfig::class.java)
            updateConfig.rawJson = jsonConfig.getOrDefault("")
            updateConfig
        } else {
            null
        }
    }

    /**
     * Compares update's buildDateUTC against system to determine if this is a new update
     * @param updateBuildDateUTC buildDateUTC of the update to compare against
     * @return Boolean indicating if this is a new update
     */
    fun newUpdateAvailable(updateBuildDateUTC: Long): Boolean {
        return updateBuildDateUTC > SystemProperties.get("ro.build.date.utc").toLong()
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
}
