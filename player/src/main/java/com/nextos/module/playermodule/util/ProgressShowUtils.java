package com.nextos.module.playermodule.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.SeekBar;

import com.nextos.module.playermodule.view.AccessibleSeekBar;


public class ProgressShowUtils {

    private static final String TAG = "ProgressShowUtils";

    public static int getProgressDimen(Context context, SeekBar seekBar, Long length, int marginLeft) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Log.d(TAG, "getProgressDimen: --seekBar.getMin()--" + seekBar.getMin() + "---seekBar.getMax()---" + seekBar.getMax() + "---seekBar.getProgress()---" + seekBar.getProgress() + "");
        }
        int available = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int thumbPos = (int) (getScale(seekBar.getMin(), seekBar.getMax(), seekBar.getProgress()) * available + 0.5f + marginLeft);
//            Log.d(TAG, "getProgressDimen:--thumbPos " + thumbPos);
            return thumbPos;
        }
//        Log.d("ProgressShowUtils", "getProgressDimen: --offset" + offset + "---time---" + time);
        return px2dip(context, new Float((1.0 * seekBar.getProgress()/seekBar.getMax() * length) * (getScreenWidth(context) - dip2px(context, 0 * 2)))
                + marginLeft);
//        return px2dip(context, new Float((1.0*time/length)*(getScreenWidth(context)-dip2px(context,32*2)))
//                +marginLeft);
    }

    private static float getScale(int min, int max, int progress) {
        int range = max - min;
        return range > 0 ? (progress - min) / (float) range : 0;
    }

//    public static Boolean getLengthShow(AccessibleSeekBar seekBar, View time, View length) {
//        int timeWidth = getProgressDimen(seekBar.getContext(),seekBar,0l,0l,0);
////        Log.d(TAG, "getLengthShow: ---time.getWidth()---time.getWidth()" + time.getWidth() + "---length.getWidth()---" + length.getWidth() + "---dip2px(time.getContext(),32)---" + dip2px(time.getContext(), 32) + "---getScreenWidth(time.getContext()---" + getScreenWidth(time.getContext()));
//        if (timeWidth + dip2px(time.getContext(), 90f)> getScreenWidth(time.getContext())) {
//            return false;
//        }
//
//        return true;
//    }

    /**
     * dip to px
     *
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
        return (int) (0.5f + dpValue * context.getApplicationContext()
                .getResources().getDisplayMetrics().density);
    }

    /**
     * getScreenWidth
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        return localDisplayMetrics.widthPixels;
    }

    public static int px2dip(Context context, float pxValue) {

        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((pxValue - 0.5f) / scale + 0.5f);
    }
}
