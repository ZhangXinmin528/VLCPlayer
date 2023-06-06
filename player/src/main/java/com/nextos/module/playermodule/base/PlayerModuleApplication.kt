package com.nextos.module.playermodule.base

import android.annotation.TargetApi
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.nextos.module.playermodule.util.DialogDelegate
import org.videolan.libvlc.Dialog

class PlayerModuleApplication : MultiDexApplication(), Dialog.Callbacks by DialogDelegate, AppDelegate by AppSetupDelegate() {

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    @TargetApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        setupApplication()
        super.onCreate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        appContextProvider.updateContext()
    }

}