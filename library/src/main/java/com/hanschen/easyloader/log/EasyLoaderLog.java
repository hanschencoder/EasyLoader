package com.hanschen.easyloader.log;

import android.util.Log;

/**
 * Created by Hans.Chen on 2016/8/1.
 */
public class EasyLoaderLog {

    private static boolean logEnable = true;
    private static Logger  logger    = new Logger() {
        @Override
        public void v(String tag, String msg) {
            Log.v(tag, msg);
        }

        @Override
        public void d(String tag, String msg) {
            Log.d(tag, msg);
        }

        @Override
        public void i(String tag, String msg) {
            Log.i(tag, msg);
        }

        @Override
        public void w(String tag, String msg) {
            Log.w(tag, msg);
        }

        @Override
        public void e(String tag, String msg) {
            Log.e(tag, msg);
        }

        @Override
        public void e(String tag, String msg, Throwable t) {
            Log.e(tag, msg, t);
        }

        @Override
        public void wtf(String tag, String msg) {
            Log.wtf(tag, msg);
        }
    };

    public static void logger(Logger logger) {
        EasyLoaderLog.logger = logger;
    }

    public static void logEnable(boolean logEnable) {
        EasyLoaderLog.logEnable = logEnable;
    }

    public static void v(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        if (!logEnable) {
            return;
        }
        logger.e(tag, msg, t);
    }

    public static void wtf(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.wtf(tag, msg);
    }
}
