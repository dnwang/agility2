package org.pinwheel.agility2.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.pinwheel.agility2.action.Function1;
import org.pinwheel.agility2.view.ViewHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2016/7/1,16:12
 * @see
 */
public final class CommonTools {

    private CommonTools() {
        throw new AssertionError();
    }

    private static WeakReference<Application> appReference;

    @Nullable
    public static Application getApplication() {
        Application application = (null != appReference) ? appReference.get() : null;
        if (null == application) {
            try {
                application = FieldUtils.invokeStaticMethod(Class.forName("android.app.ActivityThread"),
                        "currentApplication");
                appReference = new WeakReference<>(application);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return application;
    }

    @UiThread
    @Nullable
    public static Activity getTopActivity() {
        Activity activity = null;
        try {
            // currentActivityThread must be run in uiThread
            final Object activityThread = FieldUtils.invokeStaticMethod(Class.forName("android.app.ActivityThread"),
                    "currentActivityThread");
            final Map activities = FieldUtils.getFieldValue(activityThread, "mActivities");
            for (Object activityRecord : activities.values()) {
                Activity topObj = FieldUtils.getFieldValue(activityRecord, "activity");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Boolean isTop = FieldUtils.invokeMethod(topObj, "isTopOfTask");
                    if (null != isTop && isTop) {
                        activity = topObj;
                        break;
                    }
                } else {
                    Boolean paused = FieldUtils.getFieldValue(activityRecord, "paused");
                    if (null != paused && !paused) {
                        activity = topObj;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activity;
    }

    public static Activity finishAllActivities() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map activities = (Map) activitiesField.get(activityThread);
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field activityField = activityRecordClass.getDeclaredField("activity");
                activityField.setAccessible(true);
                ((Activity) activityField.get(activityRecord)).finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getVersionName(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getVersionCode(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (Exception ignore) {
        }
    }

    public static View getWindowContentView(Window window) {
        // decor/frameLayout/(stub, frameLayout)/content
        ViewGroup group = (ViewGroup) ((ViewGroup) window.getDecorView()).getChildAt(0);
        int size = group.getChildCount();
        for (int i = 0; i < size; i++) {
            View v = group.getChildAt(i);
            if (v instanceof ViewGroup) {
                return ((ViewGroup) v).getChildAt(0);
            }
        }
        return null;
    }

    public static boolean isServiceRunning(Context context, String className) {
        boolean isRun = false;
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(40);
            final int size = serviceList.size();
            for (int i = 0; i < size; i++) {
                if (serviceList.get(i).service.getClassName().equals(className)) {
                    isRun = true;
                    break;
                }
            }
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        }
        return isRun;
    }

    public static void compressBitmap(final File inFile, final File outFile, final int maxSlide) {
        if (inFile == null || outFile == null || !inFile.exists() || 0 == maxSlide) {
            return;
        }
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(inFile.getAbsolutePath(), options);
            options.inSampleSize = getSampleSize(Math.max(options.outWidth, options.outHeight), maxSlide);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPurgeable = true;
            options.inInputShareable = true;
            bitmap = BitmapFactory.decodeFile(inFile.getAbsolutePath(), options);
            IOUtils.bitmap2Stream(new FileOutputStream(outFile), bitmap, Bitmap.CompressFormat.PNG, 100);
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
                System.gc();
            }
        }
    }

    private static int getSampleSize(int out, int max) {
        int sampleSize = 1;
        while (out / sampleSize > max) {
            sampleSize = sampleSize << 1;
        }
        return sampleSize;
    }

    public static ViewHolder getHolderBy(View view) {
        Object tag = view.getTag();
        ViewHolder holder = null;
        if (null == tag) {
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else if (tag instanceof ViewHolder) {
            holder = (ViewHolder) tag;
        }
        return holder;
    }

    public static boolean foreachViews(final View root, final Function1<Boolean, View> function1) {
        if (null == root || null == function1) {
            return false;
        }
        boolean isContinue = function1.call(root);
        if (isContinue) {
            if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                int size = group.getChildCount();
                for (int i = 0; i < size; i++) {
                    if (!foreachViews(group.getChildAt(i), function1)) {
                        break;
                    }
                }
            }
        }
        return isContinue;
    }

    public static Drawable getCompoundDrawables(Context context, int id) {
        if (id > 0) {
            Drawable drawable = context.getResources().getDrawable(id);
            if (null != drawable) {
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            }
            return drawable;
        }
        return null;
    }

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public static String getDeviceNetType(Context context) {
        String type = "";
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) {
            type = null;
        } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            type = "wifi";
        } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            int subType = info.getSubtype();
            if (subType == TelephonyManager.NETWORK_TYPE_CDMA || subType == TelephonyManager.NETWORK_TYPE_GPRS
                    || subType == TelephonyManager.NETWORK_TYPE_EDGE) {
                type = "2G";
            } else if (subType == TelephonyManager.NETWORK_TYPE_UMTS || subType == TelephonyManager.NETWORK_TYPE_HSDPA
                    || subType == TelephonyManager.NETWORK_TYPE_EVDO_A || subType == TelephonyManager.NETWORK_TYPE_EVDO_0
                    || subType == TelephonyManager.NETWORK_TYPE_EVDO_B) {
                type = "3G";
            } else if (subType == TelephonyManager.NETWORK_TYPE_LTE) {// LTE是3g到4g的过渡，是3.9G的全球标准
                type = "4G";
            }
        }
        return type;
    }

