package com.nextos.core

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

/**
 * Created by ZhangXinmin on 2021/05/14.
 * Copyright (c) 5/14/21 . All rights reserved.
 */
open class BaseApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        context = this

        setStrictMode()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        lateinit var context: Application

        fun getApplicationContext(): Context {
            return context
        }
    }

    private fun setStrictMode() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)
    }

}