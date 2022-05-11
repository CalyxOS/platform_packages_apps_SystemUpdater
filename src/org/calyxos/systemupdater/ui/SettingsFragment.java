package org.calyxos.systemupdater.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState,
                                    @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }
}
