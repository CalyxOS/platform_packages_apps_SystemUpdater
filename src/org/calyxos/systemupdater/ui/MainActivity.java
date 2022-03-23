/*
 * Copyright (C) 2018 The Android Open Source Project
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

package org.calyxos.systemupdater.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UpdateEngine;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.calyxos.systemupdater.R;
import org.calyxos.systemupdater.UpdateConfig;
import org.calyxos.systemupdater.UpdateManager;
import org.calyxos.systemupdater.UpdaterState;
import org.calyxos.systemupdater.settings.SettingsManager;
import org.calyxos.systemupdater.util.UpdateConfigDownloader;
import org.calyxos.systemupdater.util.UpdateEngineErrorCodes;
import org.calyxos.systemupdater.util.UpdateEngineStatuses;

import java.util.Date;

/**
 * UI for system updater app.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String CALYXOS_VERSION_PROPERTY = "ro.calyxos.version";

    private TextView mLastUpdate;
    private Button mMultiButton;
    private ProgressBar mProgressBar;

    private PowerManager mPowerManager;
    private UpdateConfig mConfig;

    private final UpdateManager mUpdateManager =
            new UpdateManager(new UpdateEngine(), new Handler());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPowerManager = getSystemService(PowerManager.class);

        // Once setup
        TextView calyxVersion = findViewById(R.id.update_calyx_version);
        String calyxVersionStr = SystemProperties.get(CALYXOS_VERSION_PROPERTY, "");
        calyxVersion.setText(getString(R.string.calyx_version, calyxVersionStr));
        TextView androidVersion = findViewById(R.id.update_android_version);
        androidVersion.setText(getString(R.string.android_version, Build.VERSION.RELEASE));
        TextView securityVersion = findViewById(R.id.update_security_version);
        securityVersion.setText(getString(R.string.security_version, Build.VERSION.SECURITY_PATCH));

        mLastUpdate = findViewById(R.id.last_update_check);
        long lastCheck = SettingsManager.getLastCheck(this);
        mLastUpdate.setText(getString(R.string.last_check, new Date(lastCheck)));

        mProgressBar = findViewById(R.id.progress_bar);

        mMultiButton = findViewById(R.id.multi_button);
        mMultiButton.setText(R.string.check_update);
        mMultiButton.setOnClickListener(v -> loadUpdateConfig());

        loadUpdateConfig();

        this.mUpdateManager.setOnStateChangeCallback(this::onUpdaterStateChange);
        this.mUpdateManager.setOnEngineStatusUpdateCallback(this::onEngineStatusUpdate);
        this.mUpdateManager.setOnEngineCompleteCallback(this::onEnginePayloadApplicationComplete);
        this.mUpdateManager.setOnProgressUpdateCallback(this::onProgressUpdate);
    }

    @Override
    protected void onDestroy() {
        this.mUpdateManager.setOnEngineStatusUpdateCallback(null);
        this.mUpdateManager.setOnProgressUpdateCallback(null);
        this.mUpdateManager.setOnEngineCompleteCallback(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Binding to UpdateEngine invokes onStatusUpdate callback,
        // persisted updater state has to be loaded and prepared beforehand.
        this.mUpdateManager.bind();
    }

    @Override
    protected void onPause() {
        this.mUpdateManager.unbind();
        super.onPause();
    }

    private void applyUpdate(UpdateConfig config) {
        try {
            mUpdateManager.applyUpdate(this, config);
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to apply update " + config.getName(), e);
        }
    }

    /**
     * suspend button clicked
     */
    public void onSuspendUpdate() {
        try {
            mMultiButton.setText(R.string.resume);
            mMultiButton.setOnClickListener(v -> onResumeUpdate());
            mUpdateManager.suspend();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to suspend running update", e);
        }
    }

    /**
     * resume button clicked
     */
    public void onResumeUpdate() {
        try {
            mMultiButton.setText(R.string.suspend);
            mMultiButton.setOnClickListener(v -> onSuspendUpdate());
            mUpdateManager.resume();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to resume running update", e);
        }
    }

    /**
     * Invoked when system updater app state changes.
     * Value of {@code state} will be one of the
     * values from {@link UpdaterState}.
     */
    private void onUpdaterStateChange(int state) {
        Log.i(TAG, "UpdaterStateChange state="
                + UpdaterState.getStateText(state)
                + "/" + state);
        runOnUiThread(() -> {
            if (state == UpdaterState.IDLE) {
                // Nothing
            } else if (state == UpdaterState.RUNNING) {
                mMultiButton.setText(R.string.suspend);
                mMultiButton.setOnClickListener(v -> onSuspendUpdate());
            } else if (state == UpdaterState.PAUSED) {
                // Nothing
            } else if (state == UpdaterState.ERROR) {
                // Nothing
            } else if (state == UpdaterState.REBOOT_REQUIRED) {

            }
        });
    }

    /**
     * Invoked when {@link UpdateEngine} status changes. Value of {@code status} will
     * be one of the values from {@link UpdateEngine.UpdateStatusConstants}.
     */
    private void onEngineStatusUpdate(int status) {
        Log.i(TAG, "StatusUpdate - status="
                + UpdateEngineStatuses.getStatusText(status)
                + "/" + status);
        runOnUiThread(() -> {
            // Nothing
        });
    }

    /**
     * Invoked when the payload has been applied, whether successfully or
     * unsuccessfully. The value of {@code errorCode} will be one of the
     * values from {@link UpdateEngine.ErrorCodeConstants}.
     */
    private void onEnginePayloadApplicationComplete(int errorCode) {
        final String completionState = UpdateEngineErrorCodes.isUpdateSucceeded(errorCode)
                ? "SUCCESS"
                : "FAILURE";
        Log.i(TAG,
                "PayloadApplicationCompleted - errorCode="
                        + UpdateEngineErrorCodes.getCodeName(errorCode) + "/" + errorCode
                        + " " + completionState);
        runOnUiThread(() -> {
            if (UpdateEngineErrorCodes.isUpdateSucceeded(errorCode)) {
                // Reboot
                mMultiButton.setText(R.string.reboot);
                mMultiButton.setOnClickListener(v -> mPowerManager.reboot("reboot-ab-update"));
            } else {
                // Retry
                mMultiButton.setText(R.string.install);
                mMultiButton.setOnClickListener(v -> applyUpdate(mConfig));
            }
        });
    }

    /**
     * Invoked when update progress changes.
     */
    private void onProgressUpdate(double progress) {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setProgress((int) (100 * progress));
    }

    /**
     * loads json configurations from server in {@link R.string.server}.
     */
    private void loadUpdateConfig() {
        new DownloadJsonTask(this, mLastUpdate).execute();
    }

    private class DownloadJsonTask extends AsyncTask<Void, Void, UpdateConfig> {
        private final Context mContext;
        private final TextView mLastUpdate;

        public DownloadJsonTask(Context context, TextView lastUpdate) {
            mContext = context;
            mLastUpdate = lastUpdate;
        }

        @Override
        protected UpdateConfig doInBackground(Void... params) {
            return UpdateConfigDownloader.getUpdateConfig(mContext);
        }

        @Override
        protected void onPostExecute(UpdateConfig updateConfig) {
            mConfig = updateConfig;
            SettingsManager.setLastCheck(mContext);
            runOnUiThread(() -> {
                long lastCheck = SettingsManager.getLastCheck(mContext);
                mLastUpdate.setText(getString(R.string.last_check, new Date(lastCheck)));

                mMultiButton.setText(R.string.install);
                mMultiButton.setOnClickListener(v -> applyUpdate(mConfig));
            });
        }
    }
}
