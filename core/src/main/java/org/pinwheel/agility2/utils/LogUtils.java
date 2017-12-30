package org.pinwheel.agility2.utils;

import android.util.Log;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved
 *
 * @author dnwang
 */
public final class LogUtils {

    private static final String TAG = ">>";

    private static boolean enable = false;

    private LogUtils() {
        throw new AssertionError();
    }

    public static boolean enable() {
        return enable;
    }

    public static void setEnable(boolean is) {
        enable = is;
    }

    public static void d(Object obj) {
        d(TAG, obj);
    }

    public static void d(String tag, Object obj) {
        if (enable) {
            String log = obj == null ? "" : obj.toString();
            Log.d(tag, log);
        }
    }

    public static void e(Object obj) {
        e(TAG, obj);
    }

    public static void e(String tag, Object obj) {
        if (enable) {
            String log = obj == null ? "" : obj.toString();
            Log.e(tag, log);
        }
    }

}
