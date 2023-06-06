/*****************************************************************************
 * AppSetupDelegate.ki
 *
 * Copyright © 2020 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package com.nextos.module.playermodule.base

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.Log
import com.nextos.module.playermodule.resources.AppContextProvider
import com.nextos.module.playermodule.tools.AppScope
import com.nextos.module.playermodule.tools.Settings
//import com.tencent.bugly.crashreport.CrashReport
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.launch
import org.videolan.libvlc.FactoryManager
import org.videolan.libvlc.LibVLCFactory
import org.videolan.libvlc.MediaFactory
import org.videolan.libvlc.interfaces.ILibVLCFactory
import org.videolan.libvlc.interfaces.IMediaFactory

interface AppDelegate {
    val appContextProvider : AppContextProvider
    fun Context.setupApplication()
}

class AppSetupDelegate : AppDelegate{

    // Store AppContextProvider to prevent GC
    override val appContextProvider = AppContextProvider

    @TargetApi(Build.VERSION_CODES.O)
    override fun Context.setupApplication() {
        appContextProvider.init(this)
//        NotificationHelper.createNotificationChannels(this)

        // Service loaders
        FactoryManager.registerFactory(IMediaFactory.factoryId, MediaFactory())
        FactoryManager.registerFactory(ILibVLCFactory.factoryId, LibVLCFactory())
        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)

        AppContextProvider.setLocale(Settings.getInstance(this).getString("set_locale", ""))

        //TODO 处理5.1和2.1声道相关
        val deviceIdx: Int = android.provider.Settings.Global.getInt(contentResolver, "is_hdmi_out", 1)
        Log.d("JYLJYL", "setupApplication: --deviceIdx--$deviceIdx")
        //Initiate Kotlinx Dispatchers in a thread to prevent ANR
        backgroundInit()
    }

    // init operations executed in background threads
    private fun Context.backgroundInit() = AppScope.launch outerLaunch@ {
        initBugly()
    }

    private fun initBugly() {
//        if (BuildConfig.DEBUG) {
//            val strategy: CrashReport.UserStrategy = CrashReport.UserStrategy(appContextProvider.appContext)
//            // 设置anr时是否获取系统trace文件，默认为false
//            strategy.isEnableCatchAnrTrace = true
//            // 设置是否获取anr过程中的主线程堆栈，默认为true
//            strategy.isEnableRecordAnrMainStack = true
//            strategy.setCrashHandleCallback(object : CrashReport.CrashHandleCallback() {
//                override fun onCrashHandleStart(
//                    crashType: Int, errorType: String?,
//                    errorMessage: String?, errorStack: String?
//                ): Map<String, String>? {
//                    val map = LinkedHashMap<String, String>()
//                    map["Key"] = "Value"
//                    return map
//                }
//
//                override fun onCrashHandleStart2GetExtraDatas(
//                    crashType: Int, errorType: String?,
//                    errorMessage: String?, errorStack: String?
//                ): ByteArray? {
//                    return try {
//                        "Extra data.".toByteArray(charset("UTF-8"))
//                    } catch (e: Exception) {
//                        null
//                    }
//                }
//            })
//            //TODO 测试完毕调换appid
//            // crashEnable和anrEnable默认值为true
//            CrashReport.setAllThreadStackEnable(appContextProvider.appContext, true, true)
//            CrashReport.setIsDevelopmentDevice(appContextProvider.appContext, true)
//            CrashReport.initCrashReport(appContextProvider.appContext, "c6ee3fcbbd", true, strategy)
//        } else {
//            CrashReport.initCrashReport(appContextProvider.appContext, "0552a011ef", false)
//        }
//        val strategy2: CrashReport.UserStrategy = CrashReport.UserStrategy(appContextProvider.appContext)
//        strategy2.appReportDelay = 40000
    }
}
