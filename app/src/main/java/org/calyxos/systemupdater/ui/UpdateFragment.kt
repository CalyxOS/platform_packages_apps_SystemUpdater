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

import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.update.models.UpdateStatus
import org.calyxos.systemupdater.util.CommonUtils
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint(Fragment::class)
class UpdateFragment : Hilt_UpdateFragment(R.layout.fragment_update) {

    private val TAG = UpdateFragment::class.java.simpleName
    private val viewModel: UpdateViewModel by viewModels()

    @Inject
    @Named(CommonUtils.calyxOSVersion)
    lateinit var calyxOSVersion: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setOnMenuItemClickListener {
                if (it.itemId == R.id.settings) {
                    view.findNavController().navigate(R.id.settingsFragment)
                }
                true
            }
        }

        // Set CalyxOS, Android, Last Check and ASB values
        view.apply {
            findViewById<TextView>(R.id.androidVersion).text =
                getString(R.string.android_version, Build.VERSION.RELEASE)
            findViewById<TextView>(R.id.calyxVersion).text =
                getString(R.string.calyx_version, calyxOSVersion)
            findViewById<TextView>(R.id.securityVersion).text =
                getString(R.string.security_version, viewModel.getASBDate())

            lifecycleScope.launchWhenStarted {
                viewModel.updateLastCheck.collect {
                    findViewById<TextView>(R.id.lastUpdateCheck).text =
                        getString(R.string.last_check, it)
                }
            }
        }

        // Update views based on update' status
        view.apply {
            val updateTitle = findViewById<TextView>(R.id.updateTitle)
            val updateCheck = findViewById<ProgressBar>(R.id.updateCheck)
            val updateContainer = findViewById<LinearLayout>(R.id.updateContainer)
            val updateButton = findViewById<Button>(R.id.updateButton)

            val infoContainer = findViewById<LinearLayout>(R.id.infoContainer)
            val installProgress = findViewById<LinearProgressIndicator>(R.id.updateInstallProgress)
            val installSteps = findViewById<TextView>(R.id.updateInstallSteps)
            val networkWarning = findViewById<TextView>(R.id.networkWarning)

            lifecycleScope.launchWhenStarted {
                viewModel.updateStatus.collect { status ->
                    viewModel.saveLastUpdate()

                    // Set update title based on status
                    updateTitle.text = when (status) {
                        UpdateStatus.IDLE -> {
                            getString(R.string.uptodate)
                        }
                        UpdateStatus.CHECKING_FOR_UPDATE -> {
                            getString(R.string.checking_updates)
                        }
                        UpdateStatus.UPDATED_NEED_REBOOT -> {
                            getString(R.string.update_done)
                        }
                        UpdateStatus.UPDATE_AVAILABLE -> {
                            getString(R.string.update_available)
                        }
                        UpdateStatus.FAILED_PREPARING_UPDATE,
                        UpdateStatus.REPORTING_ERROR_EVENT -> {
                            getString(R.string.updated_failed)
                        }
                        UpdateStatus.PREPARING_TO_UPDATE,
                        UpdateStatus.DOWNLOADING,
                        UpdateStatus.SUSPENDED,
                        UpdateStatus.VERIFYING,
                        UpdateStatus.FINALIZING -> {
                            getString(R.string.installing_update)
                        }
                        else -> {
                            String()
                        }
                    }

                    // Set update stage as normal text in installSteps
                    // Visibility is handled in the everything else block at the last
                    installSteps.text = status.name.lowercase()
                        .replace("_", " ")
                        .replaceFirstChar { it.uppercase() }

                    // Show or hide network warning
                    networkWarning.visibility = when (status) {
                        UpdateStatus.UPDATE_AVAILABLE,
                        UpdateStatus.DOWNLOADING -> { View.VISIBLE }
                        else -> { View.GONE }
                    }

                    // Set update button's behaviour based on status
                    // Visibility is handled in the everything else block at the last
                    updateButton.apply {
                        when (status) {
                            UpdateStatus.IDLE -> {
                                text = getString(R.string.check_update)
                                isEnabled = true
                                setOnClickListener {
                                    viewModel.checkUpdates()
                                }
                            }
                            UpdateStatus.UPDATED_NEED_REBOOT -> {
                                text = getString(R.string.reboot)
                                isEnabled = true
                                setOnClickListener {
                                    val pm = context.getSystemService(PowerManager::class.java)
                                    pm.reboot(null)
                                }
                            }
                            UpdateStatus.UPDATE_AVAILABLE -> {
                                text = getString(R.string.install)
                                isEnabled = true
                                setOnClickListener {
                                    viewModel.applyUpdate()
                                }
                            }
                            UpdateStatus.FAILED_PREPARING_UPDATE,
                            UpdateStatus.REPORTING_ERROR_EVENT, -> {
                                text = getString(R.string.retry)
                                isEnabled = true
                                setOnClickListener {
                                    viewModel.checkUpdates()
                                }
                            }
                            UpdateStatus.SUSPENDED -> {
                                text = getString(R.string.resume)
                                isEnabled = true
                                setOnClickListener {
                                    viewModel.resumeUpdate()
                                }
                            }
                            UpdateStatus.PREPARING_TO_UPDATE,
                            UpdateStatus.DOWNLOADING,
                            UpdateStatus.VERIFYING,
                            UpdateStatus.FINALIZING -> {
                                isEnabled = status == UpdateStatus.DOWNLOADING
                                text = getString(R.string.suspend)
                                setOnClickListener {
                                    viewModel.suspendUpdate()
                                }
                            }
                            else -> {
                                Log.d(TAG, "No update button action for: ${status.name}")
                            }
                        }
                    }

                    // Handle everything else based on status
                    when (status) {
                        UpdateStatus.IDLE -> {
                            updateContainer.visibility = View.GONE
                            infoContainer.visibility = View.VISIBLE
                            updateCheck.visibility = View.GONE
                            updateButton.visibility = View.VISIBLE
                        }
                        UpdateStatus.CHECKING_FOR_UPDATE -> {
                            updateContainer.visibility = View.GONE
                            infoContainer.visibility = View.GONE
                            updateCheck.visibility = View.VISIBLE
                            updateButton.visibility = View.GONE
                        }
                        UpdateStatus.FAILED_PREPARING_UPDATE,
                        UpdateStatus.REPORTING_ERROR_EVENT,
                        UpdateStatus.UPDATE_AVAILABLE,
                        UpdateStatus.UPDATED_NEED_REBOOT -> {
                            updateContainer.visibility = View.VISIBLE
                            infoContainer.visibility = View.GONE
                            updateCheck.visibility = View.GONE
                            installSteps.visibility = View.GONE
                            installProgress.visibility = View.GONE
                            updateButton.visibility = View.VISIBLE
                        }
                        UpdateStatus.PREPARING_TO_UPDATE,
                        UpdateStatus.DOWNLOADING,
                        UpdateStatus.SUSPENDED,
                        UpdateStatus.VERIFYING,
                        UpdateStatus.FINALIZING -> {
                            updateContainer.visibility = View.VISIBLE
                            infoContainer.visibility = View.GONE
                            updateCheck.visibility = View.GONE
                            installProgress.apply {
                                isIndeterminate = true
                                visibility = View.VISIBLE
                            }
                            installSteps.visibility = View.VISIBLE
                            updateButton.visibility = View.VISIBLE
                        }
                        else -> {
                            Log.d(TAG, "Got an unexpected status: ${status.name}")
                        }
                    }
                }
            }

            lifecycleScope.launchWhenStarted {
                viewModel.updateProgress.collect {
                    if (it != 0) {
                        installProgress.apply {
                            isIndeterminate = false
                            progress = it
                        }
                    }
                }
            }
        }
    }
}
