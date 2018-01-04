package org.pinwheel.agility2.utils;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2016/7/1,16:12
 */
public final class FormatUtils {

    private FormatUtils() {
        throw new AssertionError();
    }

    private static final DateFormat FORMAT_YESTERDAY = new SimpleDateFormat("昨天 HH:mm", Locale.PRC);
    private static final DateFormat FORMAT_BEFORE_YESTERDAY = new SimpleDateFormat("前天 HH:mm", Locale.PRC);
    private static final DateFormat FORMAT_CURRENT_YEAR = new SimpleDateFormat("MM-dd HH:mm", Locale.PRC);
    private static final DateFormat FORMAT_OTHER_YEAR = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.PRC);

    public static String simplifyDate(String targetTime) {
        return simplifyDate(parseDate(targetTime));
    }

    public static String simplifyDate(long targetTime) {
        return simplifyDate(new Date(targetTime));
    }

    public static String simplifyDate(Date targetTime) {
        if (null == targetTime) {
            return "";
        }
        final long dTime = (System.currentTimeMillis() - targetTime.getTime()) / 1000;
        if (dTime <= 10 * 60) {
            return "now";
        } else if (dTime <= 60 * 60) {
            return (dTime / 60) + "minutes ago";
        } else if (dTime <= 24 * 60 * 60) {
            return (dTime / 3600) + "hours ago";
        }
        final Calendar currCalendar = Calendar.getInstance();
        final Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.setTime(targetTime);
        int dYear = currCalendar.get(Calendar.YEAR) - targetCalendar.get(Calendar.YEAR);
        if (dYear == 0) {
            int dday = currCalendar.get(Calendar.DAY_OF_YEAR) - targetCalendar.get(Calendar.DAY_OF_YEAR);
            if (dday == 1) {
                return FORMAT_YESTERDAY.format(targetTime);
            } else if (dday == 2) {
                return FORMAT_BEFORE_YESTERDAY.format(targetTime);
            }
            return FORMAT_CURRENT_YEAR.format(targetTime);
        } else if (dYear == 1 && currCalendar.get(Calendar.MONTH) == 0
                && targetCalendar.get(Calendar.MONTH) == 11) {
            currCalendar.add(Calendar.DATE, -1);
            if (currCalendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)) {
                return FORMAT_YESTERDAY.format(targetTime);
            }
            currCalendar.add(Calendar.DATE, -1);
            if (currCalendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)) {
                return FORMAT_BEFORE_YESTERDAY.format(targetTime);
            }
            return FORMAT_OTHER_YEAR.format(targetTime);
        } else {
            return FORMAT_OTHER_YEAR.format(targetTime);
        }
    }

    public static String formatDate(Date targetTime) {
        if (null == targetTime) {
            return "";
        } else {
            return FORMAT_OTHER_YEAR.format(targetTime);
        }
    }

    public static Date parseDate(String date) {
        try {
            return FORMAT_OTHER_YEAR.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String simplifyFileSize(final long size) {
        if (size > 1073741824) {
            return String.format(Locale.PRC, "%.2f", size / 1073741824.0) + " GB";
        } else if (size > 1048576) {
            return String.format(Locale.PRC, "%.2f", size / 1048576.0) + " MB";
        } else if (size > 1024) {
            return String.format(Locale.PRC, "%.2f", size / 1024.0) + " KB";
        } else {
            return size + " B";
        }
    }

    public static String simplifyPlayTime(long duration) {
        String minuteStr;
        int minute = (int) (duration / 60000);
        if (minute < 10) {
            minuteStr = "0" + minute;
        } else {
            minuteStr = "" + minute;
        }
        String secondStr;
        int second = (int) ((duration % 60000) / 1000);
        if (second < 10) {
            secondStr = "0" + second;
        } else {
            secondStr = "" + second;
        }
        return minuteStr + ":" + secondStr;
    }

    public static String simplifyCount(long number) {
        DecimalFormat DISTANCE_DECIMAL_FORMAT = new DecimalFormat("0.0");
        new DecimalFormat("0.0").setRoundingMode(RoundingMode.DOWN);
        if (number < 0) {
            number = 0;
        }
        if (number < 1000) {
            return number + "";
        } else if (number < 10000) {
            final float f = number / 1000.0f;
            boolean isDecimals = (f - (int) f) != 0;
            if (isDecimals) {
                return DISTANCE_DECIMAL_FORMAT.format(f) + "k+";
            } else
                return ((int) f) + "k+";
        } else {
            final float f = number / 10000.0f;
            boolean isDecimals = (f - (int) f) != 0;
            if (isDecimals) {
                return DISTANCE_DECIMAL_FORMAT.format(f) + "w+";
            } else
                return ((int) f) + "w+";
        }
    }

}