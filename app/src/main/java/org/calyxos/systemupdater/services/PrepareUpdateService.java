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

package org.calyxos.systemupdater.services;

import static org.calyxos.systemupdater.util.PackageFiles.OTA_PACKAGE_DIR;
import static org.calyxos.systemupdater.util.PackageFiles.PAYLOAD_BINARY_FILE_NAME;
import static org.calyxos.systemupdater.util.PackageFiles.PAYLOAD_PROPERTIES_FILE_NAME;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.UpdateEngine;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.google.common.collect.ImmutableSet;

import org.calyxos.systemupdater.PayloadSpec;
import org.calyxos.systemupdater.UpdateConfig;
import org.calyxos.systemupdater.util.FileDownloader;
import org.calyxos.systemupdater.util.PackageFiles;
import org.calyxos.systemupdater.util.PayloadSpecs;
import org.calyxos.systemupdater.util.UpdateConfigs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

/**
 * This IntentService will download/extract the necessary files from the package zip
 * without downloading the whole package. And it constructs {@link PayloadSpec}.
 * All this work required to install streaming A/B updates.
 *
 * PrepareUpdateService runs on it's own thread. It will notify activity
 * using interface {@link UpdateResultCallback} when update is ready to install.
 */
public class PrepareUpdateService extends JobIntentService {
    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1000;

    /**
     * UpdateResultCallback result codes.
     */
    public static final int RESULT_CODE_SUCCESS = 0;
    public static final int RESULT_CODE_ERROR = 1;

    /**
     * Extra params that will be sent to IntentService.
     */
    public static final String EXTRA_PARAM_CONFIG = "config";
    public static final String EXTRA_PARAM_RESULT_RECEIVER = "result-receiver";

    /**
     * This interface is used to send results from {@link PrepareUpdateService} to
     * {@code MainActivity}.
     */
    public interface UpdateResultCallback {
        /**
         * Invoked when files are downloaded and payload spec is constructed.
         *
         * @param resultCode  result code, values are defined in {@link PrepareUpdateService}
         * @param payloadSpec prepared payload spec for streaming update
         */
        void onReceiveResult(int resultCode, PayloadSpec payloadSpec);
    }

    /**
     * Starts PrepareUpdateService.
     *
     * @param context        application context
     * @param config         update config
     * @param resultCallback callback that will be called when the update is ready to be installed
     */
    public static void startService(Context context,
            UpdateConfig config,
            Handler handler,
            UpdateResultCallback resultCallback) {
        Log.d(TAG, "Starting PrepareUpdateService");
        ResultReceiver receiver = new CallbackResultReceiver(handler, resultCallback);
        Intent intent = new Intent(context, PrepareUpdateService.class);
        intent.putExtra(EXTRA_PARAM_CONFIG, config);
        intent.putExtra(EXTRA_PARAM_RESULT_RECEIVER, receiver);
        enqueueWork(context, PrepareUpdateService.class, JOB_ID, intent);
    }

    private static final String TAG = "PrepareUpdateService";

    /**
     * The files that should be downloaded before streaming.
     */
    private static final ImmutableSet<String> PRE_STREAMING_FILES_SET =
            ImmutableSet.of(
                    PackageFiles.CARE_MAP_FILE_NAME,
                    PackageFiles.METADATA_FILE_NAME,
                    PackageFiles.PAYLOAD_PROPERTIES_FILE_NAME
            );

