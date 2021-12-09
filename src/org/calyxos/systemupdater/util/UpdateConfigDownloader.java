/*
 * Copyright (C) 2018 The Android Open Source Project
 *               2021 The Calyx Institute
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

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.calyxos.systemupdater.R;
import org.calyxos.systemupdater.UpdateConfig;
import org.calyxos.systemupdater.settings.SettingsManager;

import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class for working with json update configurations.
 */
public final class UpdateConfigDownloader {
    private UpdateConfigDownloader() {
    }

    /**
     * @return update config from remote server
     */
    public static UpdateConfig getUpdateConfig(Context context) {
        String server = context.getResources().getString(R.string.server);
        String channel = SettingsManager.getChannel(context);
        try {
            JsonDownloader d = new JsonDownloader(server + "/" + channel + "/" + Build.DEVICE);
            String json = d.download();
            return UpdateConfig.fromJson(json);
        } catch (IOException | JSONException e) {
        }
        return null;
    }

    /**
     * @param filename searches by given filename
     * @param config   searches in {@link UpdateConfig#getAbConfig()}
     * @return offset and size of {@code filename} in the package zip file
     * stored as {@link UpdateConfig.PackageFile}.
     */
    public static Optional<UpdateConfig.PackageFile> getPropertyFile(
            final String filename,
            UpdateConfig config) {
        return Arrays
                .stream(config.getAbConfig().getPropertyFiles())
                .filter(file -> filename.equals(file.getFilename()))
                .findFirst();
    }
}
