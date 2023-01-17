package org.calyxos.systemupdater.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.calyxos.systemupdater.util.OTAConfigUtil

class MainActivityViewModel: ViewModel() {

    fun checkUpdates(context: Context) {
        viewModelScope.launch {
            OTAConfigUtil.getUpdateConfig(context)
        }
    }
}