    private final PayloadSpecs mPayloadSpecs = new PayloadSpecs();
    private final UpdateEngine mUpdateEngine = new UpdateEngine();

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "On handle intent is called");
        UpdateConfig config = intent.getParcelableExtra(EXTRA_PARAM_CONFIG);
        ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_PARAM_RESULT_RECEIVER);

        try {
            PayloadSpec spec = execute(config);
            resultReceiver.send(RESULT_CODE_SUCCESS, CallbackResultReceiver.createBundle(spec));
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare streaming update", e);
            resultReceiver.send(RESULT_CODE_ERROR, null);
        }
    }

    /**
     * 1. Downloads files for streaming updates.
     * 2. Makes sure required files are present.
     * 3. Checks OTA package compatibility with the device.
     * 4. Constructs {@link PayloadSpec} for streaming update.
     */
    private PayloadSpec execute(UpdateConfig config)
            throws IOException, PreparationFailedException {

        if (config.getAbConfig().getVerifyPayloadMetadata()) {
            Log.i(TAG, "Verifying payload metadata with UpdateEngine.");
            if (!verifyPayloadMetadata(config)) {
                throw new PreparationFailedException("Payload metadata is not compatible");
            }
        }

        if (config.getInstallType() == UpdateConfig.AB_INSTALL_TYPE_NON_STREAMING) {
            return mPayloadSpecs.forNonStreaming(config.getUpdatePackageFile());
        }

        downloadPreStreamingFiles(config, OTA_PACKAGE_DIR);

        Optional<UpdateConfig.PackageFile> payloadBinary =
                UpdateConfigs.getPropertyFile(PAYLOAD_BINARY_FILE_NAME, config);

        if (!payloadBinary.isPresent()) {
            throw new PreparationFailedException(
                    "Failed to find " + PAYLOAD_BINARY_FILE_NAME + " in config");
        }

        if (!UpdateConfigs.getPropertyFile(PAYLOAD_PROPERTIES_FILE_NAME, config).isPresent()
                || !Paths.get(OTA_PACKAGE_DIR, PAYLOAD_PROPERTIES_FILE_NAME).toFile().exists()) {
            throw new IOException(PAYLOAD_PROPERTIES_FILE_NAME + " not found");
        }

        return mPayloadSpecs.forStreaming(config.getUrl(),
                payloadBinary.get().getOffset(),
                payloadBinary.get().getSize(),
                Paths.get(OTA_PACKAGE_DIR, PAYLOAD_PROPERTIES_FILE_NAME).toFile());
    }

    /**
     * Downloads only payload_metadata.bin and verifies with
     * {@link UpdateEngine#verifyPayloadMetadata}.
     * Returns {@code true} if the payload is verified or the result is unknown because of
     * exception from UpdateEngine.
     * By downloading only small portion of the package, it allows to verify if UpdateEngine
     * will install the update.
     */
    private boolean verifyPayloadMetadata(UpdateConfig config) {
        Optional<UpdateConfig.PackageFile> metadataPackageFile =
                Arrays.stream(config.getAbConfig().getPropertyFiles())
                        .filter(p -> p.getFilename().equals(
                                PackageFiles.PAYLOAD_METADATA_FILE_NAME))
                        .findFirst();
        if (!metadataPackageFile.isPresent()) {
            Log.w(TAG, String.format("ab_config.property_files doesn't contain %s",
                    PackageFiles.PAYLOAD_METADATA_FILE_NAME));
            return true;
        }
        Path metadataPath = Paths.get(OTA_PACKAGE_DIR, PackageFiles.PAYLOAD_METADATA_FILE_NAME);
        try {
            Files.deleteIfExists(metadataPath);
            FileDownloader d = new FileDownloader(
                    config.getUrl(),
                    metadataPackageFile.get().getOffset(),
                    metadataPackageFile.get().getSize(),
                    metadataPath.toFile());
            d.download();
        } catch (IOException e) {
            Log.w(TAG, String.format("Downloading %s from %s failed",
                    PackageFiles.PAYLOAD_METADATA_FILE_NAME,
                    config.getUrl()), e);
            return true;
        }
        try {
            return mUpdateEngine.verifyPayloadMetadata(metadataPath.toAbsolutePath().toString());
        } catch (Exception e) {
            Log.w(TAG, "UpdateEngine#verifyPayloadMetadata failed", e);
            return true;
        }
    }

    /**
     * Downloads files defined in {@link UpdateConfig#getAbConfig()}
     * and exists in {@code PRE_STREAMING_FILES_SET}, and put them
     * in directory {@code dir}.
     *
     * @throws IOException when can't download a file
     */
    private void downloadPreStreamingFiles(UpdateConfig config, String dir)
            throws IOException {
        Log.d(TAG, "Deleting existing files from " + dir);
        for (String file : PRE_STREAMING_FILES_SET) {
            Files.deleteIfExists(Paths.get(OTA_PACKAGE_DIR, file));
        }
        Log.d(TAG, "Downloading files to " + dir);
        for (UpdateConfig.PackageFile file : config.getAbConfig().getPropertyFiles()) {
            if (PRE_STREAMING_FILES_SET.contains(file.getFilename())) {
                Log.d(TAG, "Downloading file " + file.getFilename());
                FileDownloader downloader = new FileDownloader(
                        config.getUrl(),
                        file.getOffset(),
                        file.getSize(),
                        Paths.get(dir, file.getFilename()).toFile());
                downloader.download();
            }
        }
    }

    /**
     * Used by {@link PrepareUpdateService} to pass {@link PayloadSpec}
     * to {@link UpdateResultCallback#onReceiveResult}.
     */
    private static class CallbackResultReceiver extends ResultReceiver {

        static Bundle createBundle(PayloadSpec payloadSpec) {
            Bundle b = new Bundle();
            b.putSerializable(BUNDLE_PARAM_PAYLOAD_SPEC, payloadSpec);
            return b;
        }

        private static final String BUNDLE_PARAM_PAYLOAD_SPEC = "payload-spec";

        private UpdateResultCallback mUpdateResultCallback;

        CallbackResultReceiver(Handler handler, UpdateResultCallback updateResultCallback) {
            super(handler);
            this.mUpdateResultCallback = updateResultCallback;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            PayloadSpec payloadSpec = null;
            if (resultCode == RESULT_CODE_SUCCESS) {
                payloadSpec = (PayloadSpec) resultData.getSerializable(BUNDLE_PARAM_PAYLOAD_SPEC);
            }
            mUpdateResultCallback.onReceiveResult(resultCode, payloadSpec);
        }
    }

    private static class PreparationFailedException extends Exception {
        PreparationFailedException(String message) {
            super(message);
        }
    }

}
