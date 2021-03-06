package org.pinwheel.agility2.utils;

import android.content.Context;
import android.content.res.Resources;

public final class UIUtils {

    private UIUtils() {

    }

    public static int dip2px(float dpValue) {
        return dip2px(CommonTools.getApplication(), dpValue);
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(float pxValue) {
        return px2dip(CommonTools.getApplication(), pxValue);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int px2sp(float pxValue) {
        return px2sp(CommonTools.getApplication(), pxValue);
    }

    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    public static int sp2px(float spValue) {
        return sp2px(CommonTools.getApplication(), spValue);
    }

    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static int getDisplayWidth() {
        return CommonTools.getApplication().getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDisplayWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDisplayHeight() {
        return CommonTools.getApplication().getResources().getDisplayMetrics().heightPixels;
    }

    public static int getDisplayHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int getStatusBarHeight() {
        Resources resources = Resources.getSystem();
        return resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"));
    }

}