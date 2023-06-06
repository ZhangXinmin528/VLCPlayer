package com.nextos.module.playermodule.util

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.nextos.module.playermodule.tools.AppScope
import kotlinx.coroutines.*


fun Context.launchForeground(intent: Intent, block: () -> Unit = {}) {
    val ctx = this@launchForeground
    AppScope.launch(Dispatchers.Main) {
        intent.putExtra("foreground", true)
        try {
            ContextCompat.startForegroundService(ctx, intent)
        } catch (e: Exception) {
            if (SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                Log.w("MediaParsingService", "ForegroundServiceStartNotAllowedException caught!")
            }
        }
        block()
    }
}

/**
 * Use the new API to retrieve a parcelable extra on an [Intent]
 *
 * @param T the extra type
 * @param key the extra key
 * @return return the un-parceled result
 */
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

/**
 * Use the new API to retrieve a parcelable extra on an [Bundle]
 *
 * @param T the extra type
 * @param key the extra key
 * @return return the un-parceled result
 */
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

/**
 * Use the new API to retrieve a parcelable array list extra on an [Intent]
 *
 * @param T the extra type
 * @param key the extra key
 * @return return the un-parceled list result
 */
inline fun <reified T : Parcelable> Intent.parcelableList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}

/**
 * Use the new API to retrieve a parcelable array list extra on an [Bundle]
 *
 * @param T the extra type
 * @param key the extra key
 * @return return the un-parceled result
 */
inline fun <reified T : Parcelable> Bundle.parcelableList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

/**
 * Use the new API to retrieve a parcelable array extra on an [Intent]
 *
 * @param T the extra type
 * @param key the extra key
 * @return return the un-parceled list result
 */
inline fun <reified T : Parcelable> Intent.parcelableArray(key: String): Array<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION", "UNCHECKED_CAST") (getParcelableArrayExtra(key) as Array<T>)
}

/**
 * Use the new API to retrieve a parcelable array extra on an [Bundle]
 *
 * @param T the extra type
 * @param key the extra key
 * @return return the un-parceled result
 */
inline fun <reified T : Parcelable> Bundle.parcelableArray(key: String): Array<T>? = when {
    SDK_INT >= 33 -> getParcelableArray(key, T::class.java)
    else -> @Suppress("DEPRECATION", "UNCHECKED_CAST") (getParcelableArray(key) as Array<T>)
}

/**
 * Use the new API to stop the foreground state of a service
 *
 * @param removeNotification Removes the notification if true
 */
fun Service.stopForegroundCompat(removeNotification:Boolean = true) = when {
    SDK_INT >= 24 -> stopForeground(if (removeNotification) Service.STOP_FOREGROUND_REMOVE else Service.STOP_FOREGROUND_DETACH)
    else -> @Suppress("DEPRECATION") stopForeground(removeNotification)
}

@Suppress("DEPRECATION")
fun PackageManager.getPackageInfoCompat(packageName: String, vararg flagArgs: Int): PackageInfo {
    var flags = 0
    flagArgs.forEach { flag -> flags = flags or flag }
    return if (SDK_INT >= 33) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getPackageInfo(packageName, flags)
    }
}
