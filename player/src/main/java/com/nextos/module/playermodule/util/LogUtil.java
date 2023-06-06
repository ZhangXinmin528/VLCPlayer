package com.nextos.module.playermodule.util;

import android.util.Log;

import java.util.Map;

public class LogUtil {
    public static final String TAG = "MyVOD";
    private static final boolean DEBUG = true;
    public static void logMapOfStringString(Map<String, String> map) {
        Log.d(TAG, "==========Start MapOfStringString==========");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Log.d(TAG, "K: " + entry.getKey() + ", V: " + entry.getValue());
        }
        Log.d(TAG, "==========EndOf MapOfStringString==========");
    }

    public static void d(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    public static void i(String msg) {
        if (DEBUG) {
            Log.i(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (DEBUG) {
            Log.w(TAG, msg);
        }
    }

    public static void e(String msg) {
        if (DEBUG) {
            Log.e(TAG, msg);
        }
    }
}
