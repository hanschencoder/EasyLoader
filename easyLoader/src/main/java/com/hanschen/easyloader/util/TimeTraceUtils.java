package com.hanschen.easyloader.util;

import com.hanschen.easyloader.log.EasyLoaderLog;

import java.util.Locale;

/**
 * Created by chenhang on 2016/10/26.
 */

public class TimeTraceUtils {

    private static final String TAG = "TimeTrace";

    private static ThreadLocal<Long> startTime = new ThreadLocal<>();

    public static void markStart() {
        startTime.set(System.currentTimeMillis());
    }

    public static void traceTime(String message) {
        long consumingTime = System.currentTimeMillis() - startTime.get();
        EasyLoaderLog.d(TAG, String.format(Locale.getDefault(), "%s used: %d", message, consumingTime));
        startTime.set(System.currentTimeMillis());
    }
}
