package com.nextos.core.utils

import android.text.TextUtils
import android.widget.Toast
import com.nextos.core.BaseApp
import com.nextos.lib.core.R

/**
 * Created by ZhangXinmin on 2020/7/26.
 * Copyright (c) 2020/11/10 . All rights reserved.
 */
class ToastUtil private constructor() {
    companion object {
        /**
         * Toast
         */
        fun showToast(msg: String?) {
            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(BaseApp.getApplicationContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Toast
         */
        fun showUnKnownError() {
            Toast.makeText(
                BaseApp.getApplicationContext(), BaseApp.getApplicationContext().getString(
                    R.string.all_exception_unknown
                ), Toast.LENGTH_SHORT
            ).show()
        }
    }
}