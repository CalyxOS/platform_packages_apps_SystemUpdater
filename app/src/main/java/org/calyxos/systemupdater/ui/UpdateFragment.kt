/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.systemupdater.ui

import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemProperties
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
import kotlinx.coroutines.launch
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.update.models.UpdateStatus

@AndroidEntryPoint(Fragment::class)
class UpdateFragment : Hilt_UpdateFragment(R.layout.fragment_update) {

    private val TAG = UpdateFragment::class.java.simpleName
    private val viewModel: UpdateViewModel by viewModels()

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
                getString(R.string.calyx_version, SystemProperties.get("ro.calyxos.version"))
            findViewById<TextView>(R.id.securityVersion).text =
                getString(R.string.security_version, viewModel.getASBDate())

            lifecycleScope.launch {
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
            val updateSize = findViewById<TextView>(R.id.updateSize)
            val updateChangelogButton = findViewById<Button>(R.id.updateChangelogButton)

            lifecycleScope.launch {
                viewModel.updateStatus.collect { status ->

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
                    when (status) {
                        UpdateStatus.UPDATE_AVAILABLE,
                        UpdateStatus.DOWNLOADING -> {
                            networkWarning.visibility = View.VISIBLE

                            // Try to fetch update size
                            viewModel.getPayloadSize()
                        }
                        else -> networkWarning.visibility = View.GONE
                    }

                    // Set update button's behaviour based on status
                    // Visibility is handled in the everything else block at the last
                    updateButton.apply {
                        when (status) {
                            UpdateStatus.IDLE -> {
                                text = getString(R.string.check_update)
                                isEnabled = true
                                setOnClickListener {
                                    viewModel.checkAndApplyUpdates()
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
                                    viewModel.checkAndApplyUpdates()
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
                            updateChangelogButton.setOnClickListener {
                                viewModel.loadChangelog(it.context)
                            }
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
                            updateChangelogButton.setOnClickListener {
                                viewModel.loadChangelog(it.context)
                            }
                            infoContainer.visibility = View.GONE
                            updateCheck.visibility = View.GONE
                            installProgress.apply {
                                isIndeterminate = true
                                visibility = View.VISIBLE
                            }
                            installSteps.visibility = View.VISIBLE
                            updateButton.visibility = View.GONE
                        }
                        else -> {
                            Log.d(TAG, "Got an unexpected status: ${status.name}")
                        }
                    }
                }
            }

            lifecycleScope.launch {
                viewModel.updateProgress.collect {
                    if (it != 0) {
                        installProgress.apply {
                            isIndeterminate = false
                            progress = it
                        }
                    }
                }
            }

            lifecycleScope.launch {
                viewModel.updateSize.collect {
                    if (it.isNotBlank()) {
                        updateSize.apply {
                            visibility = View.VISIBLE
                            text = context.getString(R.string.update_size, it)
                        }
                    }
                }
            }
        }
    }
}
