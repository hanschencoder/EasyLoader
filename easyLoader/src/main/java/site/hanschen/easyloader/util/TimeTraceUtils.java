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
package site.hanschen.easyloader.util;

import site.hanschen.easyloader.log.EasyLoaderLog;

import java.util.Locale;

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
