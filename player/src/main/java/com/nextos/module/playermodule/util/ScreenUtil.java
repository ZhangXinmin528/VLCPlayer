package com.nextos.module.playermodule.util;

import android.content.Context;

public class ScreenUtil {
    public static int dip2px(Context context, float dpValue) {
        try {
            final float scale = context.getApplicationContext().getResources().getDisplayMetrics().density;
            return (int) (dpValue * scale + 0.5f);
        } catch (NullPointerException e) {
            return 0;
        }
    }
}
