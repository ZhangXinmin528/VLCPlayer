package com.nextos.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import com.zxm.utils.core.bar.StatusBarCompat

/**
 * Created by ZhangXinmin on 2020/7/19.
 * Copyright (c) 2020 . All rights reserved.
 */
abstract class BaseFragment() : Fragment() {

    protected val sTAG: String = this.javaClass.simpleName

    protected lateinit var mContext: Context

    abstract fun setContentLayout(container: ViewGroup?): View

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return setContentLayout(container)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initParamsAndValues()

        initViews()
    }

    fun setStatusbarColor(@ColorRes colorRes: Int) {
        activity?.let {
            StatusBarCompat.setColor(
                activity,
                resources.getColor(colorRes),
                0
            )
        }
    }

    abstract fun initParamsAndValues();

    abstract fun initViews()
}