/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.navigation.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.util.PreferenceUtil
import org.calyxos.systemupdater.util.PreferenceUtil.Companion.PREF_CHANNEL
import org.calyxos.systemupdater.util.PreferenceUtil.Companion.PREF_NOTIFICATION
import javax.inject.Inject

@AndroidEntryPoint(PreferenceFragmentCompat::class)
class SettingsFragment : Hilt_SettingsFragment() {

    @Inject
    lateinit var preferenceUtil: PreferenceUtil

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            view.findNavController().navigateUp()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        findPreference<ListPreference>(PREF_CHANNEL)?.apply {
            val currentChannel = preferenceUtil.currentChannel
            when (currentChannel in resources.getStringArray(R.array.channel_values)) {
                true -> {
                    isEnabled = true
                    summary = resources.getStringArray(R.array.channel_entries)[
                        resources.getStringArray(R.array.channel_values).indexOf(currentChannel)
                    ]
                }

                else -> {
                    isEnabled = false
                    summary = currentChannel
                }
            }

            setOnPreferenceChangeListener { _, newValue ->
                summary = resources.getStringArray(R.array.channel_entries)[
                    resources.getStringArray(R.array.channel_values).indexOf(newValue)
                ]
                true
            }
        }

        findPreference<Preference>(PREF_NOTIFICATION)?.apply {
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
