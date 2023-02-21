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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val otaPackageDir = "/data/ota_package"
    private val payloadBinary = "payload.bin"
    private val payloadMetadata = "payload_metadata.bin"
    private val payloadProperties = "payload_properties.txt"
    private val metadata = "metadata"

    suspend fun downloadPreStreamingFiles() {
        // TODO: Write and actual implementation
    }

    suspend fun downloadFile(url: String, name: String) {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                val file = File("${context.filesDir}/$name")
                ZipInputStream(connection.inputStream.buffered()).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name == name) {
                            Files.copy(zipInput, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }
}
