package com.hanschen.easyloader.util;

import android.os.Looper;

/**
 * Created by Hans.Chen on 2016/8/1.
 */
public class ThreadChecker {

    public static void checkNotMain() {
        if (isMain()) {
            throw new IllegalStateException("Method call should not happen from the main thread.");
        }
    }

    public static void checkMain() {
        if (!isMain()) {
            throw new IllegalStateException("Method call should happen from the main thread.");
        }
    }

    public static boolean isMain() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
