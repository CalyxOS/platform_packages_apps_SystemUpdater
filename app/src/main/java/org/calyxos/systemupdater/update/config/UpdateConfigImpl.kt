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

package org.calyxos.systemupdater.update.config

import android.content.SharedPreferences
import android.os.Build
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.calyxos.systemupdater.update.models.UpdateConfig
import org.calyxos.systemupdater.util.CommonUtils
import org.calyxos.systemupdater.util.CommonUtils.defaultChannel
import org.calyxos.systemupdater.util.CommonUtils.updateChannel
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class UpdateConfigImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    @Named(CommonUtils.calyxOSVersion) private val calyxOSVersion: String,
) {

    private val otaServerURL = "https://release.calyxinstitute.org"

    /**
     * Returns an instance of [UpdateConfig] to fetch OTA from
     *
     * Fetches the config from remote server from the channel chosen by user
     * @return An instance of [UpdateConfig], placeholder if remote update is N/A
     */
    suspend fun getUpdateConfig(): UpdateConfig {
        val channel = sharedPreferences.getString(updateChannel, defaultChannel)
        val jsonConfig = fetchJsonConfig("$otaServerURL/$channel/${Build.DEVICE}")
        return if (jsonConfig.isNotEmpty()) {
            val updateConfig = gson.fromJson(jsonConfig, UpdateConfig::class.java)
            updateConfig.rawJson = jsonConfig
            updateConfig
        } else {
            UpdateConfig()
        }
    }

    /**
     * Compares update's version against system to determine if this is a new update
     * @param version Version of the update to compare against
     * @return Boolean indicating if this is a new update
     */
    fun newUpdateAvailable(version: String): Boolean {
        // Unofficial builds have "-UNOFFICIAL" attached that must be removed
        // Example: [ro.calyxos.version]: [4.4.1-UNOFFICIAL]
        val currentVersion = calyxOSVersion.split("-")[0].replace(".", "")
        val updateVersion = version.replace(".", "")
        return updateVersion > currentVersion
    }

    private suspend fun fetchJsonConfig(url: String): String {
        var jsonConfig = String()
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                jsonConfig = connection.inputStream.bufferedReader().use { it.readText() }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
        return jsonConfig
    }
}
