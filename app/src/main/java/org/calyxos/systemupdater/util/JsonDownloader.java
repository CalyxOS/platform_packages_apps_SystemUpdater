/*
 * Copyright (C) 2021 The Calyx Institute
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

package org.calyxos.systemupdater.util;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class JsonDownloader {
    private final String mUrl;

    public JsonDownloader(String url) {
        this.mUrl = url;
    }

    /**
     * Downloads the JSON file.
     *
     * @throws IOException when can't download the JSON
     */
    public String download() throws IOException {
        Log.d("JsonDownloader", "downloading from " + mUrl);

        URL url = new URL(mUrl);
        URLConnection connection = url.openConnection();
        connection.connect();

        // download the file
        try (InputStream input = connection.getInputStream()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                return sb.toString();
            }
        }
    }
}
