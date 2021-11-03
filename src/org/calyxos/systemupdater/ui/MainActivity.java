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

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UpdateEngine;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.calyxos.systemupdater.R;
import org.calyxos.systemupdater.UpdateConfig;
import org.calyxos.systemupdater.UpdateManager;
import org.calyxos.systemupdater.UpdaterState;
import org.calyxos.systemupdater.util.UpdateConfigDownloader;
import org.calyxos.systemupdater.util.UpdateEngineErrorCodes;
import org.calyxos.systemupdater.util.UpdateEngineStatuses;

import java.util.List;

/**
 * UI for system updater app.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView mTextViewBuild;
    private Button mButtonReload;
    private Button mButtonApplyConfig;
    private Button mButtonStop;
    private Button mButtonReset;
    private Button mButtonSuspend;
    private Button mButtonResume;
    private ProgressBar mProgressBar;
    private TextView mTextViewUpdaterState;
    private TextView mTextViewEngineStatus;
    private TextView mTextViewEngineErrorCode;
    private TextView mTextViewUpdateInfo;

    private UpdateConfig mConfig;

    private final UpdateManager mUpdateManager =
            new UpdateManager(new UpdateEngine(), new Handler());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mTextViewBuild = findViewById(R.id.textViewBuild);
        this.mButtonReload = findViewById(R.id.buttonReload);
        this.mButtonApplyConfig = findViewById(R.id.buttonApplyConfig);
        this.mButtonStop = findViewById(R.id.buttonStop);
        this.mButtonReset = findViewById(R.id.buttonReset);
        this.mButtonSuspend = findViewById(R.id.buttonSuspend);
        this.mButtonResume = findViewById(R.id.buttonResume);
        this.mProgressBar = findViewById(R.id.progressBar);
        this.mTextViewUpdaterState = findViewById(R.id.textViewUpdaterState);
        this.mTextViewEngineStatus = findViewById(R.id.textViewEngineStatus);
        this.mTextViewEngineErrorCode = findViewById(R.id.textViewEngineErrorCode);
        this.mTextViewUpdateInfo = findViewById(R.id.textViewUpdateInfo);

        uiResetWidgets();
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

    /**
     * reload button is clicked
     */
    public void onReloadClick(View view) {
        loadUpdateConfig();
    }

    /**
     * view config button is clicked
     */
    public void onViewConfigClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle(mConfig.getName())
                .setMessage(mConfig.getRawJson())
                .setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss())
                .show();
    }

    /**
     * apply config button is clicked
     */
    public void onApplyConfigClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Apply Update")
                .setMessage("Do you really want to apply this update?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    uiResetWidgets();
                    uiResetEngineText();
                    applyUpdate(mConfig);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applyUpdate(UpdateConfig config) {
        try {
            mUpdateManager.applyUpdate(this, config);
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to apply update " + config.getName(), e);
        }
    }

    /**
     * stop button clicked
     */
    public void onStopClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Stop Update")
                .setMessage("Do you really want to cancel running update?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    cancelRunningUpdate();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void cancelRunningUpdate() {
        try {
            mUpdateManager.cancelRunningUpdate();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to cancel running update", e);
        }
    }

    /**
     * reset button clicked
     */
    public void onResetClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Reset Update")
                .setMessage("Do you really want to cancel running update"
                        + " and restore old version?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    resetUpdate();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void resetUpdate() {
        try {
            mUpdateManager.resetUpdate();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to reset update", e);
        }
    }

    /**
     * suspend button clicked
     */
    public void onSuspendClick(View view) {
        try {
            mUpdateManager.suspend();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to suspend running update", e);
        }
    }

    /**
     * resume button clicked
     */
    public void onResumeClick(View view) {
        try {
            uiResetWidgets();
            uiResetEngineText();
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
            setUiUpdaterState(state);

            if (state == UpdaterState.IDLE) {
                uiStateIdle();
            } else if (state == UpdaterState.RUNNING) {
                uiStateRunning();
            } else if (state == UpdaterState.PAUSED) {
                uiStatePaused();
            } else if (state == UpdaterState.ERROR) {
                uiStateError();
            } else if (state == UpdaterState.REBOOT_REQUIRED) {
                uiStateRebootRequired();
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
            setUiEngineStatus(status);
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
            setUiEngineErrorCode(errorCode);
        });
    }

    /**
     * Invoked when update progress changes.
     */
    private void onProgressUpdate(double progress) {
        mProgressBar.setProgress((int) (100 * progress));
    }

    /** resets ui */
    private void uiResetWidgets() {
        mTextViewBuild.setText(Build.DISPLAY);
        mButtonReload.setEnabled(false);
        mButtonApplyConfig.setEnabled(false);
        mButtonStop.setEnabled(false);
        mButtonReset.setEnabled(false);
        mButtonSuspend.setEnabled(false);
        mButtonResume.setEnabled(false);
        mProgressBar.setEnabled(false);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        mTextViewUpdateInfo.setTextColor(Color.parseColor("#aaaaaa"));
    }

    private void uiResetEngineText() {
        mTextViewEngineStatus.setText(R.string.unknown);
        mTextViewEngineErrorCode.setText(R.string.unknown);
        // Note: Do not reset mTextViewUpdaterState; UpdateManager notifies updater state properly.
    }

    private void uiStateIdle() {
        uiResetWidgets();
        mButtonReset.setEnabled(true);
        mButtonReload.setEnabled(true);
        mButtonApplyConfig.setEnabled(true);
        mProgressBar.setProgress(0);
    }

    private void uiStateRunning() {
        uiResetWidgets();
        mProgressBar.setEnabled(true);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        mButtonStop.setEnabled(true);
        mButtonSuspend.setEnabled(true);
    }

    private void uiStatePaused() {
        uiResetWidgets();
        mButtonReset.setEnabled(true);
        mProgressBar.setEnabled(true);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        mButtonResume.setEnabled(true);
    }

    private void uiStateError() {
        uiResetWidgets();
        mButtonReset.setEnabled(true);
        mProgressBar.setEnabled(true);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
    }

    private void uiStateRebootRequired() {
        uiResetWidgets();
        mButtonReset.setEnabled(true);
    }

    /**
     * loads json configurations from server in {@link R.string.server}.
     */
    private void loadUpdateConfig() {
        // TODO Move to another thread
        mConfig = UpdateConfigDownloader.getUpdateConfig();
    }

    /**
     * @param status update engine status code
     */
    private void setUiEngineStatus(int status) {
        String statusText = UpdateEngineStatuses.getStatusText(status);
        mTextViewEngineStatus.setText(statusText + "/" + status);
    }

    /**
     * @param errorCode update engine error code
     */
    private void setUiEngineErrorCode(int errorCode) {
        String errorText = UpdateEngineErrorCodes.getCodeName(errorCode);
        mTextViewEngineErrorCode.setText(errorText + "/" + errorCode);
    }

    /**
     * @param state updater sample state
     */
    private void setUiUpdaterState(int state) {
        String stateText = UpdaterState.getStateText(state);
        mTextViewUpdaterState.setText(stateText + "/" + state);
    }
}
