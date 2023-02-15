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
import kotlinx.coroutines.flow.collect
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.util.UpdateStatus

@AndroidEntryPoint(Fragment::class)
class UpdateFragment : Hilt_UpdateFragment(R.layout.fragment_update) {

    private val TAG = UpdateFragment::class.java.simpleName
    private val viewModel: UpdateViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var updateStatus = UpdateStatus.IDLE

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

            // TODO: Switch to a better view restore strategy
            lifecycleScope.launchWhenStarted {
                viewModel.updateStatus.collect { status ->
                    updateStatus = status
                    viewModel.saveLastUpdate()
                    installSteps.text = status.name.lowercase().replaceFirstChar { it.uppercase() }
                    when (status) {
                        UpdateStatus.IDLE -> {
                            updateTitle.text = getString(R.string.uptodate)
                            updateContainer.visibility = View.GONE
                            infoContainer.visibility = View.VISIBLE
                            updateCheck.visibility = View.GONE
                            updateButton.apply {
                                text = getString(R.string.check_update)
                                visibility = View.VISIBLE
                                setOnClickListener {
                                    viewModel.checkUpdates()
                                }
                            }
                        }
                        UpdateStatus.CHECKING_FOR_UPDATE -> {
                            updateTitle.text = getString(R.string.checking_updates)
                            updateContainer.visibility = View.GONE
                            infoContainer.visibility = View.GONE
                            updateCheck.visibility = View.VISIBLE
                            updateButton.visibility = View.GONE
                        }
                        UpdateStatus.UPDATE_AVAILABLE -> {
                            updateTitle.text = getString(R.string.update_available)
                            updateContainer.visibility = View.VISIBLE
                            infoContainer.visibility = View.GONE
                            updateCheck.visibility = View.GONE
                            updateButton.apply {
                                text = getString(R.string.install)
                                visibility = View.VISIBLE
                                setOnClickListener {
                                    viewModel.applyUpdate()
                                }
                            }
                            networkWarning.visibility = View.VISIBLE
                        }
                        UpdateStatus.DOWNLOADING -> {
                            updateTitle.text = getString(R.string.installing_update)
                            updateContainer.visibility = View.VISIBLE
                            infoContainer.visibility = View.GONE
                            installProgress.apply {
                                isIndeterminate = true
                                visibility = View.VISIBLE
                            }
                            installSteps.visibility = View.VISIBLE
                            updateButton.apply {
                                text = getString(R.string.suspend)
                                setOnClickListener {
                                    viewModel.suspendUpdate()
                                }
                            }
                            networkWarning.visibility = View.VISIBLE
                        }
                        UpdateStatus.SUSPENDED -> {
                            updateTitle.text = getString(R.string.installing_update)
                            updateContainer.visibility = View.VISIBLE
                            infoContainer.visibility = View.GONE
                            installProgress.apply {
                                isIndeterminate = false
                                visibility = View.VISIBLE
                            }
                            installSteps.visibility = View.VISIBLE
                            updateButton.apply {
                                text = getString(R.string.resume)
                                setOnClickListener {
                                    viewModel.resumeUpdate()
                                }
                            }
                            networkWarning.visibility = View.VISIBLE
                        }
                        UpdateStatus.VERIFYING, UpdateStatus.FINALIZING -> {
                            updateTitle.text = getString(R.string.installing_update)
                            updateContainer.visibility = View.VISIBLE
                            infoContainer.visibility = View.GONE
                            installProgress.apply {
                                isIndeterminate = true
                                visibility = View.VISIBLE
                            }
                            installSteps.visibility = View.VISIBLE
                            updateButton.apply {
                                text = getString(R.string.suspend)
                                isEnabled = false
                            }
                            networkWarning.visibility = View.GONE
                        }
                        UpdateStatus.UPDATED_NEED_REBOOT -> {
                            updateTitle.text = getString(R.string.update_done)
                            updateContainer.visibility = View.VISIBLE
                            infoContainer.visibility = View.GONE
                            updateCheck.visibility = View.GONE
                            installSteps.visibility = View.GONE
                            installProgress.visibility = View.GONE
                            updateButton.apply {
                                text = getString(R.string.reboot)
                                visibility = View.VISIBLE
                                setOnClickListener {
                                    val pm = context.getSystemService(PowerManager::class.java)
                                    pm.reboot(null)
                                }
                            }
                            networkWarning.visibility = View.GONE
                        }
                        else -> {
                            Log.d(TAG, "Got an unexpected status: ${status.name}")
                        }
                    }
                }
            }

            lifecycleScope.launchWhenStarted {
                viewModel.updateProgress.collect {
                    when (updateStatus) {
                        UpdateStatus.IDLE,
                        UpdateStatus.CHECKING_FOR_UPDATE,
                        UpdateStatus.UPDATE_AVAILABLE,
                        UpdateStatus.UPDATED_NEED_REBOOT -> {
                            // do nothing
                        }
                        else -> {
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
    }
}
