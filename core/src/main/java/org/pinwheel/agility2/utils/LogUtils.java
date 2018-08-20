package org.pinwheel.agility2.utils;

import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    private static boolean isSystemOut = false;

    private LogUtils() {
        throw new AssertionError();
    }

    public static boolean enable() {
        return enable;
    }

    public static void setEnableCrashCaught(File outPath) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(outPath));
    }

    public static void setEnable(boolean is) {
        enable = is;
    }

    public static void isSystemOut(boolean is) {
        isSystemOut = is;
    }

    public static void d(Object obj) {
        d(TAG, obj);
    }

    public static void d(String tag, Object obj) {
        if (enable) {
            String log = obj == null ? "" : obj.toString();
            if (isSystemOut) {
                if (CommonTools.isEmpty(tag)) {
                    System.out.println(log);
                } else {
                    System.out.println(tag + ": " + log);
                }
            } else {
                Log.d(tag, log);
            }
        }
    }

    public static void e(Object obj) {
        e(TAG, obj);
    }

    public static void e(String tag, Object obj) {
        if (enable) {
            String log = obj == null ? "" : obj.toString();
            if (isSystemOut) {
                if (CommonTools.isEmpty(tag)) {
                    System.out.println(log);
                } else {
                    System.out.println(tag + ": " + log);
                }
            } else {
                Log.e(tag, log);
            }
        }
    }

    public final static class CrashHandler implements Thread.UncaughtExceptionHandler {

        static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.PRC);

        File outPath;

        CrashHandler(File path) {
            outPath = path;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            try {
                saveCrashInfoFile(throwable);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                CommonTools.finishAllActivities();
                SystemClock.sleep(300);
                Process.killProcess(Process.myPid());
                System.exit(0);
            }
        }

        private void saveCrashInfoFile(Throwable ex) throws Exception {
            final StringBuffer sb = new StringBuffer();
            StringWriter writer = null;
            try {
                writer = new StringWriter();
                PrintWriter printWriter = new PrintWriter(writer);
                ex.printStackTrace(printWriter);
                Throwable cause = ex.getCause();
                while (cause != null) {
                    cause.printStackTrace(printWriter);
                    cause = cause.getCause();
                }
                printWriter.flush();
                printWriter.close();
                String result = writer.toString();
                sb.append(result);
            } catch (Exception e) {
                sb.append("an error occured while writing file...\r\n").append(e.getMessage());
            } finally {
                IOUtils.close(writer);
                writeFile(sb.toString());
            }
        }

        private void writeFile(String log) throws Exception {
            final String fileName = "crash-" + FORMATTER.format(new Date()) + ".log";
            final File file = new File(outPath, fileName);
            FileUtils.prepareDirs(file);
            IOUtils.string2Stream(new FileOutputStream(file), log);
        }

    }

}