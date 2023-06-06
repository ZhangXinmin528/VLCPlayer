package com.nextos.core

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.nextos.lib.core.R
import com.nextos.utils.LanguageUtil
import com.nextos.utils.SPConfig
import com.zxm.utils.core.bar.StatusBarCompat
import com.zxm.utils.core.log.MLogger
import com.zxm.utils.core.sp.SharedPreferencesUtil

/**
 * Created by ZhangXinmin on 2020/7/19.
 * Copyright (c) 2020 . All rights reserved.
 * Base activity~
 */
abstract class BaseActivity : AppCompatActivity() {
    protected val sTAG: String = this.javaClass.simpleName

    protected lateinit var mContext: Context


    protected var mMode: Int = AppCompatDelegate.MODE_NIGHT_NO

    /**
     * Set content layout for the activity,if you will use the 'ViewBinding'.
     */
    abstract fun setContentLayout(): Any

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = setContentLayout()
        if (layout != null) {
            if (layout is Int) {
                setContentView(layout)
            } else if (layout is View) {
                setContentView(layout)
            } else {
                throw IllegalStateException("Content layout type is illegal!")
            }
        } else {
            throw NullPointerException("Content layout is null or empty!")
        }
        mContext = this

        LanguageUtil.setLanguageConfig(mContext!!)

        initParamsAndValues()

        mMode = SharedPreferencesUtil.get(
            mContext,
            SPConfig.CONFIG_APP_NIGHT_MODE,
            AppCompatDelegate.MODE_NIGHT_NO
        ) as Int
        updateStatusBar(mMode)

        initViews()
    }

    /**
     * 设置字体大小
     */
    override fun getResources(): Resources {
        val res = super.getResources()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            val config: Configuration = res.configuration
            config.fontScale =
                SharedPreferencesUtil.get(
                    this,
                    SPConfig.CONFIG_FONT_SCALE,
                    1.0f
                ) as Float
            res.updateConfiguration(config, res.displayMetrics)
        }
        return res
    }

    /**
     * 设置字体大小
     */
    override fun attachBaseContext(newBase: Context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            val res = newBase.resources
            val configuration = res.configuration

            configuration.let {

                it.fontScale = SharedPreferencesUtil.get(
                    newBase,
                    SPConfig.CONFIG_FONT_SCALE,
                    1.0f
                ) as Float

                val newContext = newBase.createConfigurationContext(it)
                super.attachBaseContext(newContext)
            }
        } else {
            super.attachBaseContext(newBase)
        }
    }

    abstract fun initParamsAndValues()

    abstract fun initViews()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        MLogger.d(sTAG, "onConfigurationChanged()~")
    }

    override fun onNightModeChanged(mode: Int) {
        super.onNightModeChanged(mode)
        mMode = mode
        updateStatusBar(mode)
    }

    protected fun updateStatusBar(mode: Int) {
        StatusBarCompat.setStatusBarLightMode(this, mode != AppCompatDelegate.MODE_NIGHT_YES)
    }

    //===============================status bar ==============================================//
    protected fun setStatusBarColorPrimary() {
        StatusBarCompat.setColorNoTranslucent(
            this,
            resources.getColor(R.color.color_toolbar_primary)
        )
    }

    protected fun setStatusBarColorLight() {
        StatusBarCompat.setColorNoTranslucent(this, resources.getColor(R.color.color_toolbar_light))
        StatusBarCompat.setStatusBarLightMode(this, true)
    }

    protected fun setStatusBarDark() {
        StatusBarCompat.setColor(this, resources.getColor(R.color.color_toolbar_dark))
        StatusBarCompat.setStatusBarLightMode(this, false)
    }

    protected fun initActionBar(toolbar: Toolbar, titile: String) {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.let {
            actionBar.title = titile
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }
    }

    //========================================页面跳转=========================================//
    protected fun jumpActivity(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    protected fun jumpActivity(clazz: Class<*>) {
        val intent = Intent(mContext, clazz)
        jumpActivity(intent)
    }

    protected fun jumpActivity(bundle: Bundle, clazz: Class<*>) {
        val intent = Intent(mContext, clazz)
        intent.putExtras(bundle)
        jumpActivity(intent)
    }

    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {
        super.overridePendingTransition(enterAnim, exitAnim)
    }

}