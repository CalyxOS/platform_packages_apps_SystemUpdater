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

package org.calyxos.systemupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;

import org.calyxos.systemupdater.R;

public class SettingsManager {
    // Prop schema: ${PROP_BASE} + ${KEY_X}
    private static final String PROP_BASE = "sys.updater.";

    // Shared preferences keys, will be stored in protected storage
    private static final String KEY_CHANNEL = "channel";

    private static SharedPreferences getSharedPrefs(Context context) {
        return context.createDeviceProtectedStorageContext().getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
    }

    private static String getString(Context context, String key, String fallback) {
        return getSharedPrefs(context).getString(key, fallback);
    }

    private static int getInt(Context context, String key, int fallback) {
        return getSharedPrefs(context).getInt(key, fallback);
    }

    public static String getChannel(Context context) {
        String fallback = context.getString(R.string.default_channel);
        String pref = getString(context, KEY_CHANNEL, fallback);
        return SystemProperties.get(PROP_BASE + KEY_CHANNEL, pref);
    }
}
