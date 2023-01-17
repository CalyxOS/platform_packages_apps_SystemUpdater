/*
 * Copyright (C) 2018 The Android Open Source Project
 *               2023 The Calyx Institute
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

package org.calyxos.systemupdater.util

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.calyxos.systemupdater.models.UpdateConfig

object OTAConfigUtil {

    private const val otaServerURL = "https://release.calyxinstitute.org"
    private const val updateChannel = "channel"
    private const val defaultUpdateChannel = "stable"
    private val gson = Gson()

    /**
     * Returns an instance of [UpdateConfig] to fetch OTA from
     *
     * Checks local files directory first for a local json, if empty or doesn't exists,
     * fetches the config from remote server from the channel chosen by user
     * @param context Current context
     * @return An instance of [UpdateConfig], placeholder if both local and remote update are N/A
     */
    suspend fun getUpdateConfig(context: Context): UpdateConfig {
        val localJsonConfig = File("${context.filesDir}/${Build.DEVICE}.json")
        if (localJsonConfig.exists()) {
            val json = localJsonConfig.inputStream().bufferedReader().use { it.readText() }
            if (json.isNotEmpty()) {
                return gson.fromJson(json, UpdateConfig::class.java)
            }
        }
        return getRemoteUpdateConfig(context)
    }

    private suspend fun getRemoteUpdateConfig(context: Context): UpdateConfig {
        val channel = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(updateChannel, defaultUpdateChannel)
        val jsonConfig = fetchJsonConfig("$otaServerURL/$channel/${Build.DEVICE}")
        return if (jsonConfig.isNotEmpty()) {
            gson.fromJson(jsonConfig, UpdateConfig::class.java)
        } else {
            UpdateConfig()
        }
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