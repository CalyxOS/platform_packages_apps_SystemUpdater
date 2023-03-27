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

package org.calyxos.systemupdater.ui

import android.content.Intent
import android.os.Bundle
import android.os.SystemProperties
import android.provider.Settings
import android.view.View
import androidx.navigation.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.util.CommonUtil
import javax.inject.Inject

@AndroidEntryPoint(PreferenceFragmentCompat::class)
class SettingsFragment : Hilt_SettingsFragment() {

    @Inject
    lateinit var commonUtil: CommonUtil

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            view.findNavController().navigateUp()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        findPreference<ListPreference>("channel")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                summary = newValue.toString()
                SystemProperties.set(CommonUtil.updateChannelProp, newValue.toString())
                true
            }
            summary = commonUtil.getCurrentOTAChannel()
            setValueIndex(commonUtil.channels.indexOf(commonUtil.getCurrentOTAChannel()))
        }

        // Refresh summary when system property changes
        SystemProperties.addChangeCallback {
            findPreference<ListPreference>("channel")?.summary = commonUtil.getCurrentOTAChannel()
        }

        findPreference<Preference>("notifications")?.apply {
            setOnPreferenceClickListener {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).also {
                    it.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    startActivity(it)
                }
                true
            }
        }
    }
}
