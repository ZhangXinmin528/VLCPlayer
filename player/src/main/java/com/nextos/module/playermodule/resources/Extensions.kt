package com.nextos.module.playermodule.resources

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nextos.module.playermodule.tools.AppScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun Context.launchForeground(intent: Intent) {
    try {
        startService(intent)
    } catch (e: IllegalStateException) {
        //wait for the UI thread to be ready
        val ctx = this
        AppScope.launch(Dispatchers.Main) {
            intent.putExtra("foreground", true)
            ContextCompat.startForegroundService(ctx, intent)
        }
    }
}