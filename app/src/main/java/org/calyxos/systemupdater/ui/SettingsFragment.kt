/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.navigation.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.util.PreferenceUtil
import org.calyxos.systemupdater.util.PreferenceUtil.Companion.PREF_BATTERY
import org.calyxos.systemupdater.util.PreferenceUtil.Companion.PREF_CHANNEL
import org.calyxos.systemupdater.util.PreferenceUtil.Companion.PREF_NOTIFICATION
import org.calyxos.systemupdater.util.PreferenceUtil.Companion.PREF_REBOOT
import org.calyxos.systemupdater.work.RebootWorker
import org.calyxos.systemupdater.work.UpdateWorker
import javax.inject.Inject

@AndroidEntryPoint(PreferenceFragmentCompat::class)
class SettingsFragment : Hilt_SettingsFragment() {

    private val TAG = SettingsFragment::class.java.simpleName

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
            when (val currentChannel = preferenceUtil.currentChannel) {
                in resources.getStringArray(R.array.channel_values) -> {
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

        findPreference<SwitchPreferenceCompat>(PREF_BATTERY)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                updateAutomatedCheck()
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREF_REBOOT)?.apply {
            setOnPreferenceClickListener {
                RebootWorker.scheduleAutomaticReboot(requireContext())
                true
            }
        }
    }

    private fun updateAutomatedCheck() {
        Log.i(TAG, "Updating periodic app updates!")
        UpdateWorker.updateAutomatedCheck(
            context = requireContext(),
            requiresBatteryNotLow = preferenceUtil.requiresBatteryNotLow
        )
    }
}
