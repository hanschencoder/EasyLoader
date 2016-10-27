/*
 * Copyright 2016 Hans Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hanschen.easyloader.log;

import android.util.Log;

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

    private static String getTag(String tag) {
        return "EasyLoader-" + tag;
    }

    public static void v(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.v(getTag(tag), msg);
    }

    public static void d(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.d(getTag(tag), msg);
    }

    public static void i(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.i(getTag(tag), msg);
    }

    public static void w(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.w(getTag(tag), msg);
    }

    public static void e(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.e(getTag(tag), msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        if (!logEnable) {
            return;
        }
        logger.e(getTag(tag), msg, t);
    }

    public static void wtf(String tag, String msg) {
        if (!logEnable) {
            return;
        }
        logger.wtf(getTag(tag), msg);
    }
}
