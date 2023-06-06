package com.nextos.module.playermodule.base

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.nextos.module.playermodule.tools.Settings
import com.nextos.module.playermodule.util.DialogDelegate
import com.nextos.module.playermodule.util.IDialogManager

abstract class BasePlayerFragment: Fragment(), IDialogManager {
    lateinit var settings: SharedPreferences

    protected val dialogsDelegate = DialogDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialogsDelegate.observeDialogs(this, this)
//        Util.checkCpuCompatibility(this)

        settings = Settings.getInstance(this.requireContext())
        init()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    abstract fun getLayoutId():Int
    abstract fun init()
    abstract fun initView()
    abstract fun exit(resultCode: Int)

}