    public static String getIMEI(Context context) {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            return tm.getDeviceId();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getIMSI(Context context) {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            return tm.getSubscriberId();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getPhoneType(Context context) {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        switch (tm.getPhoneType()) {
            case TelephonyManager.PHONE_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.PHONE_TYPE_GSM:
                return "GSM";
            case TelephonyManager.PHONE_TYPE_SIP:
                return "SIP";
            default:
                return "";
        }
    }

    public static int obj2Int(Object obj, final int defaultValue) {
        try {
            return (int) Float.parseFloat(String.valueOf(obj));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double obj2Double(Object obj, final double defaultValue) {
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static float obj2Float(Object obj, final float defaultValue) {
        try {
            return Float.parseFloat(String.valueOf(obj));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean obj2Boolean(Object obj) {
        return Boolean.parseBoolean(String.valueOf(obj));
    }

    public static long obj2Long(Object obj, final long defaultValue) {
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean isSDExist() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static long readSDCardAvailableSize() {
        File path = Environment.getExternalStorageDirectory(); // 取得sdcard文件路径
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public static long readSystemAvailableSize() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File root = Environment.getRootDirectory();
            StatFs sf = new StatFs(root.getPath());
            long blockSize = sf.getBlockSize();
            long availCount = sf.getAvailableBlocks();
            return blockSize * availCount;
        }
        return 0;
    }

    /**
     * 获取字符串编码格式
     */
    @Nullable
    public static String getEncoding(String content) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        final String[] formatList = new String[]{
                "UTF-8", "GBK", "GB2312", "ISO-8859-1"
        };
        for (String encode : formatList) {
            try {
                if (content.equals(new String(content.getBytes(encode), encode))) {
                    return encode;
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public static String convertPairsToString(final Map<String, String> params, String encode) {
        if (null != params && !params.isEmpty()) {
            StringBuilder pairs = new StringBuilder();
            String key, value;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                if (!TextUtils.isEmpty(encode)) {
                    try {
                        value = URLEncoder.encode(value, encode);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                pairs.append("&").append(key).append("=").append(value);
            }
            return pairs.toString().substring(1);
        } else {
            return "";
        }
    }

    public static boolean checkPermissions(String... permissions) {
        boolean result = true;
        for (String permission : permissions) {
            result &= (0 == ContextCompat.checkSelfPermission(CommonTools.getApplication(), permission));
        }
        return result;
    }

    public static boolean isEmpty(Object obj) {
        if (null == obj) {
            return true;
        } else {
            if (obj instanceof Collection) {
                return ((Collection) obj).isEmpty();
            } else if (obj instanceof String) {
                String text = (String) obj;
                text = text.trim();
                return TextUtils.isEmpty(text) || "null".equalsIgnoreCase(text);
            } else if (obj instanceof Map) {
                return ((Map) obj).isEmpty();
            } else if (obj instanceof Bundle) {
                return ((Bundle) obj).isEmpty();
            } else {
                return false;
            }
        }
    }

    /**
     * 获取文本的真实长度，中文2；英文1
     */
    public static int getLength(String string) {
        int valueLength = 0;
        if (!isEmpty(string)) {
            String chinese = "[\u4e00-\u9fa5]";
            for (int i = 0; i < string.length(); i++) {
                String temp = string.substring(i, i + 1);
                if (temp.matches(chinese)) {
                    valueLength += 2;
                } else {
                    valueLength += 1;
                }
            }
        }
        return valueLength;
    }

    public static String urlEncode(String url) {
        if (!CommonTools.isEmpty(url)) {
            try {
                return URLEncoder.encode(url, "UTF-8");
            } catch (Exception ignore) {
            }
        }
        return url;
    }

    public static String urlDecode(String url) {
        if (!CommonTools.isEmpty(url)) {
            try {
                return URLDecoder.decode(url, "UTF-8");
            } catch (Exception ignore) {
            }
        }
        return url;
    }

    /**
     * 字符串标记颜色
     */
    public static CharSequence markText(@NonNull String raw, String match, @ColorInt int color) {
        match = (null == match) ? "" : match;
        final int start = raw.indexOf(match);
        SpannableStringBuilder ssb = new SpannableStringBuilder(raw);
        if (start >= 0) {
            ssb.setSpan(new ForegroundColorSpan(color), start, start + match.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }

}