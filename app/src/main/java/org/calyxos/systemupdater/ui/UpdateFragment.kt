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
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.calyxos.systemupdater.R
import org.calyxos.systemupdater.util.CommonUtils
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class UpdateFragment : Fragment(R.layout.fragment_update) {

    private val TAG = UpdateFragment::class.java.simpleName
    private val viewModel: UpdateViewModel by viewModels()

    @Inject
    @Named(CommonUtils.calyxOSVersion)
    lateinit var calyxOSVersion: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set CalyxOS, Android, Last Check and ASB values
        view.apply {
            findViewById<TextView>(R.id.androidVersion).text =
                getString(R.string.android_version, Build.VERSION.RELEASE)
            findViewById<TextView>(R.id.calyxVersion).text =
                getString(R.string.calyx_version, calyxOSVersion)

            lifecycleScope.launchWhenStarted {
                viewModel.updateLastCheck.collect {
                    findViewById<TextView>(R.id.lastUpdateCheck).text =
                        getString(R.string.last_check, it)
                }
            }

            try {
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val securityVersionDate = simpleDateFormat.parse(Build.VERSION.SECURITY_PATCH)
                val dateFormat = DateFormat.getLongDateFormat(context)
                securityVersionDate?.let {
                    findViewById<TextView>(R.id.securityVersion).text =
                        getString(R.string.security_version, dateFormat.format(it))
                }
            } catch (exception: Exception) {
                Log.d(TAG, "Caught an exception: " + exception.message)
            }
        }

        view.findViewById<Button>(R.id.updateButton).setOnClickListener {
            viewModel.checkUpdates()
        }
    }
}
