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
import android.content.res.Resources;
import android.os.SystemProperties;

import org.calyxos.systemupdater.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingsManager {
    // Prop schema: ${PROP_BASE} + ${KEY_X}
    private static final String PROP_BASE = "sys.updater.";

    // Shared preferences keys, will be stored in protected storage
    private static final String KEY_CHANNEL = "channel";
    private static final String KEY_LAST_CHECK = "last_check";
    private static final String KEY_BATTERY = "battery";
    private static final String KEY_REBOOT = "reboot";
    private static final String KEY_ROAMING = "roaming";

    private static SharedPreferences getSharedPrefs(Context context) {
        return context.createDeviceProtectedStorageContext().getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
    }

    private static void setString(Context context, String key, String val) {
        getSharedPrefs(context).edit().putString(key, val).apply();
    }

    private static String getString(Context context, String key, String fallback) {
        return getSharedPrefs(context).getString(key, fallback);
    }

    private static long getLong(Context context, String key, int fallback) {
        return getSharedPrefs(context).getLong(key, fallback);
    }

    private static void setLong(Context context, String key, long val) {
        getSharedPrefs(context).edit().putLong(key, val).apply();
    }

    private static boolean getBoolean(Context context, String key, boolean fallback) {
        return getSharedPrefs(context).getBoolean(key, fallback);
    }

    private static boolean getKey(Context context, String key, int res) {
        final Resources resources = context.getResources();
        boolean fallback = resources.getBoolean(res);
        boolean pref = getBoolean(context, key, fallback);
        return SystemProperties.getBoolean(PROP_BASE + key, pref);
    }

    public static String getChannel(Context context) {
        final Resources resources = context.getResources();
        String fallback = resources.getString(R.string.default_channel);
        String pref = getString(context, KEY_CHANNEL, fallback);
        return SystemProperties.get(PROP_BASE + KEY_CHANNEL, pref);
    }

    public static String getLastCheck(Context context) {
        return getString(context, KEY_LAST_CHECK, "");
    }

    public static void setLastCheck(Context context) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy kk:mm", Locale.getDefault());
        String date = simpleDateFormat.format(Calendar.getInstance().getTime());
        setString(context, KEY_LAST_CHECK, date);
    }

    public static boolean getBattery(Context context) {
        return getKey(context, KEY_BATTERY, R.bool.default_battery);
    }

    public static boolean getReboot(Context context) {
        return getKey(context, KEY_REBOOT, R.bool.default_reboot);
    }

    public static boolean getRoaming(Context context) {
        return getKey(context, KEY_ROAMING, R.bool.default_roaming);
    }
}
