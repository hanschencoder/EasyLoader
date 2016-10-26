/*
 * Copyright (C) 2013 Square, Inc.
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
package com.hanschen.easyloader;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;

import com.hanschen.easyloader.log.EasyLoaderLog;
import com.hanschen.easyloader.util.Utils;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class AdjustableExecutorService extends ThreadPoolExecutor {

    private static final int DEFAULT_THREAD_COUNT = 3;

    AdjustableExecutorService() {
        super(DEFAULT_THREAD_COUNT, DEFAULT_THREAD_COUNT, 0, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(), new Utils.ThreadFactory());
    }

    void adjustThreadCount(NetworkInfo info) {
        setThreadCount(getThreadCount(info));
    }

    private void setThreadCount(int threadCount) {
        EasyLoaderLog.d("ThreadPoolExecutor", "setThreadCount: " + threadCount);
        if (Build.VERSION.SDK_INT > 23) {
            setMaximumPoolSize(threadCount);
            setCorePoolSize(threadCount);
        } else {
            setCorePoolSize(threadCount);
            setMaximumPoolSize(threadCount);
        }
    }

    private int getThreadCount(NetworkInfo info) {
        if (info == null || !info.isConnectedOrConnecting()) {
            return DEFAULT_THREAD_COUNT;
        }
        switch (info.getType()) {
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_ETHERNET:
                return 4;
            case ConnectivityManager.TYPE_MOBILE:
                switch (info.getSubtype()) {
                    case TelephonyManager.NETWORK_TYPE_LTE:  // 4G
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                        return 3;
                    case TelephonyManager.NETWORK_TYPE_UMTS: // 3G
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        return 2;
                    case TelephonyManager.NETWORK_TYPE_GPRS: // 2G
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        return 1;
                    default:
                        return DEFAULT_THREAD_COUNT;
                }
            default:
                return DEFAULT_THREAD_COUNT;
        }
    }

    @Override
    @NonNull
    public Future<?> submit(Runnable task) {
        PriorityFutureTask futureTask = new PriorityFutureTask((BitmapHunter) task);
        execute(futureTask);
        return futureTask;
    }

    private static final class PriorityFutureTask extends FutureTask<BitmapHunter> implements Comparable<PriorityFutureTask> {
        private final BitmapHunter hunter;

        PriorityFutureTask(BitmapHunter hunter) {
            super(hunter, null);
            this.hunter = hunter;
        }

        /**
         * 返回负数那么在队列里面的优先级比较高
         *
         * @param another 比较的对象
         * @return
         */
        @Override
        public int compareTo(PriorityFutureTask another) {

            Priority own = hunter.getPriority();
            Priority other = another.hunter.getPriority();

            if (own == other) {
                if (hunter.getLoader().getQueueProcessType() == QueueProcessType.FIFO) {
                    return hunter.sequence - another.hunter.sequence;
                } else {
                    return another.hunter.sequence - hunter.sequence;
                }
            } else {
                return other.ordinal() - own.ordinal();
            }
        }
    }
}
